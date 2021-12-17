/*
Copyright (c) Ruifang Tech <http://ruifang-tech.com/> and/or its owners. All rights reserved.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.rbstore.MetaSchemaGenerator;
import com.rebuild.core.rbstore.MetaschemaImporter;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.RbAssert;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;

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

        JSONObject schemadata = (JSONObject) new MetaSchemaGenerator(sourceEntity).generate();
        clearConfig(schemadata);

        String uniqueEntityName = getUniqueEntityName(entityName);
        schemadata.put("entity", uniqueEntityName);
        schemadata.put("entityLabel", entityName);

        JSONObject detailSchema = schemadata.getJSONObject("detail");
        if (StringUtils.isBlank(detailName)) {
            schemadata.remove("detail");
        } else if (detailSchema != null) {
            clearConfig(detailSchema);

            String uniqueDetailName = getUniqueEntityName(detailName);
            detailSchema.put("entity", uniqueDetailName);
            detailSchema.put("entityLabel", detailName);
        }

        // 导入

        new MetaschemaImporter2(schemadata, user).execNow();

        return uniqueEntityName;
    }

    private String getUniqueEntityName(String name) {
        name = toPinyinName(name);
        for (int i = 0; i < 5; i++) {
            if (MetadataHelper.containsEntity(name)) {
                name += RandomUtils.nextInt(99);
            } else {
                break;
            }
        }
        return name;
    }

    private void clearConfig(JSONObject schema) {
        schema.remove(MetaSchemaGenerator.CFG_TRANSFORMS);
        schema.remove(MetaSchemaGenerator.CFG_APPROVALS);
        schema.remove(MetaSchemaGenerator.CFG_TRIGGERS);
        schema.remove(MetaSchemaGenerator.CFG_FILTERS);

        // 以下保留
//        schema.remove(MetaSchemaGenerator.CFG_FILLINS);
//        schema.remove(MetaSchemaGenerator.CFG_LAYOUTS);
    }

    private static class MetaschemaImporter2 extends MetaschemaImporter {
        MetaschemaImporter2(JSONObject data, ID user) {
            super(data);
            setUser(user);
        }

        String execNow() {
            return this.exec();
        }
    }
}
