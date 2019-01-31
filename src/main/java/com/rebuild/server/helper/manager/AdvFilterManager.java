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

package com.rebuild.server.helper.manager;

import org.springframework.util.Assert;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.service.bizz.RoleService;
import com.rebuild.server.service.bizz.UserHelper;

import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 09/30/2018
 */
public class AdvFilterManager extends SharableManager {
	
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
		
		boolean isAdmin = UserHelper.isAdmin(user);
		String sql = "select configId,filterName,createdBy from FilterConfig where belongEntity = ? and ";
		if (isAdmin) {
			sql += String.format("createdBy.roleId = '%s'", RoleService.ADMIN_ROLE);
		} else {
			sql += String.format("((shareTo = 'SELF' and createdBy = '%s') or shareTo = 'ALL')", user);
		}
		sql += " order by filterName";
		
		Object[][] array = Application.createQueryNoFilter(sql).setParameter(1, entity).array();
		for (Object[] o : array) {
			o[2] = isSelf(user, (ID) o[2]);
		}
		return array;
	}
	
	/**
	 * 获取高级查询
	 * 
	 * @param configId
	 * @return
	 */
	public static Object[] getAdvFilter(ID configId) {
		Assert.notNull(configId, "[configId] not be null");
		Object[] filter = Application.createQueryNoFilter(
				"select configId,config,filterName,shareTo,createdBy from FilterConfig where configId = ?")
				.setParameter(1, configId)
				.unique();
		if (filter == null) {
			return null;
		}
		filter[1] = JSON.parseObject((String) filter[1]);
		return filter;
	}
}