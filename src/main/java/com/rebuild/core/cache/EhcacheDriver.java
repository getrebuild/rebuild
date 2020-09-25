/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.cache;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Objects;

/**
 * ehcache
 *
 * @author devezhao
 * @since 01/02/2019
 */
public class EhcacheDriver<V extends Serializable> implements CacheTemplate<V> {

    private CacheManager ehcacheManager;

    protected EhcacheDriver(CacheManager ehcacheManager) {
        this.ehcacheManager = ehcacheManager;
    }

    @Override
    public String get(String key) {
        ValueWrapper w = cache().get(key);
        return w == null ? null : (String) w.get();
    }

    @Override
    public void put(String key, String value) {
        put(key, value, -1);
    }

    @Override
    public void put(String key, String value, int seconds) {
        Objects.requireNonNull(value, "[value] cannot be null");

        Element el = new Element(key, value);
        if (seconds > -1) {
            el.setTimeToLive(seconds);
        }
        ((Ehcache) cache().getNativeCache()).put(el);
    }

    @SuppressWarnings("unchecked")
    @Override
    public V getx(String key) {
        ValueWrapper w = cache().get(key);
        return w == null ? null : (V) w.get();
    }

    @Override
    public void putx(String key, V value) {
        putx(key, value, -1);
    }

    @Override
    public void putx(String key, V value, int seconds) {
        Objects.requireNonNull(value, "[value] cannot be null");

        Element el = new Element(key, value);
        if (seconds > -1) {
            el.setTimeToLive(seconds);
        }
        ((Ehcache) cache().getNativeCache()).put(el);
    }

    @Override
    public void evict(String key) {
        cache().evict(key);
    }

    /**
     * @return
     */
    public Cache cache() {
        Cache rebuild = ehcacheManager.getCache("rebuild");
        Assert.notNull(rebuild, "No cache `rebuild` defined in ehcache.xml");
        return rebuild;
    }
}
