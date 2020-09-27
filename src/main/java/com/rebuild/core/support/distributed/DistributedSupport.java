/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import redis.clients.jedis.JedisPool;

/**
 * 分布式支持
 *
 * @author devezhao
 * @since 2020/9/27
 */
public class DistributedSupport implements UseRedis {

    @Override
    public boolean refreshJedisPool(JedisPool pool) {
        return false;
    }

}
