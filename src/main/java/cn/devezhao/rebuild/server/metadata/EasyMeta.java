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

package cn.devezhao.rebuild.server.metadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
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
	private static final Map<String, String[]> SYSENTITY_INFO = new HashMap<>();
	static {
		BUILTIN_ENTITY.add("Role");
		BUILTIN_ENTITY.add("RolePrivileges");
		BUILTIN_ENTITY.add("RoleMember");
		BUILTIN_ENTITY.add("MetaEntity");
		BUILTIN_ENTITY.add("MetaField");
		
		SYSENTITY_INFO.put("User", new String[] { "account", "系统内建" });
		SYSENTITY_INFO.put("Department", new String[] { "accounts", "系统内建" });
	}

	private BaseMeta baseMeta;
	
	public EasyMeta(BaseMeta baseMeta) {
		this.baseMeta = baseMeta;
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
		return StringUtils.defaultIfBlank(getDescription(), getName().toUpperCase());
	}
	
	/**
	 * 取代 persist4j 中的 description，而 persist4j 中的 description 则表示 label
	 * 
	 * @return
	 */
	public String getComments() {
		// TODO
		String ii[] = SYSENTITY_INFO.get(getName());
		return ii == null ? null : ii[1];
	}
	
	/**
	 * 显示类型
	 * 
	 * @return
	 */
	public String getDisplayType() {
		if (baseMeta instanceof Field) {
			return ((Field) baseMeta).getType().getName().toUpperCase();
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
			return BUILTIN_FIELD.contains(getName());
		}
		return false;
	}
	
	/**
	 * @return
	 */
	public String getIcon() {
		String ii[] = SYSENTITY_INFO.get(getName());
		return ii == null ? "border-clear" : ii[0];
	}
}
