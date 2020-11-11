/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.JSONable;

/**
 * 统一请求返回消息体
 *
 * @author ZHAO
 * @since 2020/8/28
 * @see com.rebuild.web.ControllerResponseBodyAdvice
 */
public class RespBody implements JSONable {

    private int errorCode;
    private String errorMsg;
    private Object data;

    public RespBody(int errorCode, String errorMsg, Object data) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
        this.data = data;
    }

    /**
     * @return
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * @return
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * @return
     */
    public Object getData() {
        return data;
    }

    @Override
    public JSON toJSON() {
        JSONObject result = JSONUtils.toJSONObject(
                new String[] { "error_code", "error_msg" },
                new Object[] { getErrorCode(), getErrorMsg() });
        if (getData() != null) {
            result.put("data", getData());
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
     * @param errorMsgLang
     * @param phKeys
     * @return
     * @see Language#L(String, String...)
     */
    public static RespBody errorl(String errorMsgLang, String ... phKeys) {
        String lang = Language.L(errorMsgLang, phKeys);
        return error(lang, Controller.CODE_ERROR);
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
                errorMsg = Language.L("Error401");
            } else if (errorCode == 403) {
                errorMsg = Language.L("Error403");
            } else if (errorCode == 404) {
                errorMsg = Language.L("Error404");
            } else {
                errorMsg = Language.L("Error500");
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
        return new RespBody(Controller.CODE_OK, Language.L("Error0"), data);
    }
}
