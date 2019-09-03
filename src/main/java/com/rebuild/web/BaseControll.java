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

package com.rebuild.web;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.Controll;
import com.rebuild.utils.AppUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 基础 Controll
 * 
 * @author zhaofang123@gmail.com
 * @since 05/21/2017
 */
public abstract class BaseControll extends Controll {
	
	/**
	 * @param request
	 * @return
	 */
	protected ID getRequestUser(HttpServletRequest request) {
		ID userId = AppUtils.getRequestUser(request);
		if (userId == null) {
			throw new IllegalParameterException("无效请求用户");
		}
		return userId;
	}
	
	/**
	 * @param response
	 */
	protected void writeSuccess(HttpServletResponse response) {
		writeSuccess(response, null);
	}
	
	/**
	 * @param response
	 * @param data
	 */
	protected void writeSuccess(HttpServletResponse response, Object data) {
		writeJSON(response, formatSuccess(data));
	}
	
	/**
	 * @param response
	 */
	protected void writeFailure(HttpServletResponse response) {
		writeFailure(response, null);
	}
	
	/**
	 * @param response
	 * @param message
	 */
	protected void writeFailure(HttpServletResponse response, String message) {
		writeJSON(response, formatFailure(message));
	}
	
	/**
	 * @param response
	 * @param aJson
	 */
	protected void writeJSON(HttpServletResponse response, Object aJson) {
		if (aJson == null) {
			throw new IllegalArgumentException();
		}
		
		String aJsonString = null;
		if (aJson instanceof String) {
			aJsonString = (String) aJson;
		} else {
			aJsonString = JSON.toJSONString(aJson);
		}
		ServletUtils.writeJson(response, aJsonString);
	}
	
	/**
	 * @param req
	 * @param name
	 * @return
	 */
	protected String getParameter(HttpServletRequest req, String name) {
		return req.getParameter(name);
	}
	
	/**
	 * @param req
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	protected String getParameter(HttpServletRequest req, String name, String defaultValue) {
		return StringUtils.defaultIfBlank(getParameter(req, name), defaultValue);
	}
	
	/**
	 * @param req
	 * @param name
	 * @return
	 */
	protected String getParameterNotNull(HttpServletRequest req, String name) {
		String v = req.getParameter(name);
		if (StringUtils.isEmpty(v)) {
			throw new IllegalParameterException("无效参数 [" + name + "=" + v + "]");
		}
		return v;
	}
	
	/**
	 * @param req
	 * @param name
	 * @return
	 */
	protected Integer getIntParameter(HttpServletRequest req, String name) {
		String v = req.getParameter(name);
		if (v == null) {
			return null;
		}
		return NumberUtils.toInt(v);
	}
	
	/**
	 * @param req
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	protected Integer getIntParameter(HttpServletRequest req, String name, int defaultValue) {
		Integer v = getIntParameter(req, name);
		return v == null ? (defaultValue) : v;
	}
	
	/**
	 * @param req
	 * @param name
	 * @return
	 */
	protected boolean getBoolParameter(HttpServletRequest req, String name) {
		String v = req.getParameter(name);
		return v != null && BooleanUtils.toBoolean(v);
	}
	
	/**
	 * @param req
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	protected boolean getBoolParameter(HttpServletRequest req, String name, boolean defaultValue) {
		String v = req.getParameter(name);
		return v == null ? defaultValue : BooleanUtils.toBoolean(v);
	}
	
	/**
	 * @param req
	 * @param name
	 * @return
	 */
	protected ID getIdParameter(HttpServletRequest req, String name) {
		String v = req.getParameter(name);
		return ID.isId(v) ? ID.valueOf(v) : null;
	}
	
	/**
	 * @param req
	 * @param name
	 * @return
	 */
	protected ID getIdParameterNotNull(HttpServletRequest req, String name) {
		String v = req.getParameter(name);
		if (ID.isId(v)) {
			return ID.valueOf(v);
		}
		throw new IllegalParameterException("无效ID参数 [" + name + "=" + v + "]");
	}
}
