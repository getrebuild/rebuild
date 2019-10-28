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

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ThreadPool;
import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 任务执行调度/管理
 * 
 * @author devezhao
 * @since 09/29/2018
 */
public class TaskExecutors extends QuartzJobBean {
	
	private static final int EXECS_MAX = 4;
	private static final ExecutorService EXECS = Executors.newFixedThreadPool(EXECS_MAX);
	
	private static final Map<String, HeavyTask<?>> TASKS = new ConcurrentHashMap<>();
	
	/**
	 * 提交给任务调度（异步执行）
	 * 
	 * @param task
	 * @return 任务 ID
	 */
	public static String submit(HeavyTask<?> task) {
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
	 * @param taskid
	 */
	public static boolean cancel(String taskid) {
		HeavyTask<?> task = TASKS.get(taskid);
		if (task == null) {
			throw new RebuildException("No Task found : " + taskid);
		}
		task.interrupt();
		ThreadPool.waitFor(500);
		return task.isInterrupted();
	}
	
	/**
	 * 直接执行此方法（同步方式）
	 * 
	 * @param task
	 */
	public static void run(HeavyTask<?> task) {
		task.run();
	}
	
	/**
	 * @param taskid
	 * @return
	 */
	public static HeavyTask<?> getTask(String taskid) {
		return TASKS.get(taskid);
	}
	
	// --
	
	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		for (Map.Entry<String, HeavyTask<?>> e : TASKS.entrySet()) {
			HeavyTask<?> task = e.getValue();
			if (task.getCompletedTime() == null || !task.isCompleted()) {
				continue;
			}
			
			long leftTime = (System.currentTimeMillis() - task.getCompletedTime().getTime()) / 1000;
			if (leftTime > 60 * 120) {
				TASKS.remove(e.getKey());
				Application.LOG.info("HeavyTask self-destroying : " + e.getKey());
			}
		}
	}
}
