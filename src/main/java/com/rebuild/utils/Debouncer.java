/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 防抖任务：多次调用 run() 后只在最后一次延迟 delayMs 毫秒后执行一次。
 * runNow() 取消待执行任务并立即执行。cancel() 仅取消不执行。
 *
 * @author devezhao
 */
@Slf4j
public class Debouncer {

    private static final ScheduledExecutorService SHARED_EXECUTOR = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "debouncer-shared");
        t.setDaemon(true);
        return t;
    });

    private final long delayMs;
    private final Runnable task;
    private final ScheduledExecutorService executor;
    private volatile ScheduledFuture<?> pending;

    public Debouncer(Runnable task, long delayMs) {
        this(task, delayMs, SHARED_EXECUTOR);
    }

    public Debouncer(Runnable task, long delayMs, ScheduledExecutorService executor) {
        this.task = task;
        this.delayMs = delayMs;
        this.executor = executor;
    }

    /**
     * 防抖执行：取消上一个待执行任务，重新计时
     */
    public synchronized void run() {
        if (pending != null) {
            pending.cancel(false);
        }
        pending = executor.schedule(() -> {
            synchronized (Debouncer.this) {
                pending = null;
                try {
                    task.run();
                } catch (Throwable ex) {
                    log.error("Debounced task failed", ex);
                }
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 立即执行（取消待执行任务）
     */
    public synchronized void runNow() {
        if (pending != null) {
            pending.cancel(false);
            pending = null;
        }
        task.run();
    }

    /**
     * 取消待执行任务（不执行）
     */
    public synchronized void cancel() {
        if (pending != null) {
            pending.cancel(false);
            pending = null;
        }
    }
}
