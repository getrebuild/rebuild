/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.task;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.support.distributed.DistributedJobLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    // 线程池
    private static final ExecutorService EXEC = new ThreadPoolExecutor(
            MAX_TASKS_NUMBER, MAX_TASKS_NUMBER, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(MAX_TASKS_NUMBER * 6));

    // 异步任务
    private static final Map<String, HeavyTask<?>> ASYNC_TASKS = new ConcurrentHashMap<>();

    // 队列执行
    private static final ExecutorService SINGLE_QUEUE = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

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
        EXEC.execute(task);
        ASYNC_TASKS.put(taskid, task);
        return taskid;
    }

    /**
     * 取消执行
     *
     * @param taskid
     */
    public static boolean cancel(String taskid) {
        HeavyTask<?> task = ASYNC_TASKS.get(taskid);
        if (task == null) {
            log.warn("No task found : {}", taskid);
            return false;
        }

        task.setInterruptState();

        boolean interrupted = false;
        for (int i = 1; i <= 4; i++) {
            ThreadPool.waitFor(i * 500);

            if (task.isInterruptState()) {
                interrupted = true;
                break;
            }
        }
        return interrupted;
    }

    /**
     * 获取任务
     *
     * @param taskid
     * @return
     */
    public static HeavyTask<?> get(String taskid) {
        return ASYNC_TASKS.get(taskid);
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
     * 排队执行（单线程）
     *
     * @param command
     */
    public static void queue(Runnable command) {
        SINGLE_QUEUE.execute(command);
    }

    /**
     * 停止任务执行器
     */
    public static void shutdown() {
        List<Runnable> t = EXEC.shutdownNow();
        if (!t.isEmpty()) {
            log.warn("{} task(s) were interrupted", t.size());
        }

        List<Runnable> c = SINGLE_QUEUE.shutdownNow();
        if (!c.isEmpty()) {
            log.warn("{} command(s) were interrupted", c.size());
        }
    }

    // --

    @Scheduled(fixedRate = 300000, initialDelay = 300000)
    public void executeJob() {
        if (!tryLock()) return;

        if (!ASYNC_TASKS.isEmpty()) {
            int completed = 0;
            for (Map.Entry<String, HeavyTask<?>> e : ASYNC_TASKS.entrySet()) {
                HeavyTask<?> task = e.getValue();
                if (task.getCompletedTime() == null || !task.isCompleted()) {
                    continue;
                }

                long leftTime = (System.currentTimeMillis() - task.getCompletedTime().getTime()) / 1000;
                if (leftTime > 60 * 120) {
                    ASYNC_TASKS.remove(e.getKey());
                    log.info("HeavyTask clean-up : {}", e.getKey());
                }
                completed++;
            }
            log.info("{} task(s) in the queue. {} are completed (will clean-up later)", ASYNC_TASKS.size(), completed);
        }
        
        Queue<Runnable> queue = ((ThreadPoolExecutor) SINGLE_QUEUE).getQueue();
        if (!queue.isEmpty()) {
            log.info("{} command(s) in the single-queue", queue.size());
        }
    }
}
