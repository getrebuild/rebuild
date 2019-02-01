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
import com.rebuild.server.helper.manager.FormsManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.web.BasePageControll;
import com.rebuild.web.PortalsConfiguration;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/19/2018
 */
@Controller
@RequestMapping("/admin/entity/")
public class FormDesignControll extends BasePageControll implements PortalsConfiguration {
	
	@RequestMapping("{entity}/form-design")
	public ModelAndView pageFormDesign(@PathVariable String entity, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/entity/form-design.jsp");
		MetaEntityControll.setEntityBase(mv, entity);
		JSON cfg = FormsManager.getFormLayout(entity, getRequestUser(request));
		if (cfg != null) {
			request.setAttribute("FormConfig", cfg);
		}
		return mv;
	}
	
	@Override
	public void gets(String entity, HttpServletRequest request, HttpServletResponse response) throws IOException { }
	
	@RequestMapping({ "{entity}/form-update" })
	@Override
	public void sets(String entity, 
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSON formJson = ServletUtils.getRequestJson(request);
		Record record = EntityHelper.parse((JSONObject) formJson, getRequestUser(request));
		if (record.getPrimary() == null) {
			record.setString("shareTo", FormsManager.SHARE_ALL);
		}
		
		Application.getCommonService().createOrUpdate(record);
		writeSuccess(response);
	}
}
