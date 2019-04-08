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

package com.rebuild.server.helper.task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ThreadPool;

/**
 * 任务执行调度/管理
 * 
 * @author devezhao
 * @since 09/29/2018
 */
public class BulkTaskExecutor extends QuartzJobBean {
	
	private static final int EXECS_MAX = 4;
	private static final ExecutorService EXECS = Executors.newFixedThreadPool(EXECS_MAX);
	
	private static final Map<String, BulkTask> TASKS = new ConcurrentHashMap<>();
	
	/**
	 * 提交给任务调度（异步执行）
	 * 
	 * @param task
	 * @return 任务 ID
	 */
	public static String submit(BulkTask task) {
		ThreadPoolExecutor tpe = (ThreadPoolExecutor) EXECS;
		int queueSize = tpe.getQueue().size();
		if (queueSize > EXECS_MAX * 5) {
			throw new RejectedExecutionException("Too many task : " + tpe.getTaskCount());
		}
		
		String taskid = task.getClass().getSimpleName() + "-" + CodecUtils.randomCode(20);
		EXECS.execute(task);
		TASKS.put(taskid, task);
		return taskid;
	}
	
	/**
	 * 取消执行
	 * 
	 * @param task
	 */
	public static boolean cancel(String taskid) {
		BulkTask task = TASKS.get(taskid);
		if (task == null) {
			throw new RejectedExecutionException("No Task found : " + taskid);
		}
		task.interrupt();
		ThreadPool.waitFor(200);
		return task.isInterrupted();
	}
	
	/**
	 * 直接执行此方法（同步方式）
	 * 
	 * @param task
	 */
	public static void run(BulkTask task) {
		task.run();
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
		for (Map.Entry<String, BulkTask> e : TASKS.entrySet()) {
			BulkTask task = e.getValue();
			if (!task.isCompleted()) {
				continue;
			}
			
			// 无完成时间不移除
			if (task.getCompletedTime() == null) {
				continue;
			}
			
			long completedTime = (System.currentTimeMillis() - task.getCompletedTime().getTime()) / 1000;
			if (completedTime > 60 * 120) {
				TASKS.remove(e.getKey());
				// TODO 任务完成后发内部通知
			}
		}
	}
}
