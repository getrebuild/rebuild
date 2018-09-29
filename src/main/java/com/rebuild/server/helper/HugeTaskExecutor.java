/*
rebuild - Building your system freely.
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

package com.rebuild.server.helper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import cn.devezhao.commons.CodecUtils;

/**
 * 
 * @author devezhao
 * @since 09/29/2018
 */
public class HugeTaskExecutor extends QuartzJobBean {

	private static final Map<String, HugeTask> TASKS = new ConcurrentHashMap<>();
	
	/**
	 * @param task
	 * @return
	 */
	public static String submit(HugeTask task) {
		String taskid = task.getClass().getSimpleName() + "-" + CodecUtils.randomCode(20);
		TASKS.put(taskid, task);
		return taskid;
	}
	
	/**
	 * @param taskid
	 * @return
	 */
	public static HugeTask getTask(String taskid) {
		return TASKS.get(taskid);
	}
	
	// --
	
	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {

		// TODO 任务完成 2 小时了，移除
		
	}
}
