/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import com.rebuild.core.service.DataSpecificationException;

/**
 * 数据效验专用
 *
 * @author devezhao
 * @since 2021/6/30
 * @see com.rebuild.rbv.trigger.DataValidate
 */
public class DataValidateException extends DataSpecificationException {

    public DataValidateException(String msg) {
        super(msg);
    }
}
