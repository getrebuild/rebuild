/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.ThreadPool;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 任务操作入口类
 *
 * @author devezhao
 * @see HeavyTask
 * @see TaskExecutors
 * @since 09/29/2018
 */
@RequestMapping("/commons/task/")
@Controller
public class HeavyTaskController extends BaseController {

    // 任务状态
    @RequestMapping("state")
    public void checkState(HttpServletRequest request, HttpServletResponse response) {
        String taskid = getParameterNotNull(request, "taskid");
        HeavyTask<?> task = TaskExecutors.getTask(taskid);
        if (task == null) {
            writeFailure(response, "无效任务 : " + taskid);
            return;
        }

        JSON state = formatTaskState(task);
        writeSuccess(response, state);
    }

    // 中断任务
    @RequestMapping("cancel")
    public void importCancel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String taskid = getParameterNotNull(request, "taskid");
        HeavyTask<?> task = TaskExecutors.getTask(taskid);
        if (task == null) {
            writeFailure(response, "无效任务 : " + taskid);
            return;
        }
        if (task.isCompleted()) {
            writeFailure(response, "无法终止，因为任务已经完成");
            return;
        }

        task.interrupt();
        for (int i = 0; i < 10; i++) {
            if (task.isInterrupted()) {
                writeSuccess(response, formatTaskState(task));
                return;
            }
            ThreadPool.waitFor(200);
        }
        writeFailure(response);
    }

    /**
     * 格式化任务状态信息
     *
     * @param task
     * @return
     */
    public static JSON formatTaskState(HeavyTask<?> task) {
        JSONObject state = new JSONObject();
        state.put("progress", task.getCompletedPercent());
        state.put("completed", task.getCompleted());
        state.put("succeeded", task.getSucceeded());
        state.put("isCompleted", task.isCompleted());
        state.put("isInterrupted", task.isInterrupted());
        state.put("elapsedTime", task.getElapsedTime());
        state.put("hasError", task.getErrorMessage());
        return state;
    }
}
