/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.setup;

import cn.devezhao.bizz.security.member.NoMemberFoundException;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.impl.DynamicMetadataContextHolder;
import com.rebuild.core.metadata.impl.Entity2Schema;
import com.rebuild.core.metadata.impl.Field2Schema;
import com.rebuild.core.privileges.UserService;
import com.rebuild.utils.BlockList;
import lombok.extern.slf4j.Slf4j;

/**
 * RB示例实体 TestAllFields
 *
 * @author devezhao
 * @since 2024/12/29
 */
@Slf4j
public class SimpleEntity {

    public static final String NAME = "TestAllFields";

    /**
     * @param dropExists
     * @param includeBuiltin
     * @param addPrivileges
     * @return
     */
    public boolean create(boolean dropExists, boolean includeBuiltin, boolean addPrivileges) {
        try {
            DynamicMetadataContextHolder.setSkipLanguageRefresh();
            return this.createInternal(dropExists, includeBuiltin, addPrivileges);
        } finally {
            DynamicMetadataContextHolder.setSkipLanguageRefresh();
        }
    }

    private boolean createInternal(boolean dropExists, boolean includeBuiltin, boolean addPrivileges) {
        if (MetadataHelper.containsEntity(NAME)) {
            if (dropExists) {
                log.warn("Dropping exists : {}", NAME);
                new Entity2Schema(UserService.SYSTEM_USER).dropEntity(MetadataHelper.getEntity(NAME), true);
            } else {
                return false;
            }
        }

        Entity2Schema entity2Schema = new Entity2Schema(UserService.SYSTEM_USER);
        entity2Schema.createEntity(
                NAME, "RB示例实体", "演示实体/字段的基本用法。更多详情参阅 https://getrebuild.com/docs/admin/entity/", null, true, false);

        final Entity entity = MetadataHelper.getEntity(NAME);
        Field2Schema field2Schema = new Field2Schema(UserService.SYSTEM_USER);

        for (DisplayType dt : DisplayType.values()) {
            if (dt == DisplayType.ID) continue;
            if (!includeBuiltin) {
                if (dt == DisplayType.STATE || dt == DisplayType.ANYREFERENCE) continue;
            }

            String fieldName = dt.name();
            if (BlockList.isBlock(fieldName)) fieldName += "1";
            String fieldLabel = dt.getDisplayName();

            if (dt == DisplayType.REFERENCE || dt == DisplayType.N2NREFERENCE) {
                field2Schema.createField(entity, fieldLabel, fieldName, dt, null, NAME, null);
            } else if (dt == DisplayType.CLASSIFICATION) {
                JSON extConfig = JSON.parseObject("{classification:'018-0000000000000001'}");
                field2Schema.createField(entity, fieldLabel, fieldName, dt, null, NAME, extConfig);
            } else if (dt == DisplayType.STATE) {
                JSON extConfig = JSON.parseObject("{stateClass:'com.rebuild.core.support.state.HowtoState'}");
                field2Schema.createField(entity, fieldLabel, fieldName, dt, null, NAME, extConfig);
            } else {
                field2Schema.createField(entity, fieldLabel, fieldName, dt, null, null, null);
            }
        }

        if (addPrivileges) {
            // RB示例角色
            addPrivileges2SimpleRole(ID.valueOf("003-9000000000000001"), entity.getEntityCode());
        }

        return true;
    }

    private boolean addPrivileges2SimpleRole(ID roleId, int entityCode) {
        try {
            Application.getUserStore().getRole(roleId);
        } catch (NoMemberFoundException ex) {
            return false;
        }

        Object e = Application.getQueryFactory().createQueryNoFilter(
                "select privilegesId from RolePrivileges where roleId = ? and entity = ?")
                .setParameter(1, roleId)
                .setParameter(2, entityCode)
                .unique();
        if (e != null) return false;

        Record p = EntityHelper.forNew(EntityHelper.RolePrivileges, UserService.SYSTEM_USER);
        p.setID("roleId", roleId);
        p.setInt("entity", entityCode);
        p.setString("definition", "{'A':4,'R':4,'C':4,'S':4,'D':4,'U':4}");
        Application.getCommonsService().create(p, false);
        Application.getUserStore().refreshRole(roleId);
        return true;
    }
}
