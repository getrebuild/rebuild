/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges.bizz;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;

/**
 * @author devezhao
 * @see BizzPermission
 * @since 10/12/2018
 */
public class InternalPermission {

    /**
     * 扩展权限
     */
    public static final Permission ZERO = new BizzPermission("ZERO", 0, true);

    /**
     * 删除前触发的动作
     */
    public static final Permission DELETE_BEFORE = new BizzPermission("DELETE_BEFORE", 0, false);

    /**
     * 审批
     */
    public static final Permission APPROVAL = new BizzPermission("APPROVAL", 0, false);

}
