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

import com.alibaba.fastjson.JSON;

import cn.devezhao.commons.CalendarUtils;

/**
 * 
 * @author devezhao
 * @since 09/29/2018
 */
public abstract class TimeBulkTask extends BulkTask {

	private Date beginTime;
	private Date endTime;
	
	/**
	 * @param data
	 */
	protected TimeBulkTask(JSON data) {
		super(data);
		this.beginTime = CalendarUtils.now();
	}
	
	/**
	 * @return
	 */
	public Date getBeginTime() {
		return beginTime;
	}
	
	/**
	 * @return
	 */
	public Date getEndTime() {
		return endTime;
	}
	
	/**
	 * @param endTime
	 */
	protected void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	
	/**
	 * 任务耗时（ms）
	 * 
	 * @return
	 */
	public long getElapsedTime() {
		Date end = getEndTime();
		end = end == null ? CalendarUtils.now() : end;
		return end.getTime() - beginTime.getTime();
	}
}
