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

package com.rebuild.server.helper.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;

import cn.devezhao.persist4j.Field;

/**
 * 列表项
 * 
 * @author zhaofang123@gmail.com
 * @since 09/06/2018
 */
public class PickListManager {

	/**
	 * @param field
	 * @return
	 */
	public static JSONArray getPickList(Field field) {
		return getPickList(field.getOwnEntity().getName(), field.getName(), false);
	}
	
	/**
	 * @param entity
	 * @param field
	 * @param isAll
	 * @return
	 */
	public static JSONArray getPickList(String entity, String field, boolean isAll) {
		List<Map<String, Object>> list = getPickListRaw(entity, field, isAll, false);
		return (JSONArray) JSON.toJSON(list);
	}
	
	/**
	 * @param entity
	 * @param field
	 * @param isAll
	 * @param reload
	 * @return
	 */
	public static List<Map<String, Object>> getPickListRaw(String entity, String field, boolean isAll, boolean reload) {
		Object[][] array = Application.createQueryNoFilter(
				"select itemId,text,isDefault,isHide from PickList where belongEntity = ? and belongField = ? order by seq asc")
				.setParameter(1, entity)
				.setParameter(2, field)
				.array();
		List<Map<String, Object>> list = new ArrayList<>();
		for (Object[] o : array) {
			if (!isAll && (Boolean) o[3]) {
				continue;
			}
			
			Map<String, Object> item = new HashMap<>(2);
			item.put("id", o[0].toString());
			item.put("text", o[1]);
			item.put("default", o[2]);
			if (isAll) {
				item.put("hide", o[3]);
			}
			list.add(item);
		}
		return list;
	}
}
