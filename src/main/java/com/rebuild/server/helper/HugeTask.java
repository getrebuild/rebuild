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

import com.alibaba.fastjson.JSON;

/**
 * 前台提交的耗时操作
 * 
 * @author devezhao
 * @since 09/29/2018
 */
public abstract class HugeTask implements Runnable {

	private int totalQuantity = -1;
	private int completeQuantity = 0;
	
	/**
	 * 任务执行相关数据
	 */
	protected JSON data;
	
	/**
	 * @param data
	 */
	protected HugeTask(JSON data) {
		this.data = data;
	}
	
	/**
	 * @param totalQuantity
	 */
	protected void setTotalQuantity(int totalQuantity) {
		this.totalQuantity = totalQuantity;
	}
	
	/**
	 * @param completeQuantity
	 */
	protected void setCompleteQuantity(int completeQuantity) {
		this.completeQuantity = completeQuantity;
	}
	
	/**
	 * @return
	 */
	public int getTotalQuantity() {
		return totalQuantity;
	}
	
	/**
	 * @return
	 */
	public int getCompleteQuantity() {
		return completeQuantity;
	}
	
	/**
	 * 任务完成率
	 * 
	 * @return
	 */
	public double getCompletePercent() {
		if (totalQuantity == -1 || completeQuantity == 0) {
			return 0;
		}
		if (totalQuantity == completeQuantity) {
			return 1;
		}
		return completeQuantity / totalQuantity;
	}
	
	/**
	 * 是否完成?
	 * 
	 * @return
	 */
	public boolean isCompleted() {
		return totalQuantity == -1 || totalQuantity == completeQuantity;
	}
}
