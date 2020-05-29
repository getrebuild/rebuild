/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper;

import com.rebuild.server.Application;
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

    protected final Log LOG = LogFactory.getLog(getClass());

    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "EX";

    private static final String LOCK_KEY = "#RBJOBLOCK";
    private static final int LOCK_TIME = 1000;

    protected JobExecutionContext jobExecutionContext;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (isSafe()) {
            this.jobExecutionContext = jobExecutionContext;
            this.executeInternalSafe();
        }
    }

    /**
     * 是否可安全运行，即并发判断
     * @return
     */
    protected boolean isSafe() {
        if (Application.rbvMode() && Application.getCommonCache().isUseRedis()) {
            JedisPool pool = Application.getCommonCache().getJedisPool();
            String jobKey = getClass().getName() + LOCK_KEY;

            try (Jedis jedis = pool.getResource()) {
                String tryLock = jedis.set(jobKey, LOCK_KEY, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, LOCK_TIME);
                if (tryLock == null) {
                    LOG.info("The job has been executed by another instance");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 执行 Job
     * @throws JobExecutionException
     */
    abstract protected void executeInternalSafe() throws JobExecutionException;

}
