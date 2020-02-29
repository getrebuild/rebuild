/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
 * 为安全考虑语言文件不支持 HTML（会被转义），但支持部分 MD 语法：
 * - [] 换行 <br>
 * - [TEXT](URL) 链接
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

    private static final Pattern VARS_PATT = Pattern.compile("\\{([0-9a-zA-Z]+)}");
    private static final Pattern LINK_PATT = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)");
    /**
     * 合并语言中的变量，MD 转换
     *
     * @param bundle
     * @return
     */
    private JSONObject merge(JSONObject bundle) {
        // 合并
        String bundleString = bundle.toJSONString();
        Matcher matcher = VARS_PATT.matcher(bundleString);
        while (matcher.find()) {
            String var = matcher.group(1);
            String lang = bundle.getString(var);
            if (lang != null) {
                bundleString = bundleString.replace("{" + var +"}", lang);
            }
        }

        // MD 转换

        // 转义
        bundleString = bundleString.replaceAll("<", "&lt;").replaceAll(">", "&gt;");

        // 换行
        bundleString = bundleString.replaceAll("\\[\\]", "<br/>");

        // 链接
        matcher = LINK_PATT.matcher(bundleString);
        while (matcher.find()) {
            String text = matcher.group(1);
            String url = matcher.group(2);

            bundleString = bundleString.replace(
                    String.format("[%s](%s)", text, url),
                    String.format("<a href='%s'>%s</a>", url, text));
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
