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

package com.rebuild.web.userprofile;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.rebuild.server.Application;
import com.rebuild.server.bizz.UserService;
import com.rebuild.server.helper.SystemConfiguration;
import com.rebuild.server.helper.VCode;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 10/08/2018
 */
@Controller
public class UserProfileControll extends BaseControll {

	@RequestMapping("/settings/account")
	public ModelAndView pageView(HttpServletRequest request) throws IOException {
		return createModelAndView("/user-profile/account.jsp", "User", getRequestUser(request));
	}
	
	@RequestMapping({ "/people/{user}", "/userhome/{user}" })
	public ModelAndView pagePeople(@PathVariable String user, HttpServletRequest request) throws IOException {
		// TODO 用户首页
		return createModelAndView("/user-profile/user-home.jsp", "User", getRequestUser(request));
	}
	
	@RequestMapping("/settings/send-email-vcode")
	public void sendEmailVcode(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (SystemConfiguration.getEmailAccount() == null) {
			writeFailure(response, "邮箱账户未配置，无法发送验证码");
			return;
		}
		
		String email = getParameterNotNull(request, "email");
		if (Application.getUserStore().existsEmail(email)) {
			writeFailure(response, "邮箱已被占用，请换用其他邮箱");
			return;
		}
		
		String vcode = VCode.random(email);
		writeSuccess(response, vcode);
	}
	
	@RequestMapping("/settings/save-email")
	public void saveEmail(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		String email = getParameterNotNull(request, "email");
		String vcode = getParameterNotNull(request, "vcode");
		
		if (!VCode.verfiy(email, vcode)) {
			writeFailure(response, "验证码无效");
			return;
		}
		if (Application.getUserStore().existsEmail(email)) {
			writeFailure(response, "邮箱已被占用，请换用其他邮箱");
			return;
		}
		
		Record record = EntityHelper.forUpdate(user, user);
		record.setString("email", email);
		Application.getBean(UserService.class).update(record);
		writeSuccess(response);
	}
	
	@RequestMapping("/settings/save-passwd")
	public void savePasswd(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		String oldp = getParameterNotNull(request, "oldp");
		String newp = getParameterNotNull(request, "newp");
		
		Object[] o = Application.createQuery("select password from User where userId = ?")
				.setParameter(1, user)
				.unique();
		if (o == null || !StringUtils.equals((String) o[0], EncryptUtils.toSHA256Hex(oldp))) {
			writeFailure(response, "原密码输入有误");
			return;
		}
		
		Record record = EntityHelper.forUpdate(user, user);
		record.setString("password", newp);
		Application.getBean(UserService.class).update(record);
		writeSuccess(response);
	}
}
