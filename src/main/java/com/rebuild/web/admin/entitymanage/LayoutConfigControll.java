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

package com.rebuild.web.admin.entitymanage;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.base.FormManager;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/19/2018
 */
@Controller
@RequestMapping("/admin/entity/")
public class LayoutConfigControll extends BaseControll {
	
	@RequestMapping("{entity}/form-design")
	public ModelAndView pageFormDesign(@PathVariable String entity, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/entity/form-design.jsp");
		MetaEntityControll.setEntityBase(mv, entity);
		JSON fc = FormManager.getFormLayout(entity);
		if (fc != null) {
			request.setAttribute("FormConfig", fc);
		}
		return mv;
	}
	
	@RequestMapping("{entity}/view-design")
	public ModelAndView pageViewDesign(@PathVariable String entity, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/entity/view-design.jsp");
		MetaEntityControll.setEntityBase(mv, entity);
		JSON fc = FormManager.getViewLayout(entity);
		if (fc != null) {
			request.setAttribute("ViewConfig", fc);
		}
		return mv;
	}
	
	@RequestMapping({ "{entity}/form-update", "{entity}/view-update" })
	public void formUpdate(@PathVariable String entity, HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSON formJson = ServletUtils.getRequestJson(request);
		Record record = EntityHelper.parse((JSONObject) formJson, user);
		Application.getCommonService().createOrUpdate(record);
		writeSuccess(response);
	}
}
