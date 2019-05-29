/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.web.admin.robot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
@Controller
@RequestMapping("/admin/robot/trigger/")
public class FieldAggregationControll extends BaseControll {
	
	@RequestMapping("field-aggregation-entities")
	public void getTargetEntity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Entity sourceEntity = MetadataHelper.getEntity(getParameterNotNull(request, "source"));
		
		List<String[]> entities = new ArrayList<>();
		Set<String> set = new HashSet<>();
		for (Field refField : MetadataSorter.sortFields(sourceEntity, DisplayType.REFERENCE)) {
			Entity refEntity = refField.getReferenceEntity();
			if (!set.contains(refEntity.getName())) {
				set.add(refEntity.getName());
				entities.add(new String[] { refEntity.getName(), EasyMeta.getLabel(refEntity) });
			}
		}
		
		writeSuccess(response, entities);
	}
	
	@RequestMapping("field-aggregation-fields")
	public void getTargetField(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Entity sourceEntity = MetadataHelper.getEntity(getParameterNotNull(request, "source"));
		String target = getParameter(request, "target");
		Entity targetEntity = StringUtils.isBlank(target) ? null : MetadataHelper.getEntity(target);
		
		List<String[]> sourceFields = new ArrayList<String[]>();
		List<String[]> targetFields = new ArrayList<String[]>();
		for (Field field : MetadataSorter.sortFields(sourceEntity.getFields(), DisplayType.NUMBER, DisplayType.DECIMAL)) {
			sourceFields.add(new String[] { field.getName(), EasyMeta.getLabel(field) });
		}
		if (targetEntity != null) {
			for (Field field : MetadataSorter.sortFields(targetEntity.getFields(), DisplayType.NUMBER, DisplayType.DECIMAL)) {
				targetFields.add(new String[] { field.getName(), EasyMeta.getLabel(field) });
			}
		}
		
		JSON data = JSONUtils.toJSONObject(
				new String[] { "source", "target" }, 
				new Object[] { sourceFields.toArray(new String[sourceFields.size()][]), targetFields.toArray(new String[targetFields.size()][]) });
		writeSuccess(response, data);
	}
}
