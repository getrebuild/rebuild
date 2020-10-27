/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.BaseMeta;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.trigger.RobotTriggerManager;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.Set;

/**
 * 元数据（Entity/Field）封装
 *
 * @author zhaofang123@gmail.com
 * @since 08/13/2018
 */
public class EasyMeta implements BaseMeta {
    private static final long serialVersionUID = -6463919098111506968L;

    final private BaseMeta baseMeta;

    public EasyMeta(BaseMeta baseMeta) {
        this.baseMeta = baseMeta;
    }

    /**
     * @return Returns Entity or Field
     */
    public BaseMeta getBaseMeta() {
        return baseMeta;
    }

    @Override
    public String getName() {
        return baseMeta.getName();
    }

    @Override
    public String getPhysicalName() {
        return baseMeta.getPhysicalName();
    }

    /**
     * Use {@link #getLabel()}
     */
    @Deprecated
    @Override
    public String getDescription() {
        return baseMeta.getDescription();
    }

    @Override
    public JSONObject getExtraAttrs() {
        return getExtraAttrs(false);
    }

    /**
     * @param clean
     * @return
     */
    public JSONObject getExtraAttrs(boolean clean) {
        // see DynamicMetadataFactory
        if (clean) {
            JSONObject clone = (JSONObject) JSONUtils.clone(baseMeta.getExtraAttrs());
            clone.remove("metaId");
            clone.remove("comments");
            clone.remove("icon");
            clone.remove("displayType");
            return clone;
        }
        return baseMeta.getExtraAttrs() == null ? JSONUtils.EMPTY_OBJECT : baseMeta.getExtraAttrs();
    }

    @Override
    public boolean isCreatable() {
        return baseMeta.isCreatable();
    }

    @Override
    public boolean isUpdatable() {
        if (isField()) {
            if (!baseMeta.isUpdatable()) {
                return false;
            }

            Field field = (Field) baseMeta;
            Set<String> set = RobotTriggerManager.instance.getAutoReadonlyFields(field.getOwnEntity().getName());
            return !set.contains(field.getName());
        }

        return baseMeta.isUpdatable();
    }

    @Override
    public boolean isQueryable() {
        return baseMeta.isQueryable();
    }

    /**
     * 获取扩展属性
     *
     * @param name
     * @return
     */
    public String getExtraAttr(String name) {
        return getExtraAttrs().getString(name);
    }

    /**
     * 系统内建字段/实体，不可更改
     *
     * @return
     * @see MetadataHelper#isCommonsField(Field)
     */
    public boolean isBuiltin() {
        if (this.getMetaId() == null) {
            return true;
        }

        if (isField()) {
            Field field = (Field) baseMeta;
            if (MetadataHelper.isCommonsField(field)) {
                return true;
            } else if (getDisplayType() == DisplayType.REFERENCE) {
                // 明细-引用主记录的字段也是内建
                // @see MetadataHelper#getDetailToMainField
                Entity hasMain = field.getOwnEntity().getMainEntity();
                return hasMain != null && hasMain.equals(field.getReferenceEntity()) && !field.isCreatable();
            }
        }
        return false;
    }

    /**
     * @return
     * @see #getDescription()
     * @see com.rebuild.core.support.i18n.Language#L(BaseMeta)
     */
    public String getLabel() {
        if (isField() && ((Field) baseMeta).getType() == FieldType.PRIMARY) {
            return "ID";
        }
        return Language.L(this.baseMeta);
    }

    /**
     * 自定义实体/字段 ID
     *
     * @return
     */
    public ID getMetaId() {
        String metaId = getExtraAttr("metaId");
        return metaId == null ? null : ID.valueOf(metaId);
    }

    /**
     * 取代 persist4j 中的 description，而 persist4j 中的 description 则表示 label
     *
     * @return
     */
    public String getComments() {
        String comments = getExtraAttr("comments");
        if (getMetaId() != null) {
            return comments;
        }
        return StringUtils.defaultIfBlank(comments, Language.L("SysBuiltIn"));
    }

    @Override
    public String toString() {
        return "EASY#" + baseMeta.toString();
    }

    // -- ENTITY

    /**
     * 实体图标
     *
     * @return
     */
    public String getIcon() {
        Assert.isTrue(!isField(), "Entity supports only");
        return StringUtils.defaultIfBlank(getExtraAttr("icon"), "texture");
    }

    /**
     * 具有和业务实体一样的特性（除权限以外（因为无权限字段））
     *
     * @return
     */
    public boolean isPlainEntity() {
        Assert.isTrue(!isField(), "Entity supports only");
        return getExtraAttrs().getBooleanValue("plainEntity");
    }

    // -- FIELD

    /**
     * @return
     */
    private boolean isField() {
        return baseMeta instanceof Field;
    }

    /**
     * @param fullName
     * @return
     */
    public String getDisplayType(boolean fullName) {
        DisplayType dt = getDisplayType();
        if (fullName) {
            return dt.getDisplayName() + " (" + dt.name() + ")";
        } else {
            return dt.name();
        }
    }

    /**
     * @return
     */
    public DisplayType getDisplayType() {
        Assert.isTrue(isField(), "Field supports only");

        String displayType = getExtraAttr("displayType");
        DisplayType dt = displayType != null
                ? DisplayType.valueOf(displayType) : converBuiltinFieldType((Field) baseMeta);
        if (dt != null) {
            return dt;
        }
        throw new RebuildException("Unsupported field type : " + baseMeta);
    }

    /**
     * 将字段类型转成 DisplayType
     *
     * @param field
     * @return
     */
    private DisplayType converBuiltinFieldType(Field field) {
        Type ft = field.getType();
        if (ft == FieldType.PRIMARY) {
            return DisplayType.ID;
        } else if (ft == FieldType.REFERENCE) {
            int typeCode = field.getReferenceEntity().getEntityCode();
            if (typeCode == EntityHelper.PickList) {
                return DisplayType.PICKLIST;
            } else if (typeCode == EntityHelper.Classification) {
                return DisplayType.CLASSIFICATION;
            } else {
                return DisplayType.REFERENCE;
            }
        } else if (ft == FieldType.ANY_REFERENCE) {
            return DisplayType.ANYREFERENCE;
        } else if (ft == FieldType.TIMESTAMP) {
            return DisplayType.DATETIME;
        } else if (ft == FieldType.DATE) {
            return DisplayType.DATE;
        } else if (ft == FieldType.STRING) {
            return DisplayType.TEXT;
        } else if (ft == FieldType.TEXT || ft == FieldType.NTEXT) {
            return DisplayType.NTEXT;
        } else if (ft == FieldType.BOOL) {
            return DisplayType.BOOL;
        } else if (ft == FieldType.INT || ft == FieldType.SMALL_INT || ft == FieldType.LONG) {
            return DisplayType.NUMBER;
        } else if (ft == FieldType.DOUBLE || ft == FieldType.DECIMAL) {
            return DisplayType.DECIMAL;
        }
        return null;
    }

    // -- QUICK

    /**
     * @param entityOrField
     * @return
     */
    public static EasyMeta valueOf(BaseMeta entityOrField) {
        return new EasyMeta(entityOrField);
    }

    /**
     * @param entityCode
     * @return
     */
    public static EasyMeta valueOf(int entityCode) {
        return valueOf(MetadataHelper.getEntity(entityCode));
    }

    /**
     * @param entityName
     * @return
     */
    public static EasyMeta valueOf(String entityName) {
        return valueOf(MetadataHelper.getEntity(entityName));
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
     *
     * @param entity
     * @return Retuens { entity:xxx, entityLabel:xxx, icon:xxx }
     */
    public static JSON getEntityShow(Entity entity) {
        EasyMeta easy = valueOf(entity);
        return JSONUtils.toJSONObject(
                new String[] { "entity", "entityLabel", "icon" },
                new String[] { easy.getName(), easy.getLabel(), easy.getIcon() });
    }

    /**
     * @param entity
     * @return
     */
    public static boolean isPlainEntity(Entity entity) {
        return EasyMeta.valueOf(entity).isPlainEntity();
    }
}
