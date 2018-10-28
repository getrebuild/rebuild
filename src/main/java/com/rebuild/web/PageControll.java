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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.ServerListener;
import com.rebuild.server.entityhub.EasyMeta;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.AppUtils;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
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
		ModelAndView mv = new ModelAndView(page);
		setPageAttribute(mv);
		return mv;
	}
	
	/**
	 * @param page
	 * @param entity
	 * @param user
	 * @return
	 */
	protected ModelAndView createModelAndView(String page, String entity, ID user) {
		ModelAndView mv = createModelAndView(page);
		
		Entity entityMeta = MetadataHelper.getEntity(entity);
		EasyMeta easy = new EasyMeta(entityMeta);
		mv.getModel().put("entityName", easy.getName());
		mv.getModel().put("entityLabel", easy.getLabel());
		mv.getModel().put("entityIcon", easy.getIcon());
		
		if (EntityHelper.hasPrivilegesField(entityMeta)) {
			Privileges priv = Application.getSecurityManager().getPrivileges(user, entityMeta.getEntityCode());
			Permission[] actions = new Permission[] {
					BizzPermission.CREATE,
					BizzPermission.DELETE,
					BizzPermission.UPDATE,
					BizzPermission.READ,
					BizzPermission.ASSIGN,
					BizzPermission.SHARE,
			};
			Map<String, Boolean> actionMap = new HashMap<>();
			for (Permission act : actions) {
				actionMap.put(act.getName(), priv.allowed(act));
			}
			mv.getModel().put("entityPrivileges", JSON.toJSONString(actionMap));
		} else {
			mv.getModel().put("entityPrivileges", "{}");
		}
		return mv;
	}
	
	/**
	 * @param page
	 * @param record
	 * @param user
	 * @return
	 */
	protected ModelAndView createModelAndView(String page, ID record, ID user) {
		ModelAndView mv = createModelAndView(page);
		
		Entity entityMeta = MetadataHelper.getEntity(record.getEntityCode());
		EasyMeta easy = new EasyMeta(entityMeta);
		mv.getModel().put("entityName", easy.getName());
		mv.getModel().put("entityLabel", easy.getLabel());
		mv.getModel().put("entityIcon", easy.getIcon());
		
		// TODO 验证记录权限
		
		if (EntityHelper.hasPrivilegesField(entityMeta)) {
			Privileges priv = Application.getSecurityManager().getPrivileges(user, entityMeta.getEntityCode());
			Permission[] actions = new Permission[] {
					BizzPermission.CREATE,
					BizzPermission.DELETE,
					BizzPermission.UPDATE,
					BizzPermission.READ,
					BizzPermission.ASSIGN,
					BizzPermission.SHARE,
			};
			Map<String, Boolean> actionMap = new HashMap<>();
			for (Permission act : actions) {
				actionMap.put(act.getName(), priv.allowed(act));
			}
			mv.getModel().put("entityPrivileges", JSON.toJSONString(actionMap));
		} else {
			mv.getModel().put("entityPrivileges", "{}");
		}
		return mv;
	}
	
	// --
	
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
