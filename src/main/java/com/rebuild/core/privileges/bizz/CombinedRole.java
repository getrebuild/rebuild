/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges.bizz;

import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.EntityPrivileges;
import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.security.Principal;
import java.util.*;

/**
 * 角色（权限合并）
 *
 * @author Zixin (RB)
 * @since 09/16/2018
 */
public class CombinedRole extends Role {
    private static final long serialVersionUID = 2668222642910366512L;

    /**
     * 权限掩码
     */
    private static final Integer[] PERMISSION_MASKS = new Integer[]{
            BizzPermission.CREATE.getMask(),
            BizzPermission.DELETE.getMask(),
            BizzPermission.UPDATE.getMask(),
            BizzPermission.READ.getMask(),
            BizzPermission.ASSIGN.getMask(),
            BizzPermission.SHARE.getMask()
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
     * 合并主角色与附加角色权限（向上合并）
     */
    protected void mergePrivileges() {
        for (Privileges priv : roleMain.getAllPrivileges()) {
            addPrivileges(priv);
        }

        for (Role ra : roleAppends) {
            for (Privileges priv : ra.getAllPrivileges()) {
                Privileges mp = mergePrivileges(priv, getPrivileges(priv.getIdentity()));
                addPrivileges(mp);
            }
        }

        // DEBUG
//        for (Privileges priv : getAllPrivileges()) {
//            System.out.println();
//            System.out.println("Combined Privileges : " + priv.getIdentity());
//            System.out.println("M " + getDefinition(roleMain.getPrivileges(priv.getIdentity())));
//            for (Role ra : roleAppends) {
//                System.out.println("A " + getDefinition(ra.getPrivileges(priv.getIdentity())));
//            }
//            System.out.println("--");
//            System.out.println("T " + getDefinition(getPrivileges(priv.getIdentity())));
//        }
//        System.out.println();
    }

    private Privileges mergePrivileges(Privileges a, Privileges b) {
        if (b == null || b == Privileges.NONE) return a;
        if (b == Privileges.ROOT) return b;

        // Only one key
        if (a instanceof ZeroPrivileges) {
            JSONObject aDefMap = JSON.parseObject(getDefinition(a));
            if (aDefMap.getIntValue(ZeroPrivileges.ZERO_FLAG) == ZeroPrivileges.ZERO_MASK) {
                return a;
            } else {
                return b;
            }
        }

        Map<String, Integer> aDefMap = parseDefinitionMasks(getDefinition(a));
        Map<String, Integer> bDefMap = parseDefinitionMasks(getDefinition(b));

        Map<String, Integer> defMap = new LinkedHashMap<>();

        for (String key : aDefMap.keySet().toArray(new String[0])) {
            Integer aMask = aDefMap.remove(key);
            Integer bMask = bDefMap.remove(key);
            defMap.put(key, mergeMaskValue(aMask, bMask));
        }

        defMap.putAll(bDefMap);

        List<String> defs = new ArrayList<>();
        for (Map.Entry<String, Integer> e : defMap.entrySet()) {
            defs.add(e.getKey() + ":" + e.getValue());
        }

        String definition = StringUtils.join(defs.iterator(), ",");
        return new EntityPrivileges(((EntityPrivileges) a).getEntity(), definition);
    }

    private Map<String, Integer> parseDefinitionMasks(String d) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String s : d.split(",")) {
            String[] ss = s.split(":");
            map.put(ss[0], Integer.valueOf(ss[1]));
        }
        return map;
    }

    private int mergeMaskValue(Integer a, Integer b) {
        if (a == null || a == 0) return b;
        if (b == null || b == 0) return a;

        Set<Integer> masks = new HashSet<>();
        for (Integer mask : PERMISSION_MASKS) {
            if ((a & mask) != 0) masks.add(mask);
        }
        for (Integer mask : PERMISSION_MASKS) {
            if ((b & mask) != 0) masks.add(mask);
        }

        int maskValue = 0;
        for (Integer mask : masks) {
            maskValue += mask;
        }
        return maskValue;
    }

    private String getDefinition(Privileges priv) {
        if (priv instanceof ZeroPrivileges) return ((ZeroPrivileges) priv).getDefinition();
        else if (priv instanceof EntityPrivileges) return ((EntityPrivileges) priv).getDefinition();
        else return null;
    }
}
