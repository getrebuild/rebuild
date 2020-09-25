/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 请求响应
 *
 * @author devezhao
 * @see ResultBody
 * @since 01/10/2019
 */
public abstract class Controller {

    // 成功
    public static final int CODE_OK = 0;
    // 错误
    public static final int CODE_ERROR = 400;
    // 未知错误
    public static final int CODE_SERV_ERROR = 500;

    /**
     * Logging
     */
    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    /**
     * @param data
     * @return
     */
    protected JSON formatSuccess(Object data) {
        return ResultBody.ok(data).toJSON();
    }

    /**
     * @param errorMsg
     * @return
     */
    protected JSON formatFailure(String errorMsg) {
        return formatFailure(errorMsg, CODE_ERROR);
    }

    /**
     * @param errorMsg
     * @param errorCode
     * @return
     */
    protected JSON formatFailure(String errorMsg, int errorCode) {
        return ResultBody.error(errorMsg, errorCode).toJSON();
    }
}
