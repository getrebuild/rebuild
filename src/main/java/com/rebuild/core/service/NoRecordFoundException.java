/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.RebuildException;
import com.rebuild.core.support.i18n.Language;

/**
 * 无效记录
 *
 * @author devezhao
 * @since 11/23/2018
 */
public class NoRecordFoundException extends RebuildException {
    private static final long serialVersionUID = -427919151949591616L;

    public NoRecordFoundException() {
        super();
    }

    public NoRecordFoundException(ID recordId) {
        this(recordId, Boolean.FALSE);
    }

    public NoRecordFoundException(ID recordId, boolean i18n) {
        this(i18n
                ? Language.L("无权读取此记录或记录已被删除")
                : ("No Record found : " + recordId.toLiteral()));
    }

    public NoRecordFoundException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public NoRecordFoundException(String msg) {
        super(msg);
    }

    public NoRecordFoundException(Throwable cause) {
        super(cause);
    }
}
