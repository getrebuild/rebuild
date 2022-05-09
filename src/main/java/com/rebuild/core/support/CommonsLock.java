/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import org.springframework.util.Assert;

/**
 * 通用锁
 *
 * @author RB
 * @since 2022/4/27
 */
public class CommonsLock {

    /**
     * @param source
     * @return
     */
    public static ID getLockedUser(ID source) {
        Assert.notNull(source, "[source] cannot null");

        Object[] o = Application.createQueryNoFilter(
                "select lockUser,lockTime,lockId from CommonsLock where source = ?")
                .setParameter(1, source)
                .unique();
        return o == null || o[0] == null ? null : (ID) o[0];
    }

    /**
     * @param source
     * @return
     */
    public static Object[] getLockedUserFormat(ID source) {
        ID l = getLockedUser(source);
        if (l == null) return null;
        return new Object[] { l, UserHelper.getName(l) };
    }

    /**
     * @param source
     * @param lockUser
     * @return
     */
    public static boolean lock(ID source, ID lockUser) {
        final ID lockedUser = getLockedUser(source);
        if (lockedUser == null) {
            Record r = EntityHelper.forNew(EntityHelper.CommonsLock, UserService.SYSTEM_USER);
            r.setID("source", source);
            r.setID("lockUser", lockUser);
            r.setDate("lockTime", CalendarUtils.now());
            Application.getCommonsService().create(r);
            return true;
        }

        return lockedUser.equals(lockUser);
    }

    /**
     * @param source
     * @param unlockUser
     * @return
     */
    public static boolean unlock(ID source, ID unlockUser) {
        final ID lockedUser = getLockedUser(source);
        if (lockedUser == null) return true;

        if (lockedUser.equals(unlockUser)) {
            Object[] o = Application.createQueryNoFilter(
                            "select lockId from CommonsLock where source = ?")
                    .setParameter(1, source)
                    .unique();
            if (o != null) {
                Application.getCommonsService().delete((ID) o[0]);
            }

            return true;
        } else {
            return false;
        }
    }
}
