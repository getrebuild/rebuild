/*
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao
 * @since 2021/11/15
 */
public class CustomEntityPrivileges extends EntityPrivileges {

    private Map<String, JSON> customAdvFilters = new HashMap<>();

    /**
     * @param entity
     * @param definition
     * @param rawDefinition
     */
    public CustomEntityPrivileges(Integer entity, String definition, JSONObject rawDefinition) {
        super(entity, definition);

        Permission[] bps = new Permission[] {
                BizzPermission.READ,
                BizzPermission.UPDATE,
                BizzPermission.DELETE,
                BizzPermission.ASSIGN,
                BizzPermission.SHARE
        };
        for (Permission bp : bps) {
            String filterKey = bp.getName() + "9";
            if (rawDefinition.containsKey(filterKey)) {
                JSONObject advFilter = rawDefinition.getJSONObject(filterKey);
                if (ParseHelper.validAdvFilter(advFilter)) {
                    customAdvFilters.put(bp.getName(), advFilter);
                }
            }
        }
    }

    @Override
    public boolean allowed(Permission action, Serializable targetGuard) {
        return super.allowed(action, targetGuard);
    }

    /**
     * 获取自定义权限
     *
     * @param action
     * @return
     */
    public JSONObject getCustomFilter(Permission action) {
        return (JSONObject) customAdvFilters.getOrDefault(action.getName(), null);
    }
}
