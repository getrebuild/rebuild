/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author devezhao
 * @since 2025/3/8
 */
@ConditionalOnMissingClass("com.rebuild.Rbv")
@Component
public class RbvFunction {

    public static RbvFunction call() {
        return Application.getBean(RbvFunction.class);
    }

    // --

    public long getExpiredTime(Date createdOn, JSONObject eaConf, ID recordId) {
        return 0;
    }

    public void setWeakMode(ID id) {
    }

    public ID getWeakMode(boolean once) {
        return null;
    }
}
