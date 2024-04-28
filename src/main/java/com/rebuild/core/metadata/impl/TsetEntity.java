/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.privileges.UserService;
import com.rebuild.utils.BlockList;

/**
 * @author Zixin
 * @since 4/26/2024
 */
public class TsetEntity {

    /**
     * @return
     */
    public String create() {
        return create("TestAllFields999");
    }

    /**
     * @param name
     * @return
     */
    public String create(final String name) {
        if (MetadataHelper.containsEntity(name)) return "EXISTS:" + name;

        final ID user = UserService.SYSTEM_USER;

        Entity2Schema entity2Schema = new Entity2Schema(user);
        String entityName = entity2Schema.createEntity(
                null, name.toUpperCase(), "测试专用实体", null, Boolean.TRUE, Boolean.FALSE);
        Entity entityMeta = MetadataHelper.getEntity(entityName);

        for (DisplayType dt : DisplayType.values()) {
            if (dt == DisplayType.ID) continue;

            String fieldLabel = dt.getDisplayName();
            String fieldName = dt.name().toUpperCase();
            if (BlockList.isBlock(fieldName)) fieldName += "1";

            if (dt == DisplayType.REFERENCE || dt == DisplayType.N2NREFERENCE) {
                new Field2Schema(user)
                        .createField(entityMeta, fieldLabel, fieldName, dt, null, entityName, null);
            } else if (dt == DisplayType.CLASSIFICATION) {
                JSON extra = JSON.parseObject("{classification:'018-0000000000000001'}");
                new Field2Schema(user)
                        .createField(entityMeta, fieldLabel, fieldName, dt, null, entityName, extra);
            } else if (dt == DisplayType.STATE) {
                JSON extra = JSON.parseObject("{stateClass:'com.rebuild.core.support.state.HowtoState'}");
                new Field2Schema(user)
                        .createField(entityMeta, fieldLabel, fieldName, dt, null, entityName, extra);
            } else {
                new Field2Schema(user)
                        .createField(entityMeta, fieldLabel, fieldName, dt, null, null, null);
            }
        }

        return entityName;
    }
}
