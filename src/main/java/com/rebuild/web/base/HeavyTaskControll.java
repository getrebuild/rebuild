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

import com.alibaba.fastjson.JSON;
import com.rebuild.server.helper.task.HeavyTask;
import com.rebuild.server.helper.task.TaskExecutors;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * @author devezhao
 * @since 09/29/2018
 */
@RequestMapping("/commons/task/")
@Controller
public class HeavyTaskControll extends BaseControll {

	@RequestMapping("state")
	public void checkState(HttpServletRequest request, HttpServletResponse response) {
		String taskid = getParameterNotNull(request, "taskid");
		HeavyTask<?> task = TaskExecutors.getTask(taskid);
		JSON ret = JSONUtils.toJSONObject(
				new String[] { "taskid", "completed", "hasError" },
				new Object[] { taskid, task.getCompletedPercent(), task.getErrorMessage() });
		writeSuccess(response, ret);
	}
}
