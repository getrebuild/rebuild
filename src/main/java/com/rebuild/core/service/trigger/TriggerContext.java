/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import java.util.Map;

/**
 * @author devezhao
 * @since 2026/06/14
 */
public class TriggerContext {

    // 用于触发器执行时前端传值
    private static final ThreadLocal<Map<String, Object>> LITEFORM_DATA = new ThreadLocal<>();

    /**
     * @param map
     */
    public static void setLiteFormData(Map<String, Object> map) {
        LITEFORM_DATA.set(map);
    }

    /**
     * @param once
     * @return
     */
    public static Map<String, Object> getLiteFormData(boolean once) {
        Map<String, Object> map = LITEFORM_DATA.get();
        if (map != null && once) LITEFORM_DATA.remove();
        return map;
    }
}
