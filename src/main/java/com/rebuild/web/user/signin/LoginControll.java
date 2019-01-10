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

package com.rebuild.web.user.signin;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.rebuild.server.Application;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.utils.AES;
import com.rebuild.web.BasePageControll;
import com.wf.captcha.utils.CaptchaUtil;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.engine.ID;

/**
 * @author zhaofang123@gmail.com
 * @since 07/25/2018
 */
@Controller
@RequestMapping("/user/")
public class LoginControll extends BasePageControll {
	
	private static final String AUTOLOGIN_KEY = "rb.alt";

	@RequestMapping("login")
	public ModelAndView checkLogin(HttpServletRequest request, HttpServletResponse response) {
		String alt = ServletUtils.readCookie(request, AUTOLOGIN_KEY);
		if (StringUtils.isNotBlank(alt)) {
			try {
				alt = AES.decrypt(alt);
				String alt_s[] = alt.split(",");
				ID user = ID.valueOf(alt_s[0]);
				if (Application.getUserStore().exists(user)) {
					loginSuccessed(request, response, user, true);
					
					// TODO 安全性检查
					
					String nexturl = StringUtils.defaultIfBlank(request.getParameter("nexturl"), "../dashboard/home");
					nexturl = CodecUtils.urlDecode(nexturl);
					response.sendRedirect(nexturl);
				}
			} catch (Exception ex) {
				LOG.error("自动登录失败 : " + alt, ex);
			}
		}
		
		return createModelAndView("/user/login.jsp");
	}
	
	@RequestMapping("user-login")
	public void userLogin(HttpServletRequest request, HttpServletResponse response) {
		String user = getParameterNotNull(request, "user");
		String passwd = getParameterNotNull(request, "passwd");
		
		String vcode = getParameter(request, "vcode");
		if (StringUtils.isNotBlank(vcode) && !CaptchaUtil.ver(vcode, request)) {
			writeFailure(response, "验证码错误");
			return;
		}
		
		int retry = getLoginRetry(user, 1);
		if (retry >= 3 && StringUtils.isBlank(vcode)) {
			ServletUtils.setSessionAttribute(request, "needVcode", retry);
			writeFailure(response, "VCODE");
			return;
		}
		
		if (!Application.getUserStore().exists(user)) {
			writeFailure(response, "用户名或密码错误");
			return;
		}
		
		Object[] foundUser = Application.createQueryNoFilter(
				"select userId,password from User where loginName = ?")
				.setParameter(1, user)
				.unique();
		if (!foundUser[1].equals(EncryptUtils.toSHA256Hex(passwd))) {
			writeFailure(response, "用户名或密码错误");
			return;
		}
		
		User loginUser = Application.getUserStore().getUser((ID) foundUser[0]);
		if (!loginUser.isActive()) {
			writeFailure(response, "用户未激活");
			return;
		}
		
		loginSuccessed(request, response, (ID) foundUser[0], getBoolParameter(request, "autoLogin", false));
		getLoginRetry(user, -1);
		ServletUtils.setSessionAttribute(request, "needVcode", null);
		
		writeSuccess(response);
	}
	
	private int getLoginRetry(String user, int state) {
		String key = "LoginRetry-" + user;
		if (state == -1) {
			Application.getCommonCache().evict(key);
			return 0;
		}
		
		Integer retry = (Integer) Application.getCommonCache().getx(key);
		retry = retry == null ? 0 : retry;
		if (state == 1) {
			retry += 1;
			Application.getCommonCache().putx(key, retry);
		}
		return retry;
	}
	
	/**
	 * 登录成功
	 * 
	 * @param request
	 * @param response
	 * @param user
	 * @param autoLogin
	 */
	private void loginSuccessed(HttpServletRequest request, HttpServletResponse response, ID user, boolean autoLogin) {
		// 自动登录
		if (autoLogin) {
			String alt = user + "," + System.currentTimeMillis();
			alt = AES.encrypt(alt);
			ServletUtils.addCookie(response, AUTOLOGIN_KEY, alt, 60 * 60 * 24 * 30, null, "/");
		} else {
			ServletUtils.removeCookie(request, response, AUTOLOGIN_KEY);
		}
		
		ServletUtils.setSessionAttribute(request, WebUtils.CURRENT_USER, user);
		Application.getSessionStore().storeLoginSuccessed(request);
	}
	
	@RequestMapping("logout")
	public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ServletUtils.removeCookie(request, response, AUTOLOGIN_KEY);
		ServletUtils.getSession(request).invalidate();
		response.sendRedirect("login?exit=0");
	}
	
	// --
	
	@RequestMapping("forgot-passwd")
	public ModelAndView forgotPasswd(HttpServletRequest request, HttpServletResponse response) {
		return createModelAndView("/user/forgot-passwd.jsp");
	}
	
	@RequestMapping("user-forgot-passwd")
	public void userForgotPasswd(HttpServletRequest request, HttpServletResponse response) {
	}
}
