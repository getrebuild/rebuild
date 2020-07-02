/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.momentjava.Moment;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.project.ProjectTaskService;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
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
                "select projectId.projectCode,taskNumber,taskId,taskName,createdOn,status,executor from ProjectTask where projectPlanId = ?")
                .setParameter(1, planId)
                .array();
        for (Object[] o : tasks) {
            o[1] = o[0] + "-" + o[1];
            o[4] = Moment.moment((Date) o[4]).fromNow();
            if (o[6] != null) {
                o[6] = new Object[]{ o[6], UserHelper.getName((ID) o[6]) };
            }
        }

        JSON ret = JSONUtils.toJSONObject(new String[]{"count", "tasks"}, new Object[]{tasks.length, tasks});
        writeSuccess(response, ret);
    }

    @RequestMapping("/project/tasks/get")
    public void taskGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }
}
