/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.lock;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.support.general.ContentWithFieldVars;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.JSONable;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ZHAO
 * @since 4/15/2026
 */
public class RecordAlertsBean implements JSONable {
    private static final long serialVersionUID = -5867491401470449455L;

    @Getter
    private boolean locked = false;

    // 给后端提示用
    private String lockedTips = null;
    private ID lockedRecordId = null;

    // [Text, Color]
    private List<String[]> tips = new ArrayList<>();

    // 不锁定字段可以修改
    @Getter
    private Set<String> noLockFields = new HashSet<>();

    protected RecordAlertsBean() {
        super();
    }

    /**
     * @param tips
     * @param tipsColor
     */
    protected void addTips(String tips, String tipsColor) {
        this.tips.add(new String[]{tips, tipsColor});
    }

    /**
     * @param another
     */
    protected void merge(RecordAlertsBean another) {
        tips.addAll(another.tips);
        locked = another.locked || locked;
        lockedTips = (String) ObjectUtils.defaultIfNull(another.lockedTips, lockedTips);
        lockedRecordId = (ID) ObjectUtils.defaultIfNull(another.lockedRecordId, lockedRecordId);
        noLockFields.addAll(another.noLockFields);
    }

    /**
     * @param tips
     * @param recordId
     * @param noLockFields
     */
    protected void setLocked(String tips, ID recordId, Collection<?> noLockFields) {
        this.locked = true;
        this.lockedTips = tips;
        this.lockedRecordId = recordId;
        if (CollectionUtils.isNotEmpty(noLockFields)) {
            for (Object f : noLockFields) {
                this.noLockFields.add(f.toString());
            }
        }
    }

    /**
     * @return
     */
    public String getLockedTips() {
        return getLockedTips(this.lockedRecordId);
    }

    /**
     * @param recordId
     * @return
     */
    public String getLockedTips(ID recordId) {
        String t = lockedTips;
        if (lockedTips == null) t = tips.get(0)[0];

        return formatTipps(t, recordId);
    }

    /**
     * 是否有效配置
     *
     * @return
     */
    protected boolean isValid() {
        return locked || !tips.isEmpty();
    }

    @Override
    public JSON toJSON() {
        List<String[]> tipsFix = new ArrayList<>(this.tips);
        if (lockedRecordId != null) {
            for (String[] tip : tipsFix) {
                tip[0] = formatTipps(tip[0], lockedRecordId);
            }
        }

        return JSONUtils.toJSONObject(
                new String[]{"tips", "locked"},
                new Object[]{tipsFix, locked});
    }

    private String formatTipps(String tip, ID recordId) {
        if (recordId == null) return tip;
        return ContentWithFieldVars.replaceWithRecord(tip, recordId);
    }
}
