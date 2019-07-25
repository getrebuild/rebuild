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

import com.alibaba.fastjson.JSON;

/**
 * API 基类
 *
 * @author devezhao
 * @since 01/10/2019
 */
public abstract class BaseApi extends Controll {

	/**
	 * MUST!!!
	 */
	protected BaseApi() {
		super();
	}

	/**
	 * API 名称。默认使用类名（遇大写字符加 -），如 SystemTime <tt>system-time</tt>
	 *
	 * @return
	 */
	protected String getApiName() {
		String apiName = getClass().getSimpleName();
		apiName = apiName.replaceAll("[A-Z]", "-$0").toLowerCase();
		return apiName.substring(1);
	}

	/**
	 * API 执行。所有错误应直接抛出 {@link ApiInvokeException} 异常，并在异常中写明原因
	 *
	 * @param context
	 * @return
	 * @throws ApiInvokeException
	 */
	abstract public JSON execute(ApiContext context) throws ApiInvokeException;
}
