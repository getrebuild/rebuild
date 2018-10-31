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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import com.rebuild.server.ServerListener;
import com.rebuild.server.ServersStatus;
import com.rebuild.utils.AppUtils;
import com.rebuild.web.admin.AdminEntryControll;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;

/**
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-24
 */
public class RequestWatchHandler extends HandlerInterceptorAdapter {

	private static final Log LOG = LogFactory.getLog(RequestWatchHandler.class);
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
			Object handler) throws Exception {
		
		if (!ServersStatus.isStatusOK()) {
			ServletUtils.forward(request, response, "/servers-status.jsp");
			return false;
		}
		
		Application.getSessionStore().storeLastActive(request);
		
		boolean chain = super.preHandle(request, response, handler);
		if (chain) {
			return verfiyPass(request, response);
		}
		return false;
	}
	
	@Override
	public void afterCompletion(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception exception)
			throws Exception {
		super.afterCompletion(request, response, handler, exception);
		
		final ID CALLER = Application.getSessionStore().getCurrentCaller(true);
		if (CALLER != null) {
			Application.getSessionStore().clearCurrentCaller();
		}
		
		logProgressTime(request);
		
		if (exception != null) {
			Throwable rootCause = ThrowableUtils.getRootCause(exception);
			String errorMsg = "系统繁忙！请稍后重试";
			if (rootCause instanceof RebuildException) {
				errorMsg = ((RebuildException) rootCause).toClientMsgString();
			}
			
			StringBuffer sb = new StringBuffer()
					.append("\n++ EXECUTE REQUEST ERROR(s) TRACE +++++++++++++++++++++++++++++++++++++++++++++")
					.append("\nUser      : ").append(CALLER == null ? "-" : CALLER)
					.append("\nHandler   : ").append(request.getRequestURI() + " [ " + handler + " ]")
					.append("\nIP        : ").append(ServletUtils.getRemoteAddr(request))
					.append("\nReferer   : ").append(StringUtils.defaultIfEmpty(ServletUtils.getReferer(request), "-"))
					.append("\nUserAgent : ").append(StringUtils.defaultIfEmpty(request.getHeader("user-agent"), "-"))
					.append("\nCause     : ").append(rootCause.getClass().getName())
					.append("\nMessage   : ").append(StringUtils.defaultIfBlank(rootCause.getMessage(), "-"));
			LOG.error(sb, rootCause);
			ServletUtils.writeJson(response, 
					AppUtils.formatClientMsg(BaseControll.CODE_ERROR, errorMsg));
		}
	}
	
	/**
	 * 处理时间 LOG
	 * 
	 * @param request
	 */
	protected void logProgressTime(HttpServletRequest request) {
		Long startTime = (Long) request.getAttribute(TIMEOUT_KEY);
		startTime = System.currentTimeMillis() - startTime;
		if (startTime > 500) {
			String url = request.getRequestURI();
			String qstr = request.getQueryString();
			if (qstr != null) url += '?' + qstr;
			LOG.warn("Method handle time " + startTime + " ms. Request URL [ " + url + " ] from [ " + StringUtils.defaultIfEmpty(ServletUtils.getReferer(request), "-") + " ]");
		}
	}
	
	// --
	
	private static final String TIMEOUT_KEY = "ErrorHandler_TIMEOUT";
	
	/**
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	public static boolean verfiyPass(HttpServletRequest request, HttpServletResponse response) throws IOException {
		request.setAttribute(TIMEOUT_KEY, System.currentTimeMillis());
		
		String requestUrl = request.getRequestURI();
		String qstr = request.getQueryString();
		if (StringUtils.isNotBlank(qstr)) {
			requestUrl += "?" + qstr;
		}
		
		ID user = AppUtils.getRequestUser(request);
		if (user != null) {
			Application.getSessionStore().setCurrentCaller(user);
			
			// 管理后台访问
			if (requestUrl.contains("/admin/") && !AdminEntryControll.isAdminVerified(request)) {
				if (ServletUtils.isAjaxRequest(request)) {
					ServletUtils.writeJson(response, AppUtils.formatClientMsg(403, "请验证管理员访问权限"));
				} else {
					response.sendRedirect(ServerListener.getContextPath() + "/user/admin-entry?nexturl=" + CodecUtils.urlEncode(requestUrl));
				}
				return false;
			}
			
		} else {
			if (!isIgnoreRes(requestUrl)) {
				LOG.warn("Unauthorized access [ " + requestUrl + " ] from [ " + ServletUtils.getReferer(request) + " ]");
				if (ServletUtils.isAjaxRequest(request)) {
					ServletUtils.writeJson(response, AppUtils.formatClientMsg(403, "未授权访问"));
				} else {
					response.sendRedirect(ServerListener.getContextPath() + "/user/login?nexturl=" + CodecUtils.urlEncode(requestUrl));
				}
				return false;
			}
		}
		return true;
	}
	
	static boolean isIgnoreRes(String requestUrl) {
		if (requestUrl.contains("/user/") && !requestUrl.contains("/user/admin")) {
			return true;
		} else if (requestUrl.contains("/assets")) {
			return true;
		}
		return false;
	}
}
