/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts.builtin;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.service.dashboard.charts.ChartData;
import com.rebuild.core.service.project.ProjectManager;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;

import java.util.Date;

/**
 * @author devezhao
 * @since 2021/5/26
 */
public class ProjectTasks extends ChartData implements BuiltinChart {

    // 虚拟ID
    public static final ID MYID = ID.valueOf("017-9000000000000003");

    public ProjectTasks() {
        super(null);
        this.config = getChartConfig();
    }

    @Override
    public ID getChartId() {
        return MYID;
    }

    @Override
    public String getChartTitle() {
        return Language.L("我的任务");
    }

    @Override
    public JSON build() {
        final int viewState = ObjectUtils.toInt(getExtraParams().get("state"), 0);
        Object[][] tasks = Application.createQueryNoFilter(
                "select taskId,projectId,projectPlanId,taskNumber,taskName,createdOn,deadline,endTime,status,priority" +
                        " from ProjectTask where executor = ? and status = ? order by seq asc")
                .setParameter(1, getUser())
                .setParameter(2, viewState)
                .array();

        JSONArray array = new JSONArray();
        for (Object[] o : tasks) {
            ID projectId = (ID) o[1];
            ConfigBean cbProject = ProjectManager.instance.getProject(projectId, null);
            ConfigBean cbPlan = ProjectManager.instance.getPlanOfProject((ID) o[2], projectId);

            String projectName = String.format("%s (%s)",
                    cbProject.getString("projectName"), cbPlan.getString("planName"));
            String taskNumber = String.format("%s-%s", cbProject.getString("projectCode"), o[3]);

            array.add(JSONUtils.toJSONObject(
                    new String[] { "id", "projectName", "planFlow", "taskNumber", "taskName", "createdOn", "deadline", "endTime", "status", "priority" },
                    new Object[] { o[0], projectName, cbPlan.getInteger("flowStatus"), taskNumber, o[4],
                            I18nUtils.formatDate((Date) o[5]),
                            I18nUtils.formatDate((Date) o[6]),
                            I18nUtils.formatDate((Date) o[7]),
                            o[8], o[9]
                    }));
        }
        return array;
    }
}
