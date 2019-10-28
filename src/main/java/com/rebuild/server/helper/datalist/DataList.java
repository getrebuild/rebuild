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

package com.rebuild.server.helper.datalist;

import cn.devezhao.persist4j.Entity;
import com.alibaba.fastjson.JSON;

/**
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-20
 */
public interface DataList {
	
	/**
	 * @return
	 */
	Entity getEntity();
	
	/**
	 * 默认过滤条件
	 * 
	 * @return
	 */
	String getDefaultFilter();
	
	/**
	 * JSON 结果集
	 * 
	 * @return
	 */
	JSON getJSONResult();
}
