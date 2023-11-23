/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.persist4j.exception.jdbc.ConstraintViolationException;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.support.i18n.Language;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.sql.DataTruncation;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author devezhao
 * @since 2021/9/6
 */
@Slf4j
public class KnownExceptionConverter {

    /**
     * 转换已知异常
     *
     * @param ex
     * @return
     */
    public static String convert2ErrorMsg(Throwable ex) {
        if (ex == null) return null;

        if (ex instanceof DataSpecificationException) {
            return ex.getLocalizedMessage();
        }

        final Throwable cause = ex.getCause();
        final String exMsg = cause == null ? null : cause.getLocalizedMessage();

        if (cause instanceof DataTruncation) {
            log.error("DBERR: {}", exMsg);
            // Data truncation: Incorrect datetime value: '0010-07-05 04:57:00' for column
            if (exMsg.contains("Incorrect datetime")) return Language.L("日期超出数据库限制");

            String s = Language.L("数据库字段长度超出限制");
            String key = matchsDataTruncation(exMsg);
            return key == null ? s : s + ":" + key;

        } else if (cause instanceof SQLException && StringUtils.countMatches(exMsg, "\\x") >= 4) {  // mb4
            log.error("DBERR: {}", exMsg);
            return Language.L("数据库编码不支持 4 字节编码");

        } else if (cause instanceof SQLTimeoutException) {
            log.error("DBERR: {}", exMsg);
            return Language.L("数据库语句执行超时");

        } else if (ex instanceof ConstraintViolationException) {
            log.error("DBERR: {}", exMsg);
            if (ex.getLocalizedMessage().contains("Duplicate entry")) {
                String s = Language.L("数据库字段违反唯一性约束");
                String key = matchsDuplicateEntry(exMsg);
                return key == null ? s : s + ":" + key;
            }
            return Language.L("数据库字段违反约束");
        }

        return null;
    }

    static final Pattern PATT_DE = Pattern.compile("Duplicate entry (.*?) for key", Pattern.CASE_INSENSITIVE);
    // 提取重复 KEY
    static String matchsDuplicateEntry(String s) {
        Matcher m = PATT_DE.matcher(s);
        if (m.find()) return m.group(1);
        return null;
    }

    static final Pattern PATT_DT = Pattern.compile("Data too long for column (.*?) at row", Pattern.CASE_INSENSITIVE);
    // 提取超长字段
    static String matchsDataTruncation(String s) {
        Matcher m = PATT_DT.matcher(s);
        if (m.find()) return m.group(1);
        return null;
    }
}
