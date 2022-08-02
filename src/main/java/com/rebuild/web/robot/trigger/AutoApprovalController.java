/*Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.trigger;

import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
@RestController
@RequestMapping("/admin/robot/trigger/")
public class AutoApprovalController extends BaseController {

    @RequestMapping("auto-approval-alist")
    public JSON approvalList(HttpServletRequest request) {
        Object[][] array = Application.createQueryNoFilter(
                "select configId,name from RobotApprovalConfig where belongEntity = ? and isDisabled = ? order by name")
                .setParameter(1, getParameterNotNull(request, "entity"))
                .setParameter(2, false)
                .array();

        return JSONUtils.toJSONObjectArray(new String[] { "id", "text" }, array);
    }
}
