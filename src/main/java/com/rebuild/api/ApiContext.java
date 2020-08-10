/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

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
	 * 获取 POST 数据
	 *
	 * @return
	 */
	public JSON getPostData() {
		return postData == null ? JSONUtils.EMPTY_OBJECT : postData;
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
	 * @param name
	 * @return
	 */
	public String getParameter(String name) {
		return getParameterMap().get(name);
	}

	/**
	 * @param name
	 * @return
	 */
	public ID getParameterAsId(String name) {
		String value = getParameterMap().get(name);
		return ID.isId(value) ? ID.valueOf(value) : null;
	}

	/**
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public int getParameterAsInt(String name, int defaultValue) {
		String value = getParameterMap().get(name);
		if (NumberUtils.isNumber(value)) {
			return NumberUtils.toInt(value);
		}
		return defaultValue;
	}

	/**
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public long getParameterAsLong(String name, long defaultValue) {
		String value = getParameterMap().get(name);
		if (NumberUtils.isNumber(value)) {
			return NumberUtils.toLong(value);
		}
		return defaultValue;
	}

	/**
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public boolean getParameterAsBool(String name, boolean defaultValue) {
		String value = getParameterMap().get(name);
		if (StringUtils.isBlank(value)) {
			return defaultValue;
		}
		return BooleanUtils.toBoolean(value);
	}
}
