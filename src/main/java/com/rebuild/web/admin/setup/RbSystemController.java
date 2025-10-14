/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.setup;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.api.RespBody;
import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.Application;
import com.rebuild.core.DefinedException;
import com.rebuild.core.RebuildException;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.rbstore.RbSystemImporter;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.setup.InstallState;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author devezhao
 * @since 2024/12/1
 */
@RestController
@RequestMapping("/setup/")
public class RbSystemController extends BaseController implements InstallState {

    @GetMapping({"rbsystems", "apps"})
    public ModelAndView index(HttpServletRequest request) throws IOException {
        try {
            RbAssert.isSuperAdmin(getRequestUser(request));
            RbAssert.is(AppUtils.isAdminVerified(request), null);
        } catch (Exception error403) {
            throw new DefinedException("NOT ALLOWED");
        }

        ModelAndView mv = createModelAndView("/admin/setup/rbsystem");
        mv.getModelMap().put("csrfToken", AuthTokenManager.generateCsrfToken());
        return mv;
    }

    @PostMapping("install-rbsystem")
    public RespBody install(HttpServletRequest request) throws IOException {
        RbAssert.isSuperAdmin(getRequestUser(request));
        RbAssert.is(AppUtils.isAdminVerified(request), "NOT ALLOWED");

        String csrfToken = request.getHeader(AppUtils.HF_CSRFTOKEN);
        ID pass = AuthTokenManager.verifyToken(csrfToken, true, false);
        if (pass == null) RespBody.error("NOT ALLOWED");

        String file = getParameterNotNull(request, "file");
        RbSystemImporter importer = new RbSystemImporter("rbsystems/" + file);
        try {
            importer.check();
        } catch (RebuildException ex) {
            return RespBody.error(ex.getLocalizedMessage());
        }

        TaskExecutors.run(importer);
        return RespBody.ok();
    }
}
