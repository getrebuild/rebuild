/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.approval;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.approval.RobotApprovalConfigService;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.admin.data.ReportTemplateController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/24
 */
@Controller
@RequestMapping("/admin/robot/")
public class RobotApprovalController extends BaseController {

    @GetMapping("approvals")
    public ModelAndView page() {
        return createModelAndView("/admin/robot/approval-list");
    }

    @GetMapping("approval/{id}")
    public ModelAndView page(@PathVariable String id,
                             HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID configId = ID.valueOf(id);
        Object[] config = Application.createQuery(
                "select belongEntity,name,flowDefinition from RobotApprovalConfig where configId = ?")
                .setParameter(1, configId)
                .unique();
        if (config == null) {
            response.sendError(404, getLang(request, "SomeNotExists", "ApprovalConfig"));
            return null;
        }

        Entity applyEntity = MetadataHelper.getEntity((String) config[0]);

        ModelAndView mv = createModelAndView("/admin/robot/approval-design");
        mv.getModel().put("configId", configId);
        mv.getModel().put("name", config[1]);
        mv.getModel().put("applyEntity", applyEntity.getName());
        mv.getModel().put("flowDefinition", config[2]);
        return mv;
    }

    @RequestMapping("approval/list")
    public void approvalList(HttpServletRequest request, HttpServletResponse response) {
        String belongEntity = getParameter(request, "entity");
        String q = getParameter(request, "q");
        String sql = "select configId,belongEntity,belongEntity,name,isDisabled,modifiedOn from RobotApprovalConfig" +
                " where (1=1) and (2=2)" +
                " order by modifiedOn desc, name";

        Object[][] array = ReportTemplateController.queryListOfConfig(sql, belongEntity, q);
        writeSuccess(response, array);
    }

    @RequestMapping("approval/copy")
    public void copyApproval(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        String approvalName = getParameterNotNull(request, "name");
        ID father = getIdParameterNotNull(request, "father");
        boolean disableFather = getBoolParameter(request, "disabled", true);

        Object[] copy = Application.createQueryNoFilter(
                "select belongEntity,flowDefinition,isDisabled from RobotApprovalConfig where configId = ?")
                .setParameter(1, father)
                .unique();

        Record record = EntityHelper.forNew(EntityHelper.RobotApprovalConfig, user);
        record.setString("belongEntity", (String) copy[0]);
        record.setString("flowDefinition", (String) copy[1]);
        record.setString("name", approvalName);
        record = Application.getBean(RobotApprovalConfigService.class).create(record);

        if (disableFather && !(Boolean) copy[2]) {
            Record update = EntityHelper.forUpdate(father, user);
            update.setBoolean("isDisabled", true);
            Application.getBean(RobotApprovalConfigService.class).update(update);
        }
        writeSuccess(response, JSONUtils.toJSONObject("approvalId", record.getPrimary()));
    }
}
