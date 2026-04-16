/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang3.StringUtils;
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

    // -- V4.4

    /**
     * 记录是否已锁定
     *
     * @param recordId
     * @return
     */
    public static String isLocked43(ID recordId) {
        Assert.notNull(recordId, "[recordId] cannot null");
        if (EntityHelper.isUnsavedId(recordId)) return null;

        Entity e = MetadataHelper.getEntity(recordId.getEntityCode());
        Object[][] array = Application.createQueryNoFilter(
                "select config from CommonsConfig where belongEntity = ? and type = 'RECORD_ALERTS'")
                .setParameter(1, e.getName())
                .array();

        for (Object[] o : array) {
            String conf = (String) o[0];
            if (!JSONUtils.wellFormat(conf)) continue;

            JSONObject confJson = JSON.parseObject(conf);
            if (QueryHelper.isMatchAdvFilter(recordId, confJson.getJSONObject("filter"))) {
                String tips = confJson.getString("tips");
                if (StringUtils.isBlank(tips)) tips = Language.L("记录已锁定，禁止操作");
                return tips;
            }
        }

        // 明细
        if (e.getMainEntity() != null) {
            String dtfName = MetadataHelper.getDetailToMainField(e).getName();
            ID mainid = (ID) QueryHelper.queryFieldValue(recordId, dtfName);
            return isLocked43(mainid);
        }

        return null;
    }

    /**
     * @param record
     * @return
     * @see #isLocked43(ID)
     */
    public static String isLocked43(Record record) {
        Assert.notNull(record, "[record] cannot null");

        if (record.getPrimary() == null) return null;
        return isLocked43(record.getPrimary());
    }
}
