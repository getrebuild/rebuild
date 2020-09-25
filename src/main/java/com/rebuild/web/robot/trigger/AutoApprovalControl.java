/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.trigger;

import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
@Controller
@RequestMapping("/admin/robot/trigger/")
public class AutoApprovalControl extends BaseController {

    @RequestMapping("auto-approval-alist")
    public void approvalList(HttpServletRequest request, HttpServletResponse response) {
        String entity = getParameterNotNull(request, "entity");
        Object[][] array = Application.createQueryNoFilter(
                "select configId,name from RobotApprovalConfig where belongEntity = ? and isDisabled = ? order by name")
                .setParameter(1, entity)
                .setParameter(2, false)
                .array();

        JSON ret = JSONUtils.toJSONObjectArray(new String[]{"id", "text"}, array);
        writeSuccess(response, ret);
    }
}
