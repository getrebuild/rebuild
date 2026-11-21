/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import com.rebuild.core.privileges.AdminGuard;

/**
 * @author devezhao
 * @since 2026/10/18
 */
public class BuildAviatorFormula extends RbvTool implements AdminGuard {

    @Override
    public boolean isSystem() {
        return HIDDEN_SYSTEM;
    }
}
