/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api;

import com.alibaba.fastjson.JSON;

/**
 * API 基类
 *
 * @author devezhao
 * @since 01/10/2019
 */
public abstract class BaseApi extends Controller {

    protected BaseApi() {
        super();
    }

    /**
     * API 名称。默认使用类名（遇大写字符加 -），如 SystemTime <tt>system-time</tt>
     *
     * @return
     */
    protected String getApiName() {
        String apiName = getClass().getSimpleName();
        apiName = apiName.replaceAll("[A-Z]", "-$0").toLowerCase();
        return apiName.substring(1);
    }

    /**
     * API 执行。
     * 返回结果应该使用 #formatSuccess 或 #formatFailure 封装。或者也可以直接抛出 {@link ApiInvokeException} 异常
     *
     * @param context
     * @return Use #formatSuccess or #formatFailure
     * @throws ApiInvokeException
     */
    abstract public JSON execute(ApiContext context) throws ApiInvokeException;
}
