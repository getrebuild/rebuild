/*
rebuild - Building your business-systems freely.
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SerializationUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.Serializable;

/**
 * redis
 * 
 * @author devezhao
 * @since 01/02/2019
 */
public class JedisCacheDriver<V extends Serializable> implements CacheTemplate<V> {

	private JedisPool jedisPool;

	protected JedisCacheDriver(JedisPool jedisPool) {
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
