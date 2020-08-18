/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper.cache;

import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import org.springframework.cache.CacheManager;
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
		super(jedisPool, cacheManager, "OU.");
		this.aPMFactory = aPMFactory;
	}
	
	/**
	 * 获取记录的所属人。若是明细实体则获取其主记录的所属人
	 * 
	 * @param record
	 * @param tryIfNotExists 记录不存在是否抛出异常
	 * @return
	 * @throws PrivilegesException
	 * @throws NoRecordFoundException
	 */
	public ID getOwningUser(ID record, boolean tryIfNotExists) throws PrivilegesException, NoRecordFoundException {
		final String recordKey = record.toLiteral();

		ID hits = getx(recordKey);
		if (hits != null) {
			return hits;
		}

		Entity entity = MetadataHelper.getEntity(record.getEntityCode());
		Entity useMaster = null;
		if (!MetadataHelper.hasPrivilegesField(entity)) {
			useMaster = entity.getMasterEntity();
			if (!(useMaster != null && MetadataHelper.hasPrivilegesField(useMaster))) {
				throw new PrivilegesException("None privileges entity : " + entity.getName());
			}
		}

		String sql = "select owningUser from %s where %s = '%s'";
		// 使用主记录
		if (useMaster != null) {
			Field stmField = MetadataHelper.getSlaveToMasterField(entity);
			sql = sql.replaceFirst("owningUser", stmField.getName() + ".owningUser");
		}
		sql = String.format(sql, entity.getName(), entity.getPrimaryField().getName(), record.toLiteral());

		Object[] owningUser = aPMFactory.createQuery(sql).unique();
		if (owningUser == null || owningUser[0] == null) {
			String error = "No Record found : " + record;
			if (tryIfNotExists) {
				throw new NoRecordFoundException(error);
			} else {
				LOG.warn(error);
				return null;
			}
		}

		putx(recordKey, (ID) owningUser[0]);
		return (ID) owningUser[0];
	}

	/**
	 * @param record
	 * @return
	 * @throws PrivilegesException
	 * @see #getOwningUser(ID, boolean)
	 */
	public ID getOwningUser(ID record) throws PrivilegesException {
		return getOwningUser(record, Boolean.FALSE);
	}

	/**
	 * @param record
	 */
	public void cleanOwningUser(ID record) {
		evict(record.toLiteral());
	}
}
