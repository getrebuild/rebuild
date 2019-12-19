/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.helper.language;

import cn.devezhao.commons.EncryptUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONable;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语言包
 *
 * @author ZHAO
 * @since 2019/10/31
 */
public class LanguageBundle implements JSONable {

    final private String locale;
    final private JSONObject bundle;
    private String bundleHash;

    final private Languages parent;

    /**
     * @param locale
     * @param bundle
     * @param parent
     */
    protected LanguageBundle(String locale, JSONObject bundle, Languages parent) {
        this.locale = locale;
        this.bundle = this.merge(bundle);
        this.parent = parent;
    }

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{([0-9a-zA-Z]+)\\}");
    /**
     * 合并语言中的变量
     *
     * @param bundle
     * @return
     */
    private JSONObject merge(JSONObject bundle) {
        String bundleString = bundle.toJSONString();
        Matcher matcher = VAR_PATTERN.matcher(bundleString);
        while (matcher.find()) {
            String var = matcher.group(1);
            String lang = bundle.getString(var);
            if (lang != null) {
                bundleString = bundleString.replace("{" + var +"}", lang);
            }
        }

        this.bundleHash = EncryptUtils.toMD5Hex(bundleString);
        return JSON.parseObject(bundleString);
    }

    /**
     * @return
     * @see Locale#forLanguageTag(String)
     */
    public String locale() {
        return locale;
    }

    /**
     * @param key
     * @return
     */
    public String lang(String key) {
        return lang(key, ArrayUtils.EMPTY_STRING_ARRAY);
    }

    /**
     * @param key
     * @param indexKeys 替换语言中的占位符 {0}
     * @return
     */
    public String lang(String key, String... indexKeys) {
        String lang = getLang(key);
        if (indexKeys == null || indexKeys.length == 0) {
            return lang;
        }

        int index = 0;
        for (String ik : indexKeys) {
            String iLang = getLang(ik);
            if (iLang != null) {
                lang = lang.replace("{" + index++ + "}", iLang);
            }
        }
        return lang;
    }

    /**
     * @param key
     * @param args
     * @return
     * @see String#format(String, Object...)
     */
    public String formatLang(String key, Object... args) {
        String lang = lang(key);
        return String.format(lang, args);
    }

    /**
     * @param key
     * @return
     */
    private String getLang(String key) {
        String lang = bundle.getString(key);
        if (lang == null) {
            String d = String.format("[%s]", key.toUpperCase());
            if (parent != null) {
                return parent.getDefaultBundle().getLang(key, d);
            } else {
                return d;
            }
        }
        return lang;
    }

    /**
     * @param key
     * @param defaultLang
     * @return
     */
    private String getLang(String key, String defaultLang) {
        return StringUtils.defaultIfEmpty(bundle.getString(key), defaultLang);
    }

    /**
     * Hash for bundle
     *
     * @return
     */
    public String getBundleHash() {
        return bundleHash;
    }

    @Override
    public JSON toJSON() {
        return bundle;
    }

    @Override
    public JSON toJSON(String... special) {
        return toJSON();
    }

    @Override
    public String toString() {
        return super.toString() + "#" + locale() + ":" + bundle.size();
    }
}
