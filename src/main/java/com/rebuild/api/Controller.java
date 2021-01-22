/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import com.alibaba.fastjson.JSON;

/**
 * 请求控制器
 *
 * @author devezhao
 * @since 01/10/2019
 * @see RespBody
 */
public abstract class Controller {

    /**
     * 成功
     */
    public static final int CODE_OK = 0;

    /**
     * 业务失败
     */
    public static final int CODE_ERROR = 400;

    /**
     * 服务错误
     */
    public static final int CODE_SERV_ERROR = 500;

    /**
     * @param data
     * @return
     */
    protected JSON formatSuccess(Object data) {
        return RespBody.ok(data).toJSON();
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
        return RespBody.error(errorMsg, errorCode).toJSON();
    }
}
