/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.cache.BaseCacheTemplate;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.NoRecordFoundException;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;

/**
 * 业务数据/记录所属
 *
 * @author devezhao
 * @since 10/12/2018
 */
@Service
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
        Entity useMain = null;
        if (!MetadataHelper.hasPrivilegesField(entity)) {
            useMain = entity.getMainEntity();
            if (!(useMain != null && MetadataHelper.hasPrivilegesField(useMain))) {
                throw new PrivilegesException("None privileges entity : " + entity.getName());
            }
        }

        String sql = "select owningUser from %s where %s = '%s'";
        // 使用主记录
        if (useMain != null) {
            Field dtmField = MetadataHelper.getDetailToMainField(entity);
            sql = sql.replaceFirst("owningUser", dtmField.getName() + ".owningUser");
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
