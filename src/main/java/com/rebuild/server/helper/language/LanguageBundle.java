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
     * @param key
     * @param keys
     * @return
     */
    public String lang(String key, String...keys) {
        StringBuilder langs = new StringBuilder();
        langs.append(lang((key)));
        for (String k : keys) {
            langs.append(lang((k)));
        }
        return langs.toString();
    }

    /**
     * @param key
     * @return
     */
    public String lang(String key) {
        String t = bundle.getString(key);
        if (t == null) {
            String d = String.format("[%s]", key.toUpperCase());
            if (langs != null) {
                return langs.getDefaultBundle().lang(key, d);
            } else {
                return d;
            }
        }
        return t;
    }

    /**
     * @param key
     * @param defaultLang
     * @return
     */
    protected String lang(String key, String defaultLang) {
        String t = bundle.getString(key);
        return t == null ? defaultLang : t;
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
}
