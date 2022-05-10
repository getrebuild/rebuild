/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges.bizz;

import cn.devezhao.bizz.privileges.DepthEntry;
import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.impl.BizzDepthEntry;
import cn.devezhao.bizz.security.EntityPrivileges;

import java.io.Serializable;

/**
 * 简单的权限定义
 *
 * @author devezhao
 * @see EntityPrivileges
 * @since 10/11/2018
 */
public class ZeroPrivileges implements Privileges {
    private static final long serialVersionUID = 7185091441777921842L;

    public static final String ZERO_FLAG = "Z";

    public static final int ZERO_MASK = 4;

    private final String zreoKey;
    private final String definition;

    /**
     * @param zreoKey    {@link ZeroEntry}
     * @param definition
     */
    public ZeroPrivileges(String zreoKey, String definition) {
        this.zreoKey = zreoKey;
        this.definition = definition;
    }

    @Override
    public Serializable getIdentity() {
        return zreoKey;
    }

    @Override
    public boolean allowed(Permission action) {
        return allowed(action, null);
    }

    @Override
    public boolean allowed(Permission action, Serializable targetGuard) {
        return definition.contains(":" + ZERO_MASK);  // {"Z":4}
    }

    @Override
    public DepthEntry superlative(Permission action) {
        return BizzDepthEntry.GLOBAL;
    }

    /**
     * @return
     */
    public String getDefinition() {
        return definition;
    }
}
