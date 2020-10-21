/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import com.rebuild.core.support.task.TaskExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * 关闭服务时清理
 *
 * @author devezhao
 * @since 2020/10/21
 */
@Component
public class BootShutdown implements ApplicationListener<ContextClosedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(BootShutdown.class);

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        LOG.warn("Rebuild shutting down ...");

        TaskExecutors.shutdown();
    }
}
