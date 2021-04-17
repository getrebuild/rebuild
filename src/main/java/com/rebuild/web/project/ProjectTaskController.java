/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.project.ProjectHelper;
import com.rebuild.core.service.project.ProjectManager;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Set;

/**
 * 任务
 *
 * @author devezhao
 * @since 2020/6/29
 */
@RestController
@RequestMapping("/project/")
public class ProjectTaskController extends BaseController {

    private static final JSONObject NO_TASKS = JSONUtils.toJSONObject(
            new String[] { "count", "tasks" },
            new Object[] { 0, new Object[0] });

    @GetMapping("task/{taskId}")
    public ModelAndView pageTask(@PathVariable String taskId,
                                 HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID taskId2 = ID.isId(taskId) ? ID.valueOf(taskId) : null;
        if (taskId2 == null) {
            response.sendError(404);
            return null;
        }

        final ID user = getRequestUser(request);
        if (!ProjectHelper.checkReadable(taskId2, user)) {
            response.sendError(403, getLang(request, "NoPrivViewTask"));
            return null;
        }

        ConfigBean cfg = ProjectManager.instance.getProjectByTask(taskId2, user);

        ModelAndView mv = createModelAndView("/project/task-view");
        mv.getModel().put("id", taskId2.toLiteral());
        mv.getModel().put("projectIcon", cfg.getString("iconName"));
        mv.getModel().put("isMember", cfg.get("members", Set.class).contains(user));
        mv.getModel().put("isManageable", ProjectHelper.isManageable(taskId2, user));
        return mv;
    }

    @RequestMapping("tasks/list")
    public JSON taskList(@IdParam(name = "plan") ID planId, HttpServletRequest request) {
        String queryWhere = "projectPlanId = ?";

        // 关键词搜索
        String search = getParameter(request, "search");
        if (StringUtils.isNotBlank(search)) {
            queryWhere += " and taskName like '%" + StringEscapeUtils.escapeSql(search) + "%'";
        }

        // 高级查询
        JSON advFilter = ServletUtils.getRequestJson(request);
        if (advFilter != null) {
            String filterSql = new AdvFilterParser((JSONObject) advFilter).toSqlWhere();
            if (filterSql != null) {
                queryWhere += " and (" + filterSql + ")";
            }
        }

        int pageNo = getIntParameter(request, "pageNo", 1);
        int pageSize = getIntParameter(request, "pageSize", 40);

        int count = -1;
        if (pageNo == 1) {
            String countSql = "select count(taskId) from ProjectTask where " + queryWhere;
            Object[] count2 = Application.createQueryNoFilter(countSql)
                    .setParameter(1, planId)
                    .unique();
            count = ObjectUtils.toInt(count2[0]);

            if (count == 0) {
                return NO_TASKS;
            }
        }

        // 排序
        String sort = getParameter(request, "sort");
        if ("deadline".equalsIgnoreCase(sort)) sort = "deadline desc";
        else if ("modifiedOn".equalsIgnoreCase(sort)) sort = "modifiedOn desc";
        else sort = "seq asc";
        queryWhere += " order by " + sort;

        String querySql = "select " + BASE_FIELDS + " from ProjectTask where " + queryWhere;
        Object[][] tasks = Application.createQueryNoFilter(querySql)
                .setParameter(1, planId)
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();

        JSONArray alist = new JSONArray();
        for (Object[] o : tasks) {
            alist.add(formatTask(o, true));
        }

        return JSONUtils.toJSONObject(
                new String[] { "count", "tasks" },
                new Object[] { count, alist });
    }

    @GetMapping("tasks/get")
    public JSON taskGet(@IdParam(name = "task") ID taskId) {
        Object[] task = Application.createQueryNoFilter(
                "select " + BASE_FIELDS + " from ProjectTask where taskId = ?")
                .setParameter(1, taskId)
                .unique();
        return formatTask(task, true);
    }

    @GetMapping("tasks/details")
    public JSON taskDetails(@IdParam(name = "task") ID taskId) {
        Object[] task = Application.createQueryNoFilter(
                "select " + BASE_FIELDS + ",projectId,description,attachments,relatedRecord from ProjectTask where taskId = ?")
                .setParameter(1, taskId)
                .unique();
        JSONObject details = formatTask(task, true);

        // 状态面板
        details.put("projectId", task[11]);
        details.put("description", task[12]);
        String attachments = (String) task[13];
        details.put("attachments", JSON.parseArray(attachments));

        // 相关记录
        ID relatedRecord = (ID) task[14];
        if (relatedRecord != null) {
            details.put("relatedRecord", relatedRecord);
            String text = FieldValueHelper.getLabelNotry(relatedRecord);
            details.put("relatedRecordData", FieldValueHelper.wrapMixValue(relatedRecord, text));
        }

        return details;
    }

    private static final String BASE_FIELDS =
            "projectId.projectCode,taskNumber,taskId,taskName,createdOn,deadline,executor,status,seq,priority,endTime";

    private JSONObject formatTask(Object[] o, boolean appendTags) {
        String taskNumber = o[1].toString();
        if (StringUtils.isNotBlank((String) o[0])) taskNumber = o[0] + "-" + taskNumber;

        String createdOn = I18nUtils.formatDate((Date) o[4]);
        String deadline = I18nUtils.formatDate((Date) o[5]);
        String endTime = I18nUtils.formatDate((Date) o[10]);

        Object[] executor = o[6] == null ? null : new Object[]{o[6], UserHelper.getName((ID) o[6])};

        JSONObject data = JSONUtils.toJSONObject(
                new String[] { "id", "taskNumber", "taskName", "createdOn", "deadline", "executor", "status", "seq", "priority", "endTime" },
                new Object[] { o[2], taskNumber, o[3], createdOn, deadline, executor, o[7], o[8], o[9], endTime });

        if (appendTags) {
            data.put("tags", TaskTagController.getTaskTags((ID) o[2]));
        }

        return data;
    }

    // -- for EntityView

    @GetMapping("alist")
    public RespBody getProjectAndPlans(HttpServletRequest request) {
        final ID user = getRequestUser(request);

        ConfigBean[] ps = ProjectManager.instance.getAvailable(user, true);
        JSONArray alist = new JSONArray();

        for (ConfigBean p : ps) {
            JSONObject item = (JSONObject) p.toJSON("id", "projectName");

            // 面板
            ConfigBean[] plans = ProjectManager.instance.getPlansOfProject(p.getID("id"));
            JSONArray plansList = new JSONArray();
            for (ConfigBean plan : plans) {
                plansList.add(plan.toJSON("id", "planName", "flowStatus"));
            }
            item.put("plans", plansList);

            alist.add(item);
        }

        return RespBody.ok(alist);
    }

    @RequestMapping("tasks/related-list")
    public JSON relatedTaskList(@IdParam(name = "related") ID relatedId, HttpServletRequest request) {
        String queryWhere = "relatedRecord = ?";

        // 关键词搜索
        String search = getParameter(request, "search");
        if (StringUtils.isNotBlank(search)) {
            queryWhere += " and taskName like '%" + StringEscapeUtils.escapeSql(search) + "%'";
        }

        int pageNo = getIntParameter(request, "pageNo", 1);
        int pageSize = getIntParameter(request, "pageSize", 40);
        String sort = getParameter(request, "sort");

        if ("deadline".equalsIgnoreCase(sort)) sort = "deadline desc";
        else if ("modifiedOn".equalsIgnoreCase(sort)) sort = "modifiedOn desc";
        else sort = "seq asc";
        queryWhere += " order by " + sort;

        String querySql = "select projectId.projectCode,taskNumber,taskId,taskName,executor,status,priority,deadline,endTime from ProjectTask where " + queryWhere;
        Object[][] tasks = Application.createQueryNoFilter(querySql)
                .setParameter(1, relatedId)
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();

        JSONArray alist = new JSONArray();
        for (Object[] o : tasks) {
            alist.add(JSONUtils.toJSONObject(
                    new String[] { "taskNumber", "taskId", "taskName" },
                    new Object[] { String.format("%s-%d", o[0], o[1]), o[2], o[3] }));
        }
        return alist;
    }
}
