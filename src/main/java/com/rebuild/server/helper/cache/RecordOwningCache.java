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

package com.rebuild.server.helper.cache;

import org.springframework.cache.CacheManager;

import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import redis.clients.jedis.JedisPool;

/**
 * 业务数据/记录所属
 * 
 * @author devezhao
 * @since 10/12/2018
 */
public class RecordOwningCache extends BaseCacheTemplate<ID> {

	final private PersistManagerFactory aPMFactory;
	
	protected RecordOwningCache(JedisPool jedisPool, CacheManager cacheManager, PersistManagerFactory aPMFactory) {
		super(jedisPool, cacheManager, "rb.ou.");
		this.aPMFactory = aPMFactory;
	}
	
	/**
	 * 获取记录的所属人。若是明细实体则获取其主记录的所属人
	 * 
	 * @param record
	 * @return
	 * @throws PrivilegesException
	 * @throws NoRecordFoundException
	 */
	public ID getOwningUser(ID record) throws PrivilegesException, NoRecordFoundException {
		final String recordKey = record.toLiteral();
		
		ID hits = getx(recordKey);
		if (hits != null) {
			return hits;
		}
		
		Entity entity = MetadataHelper.getEntity(record.getEntityCode());
		Entity useMaster = null;
		if (!EntityHelper.hasPrivilegesField(entity)) {
			useMaster = entity.getMasterEntity();
			if (useMaster != null && EntityHelper.hasPrivilegesField(useMaster)) {
			} else {
				throw new PrivilegesException("Non privileges entity : " + entity.getName());
			}
		}
		
		String sql = "select owningUser from %s where %s = '%s'";
		// 使用主记录
		if (useMaster != null) {
			Field stm = MetadataHelper.getSlaveToMasterField(entity);
			sql = sql.replaceFirst("owningUser", stm.getName() + ".owningUser");
		}
		sql = String.format(sql, entity.getName(), entity.getPrimaryField().getName(), record.toLiteral());
		
		Object[] owning = aPMFactory.createQuery(sql).unique();
		if (owning == null) {
			throw new NoRecordFoundException("No record found : " + record);
		}
		
		ID owningUser = (ID) owning[0];
		putx(recordKey, owningUser);
		return owningUser;
	}
	
	/**
	 * @param record
	 */
	public void cleanOwningUser(ID record) {
		evict(record.toLiteral());
	}
}
