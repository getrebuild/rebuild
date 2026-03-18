/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.utils.JSONUtils;
import org.springframework.util.Assert;

/**
 * 通用锁（用于锁定配置或记录）
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
        }

        return false;
    }

    /**
     * v4.3 记录是否已锁定
     *
     * @param source
     * @return
     */
    public static boolean isLocked43(ID source) {
        Assert.notNull(source, "[source] cannot null");

        EasyEntity ee = EasyMetaFactory.valueOf(MetadataHelper.getEntity(source.getEntityCode()));
        String lockFilter = ee.getExtraAttr("lockFilter");
        if (!JSONUtils.wellFormat(lockFilter)) return false;

        JSONObject lockFilter2 = JSON.parseObject(lockFilter);
        return QueryHelper.isMatchAdvFilter(source, lockFilter2);
    }

    /**
     * v4.3 记录是否已锁定
     *
     * @param source
     * @return
     */
    public static boolean isLocked43(Record source) {
        Assert.notNull(source, "[source] cannot null");

        if (source.getPrimary() == null) return false;
        return isLocked43(source.getPrimary());
    }
}
