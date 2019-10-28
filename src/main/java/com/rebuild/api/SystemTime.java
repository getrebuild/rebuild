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

package com.rebuild.api;

import cn.devezhao.commons.CalendarUtils;
import com.alibaba.fastjson.JSON;
import com.rebuild.utils.JSONUtils;

/**
 * 参考实现。获取系统时间
 *
 * @author devezhao
 * @since 01/10/2019
 */
public class SystemTime extends BaseApi {

	@Override
	public JSON execute(ApiContext context) {
		JSON data = JSONUtils.toJSONObject(
				new String[] { "time" },
				new Object[] { CalendarUtils.getDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(CalendarUtils.now()) });
		return formatSuccess(data);
	}
}
