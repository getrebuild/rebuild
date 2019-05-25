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
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
@RequestMapping("/admin/robot/")
public class CountsSlaveControll extends BaseControll {

	@RequestMapping("trigger/counts-slave-fields")
	public void getCountsSlaveFields(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String sourceEntity = getParameterNotNull(request, "sourceEntity");
		Entity slave = MetadataHelper.getEntity(sourceEntity);
		Entity master = slave.getMasterEntity();

		List<String[]> slaveFields = new ArrayList<String[]>();
		List<String[]> masterFields = new ArrayList<String[]>();
		for (Field field : MetadataSorter.sortFields(slave.getFields(), DisplayType.NUMBER, DisplayType.DECIMAL)) {
			slaveFields.add(new String[] { field.getName(), EasyMeta.getLabel(field) });
		}
		for (Field field : MetadataSorter.sortFields(master.getFields(), DisplayType.NUMBER, DisplayType.DECIMAL)) {
			masterFields.add(new String[] { field.getName(), EasyMeta.getLabel(field) });
		}
		
		JSON data = JSONUtils.toJSONObject(
				new String[] { "slave", "master" }, 
				new Object[] { slaveFields.toArray(new String[slaveFields.size()][]), masterFields.toArray(new String[masterFields.size()][]) });
		writeSuccess(response, data);
	}
}
