/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper;

import com.rebuild.server.Application;
import com.rebuild.server.helper.cache.JedisCacheDriver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 分布式环境下（多 RB 实例），避免一个 Job 多个实例都运行。
 * 利用 redis 加锁，因此仅启用 redis 的情况下有效。
 *
 * @author ZHAO
 * @since 2020/4/5
 */
public abstract class DistributedJobBean extends QuartzJobBean {

    private final Log LOG = LogFactory.getLog(this.getClass());

    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "EX";

    private static final String LOCK_KEY = "#JOBLOCK";
    private static final int LOCK_OFFSET_TIME = 10;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (Application.getCommonCache().isUseRedis()) {
            @SuppressWarnings("rawtypes")
            JedisPool pool = ((JedisCacheDriver) Application.getCommonCache().getCacheTemplate()).getJedisPool();
            String jobKey = getClass().getName() + LOCK_KEY;

            try (Jedis jedis = pool.getResource()) {
                String tryLock = jedis.set(jobKey, LOCK_KEY, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, LOCK_OFFSET_TIME);
                if (tryLock == null) {
                    LOG.info("The job has been executed by another instance");
                    return;
                }
            }
        }

        this.executeInternalSafe(jobExecutionContext);
    }

    /**
     * @param jobExecutionContext
     * @throws JobExecutionException
     */
    abstract protected void executeInternalSafe(JobExecutionContext jobExecutionContext) throws JobExecutionException;

}
