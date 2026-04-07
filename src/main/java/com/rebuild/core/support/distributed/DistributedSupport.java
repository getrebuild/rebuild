/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import com.rebuild.core.Application;
import com.rebuild.core.support.CommandArgs;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * 分布式支持
 *
 * @author devezhao
 * @since 2020/9/27
 */
public interface DistributedSupport {

    String KEY_PREFIX = "DISTR44:";

    /**
     * @param namespace
     * @param <K>
     * @param <V>
     * @return
     */
    <K, V> ConcurrentMap<K, V> getMap(String namespace);

    /**
     * @param namespace
     * @param <T>
     * @return
     */
    <T> List<T> getList(String namespace);

    /**
     * @param namespace
     * @param <T>
     * @return
     */
    <T> Set<T> getSet(String namespace);

    /**
     * @return
     */
    boolean isDistributedEnv();

    // -- TOOLS

    /**
     * 节点名称
     *
     * @return
     */
    static String getNodeName() {
        return CommandArgs.getString(CommandArgs._UseDistributedNode, null);
    }

    /**
     * @return
     */
    static DistributedSupport instance() {
        return (DistributedSupport) Application.getContext().getBean("rbv.DistributedSupport");
    }
}
