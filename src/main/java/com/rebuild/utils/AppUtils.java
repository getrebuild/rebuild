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

package com.rebuild.utils;

import java.io.File;
import java.nio.file.AccessDeniedException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;

import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
public class AppUtils {

	/**
	 * 获取当前请求用户
	 * 
	 * @param request
	 * @return
	 */
	public static ID getRequestUser(HttpServletRequest request) {
		Object user = request.getSession(true).getAttribute(WebUtils.CURRENT_USER);
		return user == null ? null : (ID) user;
	}
	
	/**
	 * 格式化客户端消息
	 * 
	 * @param errCode
	 * @param errMsgOrData
	 * @return
	 */
	public static String formatClientMsg(int errCode, String errMsgOrData) {
		JSONObject jo = new JSONObject();
		jo.put("error_code", errCode);
		if (errMsgOrData != null) {
			if (errCode == 0) {
				jo.put("data", errMsgOrData);
			} else {
				jo.put("error_msg", errMsgOrData);
			}
		}
		return jo.toJSONString();
	}
	
	/**
	 * 获取后台抛出的错误消息
	 * 
	 * @param request
	 * @param exception
	 * @return
	 */
	public static String getErrorMessage(HttpServletRequest request, Throwable exception) {
		String errorMsg = (String) request.getAttribute(ServletUtils.ERROR_MESSAGE);
		if (StringUtils.isNotBlank(errorMsg)) {
			return errorMsg;
		}
		
		Throwable ex = (Throwable) request.getAttribute(ServletUtils.ERROR_EXCEPTION);
		if (ex == null) {
			ex = (Throwable) request.getAttribute(ServletUtils.JSP_JSP_EXCEPTION);
		}
		if (ex == null && exception != null) {
			ex = exception;
		}
		if (ex != null) {
			ex = ThrowableUtils.getRootCause(ex);
		}
		
		if (ex == null) {
			Integer sc = (Integer) request.getAttribute(ServletUtils.ERROR_STATUS_CODE);
			if (sc == null) {
				return "系统错误";
			} else if (sc == 404) {
				return "访问的页面不存在";
			} else {
				return "系统错误 (" + sc + ")";
			}
		} else if (ex instanceof AccessDeniedException) {
			String msg = StringUtils.defaultIfEmpty(ex.getLocalizedMessage(), "");
			if (msg.contains("AJAX403")) {
				return "AJAX403";
			}
			return "权限不足";
		}
		
		if (Application.devMode()) {
			return ex.getClass().getSimpleName() + " : " + ex.getLocalizedMessage();
		} else {
			return ex.getLocalizedMessage();
		}
	}
	
	/**
	 * @param fileName
	 * @return
	 */
	public static File getFileOfTemp(String fileName) {
		return new File(FileUtils.getTempDirectory(), fileName);
	}
}
