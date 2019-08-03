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
	 * @param appId
	 * @param bindUser
	 * @param reqParams
	 * @param postData
	 */
	public ApiContext(String appId, ID bindUser, Map<String, String> reqParams, JSON postData) {
		this.appId = appId;
		this.bindUser = bindUser;
		this.reqParams = reqParams;
		this.postData = postData;
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
	 * @return
	 */
	public Map<String, String> getReqParams() {
		return reqParams;
	}

	/**
	 * @return
	 */
	public JSON getPostData() {
		return postData;
	}
}
