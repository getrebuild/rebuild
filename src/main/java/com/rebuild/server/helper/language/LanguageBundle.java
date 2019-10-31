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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONable;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 语言包
 *
 * @author ZHAO
 * @since 2019/10/31
 */
public class LanguageBundle implements JSONable {

    private final String locale;
    private final JSONObject bundle;

    private final Languages langs;

    /**
     * @param locale
     * @param bundle
     * @param langs
     */
    protected LanguageBundle(String locale, JSONObject bundle, Languages langs) {
        this.locale = locale;
        this.bundle = bundle;
        this.langs = langs;
    }

    /**
     * @return
     */
    public String locale() {
        return locale;
    }

    /**
     * @param keys
     * @return
     */
    public String lang(String... keys) {
        if (keys.length == 1) return getLang(keys[0]);

        StringBuilder langs = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            langs.append(capLetter(getLang(keys[i]), i == 0));
        }
        return langs.toString();
    }

    /**
     * @param key
     * @return
     */
    private String getLang(String key) {
        String t = bundle.getString(key);
        if (t == null) {
            String d = String.format("[%s]", key.toUpperCase());
            if (langs != null) {
                return langs.getDefaultBundle().getLang(key, d);
            } else {
                return d;
            }
        }
        return t;
    }

    /**
     * @param key
     * @param defaultt
     * @return
     */
    private String getLang(String key, String defaultt) {
        String t = bundle.getString(key);
        return StringUtils.defaultIfEmpty(t, defaultt);
    }

    /**
     * @return
     */
    public JSON toJSON() {
        return bundle;
    }

    @Override
    public JSON toJSON(String... special) {
        return bundle;
    }

    @Override
    public String toString() {
        return super.toString() + "#" + locale() + ":" + bundle.size();
    }

    /**
     * @param letter
     * @param first
     * @return
     */
    private String capLetter(String letter, boolean first) {
        if (!locale.startsWith("en")) return letter;
        char[] cs = letter.toCharArray();
        if (first) {
            if (CharUtils.isAsciiAlphaLower(cs[0])) cs[0] -= 32;
            return String.valueOf(cs);
        } else {
            if (CharUtils.isAsciiAlphaUpper(cs[0])) cs[0] += 32;
            return " " + String.valueOf(cs);
        }
    }
}
