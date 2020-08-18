/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper.task;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ThreadPool;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.RebuildException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 任务执行调度/管理
 * 
 * @author devezhao
 * @since 09/29/2018
 * @see org.springframework.core.task.SyncTaskExecutor
 * @see org.springframework.core.task.AsyncTaskExecutor
 */
public class TaskExecutors extends QuartzJobBean {

	private static final Log LOG = LogFactory.getLog(TaskExecutors.class);

	private static final int MAX_TASKS_NUMBER = Integer.max(Runtime.getRuntime().availableProcessors() / 2, 2);

	private static final ExecutorService EXECS = new ThreadPoolExecutor(
			MAX_TASKS_NUMBER, MAX_TASKS_NUMBER, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>(MAX_TASKS_NUMBER * 10));

	private static final Map<String, HeavyTask<?>> TASKS = new ConcurrentHashMap<>();

	/**
	 * 提交给任务调度（异步执行）
	 *
	 * @param task
	 * @param execUser 执行用户。因为是在线程中执行，所以必须指定
	 * @return 任务 ID，可通过任务ID获取任务对象，或取消任务
	 */
	public static String submit(HeavyTask<?> task, ID execUser) {
		String taskid = task.getClass().getSimpleName() + "-" + CodecUtils.randomCode(20);
		task.setUser(execUser);
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
	 * @param taskid
	 * @return
	 */
	public static HeavyTask<?> getTask(String taskid) {
		return TASKS.get(taskid);
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
	 * 直接执行此方法（同步方式），有返回值。
	 * 需要自行处理异常、需自行处理线程用户问题
	 *
	 * @param task
	 * @see HeavyTask#run()
	 */
	public static Object exec(HeavyTask<?> task) throws Exception {
		return task.exec();
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
				LOG.info("HeavyTask self-destroying : " + e.getKey());
			}
		}
	}

	/**
	 */
	public void shutdown() {
		List<Runnable> runs = EXECS.shutdownNow();
		if (!runs.isEmpty()) {
			LOG.warn(runs.size() + " tasks interrupted");
		}
	}
}
