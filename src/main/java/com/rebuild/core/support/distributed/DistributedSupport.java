/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import com.rebuild.core.Application;
import com.rebuild.core.support.CommandArgs;
import com.rebuild.core.support.Lab;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

/**
 * 分布式支持
 * <p>namespace 建议使用静态字符串。实现类不会主动清理已注册的 namespace，
 * 若使用含记录 ID、时间戳等动态拼接的 namespace，会造成内存或 Redis key 累积。
 *
 * @author devezhao
 * @since 2020/9/27
 */
@Lab
public interface DistributedSupport {

    /**
     * @param namespace
     * @return
     * @param <K>
     * @param <V>
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
     * 获取资源锁
     *
     * @param namespace
     * @return
     * @see #unLock(Lock, String)
     */
    Lock getLock(String namespace);

    /**
     * 释放资源锁
     *
     * @param namespace
     */
    void unLock(Lock lock, String namespace);

    // -- TOOLS

    /**
     * 节点名称（有节点表示分布式环境）
     * @return
     */
    static String getNodeName() {
        return CommandArgs.getString(CommandArgs._DistributedNode, null);
    }

    /**
     * 是否分布式环境
     * @return
     */
    static boolean isDistributedEnv() {
        return getNodeName() != null;
    }

    /**
     * @return
     */
    static DistributedSupport instance() {
        return (DistributedSupport) Application.getContext().getBean("rbv.DistributedSupport");
    }
}
