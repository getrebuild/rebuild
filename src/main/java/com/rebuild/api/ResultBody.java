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
 * 请求返回消息
 *
 * @author ZHAO
 * @since 2020/8/28
 */
public class ResultBody implements JSONable {

    final private int errorCode;
    final private String errorMsg;
    final private Object data;

    public ResultBody(int errorCode, String errorMsg, Object data) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
        this.data = data;
    }

    @Override
    public JSON toJSON() {
        JSONObject result = JSONUtils.toJSONObject(
                new String[]{"error_code", "error_msg"}, new Object[]{errorCode, errorMsg});
        if (data != null) {
            result.put("data", data);
        }
        return result;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    // --

    /**
     * @param errorMsg
     * @return
     */
    public static ResultBody error(String errorMsg) {
        return error(errorMsg, Controller.CODE_ERROR);
    }

    /**
     * @param errorCode
     * @return
     */
    public static ResultBody error(int errorCode) {
        String errorMsg = null;
        if (errorCode == 401) {
            errorMsg = Language.getLang("Error401");
        } else if (errorCode == 403) {
            errorMsg = Language.getLang("Error403");
        } else if (errorCode == 404) {
            errorMsg = Language.getLang("Error404");
        }

        return error(errorMsg, errorCode);
    }

    /**
     * @param errorMsg
     * @param errorCode
     * @return
     */
    public static ResultBody error(String errorMsg, int errorCode) {
        if (errorMsg == null) errorMsg = Language.getLang("Error500");
        return new ResultBody(errorCode, errorMsg, null);
    }

    /**
     * @return
     */
    public static ResultBody ok() {
        return ok(null);
    }

    /**
     * @param data
     * @return
     */
    public static ResultBody ok(Object data) {
        return new ResultBody(Controller.CODE_OK, Language.getLang("Error0"), data);
    }
}
