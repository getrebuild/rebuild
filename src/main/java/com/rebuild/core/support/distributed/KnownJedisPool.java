/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import lombok.Getter;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author devezhao
 * @since 2020/9/27
 */
public class KnownJedisPool extends JedisPool {

    public static final int TIMEOUT = 5000;

    /**
     * Default config
     */
    public static final JedisPoolConfig DEFAULT_CONFIG = new JedisPoolConfig() {
        @Override
        public boolean getJmxEnabled() {
            return false;
        }
    };

    @Getter
    final private String host;
    @Getter
    final private int port;
    @Getter
    final private String password;
    @Getter
    final private int database;

    public KnownJedisPool(String host, int port, String password, int database) {
        super(DEFAULT_CONFIG, host, port, TIMEOUT, password, database);
        this.host = host;
        this.port = port;
        this.password = password;
        this.database = database;
    }
}
