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

        String dbMsg = convert2DbErrorMsg(ex);
        if (dbMsg != null) log.error("DBERR: {}", ex.getCause() == null ? ex : ex.getCause().getLocalizedMessage());
        return dbMsg;
    }

    static String convert2DbErrorMsg(Throwable ex) {
        Throwable cause = ex.getCause();
        String exMsg = cause == null ? "" : cause.getLocalizedMessage();

        if (cause instanceof DataTruncation) {

            // Data truncation: Data too long for column 'NAME' at row 1
            // Data truncation: Incorrect datetime value: '0010-04-01' for column 'D_A_T_E1' at row 1
            String s = Language.L("数据库字段长度超出限制");
            String key = matchsColumn(exMsg, PATT_FC);
            return key == null ? s : s + ":" + key;

        } else if (cause instanceof SQLException && StringUtils.countMatches(exMsg, "\\x") >= 4) {  // mb4

            return Language.L("数据库编码不支持 4 字节编码");

        }  else if (cause instanceof SQLException && exMsg.contains(" doesn't have a default value")) {

            // Field 'HEJISHULIANG' doesn't have a default value
            String s = Language.L("数据库字段不允许为空");
            String key = matchsColumn(exMsg, PATT_NN);
            return key == null ? s : s + ":" + key;

        } else if (cause instanceof SQLTimeoutException) {

            return Language.L("数据库语句执行超时");

        } else if (ex instanceof ConstraintViolationException) {

            if (ex.getLocalizedMessage().contains("Duplicate entry")) {
                String s = Language.L("数据库字段违反唯一性约束");
                String key = matchsColumn(exMsg, PATT_DE);
                return key == null ? s : s + ":" + key;
            }

            return Language.L("数据库字段违反约束");
        }

        return null;
    }

    // 提取重复 KEY
    static final Pattern PATT_DE = Pattern.compile("Duplicate entry (.*?) for key", Pattern.CASE_INSENSITIVE);
    // 提取超长字段
    static final Pattern PATT_FC = Pattern.compile(" for column (.*?) at row", Pattern.CASE_INSENSITIVE);
    // 提取不允许为空字段
    static final Pattern PATT_NN = Pattern.compile("Field (.*?) doesn't have a default value", Pattern.CASE_INSENSITIVE);
    // 匹配
    static String matchsColumn(String s, Pattern pattern) {
        Matcher m = pattern.matcher(s);
        if (m.find()) return m.group(1);
        return null;
    }
}
