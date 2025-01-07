/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author devezhao
 * @since 2020/6/5
 */
@Slf4j
public class ContentWithFieldVars {

    /**
     * 通过 `{}` 包裹的变量或字段
     */
    public static final Pattern PATT_VAR = Pattern.compile("\\{([0-9a-zA-Z._$]{4,})}");

    /**
     * 替换文本中的字段变量
     *
     * @param content
     * @param recordId
     * @return
     */
    public static String replaceWithRecord(String content, ID recordId) {
        if (StringUtils.isBlank(content) || recordId == null) {
            return content;
        }

        Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
        String pkName = entity.getPrimaryField().getName();
        // 主键占位符
        content = content.replace("{ID}", String.format("{%s}", pkName));

        Set<String> fieldVars = new HashSet<>();
        for (String field : matchsVars(content)) {
            if (MetadataHelper.getLastJoinField(entity, field) != null) {
                fieldVars.add(field);
            }
        }
        if (fieldVars.isEmpty()) return content;

        fieldVars.add(pkName);
        Record o = Application.getQueryFactory().recordNoFilter(recordId, fieldVars.toArray(new String[0]));

        return replaceWithRecord(content, o);
    }

    /**
     * 替换文本中的字段变量
     *
     * @param content
     * @param record
     * @return
     */
    public static String replaceWithRecord(String content, Record record) {
        if (StringUtils.isBlank(content) || record == null) return content;

        // 主键占位符
        content = content.replace("{ID}",
                String.format("{%s}", record.getEntity().getPrimaryField().getName()));

        final Entity entity = record.getEntity();

        Map<String, String> fieldVars = new HashMap<>();
        for (String field : matchsVars(content)) {
            if (MetadataHelper.getLastJoinField(entity, field) != null) {
                fieldVars.put(field, null);
            }
        }
        if (fieldVars.isEmpty()) return content;

        for (String field : fieldVars.keySet()) {
            Object value = record.getObjectValue(field);

            value = FieldValueHelper.wrapFieldValue(value,
                    MetadataHelper.getLastJoinField(entity, field), true);
            if (value != null) {
                fieldVars.put(field, value.toString());
            }
        }

        for (Map.Entry<String, String> e : fieldVars.entrySet()) {
            final String field = e.getKey();
            String value = e.getValue();

            if (value != null) {
                DisplayType dt = EasyMetaFactory.valueOf(MetadataHelper.getLastJoinField(entity, field)).getDisplayType();
                if (dt == DisplayType.IMAGE || dt == DisplayType.FILE) {
                    // 处理图片 UnsafeImgAccess
                    if (dt == DisplayType.IMAGE && RebuildConfiguration.getBool(ConfigurationItem.UnsafeImgAccess)) {
                        StringBuilder value4Image = new StringBuilder();
                        for (Object img : JSON.parseArray(value)) {
                            String path = img.toString();
                            if (!CommonsUtils.isExternalUrl(path)) {
                                path = RebuildConfiguration.getHomeUrl("/filex/img/" + path);
                                path += "?_UNSAFEIMGACCESS=" + System.currentTimeMillis();
                            }
                            value4Image.append(String.format("![](%s)\n", path));
                        }
                        value = value4Image.toString();

                    } else {
                        StringBuilder value4Files = new StringBuilder();
                        for (Object file : JSON.parseArray(value)) {
                            String fileName = QiniuCloud.parseFileName(file.toString());
                            value4Files.append(String.format("[%s]%s; ", Language.L(dt.getDisplayName()), fileName));
                        }
                        value = value4Files.toString().trim();
                    }
                }
            }

            content = content.replace("{" + field + "}", StringUtils.defaultIfBlank(value, StringUtils.EMPTY));
        }
        return content;
    }

    /**
     * 提取内容中的变量 {xxxx}
     *
     * @param content
     * @return
     */
    public static Set<String> matchsVars(String content) {
        if (StringUtils.isBlank(content)) return Collections.emptySet();

        Set<String> vars = new HashSet<>();
        Matcher m = PATT_VAR.matcher(content);
        while (m.find()) {
            String varName = m.group(1);
            vars.add(varName);
        }
        return vars;
    }
}
