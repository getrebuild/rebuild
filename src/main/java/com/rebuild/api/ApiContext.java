/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.privileges.UserService;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.util.Collections;
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
     */
    public ApiContext(Map<String, String> reqParams) {
        this(reqParams, null, null, UserService.SYSTEM_USER);
    }

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
        this.reqParams = Collections.unmodifiableMap(reqParams);
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
        if (bindUser == null) return UserService.SYSTEM_USER;
        else return bindUser;
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
        return postData == null ? new JSONObject() : JSONUtils.clone(postData);
    }

    /**
     * @param name
     * @return
     * @throws ApiInvokeException
     */
    public String getParameterNotBlank(String name) throws ApiInvokeException {
        String value = reqParams.get(name);
        if (StringUtils.isBlank(value)) {
            throw new ApiInvokeException(ApiInvokeException.ERR_BADPARAMS, "Parameter [" + name + "] cannot be null");
        }
        return value;
    }

    /**
     * @param name
     * @return
     */
    public String getParameter(String name) {
        return reqParams.get(name);
    }

    /**
     * @param name
     * @return
     */
    public ID getParameterAsId(String name) {
        String value = getParameterNotBlank(name);
        if (ID.isId(value)) return ID.valueOf(value);
        throw new ApiInvokeException(ApiInvokeException.ERR_BADPARAMS, "Parameter [" + name + "] is invalid");
    }

    /**
     * @param name
     * @param defaultValue
     * @return
     */
    public int getParameterAsInt(String name, int defaultValue) {
        String value = reqParams.get(name);
        if (NumberUtils.isNumber(value)) return NumberUtils.toInt(value);
        else return defaultValue;
    }

    /**
     * @param name
     * @param defaultValue
     * @return
     */
    public boolean getParameterAsBool(String name, boolean defaultValue) {
        String value = reqParams.get(name);
        if (StringUtils.isBlank(value)) return defaultValue;
        else return BooleanUtils.toBoolean(value);
    }
}
