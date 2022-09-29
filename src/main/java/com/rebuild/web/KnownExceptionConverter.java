/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.persist4j.exception.jdbc.ConstraintViolationException;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.support.i18n.Language;
import org.apache.commons.lang3.StringUtils;

import java.sql.DataTruncation;
import java.sql.SQLException;

/**
 * @author devezhao
 * @since 2021/9/6
 */
public class KnownExceptionConverter {

    /**
     * 转换已知异常
     *
     * @param ex
     * @return
     */
    public static String convert2ErrorMsg(Exception ex) {
        if (ex == null) return null;

        if (ex instanceof DataSpecificationException) {
            return ex.getLocalizedMessage();
        }

        final Throwable cause = ex.getCause();
        final String exMsg = cause == null ? null : cause.getLocalizedMessage();

        if (cause instanceof DataTruncation) {
            return Language.L("字段长度超出限制");
        } else if (cause instanceof SQLException && StringUtils.countMatches(exMsg, "\\x") >= 4) {  // mb4
            return Language.L("数据库编码不支持 4 字节编码");
        } else if (ex instanceof ConstraintViolationException) {
            return Language.L("字段违反唯一性约束");
        }

        return null;
    }
}
