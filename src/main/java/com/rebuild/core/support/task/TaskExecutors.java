/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.task;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.RebuildException;
import com.rebuild.core.support.distributed.DistributedJobLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 任务执行调度/管理
 *
 * @author devezhao
 * @see org.springframework.core.task.SyncTaskExecutor
 * @see org.springframework.core.task.AsyncTaskExecutor
 * @since 09/29/2018
 */
@Slf4j
@Component
public class TaskExecutors extends DistributedJobLock {

    private static final int MAX_TASKS_NUMBER = Integer.max(Runtime.getRuntime().availableProcessors() / 2, 2);

    private static final ExecutorService EXECS = new ThreadPoolExecutor(
            MAX_TASKS_NUMBER, MAX_TASKS_NUMBER, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(MAX_TASKS_NUMBER * 6));

    private static final Map<String, HeavyTask<?>> TASKS = new ConcurrentHashMap<>();

    /**
     * 异步执行（提交给任务调度）
     *
     * @param task
     * @param execUser 执行用户。因为是在线程中执行，所以必须指定
     * @return 任务 ID，可通过任务ID获取任务对象，或取消任务
     */
    public static String submit(HeavyTask<?> task, ID execUser) {
        String taskid = task.getClass().getSimpleName() + "-" + CodecUtils.randomCode(20);
        task.setUser(execUser);
        EXECS.execute(task);
        TASKS.put(taskid, task);
        return taskid;
    }

    /**
     * 取消执行
     *
     * @param taskid
     */
    public static boolean cancel(String taskid) {
        HeavyTask<?> task = TASKS.get(taskid);
        if (task == null) {
            throw new RebuildException("No Task found : " + taskid);
        }
        task.interrupt();

        boolean interrupted = false;
        for (int i = 1; i <= 3; i++) {
            ThreadPool.waitFor(i * 500);
            if (task.isInterrupted()) {
                interrupted = true;
                break;
            }
        }
        return interrupted;
    }

    /**
     * @param taskid
     * @return
     */
    public static HeavyTask<?> get(String taskid) {
        return TASKS.get(taskid);
    }

    /**
     * 同步执行
     *
     * @param task
     */
    public static void run(HeavyTask<?> task) {
        task.run();
    }

    /**
     * 停止任务执行器
     */
    public static void shutdown() {
        List<Runnable> runs = EXECS.shutdownNow();
        if (!runs.isEmpty()) {
            log.warn("{} task(s) were interrupted", runs.size());
        }
    }

    // --

    @Scheduled(fixedRate = 300000, initialDelay = 300000)
    public void executeJob() {
        if (TASKS.isEmpty() || !tryLock()) return;

        log.info("{} task(s) in the queue", TASKS.size());

        for (Map.Entry<String, HeavyTask<?>> e : TASKS.entrySet()) {
            HeavyTask<?> task = e.getValue();
            if (task.getCompletedTime() == null || !task.isCompleted()) {
                continue;
            }

            long leftTime = (System.currentTimeMillis() - task.getCompletedTime().getTime()) / 1000;
            if (leftTime > 60 * 120) {
                TASKS.remove(e.getKey());
                log.info("HeavyTask self-destroying : " + e.getKey());
            }
        }
    }
}
