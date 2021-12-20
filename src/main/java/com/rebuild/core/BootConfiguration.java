/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.xml.XMLHelper;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.distributed.KnownJedisPool;
import com.rebuild.core.support.setup.InstallState;
import com.rebuild.utils.CommonsUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.springframework.cache.CacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author devezhao
 * @since 2020/9/23
 */
@Configuration
public class BootConfiguration implements InstallState {

    /**
     * Fake instance
     * FIXME 直接 `==` 比较不安全 ???
     */
    public static final JedisPool USE_EHCACHE = new JedisPool();

    @Bean
    JedisPool createJedisPool() {
        if (checkInstalled()) return createJedisPoolInternal();
        else return USE_EHCACHE;
    }

    @Bean
    CacheManager createCacheManager() throws IOException {
        net.sf.ehcache.CacheManager cacheManager;

        String datadir = BootEnvironmentPostProcessor.getProperty(ConfigurationItem.DataDirectory.name());
        if (StringUtils.isBlank(datadir)) {
            cacheManager = new net.sf.ehcache.CacheManager(CommonsUtils.getStreamOfRes("ehcache.xml"));
        } else {
            // 使用数据目录存储缓存文件
            Document config = XMLHelper.createDocument(CommonsUtils.getStreamOfRes("ehcache.xml"));
            Element diskStore = (Element) config.getRootElement().selectSingleNode("//diskStore");
            File tempdir = RebuildConfiguration.getFileOfTemp(".ehcache");
            diskStore.addAttribute("path", tempdir.getAbsolutePath());

            InputStream is = new ByteArrayInputStream(config.asXML().getBytes(StandardCharsets.UTF_8));
            cacheManager = new net.sf.ehcache.CacheManager(is);
        }

        EhCacheCacheManager manager = new EhCacheCacheManager();
        manager.setCacheManager(cacheManager);
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
        String useHost = BootEnvironmentPostProcessor.getProperty("db.CacheHost");
        if ("0".equals(useHost)) return USE_EHCACHE;

        String spec = BootEnvironmentPostProcessor.getProperty(ConfigurationItem.RedisDatabase.name());
        int database = NumberUtils.toInt(spec, (Integer) ConfigurationItem.RedisDatabase.getDefaultValue());

        return new KnownJedisPool(
                StringUtils.defaultIfBlank(useHost, "127.0.0.1"),
                ObjectUtils.toInt(BootEnvironmentPostProcessor.getProperty("db.CachePort"), 6379),
                BootEnvironmentPostProcessor.getProperty("db.CachePassword", null),
                database);
    }
}
