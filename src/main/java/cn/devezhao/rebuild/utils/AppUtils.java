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

package cn.devezhao.rebuild.utils;

import java.io.File;
import java.nio.file.AccessDeniedException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSONObject;

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
	 * 获取请求用户
	 * 
	 * @param request
	 * @return
	 */
	public static ID getRequestUser(HttpServletRequest request) {
		Object current = request.getSession(true).getAttribute(WebUtils.CURRENT_USER);
		return current == null ? null : (ID) current;
	}
	
	/**
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
			Integer statusCode = (Integer) request.getAttribute(ServletUtils.ERROR_STATUS_CODE);
			if (statusCode != null) {
				return "系统错误:SC" + statusCode;
			} else {
				return "系统错误";
			}
		} else if (ex instanceof AccessDeniedException) {
			String msg = StringUtils.defaultIfEmpty(ex.getLocalizedMessage(), "");
			if (msg.contains("AJAX403")) {
				return "AJAX403";
			}
			return "权限不足";
		}
		return ex.getClass().getSimpleName() + ":" + ex.getLocalizedMessage();
	}
	
	/**
	 * @param fileName
	 * @return
	 */
	public static File getFileOfTemp(String fileName) {
		return new File(FileUtils.getTempDirectory(), fileName);
	}
}
