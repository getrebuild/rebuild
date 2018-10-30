/*
rebuild - Building your system freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.helper.cache;

import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cache.Cache;

import com.rebuild.server.Application;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 
 * @author devezhao
 * @since 10/30/2018
 */
public class RedisCache implements Cache {

	private static final Log LOG = LogFactory.getLog(RedisCache.class);
	
	final private JedisPool JEDISPOOL;
	
	/**
	 * @param host
	 * @param port
	 * @param passwd
	 */
	public RedisCache(String host, int port, String passwd) {
		JedisPoolConfig config = new JedisPoolConfig();
		this.JEDISPOOL = new JedisPool(config, host, port, 15 * 1000, passwd, false);
		Jedis jedis = getJedis();
		try {
			LOG.warn("Connection to (j)redis ... stats " + jedis.dbSize() + " keys");
		} finally {
			close(jedis);
		}
		
		Application.addShutdownHook(new Thread() {
			@Override
			public void run() {
				LOG.warn("Closing (j)redis ... ");
				JEDISPOOL.close();
			}
		});
	}
	
	public Jedis getJedis() {
		return this.JEDISPOOL.getResource();
	}
	
	public void close(Jedis jedis) {
		jedis.close();
	}
	
	// --
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getNativeCache() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ValueWrapper get(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T get(Object key, Class<T> type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void put(Object key, Object value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ValueWrapper putIfAbsent(Object key, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void evict(Object key) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}
}
