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

import java.io.Serializable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SerializationUtils;
import org.springframework.util.Assert;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 
 * @author devezhao
 * @since 01/02/2019
 */
public class JedisCacheTemplate implements CacheTemplate {

	private JedisPool jedisPool;
	private String keyPrefix;

	protected JedisCacheTemplate(JedisPool jedisPool, String keyPrefix) {
		this.jedisPool = jedisPool;
		this.keyPrefix = keyPrefix == null ? "" : keyPrefix;
	}

	public String get(String key) {
		key = unityKey(key);
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			return jedis.get(unityKey(key));
		} finally {
			IOUtils.closeQuietly(jedis);
		}
	}

	@Override
	public void put(String key, String value) {
		put(key, value, -1);
	}

	@Override
	public void put(String key, String value, int exp) {
		key = unityKey(key);
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			jedis.set(key, value);
			if (exp > 0) {
				jedis.expire(value, exp);
			}
		} finally {
			IOUtils.closeQuietly(jedis);
		}
	}

	@Override
	public Serializable getx(String key) {
		key = unityKey(key);
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			byte bs[] = jedis.get(key.getBytes());
			return bs == null ? null : (Serializable) SerializationUtils.deserialize(bs);
		} finally {
			IOUtils.closeQuietly(jedis);
		}
	}

	@Override
	public void putx(String key, Serializable value) {
		putx(key, value, -1);
	}

	@Override
	public void putx(String key, Serializable value, int exp) {
		key = unityKey(key);
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			byte[] bKey = key.getBytes();
			jedis.set(bKey, SerializationUtils.serialize(value));
			if (exp > 0) {
				jedis.expire(bKey, exp);
			}
		} finally {
			IOUtils.closeQuietly(jedis);
		}
	}

	@Override
	public void evict(String key) {
		key = unityKey(key);
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			jedis.del(key);
		} finally {
			IOUtils.closeQuietly(jedis);
		}
	}
	
	@Override
	public String getKeyPrefix() {
		return keyPrefix;
	}

	/**
	 * @return
	 */
	public JedisPool getJedisPool() {
		return jedisPool;
	}

	/**
	 * @param key
	 * @return
	 */
	protected String unityKey(String key) {
		Assert.notNull(key, "[key] not be null");
		return (getKeyPrefix() + key).toUpperCase();
	}
}
