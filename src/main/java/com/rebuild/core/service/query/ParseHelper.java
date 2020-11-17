/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.query.compiler.QueryCompiler;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.easymeta.DisplayType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * @author devezhao
 * @since 2019/9/29
 */
public class ParseHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ParseHelper.class);

    // -- 可选操作符

    public static final String EQ = "EQ";
    public static final String NEQ = "NEQ";
    public static final String GT = "GT";
    public static final String LT = "LT";
    public static final String GE = "GE";
    public static final String LE = "LE";
    public static final String NL = "NL";
    public static final String NT = "NT";
    public static final String LK = "LK";
    public static final String NLK = "NLK";
    public static final String IN = "IN";
    public static final String NIN = "NIN";
    public static final String BW = "BW";
    public static final String BFD = "BFD";
    public static final String BFM = "BFM";
    public static final String BFY = "BFY";
    public static final String AFD = "AFD";
    public static final String AFM = "AFM";
    public static final String AFY = "AFY";
    public static final String RED = "RED";
    public static final String REM = "REM";
    public static final String REY = "REY";
    public static final String SFU = "SFU";
    public static final String SFB = "SFB";
    public static final String SFD = "SFD";
    public static final String YTA = "YTA";
    public static final String TDA = "TDA";
    public static final String TTA = "TTA";
    // 位运算
    public static final String BAND = "BAND";
    public static final String NBAND = "NBAND";
    /**
     * 全文索引。MySQL5.7 或以上支持中文
     * my.ini 配置分词大小 ngram_token_size=2
     * 创建索引时使用 `PARSER ngram`
     */
    public static final String FT = "FT";

    public static final String CUW = "CUW";
    public static final String CUM = "CUM";
    public static final String CUQ = "CUQ";
    public static final String CUY = "CUY";

    // 日期时间

    public static final String ZERO_TIME = " 00:00:00";
    public static final String FULL_TIME = " 23:59:59";

    /**
     * 转换操作符
     *
     * @param token
     * @return
     */
    protected static String convetOperator(String token) {
        if (EQ.equalsIgnoreCase(token)) {
            return "=";
        } else if (NEQ.equalsIgnoreCase(token)) {
            return "<>";
        } else if (GT.equalsIgnoreCase(token)) {
            return ">";
        } else if (LT.equalsIgnoreCase(token)) {
            return "<";
        } else if (GE.equalsIgnoreCase(token)) {
            return ">=";
        } else if (LE.equalsIgnoreCase(token)) {
            return "<=";
        } else if (NL.equalsIgnoreCase(token)) {
            return "is null";
        } else if (NT.equalsIgnoreCase(token)) {
            return "is not null";
        } else if (LK.equalsIgnoreCase(token)) {
            return "like";
        } else if (NLK.equalsIgnoreCase(token)) {
            return "not like";
        } else if (IN.equalsIgnoreCase(token)) {
            return "in";
        } else if (NIN.equalsIgnoreCase(token)) {
            return "not in";
        } else if (BW.equalsIgnoreCase(token)) {
            return "between";
        } else if (BFD.equalsIgnoreCase(token)) {
            return "<=";  // "$before_day(%d)";
        } else if (BFM.equalsIgnoreCase(token)) {
            return "<=";  // "$before_month(%d)";
        } else if (BFY.equalsIgnoreCase(token)) {
            return "<=";  // "$before_year(%d)";
        } else if (AFD.equalsIgnoreCase(token)) {
            return ">=";  // "$after_day(%d)";
        } else if (AFM.equalsIgnoreCase(token)) {
            return ">=";  // "$after_month(%d)";
        } else if (AFY.equalsIgnoreCase(token)) {
            return ">=";  // "$after_year(%d)";
        } else if (RED.equalsIgnoreCase(token)) {
            return ">";   // "$recent_day(%d)";
        } else if (REM.equalsIgnoreCase(token)) {
            return ">";   // "$recent_month(%d)";
        } else if (REY.equalsIgnoreCase(token)) {
            return ">";   // "$recent_year(%d)";
        } else if (SFU.equalsIgnoreCase(token)) {
            return "=";
        } else if (SFB.equalsIgnoreCase(token)) {
            return "=";
        } else if (SFD.equalsIgnoreCase(token)) {
            return "in";
        } else if (YTA.equalsIgnoreCase(token)) {
            return "=";
        } else if (TDA.equalsIgnoreCase(token)) {
            return "=";
        } else if (TTA.equalsIgnoreCase(token)) {
            return "=";
        } else if (BAND.equalsIgnoreCase(token)) {
            return "&&";
        } else if (NBAND.equalsIgnoreCase(token)) {
            return "!&";
        } else if (FT.equalsIgnoreCase(token)) {
            return "match";
        } else if (CUW.equalsIgnoreCase(token) || CUM.equalsIgnoreCase(token)
                || CUQ.equalsIgnoreCase(token) || CUY.equalsIgnoreCase(token)) {
            return ">=";
        }

        throw new UnsupportedOperationException("Unsupported token of operator : " + token);
    }

    // --

    /**
     * @param field
     * @return
     */
    public static String useQuickFilter(Field field) {
        return useQuickFilter(EasyMetaFactory.valueOf(field));
    }

    /**
     * @param field
     * @return
     */
    public static String useQuickFilter(EasyField field) {
        DisplayType dt = field.getDisplayType();

        // 引用字段不能作为名称字段（前端限制），此处的处理是因为某些系统实体有用到
        // 主要要保证其兼容 LIKE 条件的语法要求
        if (dt == DisplayType.REFERENCE
                || dt == DisplayType.PICKLIST
                || dt == DisplayType.CLASSIFICATION) {
            return QueryCompiler.NAME_FIELD_PREFIX + field.getName();

        } else if (dt == DisplayType.TEXT
                || dt == DisplayType.EMAIL
                || dt == DisplayType.URL
                || dt == DisplayType.PHONE
                || dt == DisplayType.SERIES) {
            return field.getName();
        } else {
            return null;
        }
    }

    /**
     * @param entity
     * @param quickFields
     * @return
     */
    public static Set<String> buildQuickFields(Entity entity, String quickFields) {
        final Set<String> usesFields = new HashSet<>();

        // 未指定则使用系统配置的
        if (StringUtils.isBlank(quickFields)) {
            quickFields = EasyMetaFactory.valueOf(entity).getExtraAttr("quickFields");
        }

        // 验证
        if (StringUtils.isNotBlank(quickFields)) {
            for (String field : quickFields.split(",")) {
                Field validField = MetadataHelper.getLastJoinField(entity, field);
                if (validField != null) {
                    String can = useQuickFilter(validField);
                    if (can != null) {
                        usesFields.add(field);
                    }

                } else {
                    LOG.warn("No field found by QuickFilter : " + field + " in " + entity.getName());
                }
            }
        }

        if (usesFields.isEmpty()) {
            // 名称字段
            if (entity.getNameField() != null) {
                usesFields.add(entity.getNameField().getName());
            }
            // 自动编号字段
            for (Field field : MetadataSorter.sortFields(entity, DisplayType.SERIES)) {
                usesFields.add(field.getName());
            }
        }

        // QuickCode
        if (entity.containsField(EntityHelper.QuickCode)) {
            usesFields.add(EntityHelper.QuickCode);
        }

        return usesFields;
    }
}
