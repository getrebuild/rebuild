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

package com.rebuild.server.helper.cache;

import org.springframework.cache.CacheManager;

import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;

/**
 * 业务数据/记录所属
 * 
 * @author devezhao
 * @since 10/12/2018
 */
public class RecordOwningCache extends CacheTemplate<ID> {

	final private PersistManagerFactory aPMFactory;
	
	protected RecordOwningCache(CacheManager cacheManager, PersistManagerFactory aPMFactory) {
		super(cacheManager);
		this.aPMFactory = aPMFactory;
	}
	
	/**
	 * @param record
	 * @return
	 */
	public ID getOwningUser(ID record) {
		final String recordKey = record.toLiteral();
		
		Object hit = get(recordKey);
		if (hit != null) {
			return (ID) hit;
		}
		
		Entity entity = MetadataHelper.getEntity(record.getEntityCode());
		if (!EntityHelper.hasPrivilegesField(entity)) {
			throw new PrivilegesException("No has privileges : " + entity.getName());
		}
		
		String sql = "select %s from %s where %s = '%s'";
		sql = String.format(sql, EntityHelper.owningUser, entity.getName(), entity.getPrimaryField().getName(), record.toLiteral());
		Object[] own = aPMFactory.createQuery(sql).unique();
		if (own == null) {
			return null;
		}
		
		ID ownUser = (ID) own[0];
		put(recordKey, ownUser);
		return ownUser;
	}
	
	@Override
	protected String unityKey(Object key) {
		return "OU__" + super.unityKey(key);
	}
}
