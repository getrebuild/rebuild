/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

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
 * 分布式环境下，避免一个 Job 多个运行。
 * 利用 redis 加锁，因此仅启用 redis 的情况下有效。
 *
 * @author ZHAO
 * @since 2020/4/5
 */
public abstract class DistributedJobBean extends QuartzJobBean {

    private static final Log LOG = LogFactory.getLog(DistributedJobBean.class);

    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (Application.getCommonCache().isUseRedis()) {
            @SuppressWarnings("rawtypes")
            JedisPool pool = ((JedisCacheDriver) Application.getCommonCache().getCacheTemplate()).getJedisPool();
            String jobKey = getClass().getName() + "#LOCK";

            try (Jedis jedis = pool.getResource()) {
                String tryLock = jedis.set(jobKey, "TRYLOCK", SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, 15);
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
