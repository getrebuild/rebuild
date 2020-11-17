/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.metadata.BaseMeta;
import cn.devezhao.persist4j.metadata.MetadataException;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.FieldExtConfigProps;
import com.rebuild.core.support.state.StateHelper;
import com.rebuild.utils.JSONUtils;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyMetaFactory {

    /**
     * @param entity
     * @return
     */
    public static EasyEntity valueOf(Entity entity) {
        return new EasyEntity(entity);
    }

    /**
     * @param entityCode
     * @return
     */
    public static EasyEntity valueOf(int entityCode) {
        return valueOf(MetadataHelper.getEntity(entityCode));
    }

    /**
     * @param entityName
     * @return
     */
    public static EasyEntity valueOf(String entityName) {
        return valueOf(MetadataHelper.getEntity(entityName));
    }

    /**
     * @param field
     * @return
     */
    public static EasyField valueOf(Field field) {
        return new EasyField(field);
    }

    /**
     * @param entityOrField
     * @return
     */
    public static BaseEasyMeta<?> valueOf(BaseMeta entityOrField) {
        if (entityOrField instanceof Entity) return valueOf((Entity) entityOrField);
        if (entityOrField instanceof Field) return valueOf((Field) entityOrField);
        throw new MetadataException("Unsupport meta type : " + entityOrField);
    }

    /**
     * @param field
     * @return
     */
    public static DisplayType getDisplayType(Field field) {
        return valueOf(field).getDisplayType();
    }

    /**
     * @param entityName
     * @return
     */
    public static String getLabel(String entityName) {
        return getLabel(MetadataHelper.getEntity(entityName));
    }

    /**
     * @param entityOrField
     * @return
     */
    public static String getLabel(BaseMeta entityOrField) {
        return valueOf(entityOrField).getLabel();
    }

    /**
     * 获取字段 Label（支持两级字段，如 owningUser.fullName）
     *
     * @param entity
     * @param fieldPath
     * @return
     */
    public static String getLabel(Entity entity, String fieldPath) {
        String[] fieldPathSplit = fieldPath.split("\\.");
        Field firstField = entity.getField(fieldPathSplit[0]);
        if (fieldPathSplit.length == 1) {
            return getLabel(firstField);
        }

        Entity refEntity = firstField.getReferenceEntity();
        Field secondField = refEntity.getField(fieldPathSplit[1]);
        return String.format("%s.%s", getLabel(firstField), getLabel(secondField));
    }

    /**
     * 前端使用
     *
     * @param entity
     * @return returns { entity:xxx, entityLabel:xxx, icon:xxx }
     */
    public static JSONObject getEntityShow(Entity entity) {
        EasyEntity easy = valueOf(entity);
        return JSONUtils.toJSONObject(
                new String[] { "entity", "entityLabel", "icon" },
                new String[] { easy.getName(), easy.getLabel(), easy.getIcon() });
    }

    /**
     * 前端使用
     *
     * @param field
     * @return
     */
    public static JSONObject getFieldShow(Field field) {
        JSONObject map = new JSONObject();

        EasyField easy = valueOf(field);
        map.put("name", field.getName());
        map.put("label", easy.getLabel());
        map.put("type", easy.getDisplayType().name());
        map.put("nullable", field.isNullable());
        map.put("creatable", field.isCreatable());
        map.put("updatable", field.isUpdatable());

        DisplayType dt = getDisplayType(field);
        if (dt == DisplayType.REFERENCE || dt == DisplayType.N2NREFERENCE) {
            Entity refEntity = field.getReferenceEntity();
            Field nameField = MetadataHelper.getNameField(refEntity);
            map.put("ref", new String[] { refEntity.getName(), getDisplayType(nameField).name() });
        } if (dt == DisplayType.ID) {
            Entity refEntity = field.getOwnEntity();
            Field nameField = MetadataHelper.getNameField(refEntity);
            map.put("ref", new String[] { refEntity.getName(), getDisplayType(nameField).name() });
        } else if (dt == DisplayType.STATE) {
            map.put("stateClass", StateHelper.getSatetClass(field).getName());
        } else if (dt == DisplayType.CLASSIFICATION) {
            map.put("classification", easy.getExtraAttr(FieldExtConfigProps.CLASSIFICATION_USE));
        }

        return map;
    }
}
