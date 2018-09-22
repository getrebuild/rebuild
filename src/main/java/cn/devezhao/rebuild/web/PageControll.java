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

package cn.devezhao.rebuild.web;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.ModelAndView;

import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.rebuild.server.Startup;
import cn.devezhao.rebuild.server.metadata.MetadataHelper;
import cn.devezhao.rebuild.server.service.entitymanage.EasyMeta;
import cn.devezhao.rebuild.utils.AppUtils;

/**
 * @author zhaofang123@gmail.com
 * @since 09/20/2018
 */
public abstract class PageControll {

	/**
	 * @param req
	 * @return
	 */
	protected ID getRequestUser(HttpServletRequest req) {
		ID fansId = AppUtils.getRequestUser(req);
		if (fansId == null) {
			throw new InvalidRequestException("无效请求用户");
		}
		return fansId;
	}
	
	/**
	 * @param page
	 * @return
	 */
	protected ModelAndView createModelAndView(String page) {
		return createModelAndView(page, null);
	}
	
	/**
	 * @param page
	 * @param entity
	 * @return
	 */
	protected ModelAndView createModelAndView(String page, String entity) {
		ModelAndView mv = new ModelAndView(page);
		setPageAttribute(mv);
		
		if (entity != null) {
			if (!MetadataHelper.containsEntity(entity)) {
				throw new InvalidRequestException("无效实体 : " + entity);
			}
			
			EasyMeta entityMeta = new EasyMeta(MetadataHelper.getEntity(entity));
			mv.getModel().put("entityName", entityMeta.getName());
			mv.getModel().put("entityLabel", entityMeta.getLabel());
			mv.getModel().put("icon", entityMeta.getIcon());
		}
		return mv;
	}
	

	/**
	 * 页面公用属性
	 * 
	 * @param request
	 */
	public static void setPageAttribute(HttpServletRequest request) {
		request.setAttribute("baseUrl", Startup.getContextPath());
	}
	
	/**
	 * 页面公用属性
	 * 
	 * @param request
	 */
	public static void setPageAttribute(ModelAndView modelAndView) {
		modelAndView.getModel().put("baseUrl", Startup.getContextPath());
	}
}
