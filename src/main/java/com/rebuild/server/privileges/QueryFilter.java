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

package com.rebuild.server.privileges;

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
 * @version $Id: QueryFilter.java 22 2013-06-26 12:15:01Z zhaoff@qidapp.com $
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
