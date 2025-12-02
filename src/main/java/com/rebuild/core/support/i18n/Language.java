/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.i18n;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.metadata.BaseMeta;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.Initialization;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.state.StateSpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 多语言
 *
 * @author ZHAO
 * @since 2019/10/31
 */
@Slf4j
@Component
public class Language implements Initialization {

    private Map<String, LanguageBundle> bundleMap = new HashMap<>();

    @Override
    public void init() throws IOException {
        bundleMap.put(LanguageBundle.SYS_LC, LanguageBundle.SYS_BUNDLE);

        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(
                "classpath:i18n/lang.*.json");
        // v4.2.5 从数据目录
        File i18n = RebuildConfiguration.getFileOfData("_i18n");
        if (i18n.exists() && i18n.isDirectory()) {
            File[] files = i18n.listFiles(pathname -> {
                String s = pathname.getName();
                return s.startsWith("lang.") && s.endsWith(".json");
            });
            if (files != null) {
                for (File f : files) {
                    resources = ArrayUtils.addAll(resources, new FileSystemResource(f));
                }
            }
        }

        for (Resource res : resources) {
            log.info("Loading language bundle : {}", res);
            String locale = Objects.requireNonNull(res.getFilename()).split("\\.")[1];

            try {
                JSONObject o = JSON.parseObject(res.getInputStream(), null);
                LanguageBundle bundle = new LanguageBundle(locale, o);
                bundleMap.put(locale, bundle);

            } catch (IOException ex) {
                log.error("Cannot load language bundle : {}", res, ex);
            }
        }
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    /**
     * 刷新语言包
     */
    public void refresh() {
        if (bundleMap.isEmpty()) return;

        try {
            this.init();
        } catch (Exception e) {
            log.error("Refresh language-bundle error", e);
        }
    }

    /**
     * @param locale
     * @return
     * @see java.util.Locale
     */
    public LanguageBundle getBundle(String locale) {
        if (Application.isWaitLoad()) return LanguageBundle.SYS_BUNDLE;

        if (locale != null) {
            if (bundleMap.containsKey(locale)) {
                return bundleMap.get(locale);
            }

            locale = useLanguageCode(locale.split("[-_]")[0]);
            if (locale != null) {
                return bundleMap.get(locale);
            }
        }

        return getDefaultBundle();
    }

    /**
     * 默认语言包
     *
     * @return
     */
    public LanguageBundle getDefaultBundle() {
        String d = RebuildConfiguration.get(ConfigurationItem.DefaultLanguage);
        if (available(d) == null) {
            return LanguageBundle.SYS_BUNDLE;
        } else {
            return bundleMap.get(d);
        }
    }

    /**
     * @param locale
     * @return
     */
    private String useLanguageCode(String locale) {
        for (String key : bundleMap.keySet()) {
            if (key.equals(locale) || key.startsWith(locale)) {
                return key;
            }
        }
        return null;
    }

    /**
     * 是否为可用语言
     *
     * @param locale
     * @return
     */
    public String available(String locale) {
        if (StringUtils.isBlank(locale)) {
            locale = RebuildConfiguration.get(ConfigurationItem.DefaultLanguage);
        }

        String[] lc = locale.split("[-_]");
        locale = lc[0].toLowerCase();
        if (lc.length > 1) locale += "_" + lc[1].toUpperCase();

        boolean a = bundleMap.containsKey(locale);
        if (a) return locale;

        if ((locale = useLanguageCode(lc[0])) != null) {
            return locale;
        }
        return null;
    }

    /**
     * @return
     */
    public Map<String, String> availableLocales() {
        Map<String, String> map = new TreeMap<>();
        for (Map.Entry<String, LanguageBundle> item : bundleMap.entrySet()) {
            map.put(item.getKey(), item.getValue().L("_"));
        }
        return map;
    }

    // -- Quick Methods

    /**
     * 获取系统默认语言
     *
     * @return
     * @see Language#getDefaultBundle()
     */
    public static LanguageBundle getSysDefaultBundle() {
        return Application.getLanguage().getDefaultBundle();
    }

    /**
     * 获取当前用户语言
     *
     * @return
     * @see UserContextHolder#getLocale()
     */
    public static LanguageBundle getCurrentBundle() {
        return Application.getLanguage().getBundle(UserContextHolder.getLocale());
    }

    public static String L(String key, Object... placeholders) {
        return getCurrentBundle().L(key, placeholders);
    }

    public static String L(BaseMeta meta) {
        // v4.2
        String key = "META.";
        if (meta instanceof Field) key += ((Field) meta).getOwnEntity().getName() + "." + meta.getName();
        else key += meta.getName();
        String L42 = getCurrentBundle().getLang(key.toUpperCase());

        if (L42 == null) L42 = getCurrentBundle().getLang(meta.getDescription());
        return L42 == null ? meta.getDescription() : L42;
    }

    public static String L(Entity entity, String fieldsPath) {
        List<String> join = new ArrayList<>();
        join.add(Language.L(entity));

        Field lastField;
        Entity father = entity;
        for (String field : fieldsPath.split("\\.")) {
            if (father != null && father.containsField(field)) {
                lastField = father.getField(field);
                join.add(Language.L(lastField));
                if (lastField.getType() == FieldType.REFERENCE || lastField.getType() == FieldType.REFERENCE_LIST) {
                    father = lastField.getReferenceEntity();
                }  else {
                    father = null;
                }
            } else {
                return "[" + fieldsPath.toUpperCase() + "]";
            }
        }
        return StringUtils.join(join, ".");
    }

    public static String L(DisplayType type) {
        String lang = getCurrentBundle().getLang(type.getDisplayName());
        return lang == null ? type.getDisplayName() : lang;
    }

    public static String L(ActionType type) {
        String lang = getCurrentBundle().getLang(type.getDisplayName());
        return lang == null ? type.getDisplayName() : lang;
    }

    public static String L(StateSpec state) {
        String lang = getCurrentBundle().getLang(state.getName());
        return lang == null ? state.getName() : lang;
    }
}
