/*!
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
import lombok.Data;

/**
 * 统一请求返回消息体
 *
 * @author ZHAO
 * @see com.rebuild.web.ControllerRespBodyAdvice
 * @since 2020/8/28
 */
@Data
public class RespBody implements JSONable {
    private static final long serialVersionUID = 7858909284223713830L;

    private int errorCode;
    private String errorMsg;
    private Object data;

    public RespBody(int errorCode, String errorMsg, Object data) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
        this.data = data;
    }

    @Override
    public JSON toJSON() {
        JSONObject res = JSONUtils.toJSONObject(
                new String[]{"error_code", "error_msg"},
                new Object[]{getErrorCode(), getErrorMsg()});
        if (getData() != null) res.put("data", getData());
        return res;
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
                errorMsg = Language.L("未授权访问");
            } else if (errorCode == 403) {
                errorMsg = Language.L("权限不足，访问被阻止");
            } else if (errorCode == 404) {
                errorMsg = Language.L("访问的页面/资源不存在");
            } else {
                errorMsg = Language.L("系统繁忙，请稍后重试");
            }
        }
        return new RespBody(errorCode, errorMsg, null);
    }

    /**
     * @param errorMsg
     * @param placeholders
     * @return
     * @see Language#L(String, Object...)
     */
    public static RespBody errorl(String errorMsg, Object... placeholders) {
        return error(Language.L(errorMsg, placeholders), Controller.CODE_ERROR);
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
        return new RespBody(Controller.CODE_OK, Language.L("调用成功"), data);
    }
}
