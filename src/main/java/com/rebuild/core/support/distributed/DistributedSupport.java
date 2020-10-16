/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 分布式支持
 *
 * @author devezhao
 * @since 2020/9/27
 */
public class DistributedSupport implements UseRedis {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedSupport.class);

    private RedissonClient redissonClient;

    public DistributedSupport(JedisPool jedisPool) {
        refreshJedisPool(jedisPool);
    }

    @Override
    public boolean refreshJedisPool(JedisPool jedisPool) {
        if (jedisPool instanceof KnownJedisPool && testJedisPool(jedisPool)) {
            redissonClient = Redisson.create(createConfig((KnownJedisPool) jedisPool));
            LOG.info("Use distributed env : " + redissonClient);
        }
        return false;
    }

    // useSingleServer
    private Config createConfig(KnownJedisPool kjp) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(String.format("redis://%s:%d", kjp.getHost(), kjp.getPort()))
                .setPassword(kjp.getPassword());
        return config;
    }

    /**
     * @param namespace
     * @param <K>
     * @param <V>
     * @return
     */
    public <K, V> ConcurrentMap<K, V> getMap(String namespace) {
        if (redissonClient != null) {
            return redissonClient.getMap(namespace);
        } else {
            return new ConcurrentHashMap<>();
        }
    }

    /**
     * @param namespace
     * @param <T>
     * @return
     */
    public <T> List<T> getList(String namespace) {
        if (redissonClient != null) {
            return redissonClient.getList(namespace);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * @param namespace
     * @param <T>
     * @return
     */
    public <T> Set<T> getSet(String namespace) {
        if (redissonClient != null) {
            return redissonClient.getSet(namespace);
        } else {
            return new HashSet<>();
        }
    }
}
