/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.user.signup;

import cn.devezhao.commons.RegexUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSONObject;
import com.hankcs.hanlp.HanLP;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.VerfiyCode;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.utils.BlockList;
import com.rebuild.web.BaseController;
import com.wf.captcha.utils.CaptchaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;

/**
 * 用户自助注册
 *
 * @author devezhao
 * @since 11/01/2018
 */
@Slf4j
@RestController
@RequestMapping("/user/")
public class SignUpController extends BaseController {

    @GetMapping("signup")
    public ModelAndView pageSignup(HttpServletResponse response) throws IOException {
        if (!RebuildConfiguration.getBool(ConfigurationItem.OpenSignUp)) {
            response.sendError(400, Language.L("管理员未开放公开注册"));
            return null;
        }
        return createModelAndView("/signup/signup");
    }

    @PostMapping("signup-email-vcode")
    public RespBody signupEmailVcode(HttpServletRequest request) {
        if (!SMSender.availableMail()) {
            return RespBody.errorl("邮件服务账户未配置，请联系管理员配置");
        }

        String email = getParameterNotNull(request, "email");

        if (!RegexUtils.isEMail(email)) {
            return RespBody.errorl("无效邮箱");
        } else if (Application.getUserStore().existsEmail(email)) {
            return RespBody.errorl("邮箱已存在");
        }

        String vcode = VerfiyCode.generate(email, 1);
        String title = Language.L("注册验证码");
        String content = Language.L("你的注册验证码是 : **%s**", vcode);
        String sentid = SMSender.sendMail(email, title, content);


        log.warn(email + " >>>>> " + content);
        if (sentid != null) {
            return RespBody.ok();
        } else {
            return RespBody.error();
        }
    }

    @PostMapping("signup-confirm")
    public RespBody signupConfirm(HttpServletRequest request) {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);

        String email = data.getString("email");
        String vcode = data.getString("vcode");
        if (!VerfiyCode.verfiy(email, vcode, true)) {
            return RespBody.errorl("无效验证码");
        }

        String loginName = data.getString("loginName");
        String fullName = data.getString("fullName");
        String passwd = VerfiyCode.generate(loginName, 2) + "!8";
        VerfiyCode.clean(loginName);

        Record userNew = EntityHelper.forNew(EntityHelper.User, UserService.SYSTEM_USER);
        userNew.setString("email", email);
        userNew.setString("loginName", loginName);
        userNew.setString("fullName", fullName);
        userNew.setString("password", passwd);
        userNew.setBoolean("isDisabled", true);
        try {
            Application.getBean(UserService.class).txSignUp(userNew);

            // 通知用户
            String homeUrl = RebuildConfiguration.getHomeUrl();
            String title = Language.L("管理员正在审核你的注册信息");
            String content = Language.L(
                    "%s 欢迎注册！以下是你的注册信息，请妥善保管。 [][] 登录账号 : **%s** [] 登录密码 : **%s** [] 登录地址 : [%s](%s) [][] 目前你还无法登录系统，因为系统管理员正在审核你的注册信息。完成后会通过邮件通知你，请耐心等待。",
                    fullName, loginName, passwd, homeUrl, homeUrl);
            SMSender.sendMail(email, title, content);

            return RespBody.ok();

        } catch (DataSpecificationException ex) {
            return RespBody.error(ex.getLocalizedMessage());
        }
    }

    @RequestMapping("checkout-name")
    public RespBody checkoutName(HttpServletRequest request) {
        String fullName = getParameterNotNull(request, "fullName");

        fullName = fullName.replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5]", "");
        String loginName = HanLP.convertToPinyinString(fullName, "", false);
        if (loginName.length() > 20) {
            loginName = loginName.substring(0, 20);
        }
        if (BlockList.isBlock(loginName)) {
            return RespBody.ok();
        }

        for (int i = 0; i < 5; i++) {
            if (Application.getUserStore().existsName(loginName)) {
                loginName += RandomUtils.nextInt(99);
            } else {
                break;
            }
        }

        loginName = loginName.toLowerCase();
        return RespBody.ok(loginName);
    }

    @GetMapping("captcha")
    public void captcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Font font = new Font(Font.SERIF, Font.BOLD & Font.ITALIC, 22 + RandomUtils.nextInt(8));
        int codeLen = 4 + RandomUtils.nextInt(3);
        CaptchaUtil.out(160, 41, codeLen, font, request, response);
    }
}