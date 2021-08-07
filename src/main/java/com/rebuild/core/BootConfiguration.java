/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import cn.devezhao.commons.ObjectUtils;
import com.rebuild.core.support.distributed.KnownJedisPool;
import com.rebuild.core.support.setup.InstallState;
import com.rebuild.utils.CommonsUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

import java.io.IOException;

/**
 * @author devezhao
 * @since 2020/9/23
 */
@Configuration
public class BootConfiguration implements InstallState {

    /**
     * Fake instance
     */
    public static final JedisPool USE_EHCACHE = new JedisPool();

    @Bean
    JedisPool createJedisPool() {
        if (checkInstalled()) return createJedisPoolInternal();
        else return USE_EHCACHE;
    }

    @Bean
    CacheManager createCacheManager() throws IOException {
        EhCacheCacheManager manager = new EhCacheCacheManager();
        manager.setCacheManager(
                new net.sf.ehcache.CacheManager(CommonsUtils.getStreamOfRes("ehcache.xml")));
        return manager;
    }

//    @Bean
//    DistributedSupport createDistributedSupport() {
//        return new DistributedSupport(createJedisPool());
//    }

    /**
     * @return
     */
    public static JedisPool createJedisPoolInternal() {
        String use = BootEnvironmentPostProcessor.getProperty("db.CacheHost");
        if ("0".equals(use)) return USE_EHCACHE;

        return new KnownJedisPool(
                StringUtils.defaultIfBlank(use, "127.0.0.1"),
                ObjectUtils.toInt(BootEnvironmentPostProcessor.getProperty("db.CachePort"), 6379),
                BootEnvironmentPostProcessor.getProperty("db.CachePassword", null));
    }
}
