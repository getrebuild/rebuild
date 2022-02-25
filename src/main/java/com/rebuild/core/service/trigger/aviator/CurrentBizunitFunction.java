/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import cn.devezhao.persist4j.engine.ID;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;

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
    public AviatorObject call() throws Exception {
        ID user = UserContextHolder.getUser();
        ID bizunit = (ID) Application.getUserStore().getUser(user).getOwningBizUnit().getIdentity();
        return new AviatorID(bizunit);
    }

    @Override
    public String getName() {
        return "CURRENTBIZUNIT";
    }
}
