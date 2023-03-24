/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;
import cn.devezhao.persist4j.metadata.BaseMeta;
import cn.devezhao.persist4j.metadata.MetadataException;
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
import com.alibaba.fastjson.JSONObject;
import com.esotericsoftware.minlog.Log;
import com.rebuild.core.RebuildException;
import com.rebuild.core.configuration.general.AutoFillinManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.trigger.RobotTriggerManager;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

import static com.rebuild.core.metadata.easymeta.DisplayType.ANYREFERENCE;
import static com.rebuild.core.metadata.easymeta.DisplayType.BOOL;
import static com.rebuild.core.metadata.easymeta.DisplayType.CLASSIFICATION;
import static com.rebuild.core.metadata.easymeta.DisplayType.DATE;
import static com.rebuild.core.metadata.easymeta.DisplayType.DATETIME;
import static com.rebuild.core.metadata.easymeta.DisplayType.DECIMAL;
import static com.rebuild.core.metadata.easymeta.DisplayType.ID;
import static com.rebuild.core.metadata.easymeta.DisplayType.N2NREFERENCE;
import static com.rebuild.core.metadata.easymeta.DisplayType.NTEXT;
import static com.rebuild.core.metadata.easymeta.DisplayType.NUMBER;
import static com.rebuild.core.metadata.easymeta.DisplayType.PICKLIST;
import static com.rebuild.core.metadata.easymeta.DisplayType.REFERENCE;
import static com.rebuild.core.metadata.easymeta.DisplayType.TEXT;

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
        String displayType = field.getExtraAttrs() == null
                ? null : field.getExtraAttrs().getString("displayType");
        DisplayType dt = displayType == null ? convertBuiltinFieldType(field) : DisplayType.valueOf(displayType);
        if (dt == null) {
            throw new RebuildException("Unsupported field type : " + field);
        }

        try {
            Constructor<?> c = ReflectionUtils.accessibleConstructor(dt.getEasyClass(), Field.class, DisplayType.class);
            return (EasyField) c.newInstance(field, dt);
        } catch (Exception ex) {
            throw new RebuildException(ex);
        }
    }

    /**
     * 将字段类型转成 DisplayType
     *
     * @param field
     * @return
     */
    static DisplayType convertBuiltinFieldType(Field field) {
        Type ft = field.getType();
        if (ft == FieldType.PRIMARY) {
            return ID;
        } else if (ft == FieldType.REFERENCE) {
            int typeCode = field.getReferenceEntity().getEntityCode();
            if (typeCode == EntityHelper.PickList) {
                return PICKLIST;
            } else if (typeCode == EntityHelper.Classification) {
                return CLASSIFICATION;
            } else {
                return REFERENCE;
            }
        } else if (ft == FieldType.ANY_REFERENCE) {
            return ANYREFERENCE;
        } else if (ft == FieldType.REFERENCE_LIST) {
            return N2NREFERENCE;
        } else if (ft == FieldType.TIMESTAMP) {
            return DATETIME;
        } else if (ft == FieldType.DATE) {
            return DATE;
        } else if (ft == FieldType.STRING) {
            return TEXT;
        } else if (ft == FieldType.TEXT || ft == FieldType.NTEXT) {
            return NTEXT;
        } else if (ft == FieldType.BOOL) {
            return BOOL;
        } else if (ft == FieldType.INT || ft == FieldType.SMALL_INT || ft == FieldType.LONG) {
            return NUMBER;
        } else if (ft == FieldType.DOUBLE || ft == FieldType.DECIMAL) {
            return DECIMAL;
        }
        return null;
    }

    // -- QUICK

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
        if (entityOrField instanceof Entity) return valueOf((Entity) entityOrField).getLabel();
        else if (entityOrField instanceof Field) return valueOf((Field) entityOrField).getLabel();
        else throw new MetadataException("Unsupport meta type : " + entityOrField);
    }

    /**
     * 获取字段 Label（支持两级字段，如 owningUser.fullName）
     *
     * @param entity
     * @param fieldPath
     * @return
     */
    public static String getLabel(Entity entity, String fieldPath) {
        try {
            String[] fieldPathSplit = fieldPath.split("\\.");
            Field firstField = entity.getField(fieldPathSplit[0]);
            if (fieldPathSplit.length == 1) {
                return getLabel(firstField);
            }

            Entity refEntity = firstField.getReferenceEntity();
            Field secondField = refEntity.getField(fieldPathSplit[1]);
            return String.format("%s.%s", getLabel(firstField), getLabel(secondField));
        } catch (MissingMetaExcetion ex) {
            Log.error(ex.getLocalizedMessage());
            return String.format("[%s]", fieldPath.toUpperCase());
        }
    }

    /**
     * @param entity
     * @return
     * @see EasyEntity#toJSON()
     */
    public static JSONObject toJSON(Entity entity) {
        return (JSONObject) valueOf(entity).toJSON();
    }

    /**
     * @param field
     * @return
     * @see EasyField#toJSON()
     */
    public static JSONObject toJSON(Field field) {
        return (JSONObject) valueOf(field).toJSON();
    }

    /**
     * 获取自动只读的字段
     *
     * @param entity
     * @return
     * @see RobotTriggerManager#getAutoReadonlyFields(String)
     * @see AutoFillinManager#getAutoReadonlyFields(String)
     */
    public static Set<String> getAutoReadonlyFields(String entity) {
        Set<String> set = new HashSet<>();
        set.addAll(RobotTriggerManager.instance.getAutoReadonlyFields(entity));
        set.addAll(AutoFillinManager.instance.getAutoReadonlyFields(entity));
        return set;
    }
}
