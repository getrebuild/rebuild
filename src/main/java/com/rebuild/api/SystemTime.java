/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.DateFormatUtils;
import com.alibaba.fastjson.JSON;
import com.rebuild.utils.CommonsUtils;
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
				new Object[] { CommonsUtils.formatClientDate(CalendarUtils.now()) });
		return formatSuccess(data);
	}
}
