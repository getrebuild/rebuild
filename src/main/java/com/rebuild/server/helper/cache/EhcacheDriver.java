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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Objects;

/**
 * ehcache
 * 
 * @author devezhao
 * @since 01/02/2019
 */
public class EhcacheDriver<V extends Serializable> implements CacheTemplate<V> {

	private CacheManager ehcacheManager;

	protected EhcacheDriver(CacheManager ehcacheManager) {
		this.ehcacheManager = ehcacheManager;
	}
	
	@Override
	public String get(String key) {
		ValueWrapper w = cache().get(key);
		return w == null ? null : (String) w.get();
	}

	@Override
	public void put(String key, String value) {
		put(key, value, -1);
	}

	@Override
	public void put(String key, String value, int seconds) {
		Objects.requireNonNull(value, "`value` not be null");

		Element el = new Element(key, value);
		if (seconds > -1) {
			el.setTimeToLive(seconds);
		}
		((Ehcache) cache().getNativeCache()).put(el);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V getx(String key) {
		ValueWrapper w = cache().get(key);
		return w == null ? null : (V) w.get();
	}

	@Override
	public void putx(String key, V value) {
		putx(key, value, -1);
	}

	@Override
	public void putx(String key, V value, int seconds) {
		Objects.requireNonNull(value, "`value` not be null");

		Element el = new Element(key, value);
		if (seconds > -1) {
			el.setTimeToLive(seconds);
		}
		((Ehcache) cache().getNativeCache()).put(el);
	}

	@Override
	public void evict(String key) {
		cache().evict(key);
	}
	
	/**
	 * @return
	 */
	public Cache cache() {
		Cache rebuild = ehcacheManager.getCache("rebuild");
		Assert.notNull(rebuild, "No cache `rebuild` defined in ehcache.xml");
		return rebuild;
	}
}
