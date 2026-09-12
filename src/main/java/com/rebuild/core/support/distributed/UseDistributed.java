/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import com.rebuild.core.Application;
import com.rebuild.core.support.Lab;
import com.rebuild.core.support.RbvFunction;

/**
 * @author devezhao
 * @since 2026/4/7
 */
@Lab
public interface UseDistributed {

    /**
     * 收到远端节点的刷新通知后，重载本节点缓存
     */
    void refresh();

    /**
     * 本节点数据变更后，广播通知其他节点刷新
     */
    default void notifyRefresh() {
        if (Application.isStateLoaded()) {
            RbvFunction.call().refreshAllNodes();
        }
    }
}
