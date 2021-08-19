/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import cn.devezhao.commons.ThrowableUtils;
import com.rebuild.core.BootConfiguration;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @author devezhao
 * @since 2020/9/27
 */
public interface UseRedis {

    // private
    Logger _log = LoggerFactory.getLogger(UseRedis.class);

    /**
     * @param pool
     * @return
     */
    default boolean testJedisPool(JedisPool pool) {
        if (pool == BootConfiguration.USE_EHCACHE) return false;

        try {
            Jedis jedis = pool.getResource();
            IOUtils.closeQuietly(jedis);
            return true;
        } catch (Exception ex) {
            _log.warn("Acquisition J/Redis failed : " + ThrowableUtils.getRootCause(ex).getLocalizedMessage()
                    + " !!! falling back to EhCache");
            return false;
        }
    }

    /**
     * @param pool
     * @return
     */
    boolean reinjectJedisPool(JedisPool pool);
}
