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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.helper.manager.DefaultValueManager;
import com.rebuild.server.helper.manager.FormManager;
import com.rebuild.server.helper.manager.ViewFeatManager;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;

/**
 * 表单/视图
 * 
 * @author zhaofang123@gmail.com
 * @since 08/22/2018
 */
@Controller
@RequestMapping("/app/")
public class GeneralEntityControll extends BaseControll {

	@RequestMapping("{entity}/view/{id}")
	public ModelAndView pageView(@PathVariable String entity, @PathVariable String id,
			HttpServletRequest request) throws IOException {
		ID user = getRequestUser(request);
		ID record = ID.valueOf(id);
		ModelAndView mv = createModelAndView("/general-entity/record-view.jsp", record, user);
		mv.getModel().put("id", record);
		
		JSON vtab = ViewFeatManager.getViewTab(entity, user);
		mv.getModel().put("ViewTabs", vtab);
		
		JSON vadd = ViewFeatManager.getViewAdd(entity, user);
		mv.getModel().put("ViewAdds", vadd);
		
		return mv;
	}
	
	@RequestMapping("{entity}/form-model")
	public void entityForm(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		ID record = getIdParameter(request, "id");  // New or Update
		JSON model = FormManager.getFormModel(entity, user, record);
		if (record == null) {
			JSON defaultVal = ServletUtils.getRequestJson(request);
			if (defaultVal != null) {
				DefaultValueManager.setFieldsValue(MetadataHelper.getEntity(entity), model, defaultVal);
			}
		}
		writeSuccess(response, model);
	}
	
	@RequestMapping("{entity}/view-model")
	public void entityView(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		ID record = getIdParameterNotNull(request, "id");
		JSON modal = FormManager.getViewModel(entity, user, record);
		writeSuccess(response, modal);
	}
}
