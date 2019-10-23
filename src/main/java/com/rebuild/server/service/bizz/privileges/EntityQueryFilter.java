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

package com.rebuild.server.service.bizz.privileges;

import cn.devezhao.bizz.privileges.DepthEntry;
import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.impl.BizzDepthEntry;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.QueryFilter;
import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Filter;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.Set;

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
		if (!user.isActive()) {
			return DENIED.evaluate(null);
		} else if (user.isAdmin()) {
			return ALLOWED.evaluate(null);
		}
		
		Entity useMaster = null;
		if (!EntityHelper.hasPrivilegesField(entity)) {
			// NOTE BIZZ 实体全部用户可见
			if (MetadataHelper.isBizzEntity(entity.getEntityCode())) {
				return ALLOWED.evaluate(null);
			} else if (entity.getMasterEntity() != null) {
				useMaster = entity.getMasterEntity();
			} else {
				return DENIED.evaluate(null);
			}
		}
		
		// 未配置权限的默认拒绝
		// 明细实体使用主实体权限
		Privileges ep = user.getOwningRole().getPrivileges(
				useMaster != null ? useMaster.getEntityCode() : entity.getEntityCode());
		if (ep == Privileges.NONE) {
			return DENIED.evaluate(null);
		}
		
		DepthEntry de = ep.superlative(specAction);
		if (de == BizzDepthEntry.GLOBAL) {
			return ALLOWED.evaluate(null);
		}
		
		String ownFormat = "%s = '%s'";
		Field toMasterField = null;
		if (useMaster != null) {
			toMasterField = MetadataHelper.getSlaveToMasterField(entity);
			ownFormat = toMasterField.getName() + "." + ownFormat;
		}
		
		if (de == BizzDepthEntry.PRIVATE) {
			return appendShareFilter(entity, toMasterField,
					String.format(ownFormat, EntityHelper.OwningUser, user.getIdentity()));
		}
		
		Department dept = user.getOwningDept();
		String deptSql = String.format(ownFormat, EntityHelper.OwningDept, dept.getIdentity());
		
		if (de == BizzDepthEntry.LOCAL) {
			return appendShareFilter(entity, toMasterField, deptSql);
		} else if (de == BizzDepthEntry.DEEPDOWN) {
			Set<String> sqls = new HashSet<>();
			sqls.add(deptSql);
			
			for (BusinessUnit child : dept.getAllChildren()) {
				sqls.add(String.format(ownFormat, EntityHelper.OwningDept, child.getIdentity()));
			}
			return appendShareFilter(entity, toMasterField, "(" + StringUtils.join(sqls, " or ") + ")");
		}

		return DENIED.evaluate(null);
	}
	
	/**
	 * 共享权限
	 * 
	 * @param entity
	 * @param slaveToMasterField
	 * @param filtered
	 * @return
	 */
	protected String appendShareFilter(Entity entity, Field slaveToMasterField, String filtered) {
		String shareFilter = "exists (select rights from ShareAccess where belongEntity = '%s' and shareTo = '%s' and recordId = ^%s)";
		
		// 子实体。使用主实体的共享
		if (slaveToMasterField != null) {
			shareFilter = String.format(shareFilter,
					slaveToMasterField.getOwnEntity().getMasterEntity().getName(),
					user.getId(), slaveToMasterField.getName());
		} else {
			shareFilter = String.format(shareFilter,
					entity.getName(), user.getId(), entity.getPrimaryField().getName());
		}
		
		return "(" + filtered + " or " + shareFilter + ")";
	}
}
