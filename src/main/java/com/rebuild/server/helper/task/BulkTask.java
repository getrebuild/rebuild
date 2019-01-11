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

package com.rebuild.server.helper.task;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.devezhao.commons.CalendarUtils;

/**
 * 耗时操作可通过此类进行，例如大批量删除/修改等。此类提供了进度相关的约定，如总计执行条目，已完成条目/百分比。
 * 集成此类应该处理线程的 <code>isInterrupted</code> 方法，以便任务可以被终止 
 * 
 * @author devezhao
 * @since 09/29/2018
 */
public abstract class BulkTask implements Runnable {
	
	protected static final Log LOG = LogFactory.getLog(BulkTask.class);
	
	volatile 
	private boolean interrupt = false;
	
	private int total = -1;
	private int complete = 0;
	
	private Date beginTime;
	private Date completedTime;
	
	/**
	 */
	protected BulkTask() {
		this.beginTime = CalendarUtils.now();
	}
	
	/**
	 * @param total
	 */
	protected void setTotal(int total) {
		this.total = total;
	}
	
	/**
	 * @param complete
	 */
	protected void setComplete(int complete) {
		this.complete = complete;
	}
	
	/**
	 */
	protected void setCompleteOne() {
		this.complete++;
	}

	/**
	 * 子类应该在执行完毕后调用此方法
	 */
	protected void completedAfter() {
		this.completedTime = CalendarUtils.now();
	}

	/**
	 * 任务启动时间
	 * 
	 * @return
	 */
	protected Date getBeginTime() {
		return beginTime;
	}
	
	/**
	 * 任务完成时间
	 * 
	 * @return
	 */
	protected Date getCompletedTime() {
		return completedTime;
	}
	
	/**
	 * 总计执行条目
	 * 
	 * @return
	 */
	public int getTotal() {
		return total;
	}
	
	/**
	 * 已完成条目
	 * 
	 * @return
	 */
	public int getComplete() {
		return complete;
	}
	
	/**
	 * 完成率
	 * 
	 * @return
	 */
	public double getCompletePercent() {
		if (total == -1 || complete == 0) {
			return 0;
		}
		if (total == complete) {
			return 1;
		}
		return complete / total;
	}
	
	/**
	 * 是否完成?
	 * 
	 * @return
	 */
	public boolean isCompleted() {
		return total != -1 && getComplete() >= getTotal();
	}
	
	/**
	 * 任务已耗时（ms）
	 * 
	 * @return
	 */
	public long getElapsedTime() {
		return CalendarUtils.now().getTime() - beginTime.getTime();
	}
	
	// -- for Thread
	
	public void interrupt() {
		this.interrupt = true;
	}
	
	public boolean isInterrupted() {
		return interrupt;
	}
}
