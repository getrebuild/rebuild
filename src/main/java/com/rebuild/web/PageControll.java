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

package com.rebuild.web;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.ModelAndView;

import com.rebuild.server.ServerListener;
import com.rebuild.server.entityhub.AccessibleMeta;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.AppUtils;

import cn.devezhao.persist4j.engine.ID;

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
			
			AccessibleMeta entityMeta = new AccessibleMeta(MetadataHelper.getEntity(entity));
			mv.getModel().put("entityName", entityMeta.getName());
			mv.getModel().put("entityLabel", entityMeta.getLabel());
			mv.getModel().put("entityIcon", entityMeta.getIcon());
		}
		return mv;
	}
	

	/**
	 * 页面公用属性
	 * 
	 * @param request
	 */
	public static void setPageAttribute(HttpServletRequest request) {
		request.setAttribute("baseUrl", ServerListener.getContextPath());
	}
	
	/**
	 * 页面公用属性
	 * 
	 * @param request
	 */
	public static void setPageAttribute(ModelAndView modelAndView) {
		modelAndView.getModel().put("baseUrl", ServerListener.getContextPath());
	}
}
