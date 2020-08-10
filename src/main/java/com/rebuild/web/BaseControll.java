/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.rebuild.api.Controll;
import com.rebuild.server.helper.language.LanguageBundle;
import com.rebuild.server.helper.language.Languages;
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
		ID user = AppUtils.getRequestUser(request);
		if (user == null) {
			user = AppUtils.getRequestUserViaRbMobile(request, false);
		}

		if (user == null) {
			throw new InvalidParameterException("无效请求用户");
		}
		return user;
	}

	/**
	 * @param request
	 * @return
	 */
	protected LanguageBundle getBundle(HttpServletRequest request) {
		String locale = AppUtils.getLocale(request);
		return Languages.instance.getBundle(locale);
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
		
		String aJsonString;
		if (aJson instanceof String) {
			aJsonString = (String) aJson;
		} else {
			// fix: $ref.xxx
			aJsonString = JSON.toJSONString(aJson,
					SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteMapNullValue);
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
			throw new InvalidParameterException("无效参数 [" + name + "=" + v + "]");
		}
		return v;
	}
	
	/**
	 * @param req
	 * @param name
	 * @return
	 */
	protected Integer getIntParameter(HttpServletRequest req, String name) {
		return getIntParameter(req, name, null);
	}
	
	/**
	 * @param req
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	protected Integer getIntParameter(HttpServletRequest req, String name, Integer defaultValue) {
		String v = req.getParameter(name);
		if (v == null) {
			return defaultValue;
		}
		return NumberUtils.toInt(v, defaultValue);
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
		throw new InvalidParameterException("无效ID参数 [" + name + "=" + v + "]");
	}
}
