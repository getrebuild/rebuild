/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.vector;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;

/**
 * 实体元数据
 *
 * @author Zixin
 * @since 2025/5/10
 */
public class EntityData implements VectorData {

    private final Entity entity;
    private final boolean hasDetails;

    public EntityData(Entity entity) {
        this(entity, false);
    }

    public EntityData(Entity entity, boolean hasDetails) {
        this.entity = entity;
        this.hasDetails = hasDetails;
    }

    @Override
    public String toVector() {
        StringBuilder sb = new StringBuilder();
        sb.append(buildEntity(entity));

        // 明细
        if (hasDetails && entity.getDetailEntity() != null) {
            for (Entity de : entity.getDetialEntities()) {
                sb.append(NN).append(buildEntity(de));
            }
        }

        return sb.toString();
    }

    private String buildEntity(Entity entity) {
        MdTable mt = new MdTable(EasyMetaFactory.getLabel(entity));
        mt.addHead("字段");
        mt.addHead("标识");
        mt.addHead("类型");
        mt.addHead("备注");

        for (Field field : entity.getFields()) {
            if (MetadataHelper.isSystemField(field)) continue;

            EasyField e = EasyMetaFactory.valueOf(field);
            DisplayType dt = e.getDisplayType();
            if (dt == DisplayType.ID) continue;

            mt.addRowData(new String[]{
                    e.getLabel(),
                    field.getName(),
                    e.getDisplayType(true),
                    e.getComments()
            });
        }

        return mt.toMdTable();
    }
}
