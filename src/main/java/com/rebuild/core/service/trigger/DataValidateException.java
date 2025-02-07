/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.service.DataSpecificationException;
import lombok.Getter;

/**
 * 数据校验专用
 *
 * @author devezhao
 * @since 2021/6/30
 * @see com.rebuild.rbv.trigger.DataValidate
 */
@Getter
public class DataValidateException extends DataSpecificationException {
    private static final long serialVersionUID = 4178910284594338317L;

    final private boolean weakMode;
    final private ID weakModeTriggerId;

    public DataValidateException(String msg) {
        this(msg, false, null);
    }

    public DataValidateException(String msg, boolean weakMode) {
        this(msg, weakMode, null);
    }

    public DataValidateException(String msg, boolean weakMode, ID triggerId) {
        super(msg);
        this.weakMode = weakMode;
        this.weakModeTriggerId = triggerId;
    }
}
