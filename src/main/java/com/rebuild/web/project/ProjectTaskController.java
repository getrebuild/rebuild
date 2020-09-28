/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.project.ProjectHelper;
import com.rebuild.core.service.project.ProjectManager;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
@Controller
public class ProjectTaskController extends BaseController {

    @RequestMapping("/project/task/{taskId}")
    public ModelAndView pageTask(@PathVariable String taskId,
                                 HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID taskId2 = ID.isId(taskId) ? ID.valueOf(taskId) : null;
        if (taskId2 == null) {
            response.sendError(404);
            return null;
        }

        final ID user = getRequestUser(request);
        if (!ProjectHelper.checkReadable(taskId2, user)) {
            response.sendError(403, "你无权查看该任务");
        }

        ConfigBean cfg = ProjectManager.instance.getProjectByTask(taskId2, user);

        ModelAndView mv = createModelAndView("/project/task-view");
        mv.getModel().put("id", taskId2.toLiteral());
        mv.getModel().put("projectIcon", cfg.getString("iconName"));
        mv.getModel().put("isMember", cfg.get("members", Set.class).contains(user));
        mv.getModel().put("isManageable", ProjectHelper.isManageable(taskId2, user));
        return mv;
    }

    @RequestMapping("/project/tasks/list")
    public void taskList(HttpServletRequest request, HttpServletResponse response) {
        final ID planId = getIdParameterNotNull(request, "plan");

        String querySql = "select " + BASE_FIELDS + " from ProjectTask where projectPlanId = ?";

        // 关键词搜索
        String search = getParameter(request, "search");
        if (StringUtils.isNotBlank(search)) {
            querySql += " and taskName like '%" + StringEscapeUtils.escapeSql(search) + "%'";
        }

        // 高级查询
        JSON advFilter = ServletUtils.getRequestJson(request);
        if (advFilter != null) {
            String filterSql = new AdvFilterParser((JSONObject) advFilter).toSqlWhere();
            if (filterSql != null) {
                querySql += " and (" + filterSql + ")";
            }
        }

        // 排序
        String sort = getParameter(request, "sort");
        if ("deadline".equalsIgnoreCase(sort)) sort = "deadline desc";
        else if ("modifiedOn".equalsIgnoreCase(sort)) sort = "modifiedOn desc";
        else sort = "seq asc";
        querySql += " order by " + sort;

        Object[][] tasks = Application.createQueryNoFilter(querySql)
                .setParameter(1, planId)
                .array();

        JSONArray alist = new JSONArray();
        for (Object[] o : tasks) {
            alist.add(formatTask(o));
        }

        JSON ret = JSONUtils.toJSONObject(new String[]{"count", "tasks"}, new Object[]{tasks.length, alist});
        writeSuccess(response, ret);
    }

    @RequestMapping("/project/tasks/get")
    public void taskGet(HttpServletRequest request, HttpServletResponse response) {
        ID taskId = getIdParameterNotNull(request, "task");
        Object[] task = Application.createQueryNoFilter(
                "select " + BASE_FIELDS + " from ProjectTask where taskId = ?")
                .setParameter(1, taskId)
                .unique();

        writeSuccess(response, formatTask(task));
    }

    @RequestMapping("/project/tasks/details")
    public void taskDetails(HttpServletRequest request, HttpServletResponse response) {
        ID taskId = getIdParameterNotNull(request, "task");
        Object[] task = Application.createQueryNoFilter(
                "select " + BASE_FIELDS + ",projectId,description,attachments from ProjectTask where taskId = ?")
                .setParameter(1, taskId)
                .unique();

        JSONObject details = formatTask(task);

        // 状态面板
        details.put("projectId", task[11]);
        details.put("description", task[12]);
        String attachments = (String) task[13];
        details.put("attachments", JSON.parseArray(attachments));

        writeSuccess(response, details);
    }

    private static final String BASE_FIELDS =
            "projectId.projectCode,taskNumber,taskId,taskName,createdOn,deadline,executor,status,seq,priority,endTime";

    /**
     * @param o
     * @return
     */
    private JSONObject formatTask(Object[] o) {
        String taskNumber = o[1].toString();
        if (StringUtils.isNotBlank((String) o[0])) taskNumber = o[0] + "-" + taskNumber;

        String createdOn = I18nUtils.formatDate((Date) o[4]);
        String deadline = I18nUtils.formatDate((Date) o[5]);
        String endTime = I18nUtils.formatDate((Date) o[10]);

        Object[] executor = o[6] == null ? null : new Object[]{o[6], UserHelper.getName((ID) o[6])};

        return JSONUtils.toJSONObject(
                new String[]{"id", "taskNumber", "taskName", "createdOn", "deadline", "executor", "status", "seq", "priority", "endTime"},
                new Object[]{o[2], taskNumber, o[3], createdOn, deadline, executor, o[7], o[8], o[9], endTime});
    }
}
