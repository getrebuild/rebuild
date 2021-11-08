/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.AdminDiagnosis;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author devezhao
 * @since 10/13/2018
 */
@RestController
public class AdminVerfiyController extends BaseController {

    /**
     * Admin 验证标志
     */
    public static final String KEY_VERIFIED = WebUtils.KEY_PREFIX + "-AdminVerified";

    @GetMapping("/user/admin-verify")
    public ModelAndView pageAdminVerify(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        User admin = Application.getUserStore().getUser(getRequestUser(request));
        if (admin.isAdmin()) {
            return createModelAndView("/admin/admin-verify");
        } else {
            response.sendError(403, Language.L("非管理员用户"));
            return null;
        }
    }

    @PostMapping("/user/admin-verify")
    public RespBody adminVerify(HttpServletRequest request) {
        ID adminId = getRequestUser(request);
        String passwd = getParameterNotNull(request, "passwd");

        Object[] foundUser = Application.createQueryNoFilter(
                "select password from User where userId = ?")
                .setParameter(1, adminId)
                .unique();

        if (foundUser[0].equals(EncryptUtils.toSHA256Hex(passwd))) {
            ServletUtils.setSessionAttribute(request, KEY_VERIFIED, CalendarUtils.now());
            return RespBody.ok();
        } else {
            ServletUtils.setSessionAttribute(request, KEY_VERIFIED, null);
            return RespBody.errorl("密码错误");
        }
    }

    @RequestMapping("/user/admin-cancel")
    public RespBody adminCancel(HttpServletRequest request) {
        ServletUtils.setSessionAttribute(request, KEY_VERIFIED, null);
        return RespBody.ok();
    }

    @RequestMapping("/user/admin-dangers")
    public RespBody adminDangers() {
        return RespBody.ok(AdminDiagnosis.getAdminDanger());
    }

    // -- CLI

    @RequestMapping("/admin/cli/console")
    public ModelAndView adminCliConsole() {
        return createModelAndView("/admin/admin-cli");
    }

    @RequestMapping("/admin/cli/exec")
    public RespBody adminCliExec(HttpServletRequest request) {
        String command = ServletUtils.getRequestString(request);
        if (StringUtils.isBlank(command)) {
            return RespBody.error();
        }

        String result = new AdminCLI2(command).exec();
        return RespBody.ok(result);
    }
}
