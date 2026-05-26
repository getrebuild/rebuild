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
import org.apache.commons.lang.StringUtils;

/**
 * 实体元数据
 *
 * @author Zixin
 * @since 2025/5/10
 */
public class EntitiesData implements VectorData {

    private final Entity entity;

    public EntitiesData() {
        this(null);
    }

    public EntitiesData(Entity entity) {
        this.entity = entity;
    }

    @Override
    public String toVector() {
        StringBuilder sb = new StringBuilder();

        Entity[] es = entity == null ? MetadataHelper.getEntities() : new Entity[]{entity};
        for (Entity e : es) {
            if (entity == null) {
                if (!(MetadataHelper.isBusinessEntity(e) || MetadataHelper.isBizzEntity(e))) continue;
                if (e.getMainEntity() != null) continue;
            }

            sb.append(buildEntity(e));
            // 明细
            if (e.getDetailEntity() != null) {
                for (Entity de : e.getDetialEntities()) {
                    sb.append(NN).append(buildEntity(de));
                }
            }
            sb.append(N);
        }

        return sb.toString();
    }

    /**
     * @param e
     * @return
     */
    private String buildEntity(Entity e) {
        String title = EasyMetaFactory.getLabel(e) + " (" + e.getName() + ")";
        if (e.getMainEntity() != null) {
            title += " (主实体:" + e.getMainEntity().getName() + ")";
        }

        MdTable mt = new MdTable(title);
        mt.addHead("字段");
        mt.addHead("标识");
        mt.addHead("类型");
        mt.addHead("备注");

        for (Field field : e.getFields()) {
            if (MetadataHelper.isSystemField(field)) continue;

            EasyField F = EasyMetaFactory.valueOf(field);
            DisplayType dt = F.getDisplayType();
            if (dt == DisplayType.ID) continue;

            String comment = StringUtils.defaultIfBlank(F.getComments(), "");
            if (dt == DisplayType.REFERENCE || dt == DisplayType.N2NREFERENCE) {
                comment += " (引用:" + F.getRawMeta().getReferenceEntity().getName() + ")";
            } else if (dt == DisplayType.ANYREFERENCE) {
                comment += " (引用:任意实体)";
            }

            mt.addRowData(new String[]{
                    F.getLabel(),
                    field.getName(),
                    F.getDisplayType(true),
                    comment.trim()
            });
        }

        return mt.toMdTable();
    }
}
