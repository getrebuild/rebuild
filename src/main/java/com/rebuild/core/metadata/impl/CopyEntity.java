/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.rbstore.MetaSchemaGenerator;
import com.rebuild.core.rbstore.MetaschemaImporter;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.RbAssert;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomUtils;

/**
 * 复制实体
 *
 * @author devezhao
 * @see MetaSchemaGenerator
 * @see MetaschemaImporter
 * @since 2021/12/17
 */
public class CopyEntity extends Entity2Schema {

    final private Entity sourceEntity;

    public CopyEntity(Entity sourceEntity) {
        this.sourceEntity = sourceEntity;
    }

    /**
     * @param entityName
     * @param detailName
     * @return
     */
    public String copy(String entityName, String detailName) {
        final ID user = getUser();
        RbAssert.isAllow(UserHelper.isSuperAdmin(user), Language.L("仅超级管理员可操作"));

        // 导出

        JSONObject schemadata = (JSONObject) new MetaSchemaGenerator(sourceEntity, false).generate();
        String uniqueEntityName = clearConfig(schemadata, entityName);

        JSONObject detailSchema = schemadata.getJSONObject("detail");
        if (StringUtils.isBlank(detailName)) {
            schemadata.remove("detail");
        } else if (detailSchema != null) {
            clearConfig(detailSchema, detailName);
        }

        // 导入

        MetaschemaImporter importer = new MetaschemaImporter(schemadata);
        importer.setUser(user);
        TaskExecutors.run(importer);

        String hasError = importer.getErrorMessage();
        if (hasError != null) {
            throw new RebuildException(hasError);
        }

        // TODO 保留审批字段？

        return uniqueEntityName;
    }

    private String clearConfig(JSONObject schema, String entityName) {
        schema.remove(MetaSchemaGenerator.CFG_TRANSFORMS);
        schema.remove(MetaSchemaGenerator.CFG_APPROVALS);
        schema.remove(MetaSchemaGenerator.CFG_TRIGGERS);
        schema.remove(MetaSchemaGenerator.CFG_FILTERS);

        String uniqueEntityName = toPinyinName(entityName);
        for (int i = 0; i < 6; i++) {
            if (MetadataHelper.containsEntity(uniqueEntityName)) {
                uniqueEntityName += RandomUtils.nextInt(0, 9);
            } else {
                break;
            }
        }

        schema.put("entity", uniqueEntityName);
        schema.put("entityLabel", entityName);

        return uniqueEntityName;
    }
}
