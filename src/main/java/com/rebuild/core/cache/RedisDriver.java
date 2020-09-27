/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.cache;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SerializationUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.Serializable;
import java.util.Objects;

/**
 * redis
 *
 * @author devezhao
 * @since 01/02/2019
 */
public class RedisDriver<V extends Serializable> implements CacheTemplate<V> {

    private JedisPool jedisPool;

    protected RedisDriver(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public String get(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return jedis.get(key);
        } finally {
            IOUtils.closeQuietly(jedis);
        }
    }

    @Override
    public void put(String key, String value) {
        put(key, value, -1);
    }

    @Override
    public void put(String key, String value, int seconds) {
        Objects.requireNonNull(value, "[value] cannot be null");

        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            if (seconds > 0) {
                jedis.setex(key, seconds, value);
            } else {
                jedis.set(key, value);
            }
        } finally {
            IOUtils.closeQuietly(jedis);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public V getx(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            byte[] bs = jedis.get(key.getBytes());
            if (bs == null || bs.length == 0) {
                return null;
            }

            Object s = SerializationUtils.deserialize(bs);
            // Check type of generic?
            return (V) s;
        } finally {
            IOUtils.closeQuietly(jedis);
        }
    }

    @Override
    public void putx(String key, V value) {
        putx(key, value, -1);
    }

    @Override
    public void putx(String key, V value, int seconds) {
        Objects.requireNonNull(value, "[value] cannot be null");

        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            byte[] bkey = key.getBytes();
            if (seconds > 0) {
                jedis.setex(bkey, seconds, SerializationUtils.serialize(value));
            } else {
                jedis.set(bkey, SerializationUtils.serialize(value));
            }
        } finally {
            IOUtils.closeQuietly(jedis);
        }
    }

    @Override
    public void evict(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            jedis.del(key);
        } finally {
            IOUtils.closeQuietly(jedis);
        }
    }

    /**
     * @return
     */
    public JedisPool getJedisPool() {
        return jedisPool;
    }
}
