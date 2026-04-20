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
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.CommonsConfigManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.i18n.Language;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.List;

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
     * @param isView 视图/表单
     * @return
     */
    public static RecordAlertsBean isLocked43(ID recordId, boolean isView) {
        Assert.notNull(recordId, "[recordId] cannot null");
        if (!License.isRbvAttached()) return null;

        boolean isNew = EntityHelper.isUnsavedId(recordId);
        Entity e = MetadataHelper.getEntity(recordId.getEntityCode());
        List<JSONObject> alerts = CommonsConfigManager.instance.getRecordAlerts(e.getName());

        RecordAlertsBean bean = new RecordAlertsBean();

        for (JSONObject conf : alerts) {
            String tips = conf.getString("tips");
            if (StringUtils.isBlank(tips)) tips = Language.L("记录已锁定，禁止操作");

            // 新记录
            if (isNew) {
                if (conf.getBooleanValue("applyToFormNew")) {
                    bean.addTips(tips, conf.getString("tipsColor"));
                }
                continue;
            }

            JSONObject filter = conf.getJSONObject("filter");
            if (QueryHelper.isMatchAdvFilter(recordId, filter)) {
                Boolean noLock = conf.getBoolean("isNoLock");
                if (conf.getBooleanValue("isLock")) noLock = false;
                if (noLock == null || !noLock) {
                    bean.setLocked(true, tips);
                }

                // 是否显示
                boolean applyToView = conf.getBooleanValue("applyToView");
                boolean applyToForm = conf.getBooleanValue("applyToForm");
                boolean apply = applyToView && applyToForm;
                if (!apply) apply = isView && applyToView;
                if (!apply) apply = !isView && applyToForm;
                if (apply) bean.addTips(tips, conf.getString("tipsColor"));
            }
        }

        // 明细
        if (e.getMainEntity() != null && !EntityHelper.isUnsavedId(recordId)) {
            String dtfName = MetadataHelper.getDetailToMainField(e).getName();
            ID mainid = (ID) QueryHelper.queryFieldValue(recordId, dtfName);
            RecordAlertsBean m = isLocked43(mainid, isView);
            if (m != null) bean.merge(m);
        }

        // TODO 关联实体的???

        return bean.isValid() ? bean : null;
    }
}
