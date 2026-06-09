/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author devezhao
 * @since 2022/1/6
 */
@Slf4j
@SuppressWarnings("unchecked")
public class DistributedSupportLocal implements DistributedSupport {

    private final Map<String, ConcurrentMap<?, ?>> LOCAL_MAP = new ConcurrentHashMap<>();
    private final Map<String, List<?>> LOCAL_LIST = new ConcurrentHashMap<>();
    private final Map<String, Set<?>> LOCAL_SET = new ConcurrentHashMap<>();

    private final Map<String, Lock> LOCAL_LOCKS = new ConcurrentHashMap<>();

    @Override
    public Lock getLock(String namespace) {
        synchronized (LOCAL_LOCKS) {
            return LOCAL_LOCKS.computeIfAbsent(namespace, k -> new ReentrantLock());
        }
    }

    @Override
    public void unLock(Lock lock, String namespace) {
        ReentrantLock rlock = (ReentrantLock) lock;
        if (rlock.isLocked()) {
            if (rlock.isHeldByCurrentThread()) {
                rlock.unlock();
            } else {
                log.warn("Cannot unlock lock by other thread : {}", namespace);
            }
        }
    }

    @Override
    public <K, V> ConcurrentMap<K, V> getMap(String namespace) {
        return (ConcurrentMap<K, V>) LOCAL_MAP.computeIfAbsent(namespace, k -> new ConcurrentHashMap<K, V>());
    }

    @Override
    public <T> List<T> getList(String namespace) {
        return (List<T>) LOCAL_LIST.computeIfAbsent(namespace, k -> new ArrayList<T>());
    }

    @Override
    public <T> Set<T> getSet(String namespace) {
        return (Set<T>) LOCAL_SET.computeIfAbsent(namespace, k -> new HashSet<T>());
    }
}
