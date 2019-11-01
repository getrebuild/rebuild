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

package com.rebuild.web.user.signin;

import cn.devezhao.commons.RegexUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSONObject;
import com.hankcs.hanlp.HanLP;
import com.rebuild.server.Application;
import com.rebuild.server.helper.BlackList;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.SMSender;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.VCode;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.web.BasePageControll;
import com.wf.captcha.utils.CaptchaUtil;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;

/**
 * 注册
 * 
 * @author devezhao
 * @since 11/01/2018
 */
@Controller
@RequestMapping("/user/")
public class SignUpControll extends BasePageControll {
	
	private static final String MSG_VCODE = "<p>你的注册邮箱验证码是 <b>%s</b></p>";
	private static final String MSG_PENDING = "<p>%s 欢迎注册！以下为你的登录信息，请妥善保管。</p><div style='margin:10px 0'>登录账号 <b>%s</b><br>登录密码 <b>%s</b><br>登录地址 <a href='%s'>%s</a></div><p>目前你还无法登录系统，因为系统管理员正在审核你的注册信息。完成后会通过邮件通知你，请耐心等待。</p>";
	
	@RequestMapping("signup")
	public ModelAndView pageSignup(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (!SysConfiguration.getBool(ConfigurableItem.OpenSignUp)) {
			response.sendError(400, "管理员未开放公开注册");
			return null;
		}
		return createModelAndView("/user/signup.jsp");
	}
	
	@RequestMapping("signup-email-vcode")
	public void signupEmailVcode(HttpServletRequest request, HttpServletResponse response) {
		if (!SMSender.availableMail()) {
			writeFailure(response, "邮件服务账户未配置，请联系管理员配置");
			return;
		}
		
		String email = getParameterNotNull(request, "email");
		if (!RegexUtils.isEMail(email)) {
			writeFailure(response, "无效邮箱");
			return;
		}
		if (Application.getUserStore().existsEmail(email)) {
			writeFailure(response, "注册邮箱已存在");
			return;
		}
		
		String vcode = VCode.generate(email, 2);
		String content = String.format(MSG_VCODE, vcode);
		String sentid = SMSender.sendMail(email, "注册验证码", content);
		LOG.warn(email + " >> " + content);
		if (sentid != null) {
			writeSuccess(response);
		} else {
			writeFailure(response, "无法发送验证码，请稍后重试");
		}
	}

	@SuppressWarnings("DuplicatedCode")
	@RequestMapping("signup-confirm")
	public void signupConfirm(HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);
		String email = data.getString("email");
		String vcode = data.getString("vcode");
		if (!VCode.verfiy(email, vcode, true)) {
			writeFailure(response, "验证码无效");
			return;
		}
		
		String loginName = data.getString("loginName");
		String fullName = data.getString("fullName");
		String passwd = VCode.generate(loginName, 2);
		VCode.clean(loginName);
		
		Record userNew = EntityHelper.forNew(EntityHelper.User, UserService.SYSTEM_USER);
		userNew.setString("email", email);
		userNew.setString("loginName", loginName);
		userNew.setString("fullName", fullName);
		userNew.setString("password", passwd);
		userNew.setBoolean("isDisabled", true);
		try {
			Application.getBean(UserService.class).txSignUp(userNew);
			
			String homeUrl = SysConfiguration.getHomeUrl();
			String content = String.format(MSG_PENDING, 
					fullName, loginName, passwd, homeUrl, homeUrl);
			SMSender.sendMail(email, "管理员正在审核你的注册信息", content);
			writeSuccess(response);
		} catch (DataSpecificationException ex) {
			writeFailure(response, ex.getLocalizedMessage());
		}
	}
	
	@RequestMapping("checkout-name")
	public void checkoutName(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String fullName = getParameterNotNull(request, "fullName");
		
		fullName = fullName.replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5]", "");
		String loginName = HanLP.convertToPinyinString(fullName, "", false);
		if (loginName.length() > 20) {
			loginName = loginName.substring(0, 20);
		}
		if (BlackList.isBlack(loginName)) {
			writeSuccess(response);
			return;
		}
		
		for (int i = 0; i < 100; i++) {
			if (Application.getUserStore().existsName(loginName)) {
				loginName += RandomUtils.nextInt(99);
			} else {
				break;
			}
		}
		
		loginName = loginName.toLowerCase();
		writeSuccess(response, loginName);
	}
	
	@RequestMapping("captcha")
	public void captcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Font font = new Font(Font.SERIF, Font.BOLD & Font.ITALIC, 22 + RandomUtils.nextInt(8));
		int codeLen = 4 + RandomUtils.nextInt(3);
		CaptchaUtil.outPng(160, 41, codeLen, font, request, response);
	}
}