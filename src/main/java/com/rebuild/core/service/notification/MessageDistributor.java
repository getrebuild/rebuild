/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.notification;

import cn.devezhao.persist4j.engine.ID;

/**
 * 消息分发
 *
 * @author devezhao
 * @since 2021/7/20
 */
public interface MessageDistributor {

    /**
     * @param message
     * @param messageId
     * @return
     */
    boolean send(Message message, ID messageId);
}
