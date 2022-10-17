/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.task;

import cn.devezhao.commons.ThreadPool;
import org.junit.jupiter.api.Test;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author devezhao
 * @since 04/08/2022
 */
class TaskExecutorsTest {

    @Test
    void queue() {
        AtomicInteger idx = new AtomicInteger(0);
        for (int i = 0; i < 10; i++) {
            TaskExecutors.queue(() -> {
                ThreadPool.waitFor(500);
                System.out.println("command " + idx.incrementAndGet());
            });
        }
    }

    @Test
    void throttled() {
        Throttled throttled = new Throttled(500);

        // print `99`
        for (int i = 0; i < 100; i++) {
            final int use = i;
            throttled.submit(new TimerTask() {
                @Override
                public void run() {
                    System.out.println(use);
                }
            });
        }

        ThreadPool.waitFor(500);

        // print `last`
        throttled.submit(new TimerTask() {
            @Override
            public void run() {
                System.out.println("last");
            }
        });

        ThreadPool.waitFor(1000);
    }
}