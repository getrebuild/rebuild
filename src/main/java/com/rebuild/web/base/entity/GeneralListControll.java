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
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.service.base.DataListManager;
import com.rebuild.web.BaseControll;
import com.rebuild.web.base.entity.datalist.DataListControl;
import com.rebuild.web.base.entity.datalist.DefaultDataListControl;

import cn.devezhao.commons.web.ServletUtils;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/22/2018
 */
@Controller
@RequestMapping("/app/")
public class GeneralListControll extends BaseControll {

	@RequestMapping("{entity}/list")
	public ModelAndView pageList(@PathVariable String entity, 
			HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/general-entity/record-list.jsp", entity);
		JSON cfg = DataListManager.getColumnLayout(entity);
		mv.getModel().put("DataListConfig", JSON.toJSONString(cfg));
		return mv;
	}
	
	@RequestMapping("{entity}/record-list")
	public void recordList(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		String reqdata = ServletUtils.getRequestString(request);
		JSONObject reqJson = JSON.parseObject(reqdata);
		
		DataListControl control = new DefaultDataListControl(reqJson);
		String json = control.getResult();
		writeSuccess(response, JSON.parse(json));
	}
}
