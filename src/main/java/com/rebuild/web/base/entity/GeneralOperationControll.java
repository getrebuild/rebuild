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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.util.StringHelper;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/30/2018
 */
@Controller
@RequestMapping("/app/entity/")
public class GeneralOperationControll extends BaseControll {

	@RequestMapping("record-save")
	public void save(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSON formJson = ServletUtils.getRequestJson(request);
		Record record = EntityHelper.parse((JSONObject) formJson, user);
		record = Application.getGeneralEntityService(record.getEntity().getEntityCode()).createOrUpdate(record);
		
		Map<String, Object> map = new HashMap<>();
		map.put("id", record.getPrimary());
		writeSuccess(response, map);
	}
	
	@RequestMapping("record-delete")
	public void delete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID id = getIdParameterNotNull(request, "id");
		Application.getGeneralEntityService(id.getEntityCode()).delete(id);
		
		Map<String, Object> map = new HashMap<>();
		map.put("id", id);
		writeSuccess(response, map);
	}
	
	@RequestMapping("record-fetch")
	public void fetchOne(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID id = getIdParameterNotNull(request, "id");
		Entity entity = MetadataHelper.getEntity(id.getEntityCode());
		
		String fields = getParameter(request, "fields");
		if (StringUtils.isBlank(fields)) {
			fields = "";
			for (Field field : entity.getFields()) {
				fields += field.getName() + ',';
			}
		} else {
			for (String field : fields.split(",")) {
				if (StringHelper.isIdentifier(field)) {
					fields += field + ',';
				} else {
					LOG.warn("忽略无效/非法字段: " + field);
				}
			}
		}
		fields = fields.substring(0, fields.length() - 1);
		
		StringBuffer sql = new StringBuffer("select ")
				.append(fields)
				.append(" from ").append(entity.getName())
				.append(" where ").append(entity.getPrimaryField().getName()).append('=').append(id);
		Record record = Application.createQuery(sql.toString()).record();
		
		Map<String, Object> map = new HashMap<>();
		for (Iterator<String> iter = record.getAvailableFieldIterator(); iter.hasNext(); ) {
			String field = iter.next();
			Object value = record.getObjectValue(field);
			map.put(field, value);
		}
		map.put(entity.getPrimaryField().getName(), id);
		writeSuccess(response, map);
	}
}
