/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.robot.approval;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
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
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.approval.RobotApprovalConfigService;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.admin.ConfigCommons;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public ModelAndView page(@PathVariable String id, HttpServletResponse response) throws IOException {
        ID configId = ID.valueOf(id);
        Object[] config = Application.createQuery(
                "select belongEntity,name,flowDefinition,isDisabled from RobotApprovalConfig where configId = ?")
                .setParameter(1, configId)
                .unique();
        if (config == null) {
            response.sendError(404, Language.L("审批流程不存在"));
            return null;
        }

        Entity applyEntity = MetadataHelper.getEntity((String) config[0]);

        ModelAndView mv = createModelAndView("/admin/robot/approval-design");
        mv.getModel().put("configId", configId);
        mv.getModel().put("name", config[1]);
        mv.getModel().put("applyEntity", applyEntity.getName());
        mv.getModel().put("flowDefinition", config[2]);
        mv.getModel().put("isDisabled", config[3]);
        return mv;
    }

    @GetMapping("approval/list")
    public RespBody approvalList(HttpServletRequest request) {
        String belongEntity = getParameter(request, "entity");
        String q = getParameter(request, "q");
        String sql = "select configId,belongEntity,belongEntity,name,isDisabled,modifiedOn from RobotApprovalConfig" +
                " where (1=1) and (2=2)" +
                " order by modifiedOn desc, name";

        Object[][] array = ConfigCommons.queryListOfConfig(sql, belongEntity, q);
        return RespBody.ok(array);
    }

    @RequestMapping("approval/copy")
    public JSON copyApproval(HttpServletRequest request) {
        final ID user = getRequestUser(request);

        String approvalName = getParameterNotNull(request, "name");
        ID father = getIdParameterNotNull(request, "father");
        boolean disableFather = getBoolParameter(request, "disabled", true);
        JSON flowDefinition = ServletUtils.getRequestJson(request);

        Object[] copy = Application.createQueryNoFilter(
                "select belongEntity,flowDefinition,isDisabled from RobotApprovalConfig where configId = ?")
                .setParameter(1, father)
                .unique();

        Record record = EntityHelper.forNew(EntityHelper.RobotApprovalConfig, user);
        record.setString("belongEntity", (String) copy[0]);
        record.setString("flowDefinition", (String) copy[1]);
        if (flowDefinition != null) record.setString("flowDefinition", flowDefinition.toString());
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
        final String textSubmitor = Language.L("发起人") + ".";
        final String textApprover = Language.L("审批人") + ".";
        final String textDept = Language.L("部门") + ".";

        List<String[]> fields = new ArrayList<>();

        // 虚拟字段
        Field[] userRefFields = MetadataSorter.sortFields(
                MetadataHelper.getEntity(EntityHelper.User), DisplayType.REFERENCE, DisplayType.N2NREFERENCE);
        Field[] deptRefFields = MetadataSorter.sortFields(
                MetadataHelper.getEntity(EntityHelper.Department), DisplayType.REFERENCE, DisplayType.N2NREFERENCE);

        Set<String> fieldsNames = new HashSet<>();
        Collections.addAll(fieldsNames, EntityHelper.ApprovalLastUser, "deptId");

        // 发起人
        for (Field field : userRefFields) {
            if (isRefUserOrDeptField(field, fieldsNames, true)) {
                fields.add(new String[] {
                        ApprovalHelper.APPROVAL_SUBMITOR + field.getName(),
                        textSubmitor + EasyMetaFactory.getLabel(field)} );
            }
        }
        for (Field field : deptRefFields) {
            if (isRefUserOrDeptField(field, fieldsNames, true)) {
                fields.add(new String[] {
                        ApprovalHelper.APPROVAL_SUBMITOR + "deptId." + field.getName(),
                        textSubmitor + textDept + EasyMetaFactory.getLabel(field)} );
            }
        }

        // （上一）审批人
        for (Field field : userRefFields) {
            if (isRefUserOrDeptField(field, fieldsNames, true)) {
                fields.add(new String[] {
                        ApprovalHelper.APPROVAL_APPROVER + field.getName(),
                        textApprover + EasyMetaFactory.getLabel(field)} );
            }
        }
        for (Field field : deptRefFields) {
            if (isRefUserOrDeptField(field, fieldsNames, true)) {
                fields.add(new String[] {
                        ApprovalHelper.APPROVAL_APPROVER + "deptId." + field.getName(),
                        textApprover + textDept + EasyMetaFactory.getLabel(field)} );
            }
        }

        // 本实体字段
        Field[] refFields = MetadataSorter.sortFields(entity, DisplayType.REFERENCE, DisplayType.N2NREFERENCE);
        for (Field field : refFields) {
            if (isRefUserOrDeptField(field, fieldsNames, false)) {
                fields.add(new String[] { field.getName(), EasyMetaFactory.getLabel(field)} );
            }
        }
        // 引用实体字段
        for (Field field : refFields) {
            if (field.getType() != FieldType.REFERENCE) continue;
            if (MetadataHelper.isCommonsField(field)) continue;

            String parentName = field.getName() + ".";
            String parentLabel = EasyMetaFactory.getLabel(field) + ".";

            Field[] refFields2 = MetadataSorter.sortFields(field.getReferenceEntity(), DisplayType.REFERENCE, DisplayType.N2NREFERENCE);
            for (Field field2 : refFields2) {
                if (isRefUserOrDeptField(field2, fieldsNames, false)) {
                    fields.add(new String[] { parentName + field2.getName(), parentLabel + EasyMetaFactory.getLabel(field2)} );
                }
            }
        }

        return JSONUtils.toJSONObjectArray(
                new String[] {  "id", "text" }, fields.toArray(new String[0][]));
    }

    private boolean isRefUserOrDeptField(Field field, Set<String> filterNames, boolean excludeCommon) {
        if (excludeCommon && MetadataHelper.isCommonsField(field)) return false;
        if (filterNames.contains(field.getName())) return false;

        int ec = field.getReferenceEntity().getEntityCode();
        return ec == EntityHelper.User || ec == EntityHelper.Department;
    }

    @PostMapping("approval/user-fields-show")
    public JSON approvalUserFieldsShow(@EntityParam Entity entity, HttpServletRequest request) {
        final JSON users = ServletUtils.getRequestJson(request);

        final String textSubmitor = Language.L("发起人") + ".";
        final String textApprover = Language.L("审批人") + ".";
        final String textDept = Language.L("部门") + ".";

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
                    if (userField.getOwnEntity().getEntityCode() == EntityHelper.Department) {
                        fieldText = textDept + fieldText;
                    }
                    fieldText = (idOrField.startsWith(ApprovalHelper.APPROVAL_SUBMITOR)
                            ? textSubmitor : textApprover)  + fieldText;

                    shows.add(new String[] { idOrField, fieldText });
                }

            } else if (MetadataHelper.getLastJoinField(entity, idOrField) != null) {
                String fieldLabel = EasyMetaFactory.getLabel(entity, idOrField);
                shows.add(new String[] { idOrField, fieldLabel });
            }
        }

        return JSONUtils.toJSONObjectArray(
                new String[] {  "id", "text" }, shows.toArray(new String[0][]));
    }

    @RequestMapping("approval/use-stats")
    public RespBody useStats(HttpServletRequest request) {
        final String[] ids = getParameterNotNull(request, "ids").split(",");

        Map<String, Object> res = new HashMap<>();
        for (String id : ids) {
            ID aid = ID.valueOf(id);
            int state2 = ApprovalHelper.checkUsed(aid, ApprovalState.PROCESSING);
            int state10 = ApprovalHelper.checkUsed(aid, ApprovalState.APPROVED);
            res.put(id, new Object[] { state2, state10 });
        }
        return RespBody.ok(res);
    }

    @RequestMapping("approval/expires-auto-fields")
    public RespBody expiresAutoFields(@EntityParam Entity entity) {
        List<Object> dateFields = new ArrayList<>();
        List<Object> urgeUsers = new ArrayList<>();

        for (Field d : MetadataSorter.sortFields(entity, DisplayType.DATE, DisplayType.DATETIME)) {
            if (!MetadataHelper.isCommonsField(d)) {
                dateFields.add(EasyMetaFactory.toJSON(d));
            }
        }

        urgeUsers.add(new String[] { ApprovalHelper.APPROVAL_APPROVER, Language.L("审批人") });
        urgeUsers.add(new String[] { ApprovalHelper.APPROVAL_SUBMITOR, Language.L("提交人") });

        Object res = JSONUtils.toJSONObject(
                new String[] { "dateFields", "urgeUsers" }, new Object[] { dateFields, urgeUsers } );
        return RespBody.ok(res);
    }
}
