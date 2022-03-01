/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author devezhao
 * @since 2022/1/6
 */
@SuppressWarnings("unchecked")
public class DistributedSupportLocal implements DistributedSupport {

    private final Map<String, ConcurrentMap<?, ?>> LOCAL_MAP = new ConcurrentHashMap<>();
    private final Map<String, List<?>> LOCAL_LIST = new ConcurrentHashMap<>();
    private final Map<String, Set<?>> LOCAL_SET = new ConcurrentHashMap<>();

    public DistributedSupportLocal() {
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
