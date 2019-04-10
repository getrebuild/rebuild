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

import java.io.Serializable;

/**
 * @author devezhao
 * @since 10/12/2018
 */
public interface CacheTemplate<V extends Serializable> {

	String get(String key);

	void put(String key, String value);
	
	void put(String key, String value, int seconds);

	V getx(String key);
	
	void putx(String key, V value);
	
	void putx(String key, V value, int seconds);
	
	void evict(String key);
	
	String getKeyPrefix();
}