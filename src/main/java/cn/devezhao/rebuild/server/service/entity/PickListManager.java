/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cn.devezhao.rebuild.server.service.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import cn.devezhao.persist4j.Field;
import cn.devezhao.rebuild.server.Application;

/**
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
		Object[][] array = Application.createQuery(
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
