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

package com.rebuild.utils;

import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.Controll;
import com.rebuild.server.Application;
import com.rebuild.server.ServerListener;
import com.rebuild.server.service.bizz.privileges.ZeroEntry;
import com.rebuild.web.admin.AdminEntryControll;
import eu.bitwalker.useragentutils.Browser;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.AccessDeniedException;

/**
 * 封裝一些有用的工具方法
 * 
 * @author zhaofang123@gmail.com
 * @since 05/19/2018
 */
public class AppUtils {
	
	/**
	 * @return
	 * @see Application#devMode()
	 */
	public static boolean devMode() {
		return Application.devMode();
	}
	
	/**
	 * @return
	 * @see ServerListener#getContextPath()
	 */
	public static String getContextPath() {
		return ServerListener.getContextPath();
	}

	/**
	 * 获取当前请求用户
	 * 
	 * @param request
	 * @return null or UserID
	 */
	public static ID getRequestUser(HttpServletRequest request) {
		Object user = request.getSession(true).getAttribute(WebUtils.CURRENT_USER);
		return user == null ? null : (ID) user;
	}
	
	/**
	 * @param request
	 * @return
	 */
	public static boolean isAdminUser(HttpServletRequest request) {
		ID uid = getRequestUser(request);
		if (uid == null) {
			return false;
		}
		return Application.getUserStore().getUser(uid).isAdmin();
	}
	
	/**
	 * @param request
	 * @return
	 */
	public static boolean isAdminVerified(HttpServletRequest request) {
		return AdminEntryControll.isAdminVerified(request);
	}
	
	/**
	 * 格式化标准的客户端消息
	 * 
	 * @param errorCode
	 * @param errorMsg
	 * @return
	 * @see Controll
	 */
	public static String formatControllMsg(int errorCode, String errorMsg) {
		JSONObject map = new JSONObject();
		map.put("error_code", errorCode);
		if (errorMsg != null) {
			if (errorCode == 0) {
				map.put("data", errorMsg);
			} else {
				map.put("error_msg", errorMsg);
			}
		}
		return map.toJSONString();
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
			Integer state = (Integer) request.getAttribute(ServletUtils.ERROR_STATUS_CODE);
			if (state != null && state == 404) {
				return "访问的地址/资源不存在";
			} else {
				return "未知错误，请稍后重试";
			}
		} else if (ex instanceof AccessDeniedException) {
			return "权限不足，访问被阻止";
		}
		
		errorMsg = StringUtils.defaultIfBlank(ex.getLocalizedMessage(), "未知错误，请稍后重试");
		return ex.getClass().getSimpleName() + " : " + errorMsg;
	}
	
	/**
	 * 是否低于 IE11
	 * 
	 * @param request
	 * @return
	 */
	public static boolean isLessIE11(HttpServletRequest request) {
		String userAgent = request.getHeader("user-agent");
		Browser browser = Browser.parseUserAgentString(userAgent);
		return browser == Browser.IE6 || browser == Browser.IE7 || browser == Browser.IE8 || browser == Browser.IE9 || browser == Browser.IE10;
	}

	/**
	 * @param request
	 * @param entry
	 * @return
	 */
	public static boolean allowed(HttpServletRequest request, ZeroEntry entry) {
		return Application.getSecurityManager().allowed(getRequestUser(request), entry);
	}
}
