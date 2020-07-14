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

import cn.devezhao.commons.ThrowableUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.Serializable;

/**
 * 缓存模板
 *
 * @author devezhao
 * @since 01/02/2019
 */
public abstract class BaseCacheTemplate<V extends Serializable> implements CacheTemplate<V> {

	protected static final Log LOG = LogFactory.getLog(BaseCacheTemplate.class);

	/**
	 * 默认缓存时间（90天）
	 */
	private static final int TS_DEFAULT = 60 * 60 * 24 * 90;

	final private CacheTemplate<V> delegate;
	final private boolean useRedis;

	final private String keyPrefix;

	/**
	 * @param jedisPool
	 * @param backup The ehcache for backup
	 * @param keyPrefix
	 */
	protected BaseCacheTemplate(JedisPool jedisPool, CacheManager backup, String keyPrefix) {
		this.useRedis = testJedisPool(jedisPool);
		if (this.useRedis) {
			this.delegate = new JedisCacheDriver<>(jedisPool);
		} else {
			this.delegate = new EhcacheDriver<>(backup);
		}

		String fix = StringUtils.defaultIfBlank(System.getProperty("cache.keyprefix"), "RB.");
		this.keyPrefix = fix + StringUtils.defaultIfBlank(keyPrefix, StringUtils.EMPTY);
	}
	
	@Override
	public String get(String key) {
		return delegate.get(unityKey(key));
	}

	@Override
	public void put(String key, String value) {
		this.put(key, value, TS_DEFAULT);
	}

	@Override
	public void put(String key, String value, int seconds) {
		if (value == null) {
			LOG.warn("Can't set `" + key + "` to null");
			return;
		}
		delegate.put(unityKey(key), value, seconds);
	}

	@Override
	public V getx(String key) {
		return delegate.getx(unityKey(key));
	}

	@Override
	public void putx(String key, V value) {
		this.putx(key, value, TS_DEFAULT);
	}

	@Override
	public void putx(String key, V value, int seconds) {
		if (value == null) {
			LOG.warn("Can't set `" + key + "` to null");
			return;
		}
		delegate.putx(unityKey(key), value, seconds);
	}

	@Override
	public void evict(String key) {
		delegate.evict(unityKey(key));
	}

	/**
	 * @return
	 */
	public CacheTemplate<V> getCacheTemplate() {
		return delegate;
	}
	
	/**
	 * @return
	 */
	public boolean isUseRedis() {
		return useRedis;
	}
	
	private boolean testJedisPool(JedisPool jedisPool) {
		try {
			Jedis jedis = jedisPool.getResource();
			IOUtils.closeQuietly(jedis);
			return true;
		} catch (Exception ex) {
			LOG.warn("Acquisition J/Redis failed : " + ThrowableUtils.getRootCause(ex).getLocalizedMessage()
                    + " !!! Using backup ehcache for " + getClass());
		}
		return false;
	}

	private String unityKey(String key) {
		Assert.isTrue(StringUtils.isNotBlank(key), "[key] not be null");
		return (keyPrefix + key).toLowerCase();
	}
}
