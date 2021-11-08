/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.bizz;

import com.rebuild.api.RespBody;
import com.rebuild.core.privileges.UserImporter;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

/**
 * @author devezhao
 * @since 2020/11/5
 */
@RestController
public class UserImportController extends BaseController {

    /**
     * @see com.rebuild.web.commons.HeavyTaskController
     */
    @RequestMapping("/admin/bizuser/user-imports")
    public RespBody imports(HttpServletRequest request) {
        String file = getParameterNotNull(request, "file");
        File useFile = RebuildConfiguration.getFileOfTemp(file);

        boolean notify = getBoolParameter(request, "notify");

        String taskid = TaskExecutors.submit(new UserImporter(useFile, notify), getRequestUser(request));
        return RespBody.ok(taskid);
    }
}
