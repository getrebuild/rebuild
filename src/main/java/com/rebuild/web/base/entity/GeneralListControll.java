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
