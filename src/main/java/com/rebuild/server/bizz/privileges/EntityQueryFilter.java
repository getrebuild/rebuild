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

package com.rebuild.server.bizz.privileges;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.bizz.privileges.DepthEntry;
import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.impl.BizzDepthEntry;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.QueryFilter;
import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Filter;

/**
 * 查询过滤器
 * 
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-21
 */
public class EntityQueryFilter implements Filter, QueryFilter {
	private static final long serialVersionUID = -7388577069739389698L;

	/**
	 * 总是拒绝 */
	public static final Filter DENIED = new EntityQueryFilter() {
		private static final long serialVersionUID = -1841438304452108874L;
		public String evaluate(Entity entity) {
			return "( 1 = 0 )";
		}
	};
	
	/**
	 * 总是允许 */
	public static final Filter ALLOWED = new EntityQueryFilter() {
		private static final long serialVersionUID = -1300184338130890817L;
		public String evaluate(Entity entity) {
			return "( 1 = 1 )";
		}
	};
	
	private EntityQueryFilter() {
		this.user = null;
		this.specAction = null;
	}
	
	// --
	
	final private User user;
	final private Permission specAction;
	
	protected EntityQueryFilter(User user) {
		this(user, BizzPermission.READ);
	}
	
	protected EntityQueryFilter(User user, Permission specAction) {
		this.user = user;
		this.specAction = specAction;
	}
	
	@Override
	public String evaluate(int entity) {
		return evaluate(MetadataHelper.getEntity(entity));
	}

	@Override
	public String evaluate(Entity entity) {
		if (!EntityHelper.hasPrivilegesField(entity)) {
			if (SecurityManager.isBizz(entity.getEntityCode())) {
				return ALLOWED.evaluate(null);
			} else {
				return DENIED.evaluate(null);
			}
		}
		
		Privileges p = user.getOwningRole().getPrivileges(entity.getEntityCode());
		if (p == BizzPermission.NONE) {
			return DENIED.evaluate(null);
		}
		
		DepthEntry de = p.superlative(specAction);
		if (de == BizzDepthEntry.GLOBAL) {
			return ALLOWED.evaluate(null);
		}
		
		String fvFormat = "%s = '%s'";
		
		if (de == BizzDepthEntry.PRIVATE) {
			return appendShareFilter(entity, 
					String.format(fvFormat, EntityHelper.owningUser, user.getIdentity()));
		}
		
		Department dept = user.getOwningDept();
		String deptSql = String.format(fvFormat, EntityHelper.owningDept, dept.getIdentity());
		
		if (de == BizzDepthEntry.LOCAL) {
			return appendShareFilter(entity, deptSql);
		}
		
		if (de == BizzDepthEntry.DEEPDOWN) {
			Set<String> sqls = new HashSet<>();
			sqls.add(deptSql);
			
			for (BusinessUnit child : dept.getAllChildren()) {
				sqls.add(String.format(fvFormat, EntityHelper.owningDept, child.getIdentity()));
			}
			return appendShareFilter(entity, "(" + StringUtils.join(sqls, " or ") + ")");
		}

		return DENIED.evaluate(null);
	}
	
	/**
	 * TODO 共享权限
	 * 
	 * @param entity
	 * @param filtered
	 * @return
	 */
	protected String appendShareFilter(Entity entity, String filtered) {
		String shareFilter = "exists (select rights from ShareAccess where entity = %d and shareTo = '%s' and recordId = ^%s)";
		shareFilter = String.format(shareFilter,
				entity.getEntityCode(), user.getIdentity().toString(), entity.getPrimaryField().getName());
		return "(" + filtered + " or " + shareFilter + ")";
	}
}
