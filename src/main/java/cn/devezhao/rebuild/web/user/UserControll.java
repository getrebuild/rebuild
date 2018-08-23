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

package cn.devezhao.rebuild.web.user;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.web.commons.BaseControll;

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
		
		Object[] foundUser = Application.createQuery(
				"select userId,password,isDisabled from User where loginName = ?")
				.setParameter(1, user)
				.unique();
		if (foundUser == null) {
			writeFailure(response, "用户名或密码错误");
			return;
		}
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
