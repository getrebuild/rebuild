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
import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 多语言
 *
 * @author ZHAO
 * @since 2019/10/31
 */
public class Languages {

    private static final Log LOG = LogFactory.getLog(Languages.class);

    public static final Languages instance = new Languages();
    private Languages() {
        this.reset();
    }

    /**
     * 默认语言
     */
    public static final String DEFAULT_LOCALE = "zh-CN";
    /**
     * 语言文件前缀
     */
    private static final String LB_PREFIX = "language_";

    private Map<String, LanguageBundle> bundleMap = new HashMap<>();

    /**
     */
    public void reset() {
        try {
            File[] files = ResourceUtils.getFile("classpath:locales/")
                    .listFiles((dir, name) -> name.startsWith(LB_PREFIX) && name.endsWith(".json"));
            for (File file : Objects.requireNonNull(files)) {
                String locale = file.getName().substring(LB_PREFIX.length());
                locale = locale.split("\\.")[0];

                try (InputStream is = new FileInputStream(file)) {
                    LOG.info("Loading language bundle : " + locale);
                    JSONObject o = JSON.parseObject(is, null);
                    bundleMap.remove(locale);
                    bundleMap.put(locale, new LanguageBundle(locale, o, this));
                }
            }
        } catch (Exception ex) {
            throw new RebuildException("Load language bundle failure!", ex);
        }
    }

    /**
     * @param locale
     * @return
     */
    public LanguageBundle getBundle(Locale locale) {
        return getBundle(locale == null ? null : locale.toString());
    }

    /**
     * @param locale
     * @return
     */
    public LanguageBundle getBundle(String locale) {
        if (locale != null) {
            locale = locale.replace("_", "-");
        }
        if (locale != null && bundleMap.containsKey(locale)) {
            return bundleMap.get(locale);
        } else {
            return getDefaultBundle();
        }
    }

    /**
     * 默认语言包
     *
     * @return
     */
    public LanguageBundle getDefaultBundle() {
        return bundleMap.get(DEFAULT_LOCALE);
    }

    /**
     * 当前用户语言包
     *
     * @return
     */
    public LanguageBundle getCurrentBundle() {
        return getBundle(Application.getSessionStore().getLocale());
    }

    /**
     * 是否为可用语言
     *
     * @param locale
     * @return
     */
    public boolean isAvailable(String locale) {
        return bundleMap.containsKey(locale.replace("_", "-"));
    }

    // -- Quick Methods

    /**
     * @param key
     * @param insideKeys
     * @return
     */
    public static String lang(String key, String...insideKeys) {
        return instance.getCurrentBundle().lang(key, insideKeys);
    }
}
