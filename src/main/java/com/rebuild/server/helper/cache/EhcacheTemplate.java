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

import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

/**
 * 
 * @author devezhao
 * @since 01/02/2019
 */
public class EhcacheTemplate implements CacheTemplate {

	private CacheManager ehcacheManager;
	private String keyPrefix;
	
	protected EhcacheTemplate(CacheManager ehcacheManager, String keyPrefix) {
		this.ehcacheManager = ehcacheManager;
		this.keyPrefix = keyPrefix == null ? "" : keyPrefix;
	}
	
	@Override
	public String get(String key) {
		key = unityKey(key);
		ValueWrapper w = cache().get(key);
		return w == null ? null : (String) w.get();
	}

	@Override
	public void put(String key, String value) {
		putx(key, value, -1);
	}

	@Override
	public void put(String key, String value, int exp) {
		putx(key, value, -1);
	}

	@Override
	public Serializable getx(String key) {
		key = unityKey(key);
		ValueWrapper w = cache().get(key);
		return w == null ? null : (Serializable) w.get();
	}

	@Override
	public void putx(String key, Serializable value) {
		putx(key, value, -1);
	}

	@Override
	public void putx(String key, Serializable value, int exp) {
		key = unityKey(key);
		cache().put(key, value);
	}

	@Override
	public void evict(String key) {
		key = unityKey(key);
		cache().evict(key);
	}
	
	/**
	 * @return
	 */
	protected Cache cache() {
		return ehcacheManager.getCache("rebuild");
	}
	
	@Override
	public String getKeyPrefix() {
		return keyPrefix;
	}

	/**
	 * @param key
	 * @return
	 */
	protected String unityKey(String key) {
		Assert.notNull(key, "[key] not be null");
		return (getKeyPrefix() + key).toLowerCase();
	}
}
