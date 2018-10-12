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

/**
 * 
 * @author devezhao
 * @since 10/12/2018
 */
public abstract class CacheTemplate<V> {

	private int ttl = 60 * 60 * 24;  // default 1 day
	
	public CacheTemplate(int ttl) {
		this.ttl = ttl;
	}

	protected V get(String key) {
		return null;
	}
	
	protected void set(String key, V value) {
		set(key, value, ttl);
	}
	
	protected void set(String key, V value, int ttl) {
	}
}