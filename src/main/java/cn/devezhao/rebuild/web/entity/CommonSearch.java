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

package cn.devezhao.rebuild.web.entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.web.commons.BaseControll;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/24/2018
 */
@Controller
@RequestMapping("/entity/")
public class CommonSearch extends BaseControll {

	@RequestMapping("common-search")
	public void search(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameter(request, "entity");
		String search = getParameter(request, "search");
		
		if (StringUtils.isBlank(search)) {
			writeSuccess(response, ArrayUtils.EMPTY_STRING_ARRAY);
			return;
		}
		
		Entity e = EntityHelper.getEntity(entity);
		Field nameField = e.getNameField();
		if (nameField == null) {
			writeSuccess(response, ArrayUtils.EMPTY_STRING_ARRAY);
			return;
		}
		
		String nameField2str = nameField.getName();
		if (nameField.getType() == FieldType.REFERENCE) {
			nameField2str = "&" + nameField2str;
		}
		
		String searchSql = "select %s,%s from %s where %s ";
		searchSql = String.format(searchSql, e.getPrimaryField().getName(), nameField2str, e.getName(), nameField2str);
		searchSql += "like '%" + search + "%'";
		
		Object[][] array = Application.createQuery(searchSql).setLimit(10).array();
		
		List<Object> list = new ArrayList<>();
		for (Object[] o : array) {
			Map<String, Object> map = new HashMap<>();
			map.put("id", o[0].toString());
			map.put("text", o[1]);
			list.add(map);
		}
		writeSuccess(response, list);
	}
}
