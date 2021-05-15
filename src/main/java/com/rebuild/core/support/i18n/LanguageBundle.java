/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.i18n;

import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.support.License;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.JSONable;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语言包
 * 为安全考虑语言文件不支持 HTML（会被转义），但支持部分 MD 语法：
 * - [] 换行 <br>
 * - [TEXT](URL) 链接
 * - **TEXT** 加粗
 *
 * @author ZHAO
 * @since 2019/10/31
 */
@Slf4j
public class LanguageBundle implements JSONable {
    private static final long serialVersionUID = 1985809451734089603L;

    // 链接
    private static final Pattern LINK_PATT = Pattern.compile("\\[(.*?)]\\((.*?)\\)");
    // 换行
    private static final Pattern BR_PATT = Pattern.compile("\\[]");
    // 加粗
    private static final Pattern BOLD_PATT = Pattern.compile("\\*\\*(.*?)\\*\\*");
    // 代码
    private static final Pattern CODE_PATT = Pattern.compile("`(.*?)`");

    protected static final String PREFIX_ENTITY = "e.";
    protected static final String PREFIX_FIELD = "f.";
    protected static final String PREFIX_DISPLAY_TYPE = "t.";
    protected static final String PREFIX_STATE = "s.";

    private String locale;
    private JSONObject bundle;
    private String bundleHash;

    /**
     * @param locale
     * @param bundle
     */
    protected LanguageBundle(String locale, JSONObject bundle) {
        this.locale = locale;
        this.bundle = this.merge(bundle);
    }

    /**
     * 合并语言
     *
     * @param bundle
     * @return
     */
    private JSONObject merge(JSONObject bundle) {
        if (Application.isReady()) {
            appendMetadata(bundle);

            if (License.isCommercial()) {
                appendDatabase(bundle);
            }
        }

        String bundleString = bundle.toJSONString();

        bundleString = BR_PATT.matcher(bundleString).replaceAll("<br/>");

        Matcher matcher = LINK_PATT.matcher(bundleString);
        while (matcher.find()) {
            String text = matcher.group(1);
            String url = matcher.group(2);

            String link = "<a href='%s'>%s</a>";
            if (url.startsWith("http:") || url.startsWith("https:")) {
                link = "<a target='_blank' href='%s'>%s</a>";
            } else if (url.startsWith("/")) {
                link = "<a href='" + AppUtils.getContextPath() + "%s'>%s</a>";
            }

            bundleString = bundleString.replace(
                    String.format("[%s](%s)", text, url),
                    String.format(link, url, text));
        }

        matcher = BOLD_PATT.matcher(bundleString);
        while (matcher.find()) {
            String text = matcher.group(1);
            String bold = "<b>%s</b>";
            bundleString = bundleString.replace(String.format("**%s**", text), String.format(bold, text));
        }

        matcher = CODE_PATT.matcher(bundleString);
        while (matcher.find()) {
            String text = matcher.group(1);
            String code = "<code>%s</code>";
            bundleString = bundleString.replace(String.format("`%s`", text), String.format(code, text));
        }

        this.bundleHash = EncryptUtils.toMD5Hex(bundleString);
        return JSON.parseObject(bundleString);
    }

    /**
     * 元数据
     * @param bundle
     */
    protected void appendMetadata(JSONObject bundle) {
        for (Entity entity : MetadataHelper.getEntities()) {
            bundle.put(PREFIX_ENTITY + entity.getName(),
                    entity.getDescription().split(" \\(")[0]);

            for (Field field : entity.getFields()) {
                if (!MetadataHelper.isCommonsField(field)) {
                    bundle.put(PREFIX_FIELD + entity.getName() + "." + field.getName(),
                            field.getDescription().split(" \\(")[0]);
                }
            }
        }
    }

    /**
     * 数据库
     * @param bundle
     */
    private void appendDatabase(JSONObject bundle) {
        Object[][] langs = Application.createQueryNoFilter(
                "select name,value from Language where locale = ?")
                .setParameter(1, getLocale())
                .array();
        for (Object[] nv : langs) {
            bundle.put((String) nv[0], nv[1]);
        }
    }

    /**
     * @return
     * @see Locale#forLanguageTag(String)
     */
    public String getLocale() {
        return locale;
    }

    /**
     * @return
     */
    public String getBundleHash() {
        return bundleHash;
    }

    /**
     * @param key
     * @param placeholders
     * @return
     */
    public String $L(String key, Object... placeholders) {
        String lang = bundle.getString(key);
        if (lang == null) {
            log.warn("Missing lang [{}] for [{}]", key, getLocale());
            lang = key;
        }
        return String.format(lang, placeholders);
    }

    @Override
    public JSON toJSON() {
        return bundle;
    }

    @Override
    public String toString() {
        return super.toString() + "#" + getLocale() + ":" + bundle.size();
    }

    // --

    static final LanguageBundle DEFAULT_BUNDLE = new LanguageBundle();

    private LanguageBundle() {
        this.bundle = JSONUtils.EMPTY_OBJECT;
        this.bundleHash = "0";
    }
}
