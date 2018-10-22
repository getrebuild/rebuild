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

package com.rebuild.server.entityhub;

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

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.dialect.Type;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.BaseMeta;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/13/2018
 */
public class EasyMeta implements BaseMeta {
	private static final long serialVersionUID = -6463919098111506968L;
	
	public static final Set<String> BUILTIN_ENTITY = new HashSet<>();
	public static final Set<String> BUILTIN_FIELD = new HashSet<>();
	private static final Map<String, String[]> SYSENTITY_INFO = new HashMap<>();
	static {
		BUILTIN_ENTITY.add("RolePrivileges");
		BUILTIN_ENTITY.add("RoleMember");
		BUILTIN_ENTITY.add("MetaEntity");
		BUILTIN_ENTITY.add("MetaField");
		BUILTIN_ENTITY.add("PickList");
		BUILTIN_ENTITY.add("LayoutConfig");
		BUILTIN_ENTITY.add("FilterConfig");
		BUILTIN_ENTITY.add("ViewTabConfig");
		BUILTIN_ENTITY.add("ShareAccess");
		
		BUILTIN_FIELD.add(EntityHelper.createdOn);
		BUILTIN_FIELD.add(EntityHelper.createdBy);
		BUILTIN_FIELD.add(EntityHelper.modifiedOn);
		BUILTIN_FIELD.add(EntityHelper.modifiedBy);
		BUILTIN_FIELD.add(EntityHelper.owningUser);
		BUILTIN_FIELD.add(EntityHelper.owningDept);
		
		SYSENTITY_INFO.put("User", new String[] { "account", "系统内建" });
		SYSENTITY_INFO.put("Department", new String[] { "accounts", "系统内建" });
		SYSENTITY_INFO.put("Role", new String[] { "lock", "系统内建" });
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
		if (isField()) {
			DisplayType dt = getDisplayType();
			if (fullName) {
				return dt.getDisplayName() + " (" + dt.name() + ")";
			} else {
				return dt.name();
			}
		}
		throw new UnsupportedOperationException("Non `field` entry");
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
			
			Field field = (Field) baseMeta;
			if (field.getOwnEntity().getEntityCode() == EntityHelper.User && "email".equals(field.getName())) {
				return DisplayType.EMAIL;
			}
			
			Type ft = field.getType();
			if (ft == FieldType.PRIMARY) {
				return DisplayType.ID;
			} else if (ft == FieldType.REFERENCE) {
				return DisplayType.REFERENCE;
			} else if (ft == FieldType.TIMESTAMP) {
				return DisplayType.DATETIME;
			} else if (ft == FieldType.DATE) {
				return DisplayType.DATE;
			} else if (ft == FieldType.STRING) {
				return DisplayType.TEXT;
			} else if (ft == FieldType.BOOL) {
				return DisplayType.BOOL;
			}
		}
		throw new RebuildException("Unsupported field type : " + this.baseMeta);
	}
	
	/**
	 * 系统内建字段，一般系统用
	 * 
	 * @return
	 */
	public boolean isBuiltin() {
		return isBuiltin(baseMeta);
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
		String def[] = SYSENTITY_INFO.get(getName());
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
			return null;
		}
		
		String def[] = SYSENTITY_INFO.get(getName());
		String defIcon = def == null ? null : def[0];
		
		String customIcon = null;
		Object[] ext = getMetaExt();
		if (ext != null) {
			customIcon = StringUtils.defaultIfBlank((String) ext[2], "texture");
		}
		return StringUtils.defaultIfBlank(customIcon, defIcon);
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
//				return null;
				return JSON.parseObject("{}");
			}
			return JSON.parseObject(StringUtils.defaultIfBlank((String) ext[3], "{}"));
		}
		return null;
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
	 * @return
	 */
	private boolean isField() {
		return baseMeta instanceof Field;
	}
	
	// --
	
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
	 * @param meta
	 * @return
	 */
	public static boolean isBuiltin(BaseMeta meta) {
		String metaName = meta.getName();
		if (meta instanceof Entity) {
			return BUILTIN_ENTITY.contains(metaName);
		}
		
		if (((Field) meta).getType() == FieldType.PRIMARY) {
			return true;
		}
		return BUILTIN_FIELD.contains(metaName);
	}
}
