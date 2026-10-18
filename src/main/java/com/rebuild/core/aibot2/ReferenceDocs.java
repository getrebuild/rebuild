/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import com.rebuild.utils.CommonsUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 参考文档（md）注册与获取，供 AI 工具按需注入给模型。
 * 与 JsonSchemaValidator 的配置校验 Schema（json）职责区分
 *
 * @author devezhao
 * @since 2026/10/18
 */
public class ReferenceDocs {

    private static final Map<String, String> RES_MAP = new HashMap<>();

    static {
        RES_MAP.put("frontjs-api", "json-schema/frontjs-api-reference.md");
        RES_MAP.put("minapp-page", "json-schema/minapp-page-reference.md");
    }

    /**
     * 获取参考文档原文，未注册返回 null
     *
     * @param name
     * @return
     */
    public static String getContent(String name) {
        String res = RES_MAP.get(name);
        return res == null ? null : CommonsUtils.getStringOfRes(res);
    }
}
