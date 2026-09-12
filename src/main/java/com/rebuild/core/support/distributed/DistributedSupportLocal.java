/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author devezhao
 * @since 2022/1/6
 */
@Slf4j
@SuppressWarnings("unchecked")
public class DistributedSupportLocal implements DistributedSupport {

    private final Map<String, ConcurrentMap<?, ?>> localMap = new ConcurrentHashMap<>();
    private final Map<String, List<?>> localList = new ConcurrentHashMap<>();
    private final Map<String, Set<?>> localSet = new ConcurrentHashMap<>();
    private final Map<String, Lock> localLocks = new ConcurrentHashMap<>();

    @Override
    public Lock getLock(String namespace) {
        Assert.notNull(namespace, "[namespace] must not be null");
        return localLocks.computeIfAbsent(namespace, k -> new ReentrantLock());
    }

    @Override
    public void unLock(Lock lock, String namespace) {
        ReentrantLock rlock = (ReentrantLock) lock;
        if (rlock.isHeldByCurrentThread()) {
            rlock.unlock();
        } else {
            log.warn("Cannot unlock lock by other thread : {}", namespace);
        }
    }

    @Override
    public <K, V> ConcurrentMap<K, V> getMap(String namespace) {
        Assert.notNull(namespace, "[namespace] must not be null");
        return (ConcurrentMap<K, V>) localMap.computeIfAbsent(namespace, k -> new ConcurrentHashMap<K, V>());
    }

    @Override
    public <T> List<T> getList(String namespace) {
        Assert.notNull(namespace, "[namespace] must not be null");
        return (List<T>) localList.computeIfAbsent(namespace, k -> new CopyOnWriteArrayList<T>());
    }

    @Override
    public <T> Set<T> getSet(String namespace) {
        Assert.notNull(namespace, "[namespace] must not be null");
        return (Set<T>) localSet.computeIfAbsent(namespace, k -> ConcurrentHashMap.newKeySet());
    }
}
