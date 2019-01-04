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

package com.rebuild.web.user.settings;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.rebuild.server.Application;
import com.rebuild.server.helper.SMSender;
import com.rebuild.server.helper.SystemConfiguration;
import com.rebuild.server.helper.VCode;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 10/08/2018
 */
@RequestMapping("/settings")
@Controller
public class UserSettingsControll extends BaseControll {

	@RequestMapping("/account")
	public ModelAndView pageView(HttpServletRequest request) throws IOException {
		return createModelAndView("/user-settings/account.jsp", "User", getRequestUser(request));
	}
	
	@RequestMapping("/account/send-email-vcode")
	public void sendEmailVcode(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String email = getParameterNotNull(request, "email");
		if (Application.getUserStore().existsEmail(email)) {
			writeFailure(response, "邮箱已被占用，请换用其他邮箱");
			return;
		}
		
		String vcode = VCode.random(email);
		String content = "<p>你的邮箱验证码是 <b>" + vcode + "</b><p>";
		String sentid = SMSender.sendMail(email, "邮箱验证码", content);
		if (sentid != null) {
			writeSuccess(response);
		} else {
			if (SystemConfiguration.getMailAccount() == null) {
				writeFailure(response, "邮件账户未配置，无法发送验证码");
			} else {
				writeFailure(response, "验证码发送失败，请稍后重试");
			}
		}
	}
	
	@RequestMapping("/account/save-email")
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
	
	@RequestMapping("/account/save-passwd")
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
