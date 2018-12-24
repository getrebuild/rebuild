/*
rebuild - Building your system freely.
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

package com.rebuild.server.metadata.entityhub;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.RebuildException;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.BaseMeta;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/13/2018
 */
public class EasyMeta implements BaseMeta {
	private static final long serialVersionUID = -6463919098111506968L;
	
	private static final Set<String> BUILTIN_ENTITY = new HashSet<>();
	private static final Set<String> BUILTIN_FIELD = new HashSet<>();
	private static final Map<String, String[]> SYSENTITY_EXTMETA = new HashMap<>();
	static {
		BUILTIN_FIELD.add(EntityHelper.AutoId);
		BUILTIN_FIELD.add(EntityHelper.QuickCode);
		BUILTIN_FIELD.add(EntityHelper.CreatedOn);
		BUILTIN_FIELD.add(EntityHelper.CreatedBy);
		BUILTIN_FIELD.add(EntityHelper.ModifiedOn);
		BUILTIN_FIELD.add(EntityHelper.ModifiedBy);
		BUILTIN_FIELD.add(EntityHelper.OwningUser);
		BUILTIN_FIELD.add(EntityHelper.OwningDept);
		
		SYSENTITY_EXTMETA.put("User", new String[] { "account", "系统内建" });
		SYSENTITY_EXTMETA.put("Department", new String[] { "accounts", "系统内建" });
		SYSENTITY_EXTMETA.put("Role", new String[] { "lock", "系统内建" });
	}

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
	@Deprecated
	@Override
	public String getDescription() {
		return baseMeta.getDescription();
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
		if (isField()) {
			Object[] ext = getMetaExt();
			if (ext != null) {
				DisplayType dt = (DisplayType) ext[2];
				return dt;
			}
			
			DisplayType dt = MetadataHelper.getBuiltinFieldType((Field) baseMeta);
			if (dt != null) {
				return dt;
			} 
			throw new RebuildException("Unsupported field type : " + this.baseMeta);
		}
		throw new UnsupportedOperationException("Field only");
	}
	
	/**
	 * 系统内建字段，一般系统用
	 * 
	 * @return
	 */
	public boolean isBuiltin() {
		if (this.getMetaId() == null) {
			return true;
		}
		
		if (isField()) {
			DisplayType dt = getDisplayType();
			Field field = (Field) this.baseMeta;
			if (dt == DisplayType.ID || BUILTIN_FIELD.contains(getName())) {
				return true;
			} else if (dt == DisplayType.REFERENCE) {
				// 明细-引用主记录的字段也是内建
				Entity hasMaster = field.getOwnEntity().getMasterEntity();
				if (hasMaster != null && hasMaster.equals(field.getReferenceEntity()) && !field.isCreatable()) {
					return true;
				}
			}
		} else {
			return BUILTIN_ENTITY.contains(getName());
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
		String def[] = SYSENTITY_EXTMETA.get(getName());
		String defComments = def == null ? null : def[1];
		if (getMetaId() == null && defComments == null) {
			defComments = "系统内建";
		}
		
		String customComments = null;
		Object[] ext = getMetaExt();
		if (ext != null) {
			customComments = (String) ext[1];
		}
		return StringUtils.defaultIfBlank(customComments, defComments);
	}
	
	/**
	 * @return
	 */
	public String getIcon() {
		if (isField()) {
			throw new UnsupportedOperationException("Entity only");
		}
		
		String def[] = SYSENTITY_EXTMETA.get(getName());
		String defIcon = def == null ? null : def[0];
		
		String customIcon = null;
		Object[] ext = getMetaExt();
		if (ext != null) {
			customIcon = StringUtils.defaultIfBlank((String) ext[2], "texture");
		}
		return StringUtils.defaultIfBlank(customIcon, defIcon);
	}
	
	/**
	 * 扩展属性
	 * 
	 * @return
	 */
	private Object[] getMetaExt() {
		Object[] ext = null;
		if (isField()) {
			ext = MetadataHelper.getFieldExtmeta((Field) baseMeta);
		} else {
			ext = MetadataHelper.getEntityExtmeta((Entity) baseMeta);
		}
		return ext;
	}
	
	/**
	 * 字段扩展配置
	 * 
	 * @return
	 */
	public JSONObject getFieldExtConfig() {
		if (isField()) {
			Object[] ext = getMetaExt();
			if (ext == null) {
				return JSONUtils.EMPTY_OBJECT;
			}
			return JSON.parseObject(StringUtils.defaultIfBlank((String) ext[3], JSONUtils.EMPTY_OBJECT_STR));
		}
		throw new UnsupportedOperationException("Field only");
	}
	
	/**
	 * @return
	 */
	private boolean isField() {
		return baseMeta instanceof Field;
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
	 * @return [Name, Label, Icon]
	 */
	public static String[] getEntityShows(Entity entity) {
		EasyMeta em = valueOf(entity);
		return new String[] { entity.getName(), em.getLabel(), em.getIcon() };
	}
}
