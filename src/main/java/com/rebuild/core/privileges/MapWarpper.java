/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import com.rebuild.core.Application;
import com.rebuild.core.support.distributed.DistributedSupport;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Zixin (RB)
 * @since 4/6/2026
 *
 * @see com.rebuild.rbv.core.support.DistributedSupportImpl#getMap(String)
 */
public class MapWarpper<K, V> implements Map<K, V> {

    final private String namespace;
    final private Map<K, V> map;

    @Setter
    private boolean autoSync = false;

    public MapWarpper(String namespace, Map<K, V> map) {
        this.namespace = DistributedSupport.KEY_PREFIX + namespace;
        this.map = map;
    }

    /**
     * 获取原始 Map
     *
     * @return
     */
    protected Map<K, V> getMap() {
        return map;
    }

    /**
     * 手动同步
     */
    public void sync() {
        if (DistributedSupport.instance().isDistributedEnv()) {
            Application.getCommonsCache().putx(namespace, (Serializable) map);
        }
        // 非分布式环境
    }

    // --

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public @Nullable V put(K key, V value) {
        V v = map.put(key, value);
        if (autoSync) this.sync();
        return v;
    }

    @Override
    public V remove(Object key) {
        V v = map.remove(key);
        if (autoSync) this.sync();
        return v;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        map.putAll(m);
        if (autoSync) this.sync();
    }

    @Override
    public void clear() {
        map.clear();
        if (autoSync) this.sync();
    }

    @Override
    public @NotNull Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public @NotNull Collection<V> values() {
        return map.values();
    }

    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    // --

    /**
     * @param map
     */
    public static void enableAutoSync(Map<?, ?> map) {
        ((MapWarpper<?, ?>) map).setAutoSync(true);
        ((MapWarpper<?, ?>) map).sync();
    }
}
