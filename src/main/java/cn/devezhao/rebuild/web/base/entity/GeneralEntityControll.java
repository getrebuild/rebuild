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

package cn.devezhao.rebuild.web.base.entity;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;

import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.rebuild.server.service.base.FormManager;
import cn.devezhao.rebuild.web.BaseControll;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/22/2018
 */
@Controller
@RequestMapping("/app/")
public class GeneralEntityControll extends BaseControll {
	
	@RequestMapping("{entity}/{id}")
	public ModelAndView pageView(@PathVariable String entity, @PathVariable String id, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/general-entity/record-view.jsp", entity);
		JSON formConfig = FormManager.getFormLayout(entity, getRequestUser(request));
		mv.getModel().put("FormConfig", formConfig);
		return mv;
	}
	
	@RequestMapping("entity/form-config")
	public void entityForm(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		ID recordId = getIdParameter(request, "id");
		JSON fc = FormManager.getFormLayout(entity, getRequestUser(request), recordId);
		writeSuccess(response, fc);
	}
}
