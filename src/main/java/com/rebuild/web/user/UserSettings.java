/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.user;

import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.VerfiyCode;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.web.EntityController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * 用户设置
 *
 * @author devezhao
 * @since 10/08/2018
 */
@RestController
@RequestMapping("/settings")
public class UserSettings extends EntityController {

    @GetMapping("/user")
    public ModelAndView pageUser(HttpServletRequest request) {
        return createModelAndView("/settings/user-settings", "User", getRequestUser(request));
    }

    @RequestMapping("/user/send-email-vcode")
    public RespBody sendEmailVcode(HttpServletRequest request) {
        if (!SMSender.availableMail()) {
            return RespBody.error(getLang(request, "EmailAccountUnset"));
        }

        String email = getParameterNotNull(request, "email");
        if (Application.getUserStore().existsEmail(email)) {
            return RespBody.error(getLang(request, "EmailBeUsed"));
        }

        String vcode = VerfiyCode.generate(email);
        String subject = getLang(request, "EmailVcode");
        String content = String.format(getLang(request, "YourVCode", "Email"), vcode);
        String sentid = SMSender.sendMail(email, subject, content);

        if (sentid != null) {
            return RespBody.ok();
        } else {
            return RespBody.error(getLang(request, "OperationFailed"));
        }
    }

    @RequestMapping("/user/save-email")
    public RespBody saveEmail(HttpServletRequest request) {
        ID user = getRequestUser(request);
        String email = getParameterNotNull(request, "email");
        String vcode = getParameterNotNull(request, "vcode");

        if (!VerfiyCode.verfiy(email, vcode)) {
            return RespBody.error(getLang(request, "SomeInvalid", "Vcode"));
        }

        if (Application.getUserStore().existsEmail(email)) {
            return RespBody.error(getLang(request, "EmailBeUsed"));
        }

        Record record = EntityHelper.forUpdate(user, user);
        record.setString("email", email);
        Application.getBean(UserService.class).update(record);
        return RespBody.ok();
    }

    @RequestMapping("/user/save-passwd")
    public RespBody savePasswd(HttpServletRequest request) {
        ID user = getRequestUser(request);
        String oldp = getParameterNotNull(request, "oldp");
        String newp = getParameterNotNull(request, "newp");

        Object[] o = Application.createQuery("select password from User where userId = ?")
                .setParameter(1, user)
                .unique();
        if (o == null || !StringUtils.equals((String) o[0], EncryptUtils.toSHA256Hex(oldp))) {
            return RespBody.error(getLang(request, "OldPasswdWrong"));
        }

        Record record = EntityHelper.forUpdate(user, user);
        record.setString("password", newp);
        Application.getBean(UserService.class).update(record);
        return RespBody.ok();
    }

    @GetMapping("/user/login-logs")
    public Object[][] loginLogs(HttpServletRequest request) {
        Object[][] logs = Application.createQueryNoFilter(
                "select loginTime,ipAddr,userAgent from LoginLog where user = ? order by loginTime desc")
                .setParameter(1, getRequestUser(request))
                .setLimit(100)
                .array();

        for (Object[] o : logs) {
            o[0] = I18nUtils.formatDate((Date) o[0]);
        }
        return logs;
    }
}
