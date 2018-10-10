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
import com.rebuild.server.helper.manager.FormManager;
import com.rebuild.web.BaseControll;

import cn.devezhao.persist4j.engine.ID;

/**
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
		ID recordId = ID.valueOf(id);
		ModelAndView mv = createModelAndView("/general-entity/record-view.jsp", entity);
		mv.getModel().put("id", recordId);
		return mv;
	}
	
	@RequestMapping("{entity}/form-modal")
	public void entityForm(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID recordId = getIdParameter(request, "id");
		JSON fc = FormManager.getFormModal(entity, getRequestUser(request), recordId);
		writeSuccess(response, fc);
	}
	
	@RequestMapping("{entity}/view-modal")
	public void entityView(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		ID recordId = getIdParameterNotNull(request, "id");
		
		JSON modal = FormManager.getViewModal(entity, user, recordId);
		writeSuccess(response, modal);
	}
}
