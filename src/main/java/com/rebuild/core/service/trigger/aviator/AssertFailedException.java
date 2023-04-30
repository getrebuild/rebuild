/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import com.googlecode.aviator.runtime.function.system.AssertFunction;
import com.rebuild.core.service.trigger.DataValidateException;

/**
 * @author devezhao
 * @since 2023/4/16
 * @see com.googlecode.aviator.runtime.function.system.AssertFunction.AssertFailed
 */
public class AssertFailedException extends DataValidateException {
    private static final long serialVersionUID = -4785784500930570769L;

    public AssertFailedException(AssertFunction.AssertFailed cause) {
        super(cause.getLocalizedMessage(), Boolean.FALSE);
    }
}
