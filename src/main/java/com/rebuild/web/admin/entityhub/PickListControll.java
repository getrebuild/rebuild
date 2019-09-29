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

package com.rebuild.web.admin.entityhub;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.PickListManager;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.configuration.PickListService;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author zhaofang123@gmail.com
 * @since 09/06/2018
 */
@Controller
@RequestMapping("/admin/field/")
public class PickListControll extends BaseControll {
	
	@RequestMapping({ "picklist-gets", "multiselect-gets" })
	public void picklistGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		String field = getParameterNotNull(request, "field");
		boolean isAll = "true".equals(getParameter(request, "isAll"));
		
		Field fieldMeta = MetadataHelper.getField(entity, field);
		JSONArray picklist = PickListManager.instance.getPickList(fieldMeta, isAll);
		writeSuccess(response, picklist);
	}
	
	@RequestMapping({ "picklist-sets", "multiselect-sets" })
	public void picklistSet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		String field = getParameterNotNull(request, "field");
		JSONObject config = (JSONObject) ServletUtils.getRequestJson(request);
		
		Field fieldMeta = MetadataHelper.getField(entity, field);
		Application.getBean(PickListService.class).updateBatch(fieldMeta, config);
		writeSuccess(response);
	}
}