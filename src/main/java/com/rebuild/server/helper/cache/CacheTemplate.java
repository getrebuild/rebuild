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

import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

/**
 * @author devezhao
 * @since 10/12/2018
 */
public abstract class CacheTemplate<V> {

	final private CacheManager cacheManager;
	
	protected CacheTemplate(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	@SuppressWarnings("unchecked")
	public V get(String key) {
		ValueWrapper vw = getCache().get(standardizationKey(key));
		return vw == null ? null : (V) vw.get();
	}
	
	public void put(String key, V value) {
		getCache().put(standardizationKey(key), value);
	}
	
	public void evict(String key, V value) {
		getCache().evict(standardizationKey(key));
	}
	
	protected Cache getCache() {
		return cacheManager.getCache("default");
	}
	
	/**
	 * KEY 全大写
	 * 
	 * @param key
	 * @return
	 */
	protected String standardizationKey(Object key) {
		Assert.notNull(key, "[key] not be null");
		return key.toString().toUpperCase();
	}
}