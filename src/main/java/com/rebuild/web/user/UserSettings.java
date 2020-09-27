/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.user;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.core.support.VerfiyCode;
import com.rebuild.web.EntityController;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用户设置
 *
 * @author devezhao
 * @since 10/08/2018
 */
@Controller
@RequestMapping("/settings")
public class UserSettings extends EntityController {

    @GetMapping("/user")
    public ModelAndView pageUser(HttpServletRequest request) {
        return createModelAndView("/settings/user-settings", "User", getRequestUser(request));
    }

    @RequestMapping("/user/send-email-vcode")
    public void sendEmailVcode(HttpServletRequest request, HttpServletResponse response) {
        String email = getParameterNotNull(request, "email");
        if (Application.getUserStore().existsEmail(email)) {
            writeFailure(response, "邮箱已被占用，请换用其他邮箱");
            return;
        }

        String vcode = VerfiyCode.generate(email);
        String content = "<p>你的邮箱验证码是 <b>" + vcode + "</b><p>";
        String sentid = SMSender.sendMail(email, "邮箱验证码", content);
        if (sentid != null) {
            writeSuccess(response);
        } else {
            writeFailure(response, "验证码发送失败，请稍后重试");
        }
    }

    @RequestMapping("/user/save-email")
    public void saveEmail(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        String email = getParameterNotNull(request, "email");
        String vcode = getParameterNotNull(request, "vcode");

        if (!VerfiyCode.verfiy(email, vcode)) {
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

    @RequestMapping("/user/save-passwd")
    public void savePasswd(HttpServletRequest request, HttpServletResponse response) {
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

    @GetMapping("/user/login-logs")
    public void loginLogs(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        Object[][] logs = Application.createQueryNoFilter(
                "select loginTime,ipAddr,userAgent from LoginLog where user = ? order by loginTime desc")
                .setParameter(1, user)
                .setLimit(100)
                .array();
        for (Object[] o : logs) {
            o[0] = CalendarUtils.getUTCDateTimeFormat().format(o[0]);
        }
        writeSuccess(response, logs);
    }
}
