/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
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
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 元数据元素封装
 * 
 * @author zhaofang123@gmail.com
 * @since 08/13/2018
 */
public class EasyMeta implements BaseMeta {
	private static final long serialVersionUID = -6463919098111506968L;

	private BaseMeta baseMeta;
	
	public EasyMeta(BaseMeta baseMeta) {
		this.baseMeta = baseMeta;
	}
	
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
	public String getExtraAttrs() {
		return baseMeta.getExtraAttrs();
	}

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
		if (!isField()) {
			throw new UnsupportedOperationException("Field only");
		}
		
		Object[] ext = getMetaExt();
		if (ext != null) {
			return (DisplayType) ext[2];
		}
		
		DisplayType dt;
		String dtInExtra = getExtraAttrsJson().getString("displayType");
		if (dtInExtra != null) {
			dt = DisplayType.valueOf(dtInExtra);
		} else {
			dt = converBuiltinFieldType((Field) baseMeta);
		}
		
		if (dt != null) {
			return dt;
		}
		throw new RebuildException("Unsupported field type : " + this.baseMeta);
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
			Field field = (Field) this.baseMeta;
			if (MetadataHelper.isCommonsField(field)) {
				return true;
			} else if (getDisplayType() == DisplayType.REFERENCE) {
				// 明细-引用主记录的字段也是内建
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
		return StringUtils.defaultIfBlank(getExtraAttrsJson().getString("comments"), "系统内建");
	}
	
	/**
	 * 实体图标
	 * 
	 * @return
	 */
	public String getIcon() {
		if (isField()) {
			throw new UnsupportedOperationException("Entity only");
		}
		
		String customIcon = null;
		Object[] ext = getMetaExt();
		if (ext != null) {
			customIcon = StringUtils.defaultIfBlank((String) ext[2], "texture");
		}
		if (StringUtils.isNotBlank(customIcon)) {
			return customIcon;
		}
		return StringUtils.defaultIfBlank(getExtraAttrsJson().getString("icon"), "texture");
	}
	
	/**
	 * 字段扩展配置
	 * 
	 * @return
	 */
	public JSONObject getFieldExtConfig() {
		if (!isField()) {
			throw new UnsupportedOperationException("Field only");
		}
		
		Object[] ext = getMetaExt();
		if (ext == null || StringUtils.isBlank((String) ext[3])) {
			JSONObject extConfig = getExtraAttrsJson().getJSONObject("extConfig");
			return extConfig == null ? JSONUtils.EMPTY_OBJECT : extConfig;
		}
		return JSON.parseObject((String) ext[3]);
	}
	
	private boolean isField() {
		return baseMeta instanceof Field;
	}
	
	private Object[] getMetaExt() {
		Object[] ext;
		if (isField()) {
			ext = MetadataHelper.getMetadataFactory().getFieldExtmeta((Field) baseMeta);
		} else {
			ext = MetadataHelper.getMetadataFactory().getEntityExtmeta((Entity) baseMeta);
		}
		return ext;
	}
	
	private JSONObject getExtraAttrsJson() {
		return StringUtils.isBlank(getExtraAttrs())
				? JSONUtils.EMPTY_OBJECT : JSON.parseObject(getExtraAttrs());
	}

	// 将字段类型转成 DisplayType
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
		return "EASY#" + this.baseMeta.toString();
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
		return meta.getDescription();
	}
	
	/**
	 * @param entity
	 * @param joinFields
	 * @return
	 */
	public static String getLabel(Entity entity, String joinFields) {
		String[] fieldPath = joinFields.split("\\.");
		Field firstField = entity.getField(fieldPath[0]);
		if (fieldPath.length == 1) {
			return getLabel(firstField);
		}
		
		Entity refEntity = firstField.getReferenceEntity();
		Field secondField = refEntity.getField(fieldPath[1]);
		return String.format("%s.%s", getLabel(refEntity), getLabel(secondField));
	}
	
	/**
	 * @return [Name, Label, Icon]
	 */
	public static String[] getEntityShow(Entity entity) {
		EasyMeta em = valueOf(entity);
		return new String[] { entity.getName(), em.getLabel(), em.getIcon() };
	}
}
