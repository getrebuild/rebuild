/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.setup;

import com.rebuild.api.RespBody;
import com.rebuild.core.DefinedException;
import com.rebuild.core.RebuildException;
import com.rebuild.core.rbstore.RbSystemImporter;
import com.rebuild.core.support.setup.InstallState;
import com.rebuild.core.support.task.TaskExecutors;
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

    @GetMapping({"rbsystems", "appstore"})
    public ModelAndView index(HttpServletRequest request) throws IOException {
        try {
            RbAssert.isSuperAdmin(getRequestUser(request));
        } catch (Exception error403) {
            throw new DefinedException("NOT ALLOWED");
        }
        return createModelAndView("/admin/setup/rbsystem");
    }

    @PostMapping("install-rbsystem")
    public RespBody install(HttpServletRequest request) throws IOException {
        RbAssert.isSuperAdmin(getRequestUser(request));

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
