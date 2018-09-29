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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.bizz.security.EntityQueryFilter;
import cn.devezhao.bizz.security.member.User;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Filter;

/**
 * 查询过滤器
 * 
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-21
 */
public class QueryFilter extends EntityQueryFilter implements Filter {
	private static final long serialVersionUID = -7388577069739389698L;

	/**
	 * 总是拒绝
	 */
	public static final QueryFilter DENIED = new QueryFilter() {
		private static final long serialVersionUID = 13232323232L;
		public String evaluate(int entity) {
			return "( 1 = 0 )";
		}
		public String evaluate(Entity entity) {
			return "( 1 = 0 )";
		}
	};
	
	/**
	 * 总是允许
	 */
	public static final QueryFilter ALLOWED = new QueryFilter() {
		private static final long serialVersionUID = 165789635786543L;
		public String evaluate(int entity) {
			return "( 1 = 1 )";
		}
		public String evaluate(Entity entity) {
			return "( 1 = 1 )";
		}
	};
	
	private QueryFilter() {
		super(null, null);
	}
	
	// -----------------------------------------------------------------------------------
	
	private static final Log LOG = LogFactory.getLog(QueryFilter.class);
	
	/**
	 * @param user
	 */
	public QueryFilter(User user) {
		super(user);
	}
	
	/**
	 * @see #evaluate(int)
	 */
	public String evaluate(Entity entity) {
		if (!entity.containsField(EntityHelper.owningUser)) {
			if (LOG.isDebugEnabled()) {
				LOG.warn("No privilege field supported for QueryFilter! Entity: " + entity.getName());
			}
			return ALLOWED.evaluate(entity.getEntityCode());
		}
		return evaluate(entity.getEntityCode());
	}
	
	@Override
	protected String evaluate(int entityCode, StringBuffer filtered) {
		Entity entity = MetadataHelper.getEntity(entityCode);
		if (!entity.containsField(EntityHelper.owningUser)) {
			if (LOG.isDebugEnabled()) {
				LOG.warn("No privilege field supported for QueryFilter! Entity: " + entity.getName());
			}
			return ALLOWED.evaluate(entity);
//		} else if (entityCode == EntityHelper.User) {
//			filtered.insert(filtered.lastIndexOf(")"), "or userId = '" + user.getIdentity() + "' ");
		}
		return super.evaluate(entityCode, filtered);
	}
}
