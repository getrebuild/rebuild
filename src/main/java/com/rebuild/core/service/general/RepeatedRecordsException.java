/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.persist4j.Record;
import com.rebuild.core.DefinedException;

import java.util.List;

/**
 * @author Zixin
 * @since 2021/01/12
 */
public class RepeatedRecordsException extends DefinedException {
    private static final long serialVersionUID = 8769785498603769556L;

    // 重复记录错误码
    public static final int ERROR_CODE = 499;

    private final List<Record> repeatedRecords;

    public RepeatedRecordsException(List<Record> repeated) {
        super("There are " + repeated.size() + " repeated records");
        this.repeatedRecords = repeated;
    }

    @Override
    public int getErrorCode() {
        return ERROR_CODE;
    }

    public List<Record> getRepeatedRecords() {
        return repeatedRecords;
    }
}
