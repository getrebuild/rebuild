/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.project.ProjectTaskService;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * 任务
 *
 * @author devezhao
 * @since 2020/6/29
 */
@Controller
public class ProjectTaskControll extends BasePageControll {

    @RequestMapping("/project/task/{taskId}")
    public ModelAndView pageTask(@PathVariable String taskId,
                                 HttpServletRequest request, HttpServletResponse response) throws IOException {
        return null;
    }

    @RequestMapping("/project/tasks/post")
    public void taskPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject post = (JSONObject) ServletUtils.getRequestJson(request);
        Record record = EntityHelper.parse(post, getRequestUser(request));
        Application.getBean(ProjectTaskService.class).createOrUpdate(record);
        writeSuccess(response);
    }

    @RequestMapping("/project/tasks/delete")
    public void taskDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }

    @RequestMapping("/project/tasks/list")
    public void taskList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID planId = getIdParameterNotNull(request, "plan");

        Object[][] tasks = Application.createQuery(
                "select projectId.projectCode,taskNumber,taskId,taskName,createdOn,executor,status,seq" +
                        " from ProjectTask where projectPlanId = ? order by seq asc")
                .setParameter(1, planId)
                .array();

        JSONArray alist = new JSONArray();
        for (Object[] o : tasks) {
            String taskNumber = o[1].toString();
            if (StringUtils.isNotBlank((String) o[0])) taskNumber = o[0] + "-" + taskNumber;

            String utcTime = CommonsUtils.formatUTCWithZone((Date) o[4]);

            Object[] executor = null;
            if (o[5] != null) {
                executor = new Object[]{ o[5], UserHelper.getName((ID) o[5]) };
            }

            JSON item = JSONUtils.toJSONObject(
                    new String[] { "id", "taskNumber", "taskName", "createdOn", "executor", "status", "seq" },
                    new Object[] { o[2], taskNumber, o[3], utcTime, executor, o[6], o[7] });
            alist.add(item);
        }

        JSON ret = JSONUtils.toJSONObject(new String[]{"count", "tasks"}, new Object[]{tasks.length, alist});
        writeSuccess(response, ret);
    }

    @RequestMapping("/project/tasks/get")
    public void taskGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }
}
