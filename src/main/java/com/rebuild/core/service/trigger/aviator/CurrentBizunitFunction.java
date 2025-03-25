/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.persist4j.engine.ID;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorNil;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.privileges.bizz.User;

import java.util.Map;

/**
 * Usage: CURRENTBIZUNIT()
 * Return: ID
 *
 * @author devezhao
 * @since 2022/2/25
 */
public class CurrentBizunitFunction extends AbstractFunction {
    private static final long serialVersionUID = -6731627245536290306L;

    @Override
    public AviatorObject call(Map<String, Object> env) {
        ID user = UserContextHolder.getUser();
        User ub = Application.getUserStore().getUser(user);
        if (ub.getOwningDept() == null) return AviatorNil.NIL;
        return AviatorUtils.wrapReturn(ub.getOwningDept().getIdentity());
    }

    @Override
    public String getName() {
        return "CURRENTBIZUNIT";
    }
}
