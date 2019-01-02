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
import org.springframework.cache.CacheManager;

import com.rebuild.server.Application;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 
 * @author devezhao
 * @since 01/02/2019
 */
public abstract class BaseCacheTemplate implements CacheTemplate {

	final private CacheTemplate delegate;
	final private boolean useRedis;

	/**
	 * @param jedisPool
	 * @param backup The ehcache for backup
	 * @param keyPrefix
	 */
	protected BaseCacheTemplate(JedisPool jedisPool, CacheManager backup, String keyPrefix) {
		if (testJedisPool(jedisPool)) {
			this.delegate = new JedisCacheTemplate(jedisPool, keyPrefix);
			this.useRedis = true;
		} else {
			this.delegate = new EhcacheTemplate(backup, keyPrefix);
			this.useRedis = false;
		}
	}
	
	/**
	 * @param jedisPool
	 * @param backup The ehcache for backup
	 */
	protected BaseCacheTemplate(JedisPool jedisPool, CacheManager backup) {
		this(jedisPool, backup, null);
	}

	@Override
	public String get(String key) {
		return delegate.get(key);
	}

	@Override
	public void put(String key, String value) {
		delegate.put(key, value);
	}

	@Override
	public void put(String key, String value, int exp) {
		delegate.put(key, value, exp);
	}

	@Override
	public Serializable getx(String key) {
		return delegate.getx(key);
	}

	@Override
	public void putx(String key, Serializable value) {
		delegate.putx(key, value);
	}

	@Override
	public void putx(String key, Serializable value, int exp) {
		delegate.putx(key, value, exp);
	}

	@Override
	public void evict(String key) {
		delegate.evict(key);
	}
	
	@Override
	public String getKeyPrefix() {
		// TODO Auto-generated method stub
		return delegate.getKeyPrefix();
	}
	
	/**
	 * @return
	 */
	public CacheTemplate getCacheTemplate() {
		return delegate;
	}
	
	/**
	 * @return
	 */
	public boolean isUseRedis() {
		return useRedis;
	}
	
	/**
	 * @param jedisPool
	 * @return
	 */
	private boolean testJedisPool(JedisPool jedisPool) {
		try {
			Jedis jedis = jedisPool.getResource();
			IOUtils.closeQuietly(jedis);
			return true;
		} catch (Exception ex) {
			Application.LOG.warn("Acquisition J/Redis failed : " + ex.getLocalizedMessage() + " !!! Using backup ehcache for " + getClass());
		}
		return false;
	}
}
