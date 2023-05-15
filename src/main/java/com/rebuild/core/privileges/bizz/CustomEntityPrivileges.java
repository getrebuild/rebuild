/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges.bizz;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.EntityPrivileges;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.service.query.ParseHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 2021/11/15
 */
public class CustomEntityPrivileges extends EntityPrivileges {
    private static final long serialVersionUID = 2658045031880710476L;

    protected static final Permission[] PERMISSION_DEFS = new Permission[] {
            BizzPermission.CREATE,
            BizzPermission.READ,
            BizzPermission.UPDATE,
            BizzPermission.DELETE,
            BizzPermission.ASSIGN,
            BizzPermission.SHARE
    };

    // 自定义权限 <Action, Filter>
    private final Map<String, JSON> customFilters = new HashMap<>();

    /**
     * @param entity
     * @param rawDefinition
     */
    public CustomEntityPrivileges(Integer entity, JSONObject rawDefinition) {
        super(entity, convertEntityPrivilegesDefinition(rawDefinition));

        for (Permission p : PERMISSION_DEFS) {
            String actionKey = p.getName() + "9";
            if (rawDefinition.containsKey(actionKey)) {
                JSONObject advFilter = rawDefinition.getJSONObject(actionKey);
                if (ParseHelper.validAdvFilter(advFilter)) {
                    customFilters.put(p.getName(), advFilter);
                }
            }
        }
    }

    /**
     * @param entity
     * @param definition
     * @param customFilters
     */
    protected CustomEntityPrivileges(Integer entity, String definition, Map<String, JSON> customFilters) {
        super(entity, definition);
        this.customFilters.clear();
        this.customFilters.putAll(customFilters);
    }

    /**
     * 转换成能识别的权限定义
     *
     * @param definition
     * @return
     */
    static String convertEntityPrivilegesDefinition(JSONObject definition) {
        int C = definition.getIntValue("C");
        int D = definition.getIntValue("D");
        int U = definition.getIntValue("U");
        int R = definition.getIntValue("R");
        int A = definition.getIntValue("A");
        int S = definition.getIntValue("S");

        int deepP = 0;
        int deepL = 0;
        int deepD = 0;
        int deepG = 0;

        // {"A":0,"R":1,"C":4,"S":0,"D":0,"U":0} >> 1:9,2:1,3:1,4:1

        if (C >= 4) {
            deepP += BizzPermission.CREATE.getMask();
            deepL += BizzPermission.CREATE.getMask();
            deepD += BizzPermission.CREATE.getMask();
            deepG += BizzPermission.CREATE.getMask();
        }

        if (D >= 1) {
            deepP += BizzPermission.DELETE.getMask();
        }
        if (D >= 2) {
            deepL += BizzPermission.DELETE.getMask();
        }
        if (D >= 3) {
            deepD += BizzPermission.DELETE.getMask();
        }
        if (D >= 4) {
            deepG += BizzPermission.DELETE.getMask();
        }

        if (U >= 1) {
            deepP += BizzPermission.UPDATE.getMask();
        }
        if (U >= 2) {
            deepL += BizzPermission.UPDATE.getMask();
        }
        if (U >= 3) {
            deepD += BizzPermission.UPDATE.getMask();
        }
        if (U >= 4) {
            deepG += BizzPermission.UPDATE.getMask();
        }

        if (R >= 1) {
            deepP += BizzPermission.READ.getMask();
        }
        if (R >= 2) {
            deepL += BizzPermission.READ.getMask();
        }
        if (R >= 3) {
            deepD += BizzPermission.READ.getMask();
        }
        if (R >= 4) {
            deepG += BizzPermission.READ.getMask();
        }

        if (A >= 1) {
            deepP += BizzPermission.ASSIGN.getMask();
        }
        if (A >= 2) {
            deepL += BizzPermission.ASSIGN.getMask();
        }
        if (A >= 3) {
            deepD += BizzPermission.ASSIGN.getMask();
        }
        if (A >= 4) {
            deepG += BizzPermission.ASSIGN.getMask();
        }

        if (S >= 1) {
            deepP += BizzPermission.SHARE.getMask();
        }
        if (S >= 2) {
            deepL += BizzPermission.SHARE.getMask();
        }
        if (S >= 3) {
            deepD += BizzPermission.SHARE.getMask();
        }
        if (S >= 4) {
            deepG += BizzPermission.SHARE.getMask();
        }

        return "1:" + deepP + ",2:" + deepL + ",3:" + deepD + ",4:" + deepG;
    }

    /**
     * @return
     */
    protected Map<String, JSON> getCustomFilters() {
        return customFilters;
    }

    /**
     * 获取自定义权限
     *
     * @param action
     * @return
     */
    public JSONObject getCustomFilter(Permission action) {
        return (JSONObject) customFilters.getOrDefault(action.getName(), null);
    }

    @Override
    public String toString() {
        return getDefinition() + ";" + getCustomFilters();
    }
}
