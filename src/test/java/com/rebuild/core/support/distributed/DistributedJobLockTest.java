/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import cn.devezhao.commons.ThreadPool;
import com.rebuild.TestSupport;
import com.rebuild.core.support.setup.Installer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author devezhao
 * @since 2021/02/01
 */
class DistributedJobLockTest extends TestSupport {

    private AtomicInteger COUNT = new AtomicInteger(0);

    @Test
    void tryLock() {
        if (!Installer.isUseRedis()) {
            System.out.println("Not #isUseRedis");
            return;
        }

        for (int i = 0; i < 10; i++) {
            ThreadPool.exec(new UseJobLock());
        }

        ThreadPool.waitFor(200);
        Assertions.assertEquals(COUNT.get(), 1);
    }

    class UseJobLock extends DistributedJobLock implements Runnable {
        @Override
        public void run() {
            boolean tryLock = tryLock();
            if (tryLock) COUNT.incrementAndGet();
            System.out.println("#tryLock : " + tryLock);
        }
    }
}