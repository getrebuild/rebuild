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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * 
 * @author devezhao
 * @since 10/30/2018
 */
public class RedisCacheManager implements CacheManager {
	
	private static final List<String> CACHE_NAMES = new ArrayList<>();
	static {
		CACHE_NAMES.add("rebuild.redis");
	}
	
	private Cache cache;
	
	protected RedisCacheManager() {
//		this.cache = new RedisCache();
	}

	@Override
	public Cache getCache(String name) {
		return cache;
	}

	@Override
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableCollection(CACHE_NAMES);
	}
}
