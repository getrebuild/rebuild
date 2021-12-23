/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

class HeavyStopWatcherTest {

    @Test
    void tests() throws InterruptedException {
        System.setProperty("_HeavyStopWatcher", "true");
        
        HeavyStopWatcher.createWatcher("tests");

        HeavyStopWatcher.start(null);
        TimeUnit.MICROSECONDS.sleep(RandomUtils.nextInt(100, 1000));
        HeavyStopWatcher.stop();

        HeavyStopWatcher.start("2");
        TimeUnit.MICROSECONDS.sleep(RandomUtils.nextInt(100, 1000));
        HeavyStopWatcher.stop();

        HeavyStopWatcher.start(null);
        TimeUnit.MICROSECONDS.sleep(RandomUtils.nextInt(100, 1000));
        HeavyStopWatcher.stop();

        // finally
        HeavyStopWatcher.clean();
    }
}