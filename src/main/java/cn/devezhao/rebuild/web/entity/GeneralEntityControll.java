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

package cn.devezhao.rebuild.web.entity;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;

import cn.devezhao.rebuild.server.service.entity.FormManager;
import cn.devezhao.rebuild.web.commons.BaseControll;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/22/2018
 */
@Controller
@RequestMapping("/app/")
public class GeneralEntityControll extends BaseControll {

	@RequestMapping("{entity}/new")
	public ModelAndView pageNew(@PathVariable String entity, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/general-entity/entity-new.jsp", entity);
		JSON formConfig = FormManager.getFormLayoutForPortal(entity);
		mv.getModel().put("FormConfig", formConfig);
		return mv;
	}
	
	@RequestMapping("{entity}/{id}/edit")
	public ModelAndView pageEdit(@PathVariable String entity, @PathVariable String id, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/general-entity/entity-edit.jsp", entity);
		JSON formConfig = FormManager.getFormLayoutForPortal(entity);
		mv.getModel().put("FormConfig", formConfig);
		return mv;
	}
	
	@RequestMapping("{entity}/{id}")
	public ModelAndView pageView(@PathVariable String entity, @PathVariable String id, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/general-entity/entity-view.jsp", entity);
		JSON formConfig = FormManager.getFormLayoutForPortal(entity);
		mv.getModel().put("FormConfig", formConfig);
		return mv;
	}
	
	@RequestMapping("entity-form-config")
	public void entityFormConfig(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		JSON fc = FormManager.getFormLayoutForPortal(entity);
		writeSuccess(response, fc);
	}
}
