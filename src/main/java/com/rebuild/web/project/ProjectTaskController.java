/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.project.ProjectHelper;
import com.rebuild.core.service.project.ProjectManager;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import com.rebuild.web.InvalidParameterException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * 任务
 *
 * @author devezhao
 * @since 2020/6/29
 */
@Slf4j
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
            response.sendError(403, Language.L("你无权查看此任务"));
            return null;
        }

        ConfigBean project = ProjectManager.instance.getProjectByX(taskId2, user);

        ModelAndView mv = createModelAndView("/project/task-view");
        mv.getModel().put("id", taskId2.toLiteral());
        mv.getModel().put("projectIcon", project.getString("iconName"));
        mv.getModel().put("projectStatus", project.getInteger("status"));
        mv.getModel().put("isMember", project.get("members", Set.class).contains(user));
        return mv;
    }

    @RequestMapping("tasks/list")
    public JSON taskList(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final ID projectId = getIdParameterNotNull(request, "project");
        final String planKey = getParameterNotNull(request, "plan");

        String queryWhere = "(" + buildCustomPlanSql(planKey, projectId) + ")";

        // 关键词搜索
        String search = getParameter(request, "search");
        if (StringUtils.isNotBlank(search)) {
            queryWhere += " and (taskName like '%" + CommonsUtils.escapeSql(search) + "%'";

            // 搜编号
            if (search.matches("^([A-Za-z]{2,4}-)?[0-9]{1,9}")) {
                String[] no = search.split("-");
                queryWhere += " or taskNumber = " + (no.length > 1 ? no[1] : no[0]);
            }
            queryWhere += ")";
        }

        // 高级查询
        JSON advFilter = ServletUtils.getRequestJson(request);
        if (ParseHelper.validAdvFilter((JSONObject) advFilter)) {
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
            Object[] count2 = Application.createQueryNoFilter(countSql).unique();
            count = ObjectUtils.toInt(count2[0]);
            if (count == 0) return NO_TASKS;
        }

        String sort = buildQuerySort(request);
        // 未完成的优先显示
        if (sort.contains("deadline") && "0-0".equals(planKey)) {
            sort = "status asc, deadline desc";
        }
        queryWhere += " order by " + sort;

        ConfigBean project = ProjectManager.instance.getProject(projectId, user);
        JSONArray alist = queryCardDatas(project, user, queryWhere, new int[] { pageSize, pageNo * pageSize - pageSize });

        return JSONUtils.toJSONObject(
                new String[] { "count", "tasks" },
                new Object[] { count, alist });
    }

    private String buildQuerySort(HttpServletRequest request) {
        String sort = getParameter(request, "sort");
        if ("deadline".equalsIgnoreCase(sort)) sort = "deadline desc";
        else if ("modifiedOn".equalsIgnoreCase(sort)) sort = "modifiedOn desc";
        else sort = "seq asc";
        return sort;
    }

    private String buildCustomPlanSql(String planKey, ID projectId) {
        if (ID.isId(planKey)) {
            return "projectPlanId = '" + planKey + "'";
        }

        String planValue = planKey.split("-")[1];
        String baseSql = "projectId = '" + projectId + "' and ";

        if (planKey.startsWith(ProjectController.GROUP_PRIORITY)) {
            return baseSql + "priority = " + planValue;
        }

        final DateFormat dtf = CalendarUtils.getUTCDateTimeFormat();
        final String today = dtf.format(CalendarUtils.now());

        if (planKey.startsWith(ProjectController.GROUP_DEADLINE)) {
            if ("1".equals(planValue)) {
                return baseSql + String.format("status = 0 and deadline <= '%s'", today);
            } else if ("2".equals(planValue)) {
                return baseSql + String.format("status = 0 and (deadline > '%s' and deadline <= '%s 23:59:59')",
                        today, today.split(" ")[0]);
            } else if ("3".equals(planValue)) {
                return baseSql + String.format("status = 0 and (deadline > '%s 23:59:59' and deadline <= '%s')",
                        today.split(" ")[0], dtf.format(CalendarUtils.addDay(7)));
            } else if ("4".equals(planValue)) {
                return baseSql + String.format("status = 0 and (deadline is null or deadline > '%s')",
                        dtf.format(CalendarUtils.addDay(7)));
            }
        } else if (planKey.startsWith(ProjectController.GROUP_MODIFIED)) {
            if ("1".equals(planValue)) {
                return baseSql + MessageFormat.format("modifiedOn >= ''{0} 00:00:00'' and modifiedOn <= ''{0} 23:59:59''",
                        today.split(" ")[0]);
            } else if ("2".equals(planValue)) {
                return baseSql + String.format("modifiedOn >= '%s' and modifiedOn < '%s 00:00:00'",
                        dtf.format(CalendarUtils.addDay(-7)), today.split(" ")[0]);
            } else if ("3".equals(planValue)) {
                return baseSql + String.format("modifiedOn > '%s' and modifiedOn < '%s'",
                        dtf.format(CalendarUtils.addDay(-14)), dtf.format(CalendarUtils.addDay(-7)));
            } else if ("4".equals(planValue)) {
                return baseSql + String.format("modifiedOn < '%s'", dtf.format(CalendarUtils.addDay(-14)));
            }
        } else if (planKey.equals("0-0")) {
            return "1=1";
        }

        throw new InvalidParameterException(Language.L("无效请求参数 (%s=%s)", "id", planKey));
    }

    @GetMapping("tasks/get")
    public JSON taskGet(@IdParam(name = "task") ID taskId, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        ConfigBean project = ProjectManager.instance.getProjectByX(taskId, user);

        String where = "taskId = '" + taskId + "'";
        JSONArray a = queryCardDatas(project, getRequestUser(request), where, null);

        return (JSON) a.get(0);
    }

    private JSONArray queryCardDatas(ConfigBean project, ID user, String queryWhere, int[] limits) {
        // 卡片字段显示
        JSON cardFields = project.getJSON("cardFields");

        final Set<String> fields2show = new HashSet<>();
        if (cardFields == null) {
            fields2show.add("createdOn");
            fields2show.add("endTime");
            fields2show.add("_tag");
        } else {
            for (Object o : (JSONArray) cardFields) {
                fields2show.add(o.toString());
            }
        }

        String queryFields = FMT_FIELDS11 + ",";
        if (fields2show.contains("createdBy")) queryFields += "createdBy,";
        else queryFields += "taskId,";
        if (fields2show.contains("modifiedOn")) queryFields += "modifiedOn,";
        else queryFields += "taskId,";
        if (fields2show.contains("description")) queryFields += "description,";
        else queryFields += "taskId,";
        if (fields2show.contains("attachments")) queryFields += "attachments,";
        else queryFields += "taskId,";

        queryFields = queryFields.substring(0, queryFields.length() - 1);
        String querySql = String.format("select %s from ProjectTask where %s", queryFields, queryWhere);
        Query query = Application.createQueryNoFilter(querySql);
        if (limits != null) query.setLimit(limits[0], limits[1]);

        Object[][] tasks = query.array();

        JSONArray alist = new JSONArray();
        for (Object[] o : tasks) {
            JSONObject item = formatTask(o, user, fields2show.contains("_tag"), true);

            if (fields2show.contains("createdBy")) {
                item.put("createdBy", new Object[] { o[12], UserHelper.getName((ID) o[12]) });
            }
            if (!fields2show.contains("createdOn")) {
                item.remove("createdOn");
            }
            if (fields2show.contains("modifiedOn")) {
                item.put("modifiedOn", I18nUtils.formatDate((Date) o[13]));
            }
            if (!fields2show.contains("endTime")) {
                item.remove("endTime");
            }
            if (fields2show.contains("description")) {
                item.put("description", StringUtils.isNotBlank((String) o[14]));
            }
            if (fields2show.contains("attachments")) {
                item.put("attachments", o[15] != null && ((String) o[15]).length() > 10);
            }

            alist.add(item);
        }
        return alist;
    }

    @GetMapping("tasks/details")
    public JSON taskDetails(@IdParam(name = "task") ID taskId, HttpServletRequest request) {
        final ID user = getRequestUser(request);

        Object[] task = Application.createQueryNoFilter(
                String.format("select %s,description,attachments,relatedRecord from ProjectTask where taskId = ?", FMT_FIELDS11))
                .setParameter(1, taskId)
                .unique();
        if (task == null) throw new NoRecordFoundException(taskId, Boolean.TRUE);

        JSONObject details = formatTask(task, user, true, false);

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

    private static final String FMT_FIELDS11 =
            "projectId,projectPlanId,taskNumber,taskId,taskName,createdOn,deadline,executor,status,seq,priority,endTime";
    /**
     * @param o
     * @param user
     * @param needTags
     * @param needPlanName
     * @return
     * @throws ConfigurationException 如果指定用户无权限
     * @see #FMT_FIELDS11
     */
    private JSONObject formatTask(Object[] o, ID user, boolean needTags, boolean needPlanName) throws ConfigurationException {
        final ConfigBean project = ProjectManager.instance.getProject((ID) o[0], user);

        String taskNumber = String.format("%s-%s", project.getString("projectCode"), o[2]);
        String createdOn = I18nUtils.formatDate((Date) o[5]);
        String deadline = I18nUtils.formatDate((Date) o[6]);
        String endTime = I18nUtils.formatDate((Date) o[11]);
        Object[] executor = o[7] == null ? null : new Object[]{o[7], UserHelper.getName((ID) o[7])};

        JSONObject data = JSONUtils.toJSONObject(
                new String[] { "id", "taskNumber", "taskName", "createdOn", "deadline", "executor", "status", "seq", "priority", "endTime", "projectId", "projectStatus" },
                new Object[] { o[3], taskNumber, o[4], createdOn, deadline, executor, o[8], o[9], o[10], endTime, o[0], project.getInteger("status") });

        // 标签
        if (needTags) {
            data.put("tags", TaskTagController.getTaskTags((ID) o[3]));
        }

        if (user != null) {
            // 项目信息
            ConfigBean plan =  ProjectManager.instance.getPlanOfProject((ID) o[1], (ID) o[0]);
            data.put("planName", String.format("%s (%s)",
                    project.getString("projectName"), plan.getString("planName")));
            data.put("planFlow", plan.getInteger("flowStatus"));
            // 权限
            data.put("projectMember", project.get("members", Set.class).contains(user));
            data.put("isManageable", ProjectHelper.isManageable((ID) o[3], user));

            // v3.7
            if (needPlanName) data.put("planName", plan.getString("planName"));
        }
        return data;
    }

    // -- for General Entity

    @GetMapping("alist")
    public RespBody getProjectAndPlans(HttpServletRequest request) {
        final ID user = getRequestUser(request);

        ConfigBean[] ps = ProjectManager.instance.getAvailable(user);
        JSONArray alist = new JSONArray();

        for (ConfigBean p : ps) {
            if (p.getInteger("status") == ProjectManager.STATUS_ARCHIVED) continue;  // 已归档
            if (!p.get("members", Set.class).contains(user)) continue;  // 非成员

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
    public JSON relatedTaskList(@IdParam(name = "related", required = false) ID relatedId,
                                @IdParam(name = "task", required = false) ID taskId,
                                HttpServletRequest request) {
        Assert.isTrue(relatedId != null || taskId != null, Language.L("无效请求参数"));

        final ID user = getRequestUser(request);
        String queryWhere = String.format("relatedRecord = '%s'", relatedId);

        // 关键词搜索
        String search = getParameter(request, "search");
        if (StringUtils.isNotBlank(search)) {
            queryWhere += " and taskName like '%" + CommonsUtils.escapeSql(search) + "%'";
        }

        int pageNo = getIntParameter(request, "pageNo", 1);
        int pageSize = getIntParameter(request, "pageSize", 40);

        queryWhere += " order by " + buildQuerySort(request);

        // 获取指定任务的（其他条件忽略）
        if (taskId != null) {
            queryWhere = String.format("taskId = '%s'", taskId);
        }

        Object[][] tasks = Application.createQueryNoFilter(
                String.format("select %s from ProjectTask where %s", FMT_FIELDS11, queryWhere))
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();

        JSONArray array = new JSONArray();
        for (Object[] o : tasks) {
            try {
                array.add(formatTask(o, user, false, false));
            } catch (ConfigurationException ex) {
                // FIXME 无项目权限会报错（考虑任务在相关项中是否无权限也显示）
                log.warn(ex.getLocalizedMessage());
            }
        }
        return array;
    }
}
