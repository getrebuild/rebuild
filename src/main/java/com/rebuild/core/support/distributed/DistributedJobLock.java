/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import com.rebuild.core.Application;
import com.rebuild.core.support.CommandArgs;
import com.rebuild.core.support.setup.Installer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * 分布式环境下（多 RB 实例），避免一个 Job 多个实例都运行。
 * 利用 redis 加锁，因此仅启用 redis 的情况下有效。
 *
 * @author ZHAO
 * @since 2020/4/5
 * @see DistributedSupport
 */
@Slf4j
public abstract class DistributedJobLock {

    private static final String LOCK_KEY = "#RBJOBLOCK";
    private static final long LOCK_TIME = 15;  // 15s offset

    /**
     * 是否已在运行中，即并发判断（分布式环境）
     *
     * @return
     */
    protected boolean tryLock() {
        final String jobName = getClass().getSimpleName();
        if (!Application.isReady() || Application.isWaitLoad()) {
            log.info("Job [ {} ] ignored while REBUILD starting up.", jobName);
            return false;
        }

        // v4.4
        if (DistributedSupport.isDistributedEnv()) {
            String allowJobs = CommandArgs.getString(CommandArgs._DistributedAllowJobs);
            if (allowJobs != null && !allowJobs.contains(jobName)) {
                log.info("The job [ {} ] is not allowed to execute on this node : {}",
                        jobName, DistributedSupport.getNodeName());
                return false;
            }
        }

        if (Installer.isUseRedis()) {
            JedisPool pool = Application.getCommonsCache().getJedisPool();
            try (Jedis jedis = pool.getResource()) {
                String tryLock = jedis.set(jobName + LOCK_KEY, LOCK_KEY, SetParams.setParams().nx().ex(LOCK_TIME));
                if (tryLock == null) {
                    log.warn("The job [ {} ] has been executed by another instance", jobName);
                    return false;
                }
            }
        }

        log.info("The job [ {} ] will be executed safely ...", jobName);
        return true;
    }

    // --

    /**
     * 获取当前执行 JOB 线程数量
     *
     * @return
     */
    public static int getActiveCount() {
        try {
            ThreadPoolTaskScheduler taskScheduler = (ThreadPoolTaskScheduler) Application.getContext().getBean("taskScheduler");
            ScheduledThreadPoolExecutor poolExecutor = taskScheduler.getScheduledThreadPoolExecutor();
            return poolExecutor.getActiveCount();
        } catch (BeansException ex) {
            log.error(null, ex);
            return -1;
        }
    }
}
