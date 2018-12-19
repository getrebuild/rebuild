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
import com.rebuild.server.helper.manager.FieldValueWrapper;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.web.BaseControll;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;

/**
 * 引用字段搜索
 * 
 * @author zhaofang123@gmail.com
 * @since 08/24/2018
 */
@Controller
public class ReferenceSearch extends BaseControll {
	
	@RequestMapping({ "/app/entity/ref-search", "/app/entity/reference-search" })
	public void referenceSearch(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		String field = getParameterNotNull(request, "field");
		String q = getParameter(request, "q");
		
		if (StringUtils.isBlank(q)) {
			writeSuccess(response, ArrayUtils.EMPTY_STRING_ARRAY);
			return;
		}
		q = StringEscapeUtils.escapeSql(q);
		
		Entity entityMeta = MetadataHelper.getEntity(entity);
		Field refField = entityMeta.getField(field);
		Entity refEntity = refField.getReferenceEntities()[0];
		Field nameField = MetadataHelper.getNameField(refEntity);
		if (nameField == null) {
			writeSuccess(response, ArrayUtils.EMPTY_STRING_ARRAY);
			return;
		}
		
		String nameField2str = nameField.getName();
		if (nameField.getType() == FieldType.REFERENCE) {
			nameField2str = "&" + nameField2str;
		}
		
		String sql = "select %s,%s from %s where ";
		sql = String.format(sql, refEntity.getPrimaryField().getName(), nameField2str, refEntity.getName());
		sql += String.format("( %s like '%%%s%%'", nameField2str, q);
		if (entityMeta.containsField(EntityHelper.QuickCode)) {
			sql += String.format(" or %s like '%s%%' )", EntityHelper.QuickCode, q);
		} else {
			sql += " )";
		}
		if (entityMeta.containsField(EntityHelper.ModifiedOn)) {
			sql += " order by modifiedOn desc";
		}
		
		Object[][] array = Application.createQuery(sql).setLimit(10).array();
		List<Object> result = new ArrayList<>();
		for (Object[] o : array) {
			Map<String, Object> map = new HashMap<>();
			map.put("id", o[0].toString());
			map.put("text", o[1]);
			result.add(map);
		}
		writeSuccess(response, result);
	}
	
	@RequestMapping({ "/app/entity/ref-label", "/app/entity/reference-label" })
	public void referenceLabel(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String ids = getParameter(request, "ids", "");
		
		Map<String, String> labels = new HashMap<>();
		for (String id : ids.split("\\|")) {
			if (!ID.isId(id)) {
				continue;
			}
			String label = FieldValueWrapper.getLabel(ID.valueOf(id));
			if (label != null) {
				labels.put(id, label);
			}
		}
		writeSuccess(response, labels);
	}
}
