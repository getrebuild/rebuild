/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cn.devezhao.rebuild.server.service.entitymanage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.BaseMeta;
import cn.devezhao.rebuild.server.metadata.ExtRecordCreator;
import cn.devezhao.rebuild.server.metadata.MetadataHelper;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/13/2018
 */
public class EasyMeta implements BaseMeta {
	private static final long serialVersionUID = -6463919098111506968L;
	
	private static final Set<String> BUILTIN_ENTITY = new HashSet<>();
	private static final Set<String> BUILTIN_FIELD = new HashSet<>();
	private static final Map<String, String[]> SYSENTITY_INFO = new HashMap<>();
	static {
		BUILTIN_ENTITY.add("RolePrivileges");
		BUILTIN_ENTITY.add("RoleMember");
		BUILTIN_ENTITY.add("MetaEntity");
		BUILTIN_ENTITY.add("MetaField");
		BUILTIN_ENTITY.add("PickList");
		BUILTIN_ENTITY.add("Layout");
		
		BUILTIN_FIELD.add(ExtRecordCreator.createdOn);
		BUILTIN_FIELD.add(ExtRecordCreator.createdBy);
		BUILTIN_FIELD.add(ExtRecordCreator.modifiedOn);
		BUILTIN_FIELD.add(ExtRecordCreator.modifiedBy);
		BUILTIN_FIELD.add(ExtRecordCreator.owningUser);
		BUILTIN_FIELD.add(ExtRecordCreator.owningDept);
		
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
		if (baseMeta instanceof Field) {
			Object[] ext = getMetaExt();
			if (ext != null) {
				DisplayType dt = (DisplayType) ext[2];
				if (fullName) {
					return dt.getDisplayName() + " (" + dt.name() + ")";
				} else {
					return dt.name();
				}
			} else {
				return ((Field) baseMeta).getType().getName().toUpperCase();
			}
		}
		return null;
	}
	
	/**
	 * 对用户来说是否可见
	 * 
	 * @return
	 */
	public boolean isBuiltin() {
		if (baseMeta instanceof Entity) {
			return BUILTIN_ENTITY.contains(getName());
		} else if (baseMeta instanceof Field) {
			if (((Field) baseMeta).getType() == FieldType.PRIMARY) {
				return true;
			}
			return BUILTIN_FIELD.contains(getName());
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
			customIcon = StringUtils.defaultIfBlank((String) ext[2], "border-clear");
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
}
