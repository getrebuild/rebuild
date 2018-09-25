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

package com.rebuild.web.admin.entitymanage;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.base.PickListManager;
import com.rebuild.server.service.entitymanage.PickListService;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Field;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 09/06/2018
 */
@Controller
@RequestMapping("/admin/field/")
public class PickListControll extends BaseControll {
	
	@RequestMapping("picklist-gets")
	public void picklistGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		String field = getParameterNotNull(request, "field");
		boolean isAll = "true".equals(getParameter(request, "isAll"));
		
		JSONArray picklist = PickListManager.getPickList(entity, field, isAll);
		writeSuccess(response, picklist);
	}
	
	@RequestMapping("picklist-sets")
	public void picklistSet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSONObject config = (JSONObject) ServletUtils.getRequestJson(request);
		String entity = getParameterNotNull(request, "entity");
		String field = getParameterNotNull(request, "field");
		
		
		Field field2field = MetadataHelper.getField(entity, field);
		Application.getBean(PickListService.class).txBatchUpdate(field2field, config);
		writeSuccess(response);
	}
}