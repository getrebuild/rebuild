/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.setup;

import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.DefinedException;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.setup.RbSystemPackageRestorer;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.admin.rbstore.MetaschemaController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

/**
 * 安装完成后的新手引导
 *
 * @author RB
 * @since 2026/11/18
 */
@Slf4j
@RestController
@RequestMapping("/onboarding")
public class OnboardingController extends BaseController {

    @GetMapping
    public ModelAndView page(HttpServletRequest request) {
        if (!isAdminUser(request)) throw new DefinedException("NOT ALLOWED");
        return createModelAndView("/admin/setup/onboarding");
    }

    @PostMapping("imports")
    public RespBody imports(HttpServletRequest request) {
        if (!isAdminUser(request)) return RespBody.error(403);

        String[] keys = getParameterNotNull(request, "key").split(",");
        return MetaschemaController.importModels(keys);
    }

    @PostMapping("restore-package")
    public RespBody restorePackage(HttpServletRequest request) {
        if (!isAdminUser(request)) return RespBody.error(403);

        JSONObject reqJson = (JSONObject) ServletUtils.getRequestJson(request);
        String packageKey = reqJson == null ? null : reqJson.getString("package");
        if (StringUtils.isBlank(packageKey)) return RespBody.errorl("请选择应用资源包");

        try {
            new RbSystemPackageRestorer(packageKey).restore();
            return RespBody.ok();
        } catch (Exception ex) {
            log.error("Cannot restore rbsystem package", ex);
            return RespBody.errorl("导入失败 : %s", CommonsUtils.getRootMessage(ex));
        }
    }

    private boolean isAdminUser(HttpServletRequest request) {
        return UserHelper.isAdmin(getRequestUser(request));
    }
}
