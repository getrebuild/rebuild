/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.metadata.entity;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.BaseMeta;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.RebuildException;
import com.rebuild.server.configuration.RobotTriggerManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
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
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
	@Override
	public String getDescription() {
		return baseMeta.getDescription();
	}
	
	@Override
	public JSONObject getExtraAttrs() {
		return baseMeta.getExtraAttrs();
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

	// --

	/**
	 * also #getDescription()
	 * @return
	 */
	public String getLabel() {
		if (isField() && ((Field) baseMeta).getType() == FieldType.PRIMARY) {
			return "ID";
		}
		return StringUtils.defaultIfBlank(getDescription(), getName().toUpperCase());
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
		Object[] ext = getMetaExt();
		if (ext != null) {
			return (DisplayType) ext[2];
		}
		
		DisplayType dt;
		String dtInExtra = getExtraAttrs().getString("displayType");
		if (dtInExtra != null) {
			dt = DisplayType.valueOf(dtInExtra);
		} else {
			dt = converBuiltinFieldType((Field) baseMeta);
		}
		
		if (dt != null) {
			return dt;
		}
		throw new RebuildException("Unsupported field type : " + baseMeta);
	}
	
	/**
	 * 系统内建字段/实体，不可更改
	 * 
	 * @return
	 * @see MetadataHelper#isSystemField(Field)
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
				// @see MetadataHelper#getSlaveToMasterField
				Entity hasMaster = field.getOwnEntity().getMasterEntity();
				return hasMaster != null && hasMaster.equals(field.getReferenceEntity()) && !field.isCreatable();
			}
		}
		return false;
	}
	
	/**
	 * 保存的 ID
	 * 
	 * @return
	 */
	public ID getMetaId() {
		Object[] ext = getMetaExt();
		return ext == null ? null : (ID) ext[0];
	}
	
	/**
	 * 取代 persist4j 中的 description，而 persist4j 中的 description 则表示 label
	 * 
	 * @return
	 */
	public String getComments() {
		Object[] ext = getMetaExt();
		if (ext != null) {
			return (String) ext[1];
		}
		return StringUtils.defaultIfBlank(getExtraAttrs().getString("comments"), "系统内建");
	}
	
	/**
	 * 实体图标
	 * 
	 * @return
	 */
	public String getIcon() {
        Assert.isTrue(!isField(), "Entity supports only");
		String customIcon = null;
		Object[] ext = getMetaExt();
		if (ext != null) {
			customIcon = StringUtils.defaultIfBlank((String) ext[2], "texture");
		}
		if (StringUtils.isNotBlank(customIcon)) {
			return customIcon;
		}
		return StringUtils.defaultIfBlank(getExtraAttrs().getString("icon"), "texture");
	}
	
	/**
	 * 字段扩展配置
     *
	 * @return
     * @see FieldExtConfigProps
	 */
	public JSONObject getFieldExtConfig() {
        Assert.isTrue(isField(), "Field supports only");
		Object[] ext = getMetaExt();
		if (ext == null || StringUtils.isBlank((String) ext[3])) {
			JSONObject extConfig = getExtraAttrs().getJSONObject("extConfig");
			return extConfig == null ? JSONUtils.EMPTY_OBJECT : extConfig;
		}
		return JSON.parseObject((String) ext[3]);
	}

    /**
     * 字段扩展配置
     *
     * @param name
     * @return
     *
     * @see #getFieldExtConfig()
     * @see FieldExtConfigProps
     */
	public Object getPropOfFieldExtConfig(String name) {
	    return getFieldExtConfig().get(name);
    }

    /**
     * 指定实体具有和业务实体一样的特性（除权限以外（指定实体无权限字段））。
     * @return
     */
    public boolean isPlainEntity() {
        return !isField() && getExtraAttrs().getBooleanValue("plainEntity");
    }

    /**
     * @return
     */
	private boolean isField() {
		return baseMeta instanceof Field;
	}

    /**
     * @return
     */
	private Object[] getMetaExt() {
		return isField()
				? MetadataHelper.getMetadataFactory().getFieldExtmeta((Field) baseMeta)
				: MetadataHelper.getMetadataFactory().getEntityExtmeta((Entity) baseMeta);
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
			int rec = field.getReferenceEntity().getEntityCode();
			if (rec == EntityHelper.PickList) {
				return DisplayType.PICKLIST;
			} else if (rec == EntityHelper.Classification) {
				return DisplayType.CLASSIFICATION;
			} 
			return DisplayType.REFERENCE;
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

	@Override
	public String toString() {
		return "EASY#" + baseMeta.toString();
	}

	// --
	
	/**
	 * @param baseMeta
	 * @return
	 */
	public static EasyMeta valueOf(BaseMeta baseMeta) {
		return new EasyMeta(baseMeta);
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
		return new EasyMeta(field).getDisplayType();
	}
	
	/**
	 * @param meta
	 * @return
	 */
	public static String getLabel(BaseMeta meta) {
		return StringUtils.defaultIfBlank(meta.getDescription(), meta.getName().toUpperCase());
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
	 * @return Retuens { entity:xxx, entityLabel:xxx, icon:xxx }
	 */
	public static JSON getEntityShow(Entity entity) {
		EasyMeta easy = valueOf(entity);
		return JSONUtils.toJSONObject(
		        new String[] { "entity", "entityLabel", "icon" },
                new String[] { easy.getName(), easy.getLabel(), easy.getIcon() });
	}
}
