/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.JSONable;

/**
 * 统一请求返回消息体
 *
 * @author ZHAO
 * @since 2020/8/28
 */
public class RespBody implements JSONable {

    @JsonProperty("error_code")
    private int errorCode;

    @JsonProperty("error_msg")
    private String errorMsg;

    @JsonProperty("data")
    private Object data;

    public RespBody(int errorCode, String errorMsg, Object data) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
        this.data = data;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public Object getData() {
        return data;
    }

    @Override
    public JSON toJSON() {
        JSONObject result = JSONUtils.toJSONObject(
                new String[] { "error_code", "error_msg" },
                new Object[] { getErrorCode(), getErrorMsg() });
        if (data != null) {
            result.put("data", data);
        }
        return result;
    }

    @Override
    public String toString() {
        return toJSONString();
    }

    // --

    /**
     * @return
     */
    public static RespBody error() {
        return error(null, Controller.CODE_ERROR);
    }

    /**
     * @param errorMsg
     * @return
     */
    public static RespBody error(String errorMsg) {
        return error(errorMsg, Controller.CODE_ERROR);
    }

    /**
     * @param errorCode
     * @return
     */
    public static RespBody error(int errorCode) {
        return error(null, errorCode);
    }

    /**
     * @param errorMsg
     * @param errorCode
     * @return
     */
    public static RespBody error(String errorMsg, int errorCode) {
        if (errorMsg == null) {
            if (errorCode == 401) {
                errorMsg = Language.getLang("Error401");
            } else if (errorCode == 403) {
                errorMsg = Language.getLang("Error403");
            } else if (errorCode == 404) {
                errorMsg = Language.getLang("Error404");
            } else {
                errorMsg = Language.getLang("Error500");
            }
        }

        return new RespBody(errorCode, errorMsg, null);
    }

    /**
     * @return
     */
    public static RespBody ok() {
        return ok(null);
    }

    /**
     * @param data
     * @return
     */
    public static RespBody ok(Object data) {
        return new RespBody(Controller.CODE_OK, Language.getLang("Error0"), data);
    }
}
