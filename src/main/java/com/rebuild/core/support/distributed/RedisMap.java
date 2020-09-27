/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Distributed Map use Redis
 *
 * @author devezhao
 * @since 2020/9/27
 */
public class RedisMap<T> implements Map<String, T> {

    private JedisPool jedisPool;
    private String namespace;

    private Class<T> clazz;
    private Gson gson;

    public RedisMap(JedisPool jedisPool, String keyWithNamespace) {
        this.jedisPool = jedisPool;
        this.namespace = keyWithNamespace;

        Type type = getClass().getGenericSuperclass();
        Type trueType = ((ParameterizedType) type).getActualTypeArguments()[0];
        this.clazz = (Class<T>) trueType;
        this.gson = new Gson();
    }

    @Override
    public int size() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hgetAll(namespace).size();
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hexists(namespace, key.toString());
        }
    }

    @Override
    public boolean containsValue(Object value) {
        String val2str = gson.toJson(value);
        Map<String, String> map = createRedisMap();
        return map.containsValue(val2str);
    }

    @Override
    public T get(Object key) {
        try (Jedis jedis = jedisPool.getResource()) {
            String val2str = jedis.hget(namespace, key.toString());
            if (StringUtils.isNotBlank(val2str)) {
                return gson.fromJson(val2str, clazz);
            }
        }
        return null;
    }

    @Override
    public T put(String key, T value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(namespace, key, gson.toJson(value));
        }
        return value;
    }

    @Override
    public T remove(Object key) {
        T value = get(key);
        if (value != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.hdel(namespace, key.toString());
            }
        }
        return value;
    }

    @Override
    public void putAll(Map<? extends String, ? extends T> map) {
        for (String key : map.keySet()) {
            T value = map.get(key);
            if (value != null) {
                put(key, value);
            }
        }
    }

    @Override
    public void clear() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hdel(namespace);
        }
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return createRedisMap().keySet();
    }
    
    @NotNull
    @Override
    public Collection<T> values() {
        return createHashMap().values();
    }

    @NotNull
    @Override
    public Set<java.util.Map.Entry<String, T>> entrySet() {
        return createHashMap().entrySet();
    }

    private Map<String, String> createRedisMap() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hgetAll(namespace);
        }
    }

    private Map<String, T> createHashMap() {
        Map<String, T> values = new HashMap<>();
        Map<String, String> redisMap = createRedisMap();

        for (String key : redisMap.keySet()) {
            values.put(key, gson.fromJson(redisMap.get(key), clazz));
        }
        return values;
    }

    @Override
    public String toString() {
        return "RedisMap at " + namespace;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(namespace);
    }

    @SuppressWarnings("RedundantClassCall")
    @Override
    public boolean equals(Object m) {
        if (m == this) {
            return true;
        }

        if (RedisMap.class.isInstance(m)) {
            RedisMap<?> other = RedisMap.class.cast(m);
            return Objects.equals(other.namespace, namespace);
        }
        return false;
    }
}
