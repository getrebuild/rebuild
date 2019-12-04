/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.web.base;

import cn.devezhao.commons.ThreadPool;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.helper.task.HeavyTask;
import com.rebuild.server.helper.task.TaskExecutors;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 任务操作入口类
 *
 * @author devezhao
 * @since 09/29/2018
 *
 * @see HeavyTask
 * @see TaskExecutors
 */
@RequestMapping("/commons/task/")
@Controller
public class HeavyTaskControll extends BaseControll {

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
