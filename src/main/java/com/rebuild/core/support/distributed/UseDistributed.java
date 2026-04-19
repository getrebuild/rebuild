/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import com.rebuild.rbv.core.support.distributed.MasterNodeClient;

/**
 * 标记接口. 支持分布式
 *
 * @author devezhao
 * @since 2026/4/7
 */
public interface UseDistributed {

    /**
     * 通知刷新
     *
     * @return
     */
    Object refresh();

    /**
     * 数据改变
     */
    default void datasChanged() {
        MasterNodeClient.refreshAllNodes();
    }
}
