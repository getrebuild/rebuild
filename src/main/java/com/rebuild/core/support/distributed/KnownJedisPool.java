/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.distributed;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author devezhao
 * @since 2020/9/27
 */
public class KnownJedisPool extends JedisPool {

    private String host;
    private int port;
    private String password;

    public KnownJedisPool(String host, int port, String password, int database) {
        super(new JedisPoolConfig(), host, port, 5000, password, database);
        this.host = host;
        this.port = port;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }
}
