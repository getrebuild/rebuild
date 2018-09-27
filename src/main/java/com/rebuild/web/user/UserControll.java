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

package com.rebuild.web.user;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.rebuild.server.Application;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 07/25/2018
 */
@Controller
@RequestMapping("/user")
public class UserControll extends BaseControll {

	@RequestMapping("login")
	public ModelAndView checkLogin(HttpServletRequest request) {
		// TODO 检查自动登录
		return createModelAndView("/user/login.jsp");
	}
	
	@RequestMapping("user-login")
	public void userLogin(HttpServletRequest request, HttpServletResponse response) {
		String user = getParameterNotNull(request, "user");
		String passwd = getParameterNotNull(request, "passwd");
		
		if (!Application.getUserStore().exists(user)) {
			writeFailure(response, "用户名或密码错误");
			return;
		}
		
		Object[] foundUser = Application.createNoFilterQuery(
				"select userId,password,isDisabled from User where loginName = ?")
				.setParameter(1, user)
				.unique();
		if (!foundUser[1].equals(EncryptUtils.toSHA256Hex(passwd))) {
			writeFailure(response, "用户名或密码错误");
			return;
		}
		if ((boolean) foundUser[2]) {
			writeFailure(response, "用户已禁用");
			return;
		}
		
		ServletUtils.setSessionAttribute(request, WebUtils.CURRENT_USER, foundUser[0]);
		writeSuccess(response);
	}
	
	@RequestMapping("logout")
	public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ServletUtils.getSession(request).invalidate();
		response.sendRedirect("login");
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
