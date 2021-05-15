/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.approval;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.approval.ApprovalHelper;
import com.rebuild.core.service.approval.RobotApprovalConfigService;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.admin.data.ReportTemplateController;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.rebuild.core.support.i18n.Language.$L;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/24
 */
@RestController
@RequestMapping("/admin/robot/")
public class ApprovalAdminController extends BaseController {

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
            response.sendError(404, $L("审批流程不存在"));
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

    @GetMapping("approval/list")
    public RespBody approvalList(HttpServletRequest request) {
        String belongEntity = getParameter(request, "entity");
        String q = getParameter(request, "q");
        String sql = "select configId,belongEntity,belongEntity,name,isDisabled,modifiedOn from RobotApprovalConfig" +
                " where (1=1) and (2=2)" +
                " order by modifiedOn desc, name";

        Object[][] array = ReportTemplateController.queryListOfConfig(sql, belongEntity, q);
        return RespBody.ok(array);
    }

    @RequestMapping("approval/copy")
    public JSON copyApproval(HttpServletRequest request) {
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

        return JSONUtils.toJSONObject("approvalId", record.getPrimary());
    }

    @GetMapping("approval/user-fields")
    public JSON approvalUserFields(@EntityParam Entity entity) {
        final String textSubmitor = $L("发起人") + ".";
        final String textApprover = $L("审批人") + ".";

        List<String[]> fields = new ArrayList<>();

        // 虚拟字段
        Field[] userRefFields = MetadataSorter.sortFields(
                MetadataHelper.getEntity(EntityHelper.User), DisplayType.REFERENCE);
        // 发起人
        for (Field field : userRefFields) {
            if (!MetadataHelper.isCommonsField(field)
                    && field.getReferenceEntity().getEntityCode() == EntityHelper.User) {
                fields.add(new String[] {
                        ApprovalHelper.APPROVAL_SUBMITOR + field.getName(), textSubmitor + EasyMetaFactory.getLabel(field)} );
            }
        }
        // 上一审批人
        for (Field field : userRefFields) {
            if (!MetadataHelper.isCommonsField(field)
                    && field.getReferenceEntity().getEntityCode() == EntityHelper.User) {
                fields.add(new String[] {
                        ApprovalHelper.APPROVAL_APPROVER + field.getName(), textApprover + EasyMetaFactory.getLabel(field)} );
            }
        }

        // 本实体字段
        Field[] refFields = MetadataSorter.sortFields(entity, DisplayType.REFERENCE);
        for (Field field : refFields) {
            int refEntity = field.getReferenceEntity().getEntityCode();
            if (refEntity == EntityHelper.User || refEntity == EntityHelper.Department) {
                fields.add(new String[] { field.getName(), EasyMetaFactory.getLabel(field)} );
            }
        }

        return JSONUtils.toJSONObjectArray(
                new String[] {  "id", "text" }, fields.toArray(new String[0][]));
    }

    @PostMapping("approval/user-fields-show")
    public JSON approvalUserFieldsShow(@EntityParam Entity entity, HttpServletRequest request) {
        final JSON users = ServletUtils.getRequestJson(request);

        final String textSubmitor = $L("发起人") + ".";
        final String textApprover = $L("审批人") + ".";

        List<String[]> shows = new ArrayList<>();

        for (Object item : (JSONArray) users) {
            String idOrField = (String) item;
            if (ID.isId(idOrField)) {
                String name = UserHelper.getName(ID.valueOf(idOrField));
                if (name != null) {
                    shows.add(new String[] { idOrField, name });
                }

            } else if (idOrField.startsWith(ApprovalHelper.APPROVAL_SUBMITOR)
                    || idOrField.startsWith(ApprovalHelper.APPROVAL_APPROVER)) {
                Field userField = ApprovalHelper.checkVirtualField(idOrField);
                if (userField != null) {
                    String fieldText = EasyMetaFactory.getLabel(userField);
                    fieldText = (idOrField.startsWith(ApprovalHelper.APPROVAL_SUBMITOR) ? textSubmitor : textApprover) + fieldText;
                    shows.add(new String[] { idOrField, fieldText });
                }

            } else if (entity.containsField(idOrField)) {
                String fieldLabel = EasyMetaFactory.getLabel(entity, idOrField);
                shows.add(new String[] { idOrField, fieldLabel });
            }
        }

        return JSONUtils.toJSONObjectArray(
                new String[] {  "id", "text" }, shows.toArray(new String[0][]));
    }
}
