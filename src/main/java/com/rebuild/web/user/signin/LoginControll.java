/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.user.signin;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.RegexUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.LoginToken;
import com.rebuild.api.AuthTokenManager;
import com.rebuild.server.Application;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.License;
import com.rebuild.server.helper.SMSender;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.VCode;
import com.rebuild.server.helper.cache.CommonCache;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.bizz.privileges.User;
import com.rebuild.utils.AES;
import com.rebuild.utils.AppUtils;
import com.rebuild.web.BasePageControll;
import com.rebuild.web.common.LanguagesControll;
import com.wf.captcha.utils.CaptchaUtil;
import eu.bitwalker.useragentutils.UserAgent;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.rebuild.server.helper.language.Languages.lang;

/**
 * @author zhaofang123@gmail.com
 * @since 07/25/2018
 */
@Controller
@RequestMapping("/user/")
public class LoginControll extends BasePageControll {
	
	public static final String CK_AUTOLOGIN = "rb.alt";

	public static final String SK_LOGINID = WebUtils.KEY_PREFIX + ".LOGINID";

	private static final String NEED_VCODE = "needLoginVCode";

	private static final String DEFAULT_HOME = "../dashboard/home";

	@RequestMapping("login")
	public ModelAndView checkLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (AppUtils.getRequestUser(request) != null) {
			response.sendRedirect(DEFAULT_HOME);
			return null;
		}

		// 切换语言
        if (LanguagesControll.switchLanguage(request)) {
            response.sendRedirect("login?locale=" + getParameter(request, "locale"));
            return null;
        }

		// 授权 Token 登录
		String token = getParameter(request, "token");
		if (StringUtils.isNotBlank(token)) {
			ID tokenUser = AuthTokenManager.verifyToken(token, true);
			if (tokenUser != null) {
				loginSuccessed(request, response, tokenUser, false);

				String nexturl = StringUtils.defaultIfBlank(request.getParameter("nexturl"), DEFAULT_HOME);
				response.sendRedirect(CodecUtils.urlDecode(nexturl));
				return null;
			} else {
				// 立即显示验证码
				ServletUtils.setSessionAttribute(request, NEED_VCODE, true);
			}
		}

		// Cookie 记住登录
		String alt = ServletUtils.readCookie(request, CK_AUTOLOGIN);
		if (StringUtils.isNotBlank(alt)) {
			ID altUser = null;
			try {
				alt = AES.decrypt(alt);
				String[] alts = alt.split(",");
				altUser = ID.isId(alts[0]) ? ID.valueOf(alts[0]) : null;

				// 最大一个月有效期
				if (altUser != null) {
					long t = ObjectUtils.toLong(alts[1]);
					if ((System.currentTimeMillis() - t) / 1000 > 30 * 24 * 60 * 60) {
						altUser = null;
					}
				}

			} catch (Exception ex) {
				LOG.error("Can't decode User from alt : " + alt, ex);
			}
			
			if (altUser != null && Application.getUserStore().existsUser(altUser)) {
				loginSuccessed(request, response, altUser, true);
				
				String nexturl = StringUtils.defaultIfBlank(request.getParameter("nexturl"), DEFAULT_HOME);
				response.sendRedirect(CodecUtils.urlDecode(nexturl));
				return null;
			} else {
				// 显示验证码
				ServletUtils.setSessionAttribute(request, NEED_VCODE, true);
			}
		}

		// 登录页
		return createModelAndView("/user/login.jsp");
	}
	
	@RequestMapping("user-login")
	public void userLogin(HttpServletRequest request, HttpServletResponse response) {
		String vcode = getParameter(request, "vcode");
		Boolean needVcode = (Boolean) ServletUtils.getSessionAttribute(request, NEED_VCODE);
		if (needVcode != null && needVcode
				&& (StringUtils.isBlank(vcode) || !CaptchaUtil.ver(vcode, request))) {
			writeFailure(response, lang("InputWrong", "Captcha"));
			return;
		}

		final String user = getParameterNotNull(request, "user");
		final String password = ServletUtils.getRequestString(request);
		
		int retry = getLoginRetryTimes(user, 1);
		if (retry > 3 && StringUtils.isBlank(vcode)) {
			ServletUtils.setSessionAttribute(request, NEED_VCODE, true);
			writeFailure(response, "VCODE");
			return;
		}

		String hasError = LoginToken.checkUser(user, password);
		if (hasError != null) {
			writeFailure(response, hasError);
			return;
		}

		User loginUser = Application.getUserStore().getUser(user);
		loginSuccessed(request, response, loginUser.getId(), getBoolParameter(request, "autoLogin", false));

		// 清理
		getLoginRetryTimes(user, -1);
		ServletUtils.setSessionAttribute(request, NEED_VCODE, null);
		
		writeSuccess(response);
	}
	
	/**
	 * @param user
	 * @param state
	 * @return
	 */
	private int getLoginRetryTimes(String user, int state) {
		String key = "LoginRetry-" + user;
		if (state == -1) {
			Application.getCommonCache().evict(key);
			return 0;
		}
		
		Integer retry = (Integer) Application.getCommonCache().getx(key);
		retry = retry == null ? 0 : retry;
		if (state == 1) {
			retry += 1;
			Application.getCommonCache().putx(key, retry, CommonCache.TS_HOUR);
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
			String alt = user + "," + System.currentTimeMillis() + ",v1";
			alt = AES.encrypt(alt);
			ServletUtils.addCookie(response, CK_AUTOLOGIN, alt, 60 * 60 * 24 * 30, null, "/");
		} else {
			ServletUtils.removeCookie(request, response, CK_AUTOLOGIN);
		}
		
		ID loginId = createLoginLog(request, user);
		ServletUtils.setSessionAttribute(request, SK_LOGINID, loginId);
		
		ServletUtils.setSessionAttribute(request, WebUtils.CURRENT_USER, user);
		Application.getSessionStore().storeLoginSuccessed(request);	
	}

	/**
	 * 创建登陆日志
	 *
	 * @param request
	 * @param user
	 * @return
	 */
	public static ID createLoginLog(HttpServletRequest request, ID user) {
		String ipAddr = ServletUtils.getRemoteAddr(request);
		String UA = request.getHeader("user-agent");
		if (AppUtils.isRbMobile(request)) {
			UA = UA.toUpperCase();
		} else {
			UserAgent uas = UserAgent.parseUserAgentString(UA);
			try {
				UA = String.format("%s-%s (%s)",
						uas.getBrowser(), uas.getBrowserVersion().getMajorVersion(), uas.getOperatingSystem());
			} catch (Exception ex) {
				LOG.warn("Unknow user-agent : " + UA);
				UA = "UNKNOW";
			}
		}

		Record record = EntityHelper.forNew(EntityHelper.LoginLog, UserService.SYSTEM_USER);
		record.setID("user", user);
		record.setString("ipAddr", ipAddr);
		record.setString("userAgent", UA);
		record.setDate("loginTime", CalendarUtils.now());
		record = Application.getCommonService().create(record);
		return record.getPrimary();
	}
	
	@RequestMapping("logout")
	public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ServletUtils.removeCookie(request, response, CK_AUTOLOGIN);
		ServletUtils.getSession(request).invalidate();
		response.sendRedirect("login");
	}
	
	// --
	
	@RequestMapping("forgot-passwd")
	public ModelAndView forgotPasswd() {
		return createModelAndView("/user/forgot-passwd.jsp");
	}
	
	@RequestMapping("user-forgot-passwd")
	public void userForgotPasswd(HttpServletRequest request, HttpServletResponse response) {
		if (!SMSender.availableMail()) {
			writeFailure(response, lang("EmailAccountUnset"));
			return;
		}

		String email = getParameterNotNull(request, "email");
		if (!RegexUtils.isEMail(email) || !Application.getUserStore().existsEmail(email)) {
			writeFailure(response, lang("InputInvalid", "Email"));
			return;
		}

		String vcode = VCode.generate(email, 2);
		String content = String.format(lang("YourVcodeForResetPassword"), vcode);
		String sentid = SMSender.sendMail(email, lang("ResetPassword"), content);
		if (sentid != null) {
			writeSuccess(response);
		} else {
			writeFailure(response);
		}
	}
	
	@SuppressWarnings("DuplicatedCode")
	@RequestMapping("user-confirm-passwd")
	public void userConfirmPasswd(HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);
		String email = data.getString("email");
		String vcode = data.getString("vcode");
		if (!VCode.verfiy(email, vcode, true)) {
			writeFailure(response, lang("InputInvalid", "Vcode"));
			return;
		}
		
		String newpwd = data.getString("newpwd");
		User user = Application.getUserStore().getUserByEmail(email);
		Record record = EntityHelper.forUpdate(user.getId(), user.getId());
		record.setString("password", newpwd);
		try {
			Application.getSessionStore().set(user.getId());

			Application.getBean(UserService.class).update(record);
			writeSuccess(response);
			VCode.clean(email);
		} catch (DataSpecificationException ex) {
			writeFailure(response, ex.getLocalizedMessage());
		} finally {
			Application.getSessionStore().clean();
		}
	}

    @RequestMapping("live-wallpaper")
    public void getLiveWallpaper(HttpServletResponse response) throws IOException {
	    if (!SysConfiguration.getBool(ConfigurableItem.LiveWallpaper)) {
            writeFailure(response);
            return;
        }

        JSON ret = License.siteApi("api/misc/bgimg", true);
        if (ret == null) {
            writeFailure(response);
        } else {
            writeSuccess(response, ((JSONObject) ret).getString("url"));
        }
    }
}
