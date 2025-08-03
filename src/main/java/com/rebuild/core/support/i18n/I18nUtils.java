/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.i18n;

import cn.devezhao.commons.CalendarUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * @author ZHAO
 * @since 2020/09/17
 */
public class I18nUtils {

    /**
     * 客户端所需的日期时间格式（带时区偏移）
     *
     * @param date
     * @return
     */
    public static String formatDate(Date date) {
        if (date == null) return null;
        return CalendarUtils.getUTCWithZoneDateTimeFormat().format(date);
    }

    /**
     * @param key
     * @param placeholders
     * @return
     * @see Language#L(String, Object...)
     */
    public static String L(String key, Object... placeholders) {
        return Language.L(key, placeholders);
    }

    /**
     * @param origin $Lxxx
     * @return
     */
    public static String LP(String origin) {
        if (StringUtils.length(origin) > 2 && origin.startsWith("$L")) {
            origin = origin.substring(2).trim();
            return Language.L(origin);
        }
        return origin;
    }

    /**
     * 用于替换配置中的多语言
     *
     * @param n
     * @param textKey
     * @return
     */
    public static JSON replaceLP(JSON n, String textKey) {
        if (textKey == null) textKey = "text";

        if (n instanceof JSONObject) {
            JSONObject o = (JSONObject) n;
            String text = o.getString(textKey);
            if (StringUtils.isBlank(text)) return n;

            o.put(textKey, LP(text));
            return o;
        }

        for (Object item : (JSONArray) n) {
            JSONObject o = (JSONObject) item;
            String text = o.getString(textKey);
            if (StringUtils.isBlank(text)) continue;

            o.put(textKey, LP(o.getString(textKey)));
        }
        return n;
    }
}
