/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 为实体/字段添加索引
 *
 * @author devezhao
 * @since 2/25/2026
 */
public class FixRefsIndex43 {

    private final Entity[] entities;

    /**
     */
    public FixRefsIndex43() {
        List<Entity> entities = new ArrayList<>();
        for (Entity e : MetadataHelper.getEntities()) {
            if (MetadataHelper.isBusinessEntity(e)) entities.add(e);
        }

        this.entities = entities.toArray(new Entity[0]);
    }

    /**
     * @param entities
     */
    public FixRefsIndex43(Entity[] entities) {
        this.entities = entities;
    }

    /**
     */
    public void fix() {
        for (Entity e : entities) {
            fixEntity(e);
        }
    }

    /**
     * @param entity
     */
    public void fixEntity(Entity entity) {
        int index = 100;
        StringBuilder ddl = new StringBuilder(String.format("alter table %s", entity.getPhysicalName()));
        for (Field field : entity.getFields()) {
            DisplayType dt = EasyMetaFactory.getDisplayType(field);
            if (dt == DisplayType.REFERENCE) {
                if (!checkHasIndex(field)) {
                    ddl.append(String.format("\n add index FIX43_%d (%s),", index++, field.getPhysicalName()));
                }
            }
        }

        String ddl2 = ddl.substring(0, ddl.length() - 1) + ";";
        if (!ddl2.contains(" add index ")) return;

        System.out.println("Exec ...\n" + ddl2);
        Application.getSqlExecutor().execute(ddl2, 60 * 10);
    }

    /**
     * 指定字段已有索引?
     *
     * @param field
     * @return
     */
    protected boolean checkHasIndex(Field field) {
        String sql = String.format("show index from %s WHERE COLUMN_NAME = '%s'",
                field.getOwnEntity().getPhysicalName(), field.getPhysicalName());
        Object[] o = Application.getQueryFactory().createNativeQuery(sql).array();
        return o != null && o.length > 0;
    }
}
