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

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * API 请求上下文
 * 
 * @author devezhao
 * @since 01/10/2019
 */
public class ApiContext {

	final private String appId;
	final private ID bindUser;
	final private Map<String, String> reqParams;
	final private JSON postData;

	/**
	 * @param reqParams
	 * @param postData
	 */
	public ApiContext(Map<String, String> reqParams, JSON postData) {
		this(reqParams, postData, null, UserService.SYSTEM_USER);
	}

	/**
	 * @param reqParams
	 * @param postData
	 * @param appId
	 * @param bindUser
	 */
	public ApiContext(Map<String, String> reqParams, JSON postData, String appId, ID bindUser) {
		this.reqParams = reqParams;
		this.postData = postData;
		this.appId = appId;
		this.bindUser = bindUser;
	}

	/**
	 * @return
	 */
	public String getAppId() {
		return appId;
	}

	/**
	 * @return
	 */
	public ID getBindUser() {
		if (bindUser == null) {
			return UserService.SYSTEM_USER;
		}
		return bindUser;
	}

	/**
	 * 获取 URL 请求参数
	 *
	 * @return
	 */
	public Map<String, String> getParameterMap() {
		return reqParams;
	}

	/**
	 * @param name
	 * @return
	 * @throws ApiInvokeException
	 */
	public String getParameterNotBlank(String name) throws ApiInvokeException {
		String value = getParameterMap().get(name);
		if (StringUtils.isBlank(value)) {
			throw new ApiInvokeException(ApiInvokeException.ERR_BADPARAMS, "Parameter [" + name + "] cannot be empty");
		}
		return value;
	}

	/**
	 * 获取 POST 数据
	 *
	 * @return
	 */
	public JSON getPostData() {
		return postData == null ? JSONUtils.EMPTY_OBJECT : postData;
	}
}
