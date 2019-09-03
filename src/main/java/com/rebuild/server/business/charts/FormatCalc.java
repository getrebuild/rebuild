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

package com.rebuild.server.business.charts;

/**
 * 字段计算方式
 * 
 * @author devezhao
 * @since 12/15/2018
 */
public enum FormatCalc {

	// 数字字段
	SUM("求和"), AVG("平均值"), MAX("最大数"), MIN("最小数"), COUNT("计数"),
	
	// 日期字段
	Y("年"), M("月"), D("日"), H("时"),
	
	NONE("无"),
	
	;
	
	private String label;
	
	FormatCalc(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
}
