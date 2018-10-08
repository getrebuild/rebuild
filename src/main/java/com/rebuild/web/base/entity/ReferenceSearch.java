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

package com.rebuild.web.base.entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.web.BaseControll;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/24/2018
 */
@Controller
@RequestMapping("/app/commons/")
public class ReferenceSearch extends BaseControll {
	
	@RequestMapping("search")
	public void search(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		String field = getParameterNotNull(request, "field");
		String q = getParameter(request, "q");
		
		if (StringUtils.isBlank(q)) {
			writeSuccess(response, ArrayUtils.EMPTY_STRING_ARRAY);
			return;
		}
		
		Entity root = MetadataHelper.getEntity(entity);
		Field referenceField = root.getField(field);
		Entity referenceEntity = referenceField.getReferenceEntities()[0];
		Field nameField = referenceEntity.getNameField();
		if (nameField == null) {
			writeSuccess(response, ArrayUtils.EMPTY_STRING_ARRAY);
			return;
		}
		
		String nameField2str = nameField.getName();
		if (nameField.getType() == FieldType.REFERENCE) {
			nameField2str = "&" + nameField2str;
		}
		
		String searchSql = "select %s,%s from %s where %s ";
		searchSql = String.format(searchSql, 
				referenceEntity.getPrimaryField().getName(), nameField2str, referenceEntity.getName(), nameField2str);
		searchSql += "like '%" + StringEscapeUtils.escapeSql(q) + "%'";
		
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
