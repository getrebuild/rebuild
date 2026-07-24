/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.project.ProjectManager;
import com.rebuild.core.service.project.ProjectPlanConfigService;
import com.rebuild.core.service.project.ProjectTaskService;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 新建项目任务
 *
 * @author devezhao
 * @since 2026/7/24
 */
@Slf4j
public class CreateProjectTask implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
        JSONObject args = StringUtils.isBlank(arguments) ? new JSONObject() : JSON.parseObject(arguments);

        String taskName = args.getString("taskName");
        if (StringUtils.isBlank(taskName)) {
            throw new ToolException("任务标题 (taskName) 不能为空");
        }

        String project = args.getString("project");
        if (StringUtils.isBlank(project)) {
            throw new ToolException("项目 (project) 不能为空，请提供项目名称或ID");
        }

        final ID user = UserContextHolder.getUser();

        // 解析项目
        ConfigBean projectConfig = resolveProject(project, user);
        ID projectId = projectConfig.getID("id");

        // 验证成员权限
        Set<ID> members = projectConfig.get("members", Set.class);
        if (!members.contains(user)) {
            throw new ToolException("你不是项目 [" + projectConfig.getString("projectName") + "] 的成员，无法创建任务");
        }

        // 解析任务面板
        ID projectPlanId = resolveProjectPlan(args.getString("projectPlan"), projectId);

        // 创建任务记录
        Record record = EntityHelper.forNew(EntityHelper.ProjectTask, user);
        record.setID("projectId", projectId);
        record.setID("projectPlanId", projectPlanId);
        record.setString("taskName", taskName);

        // 优先级
        int priority = args.getIntValue("priority");
        if (priority >= 0 && priority <= 3) {
            record.setInt("priority", priority);
        }

        // 截止时间
        String deadline = args.getString("deadline");
        if (StringUtils.isNotBlank(deadline)) {
            Date deadlineDate = CalendarUtils.parse(deadline);
            record.setDate("deadline", deadlineDate);
        }

        // 描述
        String description = args.getString("description");
        if (StringUtils.isNotBlank(description)) {
            record.setString("description", description);
        }

        // 执行人
        String executor = args.getString("executor");
        if (StringUtils.isNotBlank(executor)) {
            ID executorId = ToolHelper.resolveUser(executor);
            if (executorId != null) {
                record.setID("executor", executorId);
            }
        }

        // 附件（支持单个 fileKey 字符串或数组）
        String attachmentsStr = ToolHelper.resolveFileKeys(args.get("attachments"));
        if (attachmentsStr != null) {
            record.setString("attachments", attachmentsStr);
        }

        record = Application.getBean(ProjectTaskService.class).create(record);

        // 获取任务编号
        Object[] taskNumber = Application.getQueryFactory().uniqueNoFilter(record.getPrimary(), "taskNumber");
        String projectCode = projectConfig.getString("projectCode");
        String taskNo = String.format("%s-%s", projectCode, taskNumber != null ? taskNumber[0] : "?");

        return JSONUtils.toJSONObject(
                new String[]{"status", "id", "taskNumber", "message"},
                new Object[]{"ok", record.getPrimary().toLiteral(), taskNo,
                        String.format("已成功创建任务 [%s]，编号: %s，项目: %s",
                                taskName, taskNo, projectConfig.getString("projectName"))});
    }

    /**
     * 解析项目（支持 ID 或名称模糊匹配）
     */
    private ConfigBean resolveProject(String projectIdent, ID user) {
        // 精确 ID
        if (ID.isId(projectIdent)) {
            return ProjectManager.instance.getProject(ID.valueOf(projectIdent), user);
        }

        // 按名称匹配
        ConfigBean[] available = ProjectManager.instance.getAvailable(user);
        List<ConfigBean> matched = new ArrayList<>();
        for (ConfigBean p : available) {
            if (p.getInteger("status") == ProjectManager.STATUS_ARCHIVED) continue;
            String name = p.getString("projectName");
            if (name.equalsIgnoreCase(projectIdent)) {
                return p;  // 精确匹配直接返回
            }
            if (name.contains(projectIdent)) {
                matched.add(p);
            }
        }

        if (matched.isEmpty()) {
            throw new ToolException("未找到匹配的项目: " + projectIdent);
        }
        if (matched.size() == 1) {
            return matched.get(0);
        }

        // 多个匹配，返回列表供选择
        JSONArray list = new JSONArray();
        for (ConfigBean p : matched) {
            list.add(JSONUtils.toJSONObject(
                    new String[]{"id", "name"},
                    new Object[]{p.getID("id").toLiteral(), p.getString("projectName")}));
        }
        throw new ToolException("匹配到多个项目，请指定更精确的名称或ID: " + list.toJSONString());
    }

    /**
     * 解析任务面板（支持 ID 或名称匹配，不传则使用第一个可新建的面板）
     */
    private ID resolveProjectPlan(String planIdent, ID projectId) {
        ConfigBean[] plans = ProjectManager.instance.getPlansOfProject(projectId);

        if (StringUtils.isBlank(planIdent)) {
            // 取第一个 flowStatus=1（可新建）的面板
            for (ConfigBean plan : plans) {
                if (plan.getInteger("flowStatus") == ProjectPlanConfigService.FLOW_STATUS_START) {
                    return plan.getID("id");
                }
            }
            // 没有可新建的面板，取第一个
            if (plans.length > 0) return plans[0].getID("id");
            throw new ToolException("该项目下没有可用的任务面板");
        }

        // 精确 ID
        if (ID.isId(planIdent)) {
            return ID.valueOf(planIdent);
        }

        // 按名称匹配
        for (ConfigBean plan : plans) {
            if (plan.getString("planName").equalsIgnoreCase(planIdent)
                    || plan.getString("planName").contains(planIdent)) {
                return plan.getID("id");
            }
        }

        throw new ToolException("未找到匹配的任务面板: " + planIdent);
    }
}
