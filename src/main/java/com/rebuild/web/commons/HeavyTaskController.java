/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.ThreadPool;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONAware;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 任务 `HeavyTask` 操作
 *
 * @author devezhao
 * @see HeavyTask
 * @see TaskExecutors
 * @since 09/29/2018
 */
@RequestMapping("/commons/task/")
@RestController
public class HeavyTaskController extends BaseController {

    // 任务状态
    @GetMapping("state")
    public JSONAware taskState(HttpServletRequest request) {
        String taskid = getParameterNotNull(request, "taskid");
        HeavyTask<?> task = TaskExecutors.get(taskid);

        if (task == null) {
            return RespBody.error("Unknow task : " + taskid);
        } else {
            return formatTaskState(task);
        }
    }

    // 中断任务
    @RequestMapping("cancel")
    public JSONAware taskCancel(HttpServletRequest request) {
        String taskid = getParameterNotNull(request, "taskid");
        HeavyTask<?> task = TaskExecutors.get(taskid);
        if (task == null) {
            return RespBody.error("Unknow task : " + taskid);
        }
        if (task.isCompleted()) {
            return RespBody.errorl("无法终止，因为任务已经完成");
        }

        task.setInterruptState();

        for (int i = 1; i <= 4; i++) {
            ThreadPool.waitFor(i * 500);

            if (task.isInterruptState()) {
                return formatTaskState(task);
            }
        }
        return RespBody.errorl("无法终止任务");
    }

    /**
     * 格式化任务状态信息
     *
     * @param task
     * @return
     */
    private JSON formatTaskState(HeavyTask<?> task) {
        JSONObject state = new JSONObject();
        state.put("total", task.getTotal());
        state.put("progress", task.getCompletedPercent());
        state.put("completed", task.getCompleted());
        state.put("succeeded", task.getSucceeded());
        state.put("isCompleted", task.isCompleted());
        state.put("isInterrupted", task.isInterruptState());
        state.put("elapsedTime", task.getElapsedTime());
        state.put("hasError", task.getErrorMessage());
        if (task.isCompleted() || task.isInterruptState()) state.put("execResults", task.getExecResults());
        return state;
    }
}
