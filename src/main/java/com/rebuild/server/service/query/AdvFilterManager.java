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

package com.rebuild.server.service.query;

import org.springframework.util.Assert;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.helper.manager.PortalsManager;

import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 09/30/2018
 */
public class AdvFilterManager implements PortalsManager {
	
	/**
	 * 获取高级查询列表
	 * 
	 * @param entity
	 * @param user
	 * @return
	 */
	public static Object[][] getAdvFilterList(String entity, ID user) {
		Assert.notNull(entity, "[entity] not be null");
		Assert.notNull(user, "[user] not be null");
		
		Object[][] array = Application.createQueryNoFilter(
				"select filterId,filterName,createdBy from FilterConfig"
				+ " where belongEntity = ? and ((applyTo = 'SELF' and createdBy = ?) or applyTo = 'ALL')"
				+ " order by filterName")
				.setParameter(1, entity)
				.setParameter(2, user)
				.array();
		for (Object[] o : array) {
			o[2] = o[2].equals(user);  // allow edit?
		}
		return array;
	}
	
	/**
	 * 获取高级查询列表
	 * 
	 * @param filterId
	 * @return
	 */
	public static Object[] getAdvFilterRaw(ID filterId) {
		Assert.notNull(filterId, "[filterId] not be null");
		Object[] filter = Application.createQueryNoFilter(
				"select filterId,config,filterName,applyTo,createdBy from FilterConfig where filterId = ?")
				.setParameter(1, filterId)
				.unique();
		if (filter == null) {
			return null;
		}
		
		String cfg = (String) filter[1];
		filter[1] = JSON.parseObject(cfg);
		return filter;
	}
}