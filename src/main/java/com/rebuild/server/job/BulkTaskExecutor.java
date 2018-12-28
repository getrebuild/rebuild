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

package com.rebuild.server.job;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ThreadPool;

/**
 * 大任务执行调度
 * 
 * @author devezhao
 * @since 09/29/2018
 */
public class BulkTaskExecutor extends QuartzJobBean {

	private static final Map<String, BulkTask> TASKS = new ConcurrentHashMap<>();
	
	/**
	 * @param task
	 * @return
	 */
	public static String submit(BulkTask task) {
		String taskid = task.getClass().getSimpleName() + "-" + CodecUtils.randomCode(20);
		TASKS.put(taskid, task);
		ThreadPool.exec(task);
		return taskid;
	}
	
	/**
	 * @param taskid
	 * @return
	 */
	public static BulkTask getTask(String taskid) {
		return TASKS.get(taskid);
	}
	
	// --
	
	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {

		// TODO 任务完成 2 小时了，移除
		
	}
}
