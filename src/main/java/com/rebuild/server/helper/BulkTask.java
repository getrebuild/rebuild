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

import java.util.Date;

import cn.devezhao.commons.CalendarUtils;

/**
 * 前台提交的耗时操作
 * 
 * @author devezhao
 * @since 09/29/2018
 */
public abstract class BulkTask implements Runnable {

	private int total = -1;
	private int complete = 0;
	
	private Date beginTime;
	
	/**
	 */
	protected BulkTask() {
		this.beginTime = CalendarUtils.now();
	}
	
	/**
	 * @param totalQuantity
	 */
	protected void setTotal(int total) {
		this.total = total;
	}
	
	/**
	 * @param completeQuantity
	 */
	protected void setComplete(int complete) {
		this.complete = complete;
	}
	
	/**
	 * @return
	 */
	public int getTotal() {
		return total;
	}
	
	/**
	 * @return
	 */
	public int getComplete() {
		return complete;
	}
	
	/**
	 * 任务完成率
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
		return total == -1 || total == complete;
	}
	
	/**
	 * 任务已耗时（ms）
	 * 
	 * @return
	 */
	public long getElapsedTime() {
		return CalendarUtils.now().getTime() - beginTime.getTime();
	}
}
