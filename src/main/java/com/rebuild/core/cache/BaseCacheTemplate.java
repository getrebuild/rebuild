/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.cache;

import com.rebuild.core.support.distributed.UseRedis;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;
import redis.clients.jedis.JedisPool;

import java.io.Serializable;

/**
 * 缓存模板
 *
 * @author devezhao
 * @since 01/02/2019
 */
@Slf4j
public abstract class BaseCacheTemplate<V extends Serializable> implements CacheTemplate<V>, UseRedis {

    /**
     * 默认缓存时间（90天）
     */
    private static final int TS_DEFAULT = 60 * 60 * 24 * 90;

    private CacheTemplate<V> delegate;

    private String keyPrefix;

    /**
     * @param jedisPool
     * @param backup    The ehcache for backup
     * @param keyPrefix
     */
    protected BaseCacheTemplate(JedisPool jedisPool, CacheManager backup, String keyPrefix) {
        if (testJedisPool(jedisPool)) {
            this.delegate = new RedisDriver<>(jedisPool);
        } else {
            this.delegate = new EhcacheDriver<>(backup);
        }

        this.keyPrefix = "RB." + StringUtils.defaultIfBlank(keyPrefix, StringUtils.EMPTY);
    }

    @Override
    public boolean reinjectJedisPool(JedisPool pool) {
        if (testJedisPool(pool)) {
            this.delegate = new RedisDriver<>(pool);
            return true;
        }
        return false;
    }

    @Override
    public String get(String key) {
        return delegate.get(unityKey(key));
    }

    @Override
    public void put(String key, String value) {
        this.put(key, value, TS_DEFAULT);
    }

    @Override
    public void put(String key, String value, int seconds) {
        if (value == null) {
            log.warn("Cannot set `{}` to null", key);
            return;
        }
        delegate.put(unityKey(key), value, seconds);
    }

    @Override
    public V getx(String key) {
        return delegate.getx(unityKey(key));
    }

    @Override
    public void putx(String key, V value) {
        this.putx(key, value, TS_DEFAULT);
    }

    @Override
    public void putx(String key, V value, int seconds) {
        if (value == null) {
            log.warn("Cannot set `{}` to null", key);
            return;
        }
        delegate.putx(unityKey(key), value, seconds);
    }

    @Override
    public void evict(String key) {
        delegate.evict(unityKey(key));
    }

    /**
     * @return
     */
    public CacheTemplate<V> getCacheTemplate() {
        return delegate;
    }

    private String unityKey(String key) {
        Assert.isTrue(StringUtils.isNotBlank(key), "[key] cannot be null");
        return (keyPrefix + key).toLowerCase();
    }
}
