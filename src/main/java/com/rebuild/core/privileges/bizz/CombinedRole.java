/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges.bizz;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.bizz.privileges.impl.BizzDepthEntry;
import cn.devezhao.bizz.security.EntityPrivileges;
import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 角色（权限合并）
 *
 * @author Zixin (RB)
 * @since 09/16/2018
 */
@Slf4j
public class CombinedRole extends Role {
    private static final long serialVersionUID = 2668222642910366512L;

    /**
     * 权限深度
     */
    private static final String[] PERMISSION_DEPTHS = new String[]{
            String.valueOf(BizzDepthEntry.PRIVATE.getMask()),
            String.valueOf(BizzDepthEntry.LOCAL.getMask()),
            String.valueOf(BizzDepthEntry.DEEPDOWN.getMask()),
            String.valueOf(BizzDepthEntry.GLOBAL.getMask()),
    };

    final private Role roleMain;
    final private Set<Role> roleAppends;

    /**
     * @param user
     * @param appends
     */
    public CombinedRole(User user, Set<Role> appends) {
        super(user.getMainRole().getIdentity(), user.getMainRole().getName(), user.getMainRole().isDisabled());
        this.roleMain = user.getMainRole();
        this.roleAppends = appends;

        this.mergePrivileges();
        user.setCombinedRole(this);
    }

    @Override
    public boolean addMember(Principal user) {
        return roleMain.addMember(user);
    }

    @Override
    public boolean removeMember(Principal user) {
        return roleMain.removeMember(user);
    }

    @Override
    public boolean isMember(Principal user) {
        return roleMain.isMember(user);
    }

    @Override
    public boolean isMember(Serializable identity) {
        return roleMain.isMember(identity);
    }

    @Override
    public Set<Principal> getMembers() {
        return roleMain.getMembers();
    }

    @SuppressWarnings("deprecation")
    @Override
    public Enumeration<? extends Principal> members() {
        return roleMain.members();
    }

    /**
     * 获取附加角色
     *
     * @return
     */
    public Set<ID> getRoleAppends() {
        Set<ID> set = new HashSet<>();
        for (Role r : roleAppends) set.add((ID) r.getIdentity());
        return Collections.unmodifiableSet(set);
    }

    /**
     * 合并主角色与附加角色权限。规则：
     * 1.高权限优先
     * 2.对于同权限（如读取本部门）且都设置了自定义权限的，由于无法区分哪个权限高，会采用主权限的 v4.3
     */
    protected void mergePrivileges() {
        for (Privileges p : roleMain.getAllPrivileges()) {
            addPrivileges(p);
        }

        for (Role ra : roleAppends) {
            for (Privileges p : ra.getAllPrivileges()) {
                Privileges mp = mergePrivileges(p, getPrivileges(p.getIdentity()));
                addPrivileges(mp);
            }
        }

        if (log.isDebugEnabled()) {
            for (Privileges p : getAllPrivileges()) {
                if (p instanceof ZeroPrivileges) continue;
                System.out.println();
                System.out.println("Combined Privileges : " + p.getIdentity());
                System.out.println("MAIN. " + descPrivileges(roleMain.getPrivileges(p.getIdentity())));
                for (Role ra : roleAppends) {
                    System.out.println("APPE. " + descPrivileges(ra.getPrivileges(p.getIdentity())));
                }
                System.out.println("--");
                System.out.println("MERG. " + descPrivileges(getPrivileges(p.getIdentity())));
            }
            System.out.println();
        }
    }

    private String descPrivileges(Privileges p) {
        String d = p.getIdentity().toString();
        if (p instanceof CustomEntityPrivileges) {
            d += "; FP:" + ObjectUtils.defaultIfNull(((CustomEntityPrivileges) p).getFpDefinition(), "N");
            d += "; CP:" + ObjectUtils.defaultIfNull(((CustomEntityPrivileges) p).getCustomFilters(), "N");
        }
        return d;
    }

    private Privileges mergePrivileges(Privileges a, Privileges bHigher) {
        if (a == null || a == Privileges.NONE) return bHigher;
        if (a == Privileges.ROOT) return a;
        if (bHigher == null || bHigher == Privileges.NONE) return a;
        if (bHigher == Privileges.ROOT) return bHigher;

        // 扩展权限

        if (a instanceof ZeroPrivileges) {
            JSONObject aDefMap = JSON.parseObject(getDefinition(a));
            // 部分权限要取反
            if (a.getIdentity().equals(ZeroEntry.EnableBizzPart)) {
                return aDefMap.getIntValue(ZeroPrivileges.ZERO_FLAG) == ZeroPrivileges.ZERO_MASK ? bHigher : a;
            }

            return aDefMap.getIntValue(ZeroPrivileges.ZERO_FLAG) == ZeroPrivileges.ZERO_MASK ? a : bHigher;
        }

        // 实体权限

        Map<String, Integer> aDefMap = parseDefinitionMasks(getDefinition(a));
        Map<String, Integer> bHigherDefMap = parseDefinitionMasks(getDefinition(bHigher));

        Map<String, Integer> mergedDefMap = new LinkedHashMap<>();
        for (String depth : PERMISSION_DEPTHS) {
            mergedDefMap.put(depth, mergeMaskValue(aDefMap.get(depth), bHigherDefMap.get(depth)));
        }

        List<String> mergedDefs = new ArrayList<>();
        for (Map.Entry<String, Integer> e : mergedDefMap.entrySet()) {
            mergedDefs.add(e.getKey() + ":" + e.getValue());
        }

        // 实体自定义权限

        Map<String, JSON> useCustomFilters = new HashMap<>();

        for (Permission action : CustomEntityPrivileges.PERMISSION_DEFS) {
            JSONObject aCustom = ((CustomEntityPrivileges) a).getCustomFilter(action);
            JSONObject bHigherCustom = ((CustomEntityPrivileges) bHigher).getCustomFilter(action);
            if (aCustom == null && bHigherCustom == null) continue;

            int gt = isGreaterThan(action.getMask(), aDefMap, bHigherDefMap);
            // a > bHigher
            if (gt == 1) {
                if (aCustom != null) useCustomFilters.put(action.getName(), aCustom);
            }
            // a < bHigher
            else if (gt == 2) {
                if (bHigherCustom != null) useCustomFilters.put(action.getName(), bHigherCustom);
            }
            // a == bHigher
            else {
                if (aCustom == null || bHigherCustom == null) {
                    // fix:4.2.2 相等权限时任意一个为空，则无自定义权限
                } else {
                    useCustomFilters.put(action.getName(), bHigherCustom);
                }
            }
        }

        // 字段权限

        Map<String, Object> aFpDefinition = ((CustomEntityPrivileges) a).getFpDefinition();
        Map<String, Object> bHigherFpDefinition = ((CustomEntityPrivileges) bHigher).getFpDefinition();
        // be:4.1.5 无法比较字段权限高低，因此随便选一个非空的更合理
        // be:4.3 主角色优先
        Map<String, Object> useFpDefinition = null;
        if (MapUtils.isEmpty(aFpDefinition)) useFpDefinition = bHigherFpDefinition;
        else if (MapUtils.isEmpty(bHigherFpDefinition)) useFpDefinition = aFpDefinition;
        else if (MapUtils.isNotEmpty(bHigherFpDefinition)) useFpDefinition = bHigherFpDefinition;

        String definition = StringUtils.join(mergedDefs.iterator(), ",");
        return new CustomEntityPrivileges(((EntityPrivileges) a).getEntity(), definition, useCustomFilters, useFpDefinition);
    }

    private Map<String, Integer> parseDefinitionMasks(String d) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String s : d.split(",")) {
            String[] ss = s.split(":");
            map.put(ss[0], Integer.valueOf(ss[1]));
        }
        return map;
    }

    private String getDefinition(Privileges priv) {
        if (priv instanceof ZeroPrivileges) return ((ZeroPrivileges) priv).getDefinition();
        if (priv instanceof EntityPrivileges) return ((EntityPrivileges) priv).getDefinition();
        throw new PrivilegesException("Unknow privileges class : " + priv);
    }

    private int mergeMaskValue(Integer a, Integer b) {
        if (a == null || a == 0) return b == null ? 0 : b;
        if (b == null || b == 0) return a;

        Set<Integer> masks = new HashSet<>();

        for (Permission action : CustomEntityPrivileges.PERMISSION_DEFS) {
            int mask = action.getMask();
            if ((a & mask) != 0) masks.add(mask);
        }
        for (Permission action : CustomEntityPrivileges.PERMISSION_DEFS) {
            int mask = action.getMask();
            if ((b & mask) != 0) masks.add(mask);
        }

        int maskValue = 0;
        for (Integer mask : masks) {
            maskValue += mask;
        }
        return maskValue;
    }

    // 指定动作是否同一权限深度 0:a=b, 1:a>b, 2:a<b
    private int isGreaterThan(int mask, Map<String, Integer> aDefMap, Map<String, Integer> bDefMap) {
        int a4 = aDefMap.get(PERMISSION_DEPTHS[3]);
        int b4 = bDefMap.get(PERMISSION_DEPTHS[3]);
        if ((a4 & mask) != 0 && (b4 & mask) != 0) return 0;
        if ((a4 & mask) != 0) return 1;
        if ((b4 & mask) != 0) return 2;

        int a3 = aDefMap.get(PERMISSION_DEPTHS[2]);
        int b3 = bDefMap.get(PERMISSION_DEPTHS[2]);
        if ((a3 & mask) != 0 && (b3 & mask) != 0) return 0;
        if ((a3 & mask) != 0) return 1;
        if ((b3 & mask) != 0) return 2;

        int a2 = aDefMap.get(PERMISSION_DEPTHS[1]);
        int b2 = bDefMap.get(PERMISSION_DEPTHS[1]);
        if ((a2 & mask) != 0 && (b2 & mask) != 0) return 0;
        if ((a2 & mask) != 0) return 1;
        if ((b2 & mask) != 0) return 2;

        int a1 = aDefMap.get(PERMISSION_DEPTHS[0]);
        int b1 = bDefMap.get(PERMISSION_DEPTHS[0]);
        if ((a1 & mask) != 0 && (b1 & mask) != 0) return 0;
        if ((a1 & mask) != 0) return 1;
        if ((b1 & mask) != 0) return 2;

        return 0;
    }
}
