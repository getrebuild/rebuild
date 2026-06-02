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

import java.util.ArrayList;
import java.util.List;

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

    protected RecordAlertsBean() {
        super();
    }

    protected void addTips(String tips, String tipsColor) {
        this.tips.add(new String[]{tips, tipsColor});
    }

    protected void merge(RecordAlertsBean another) {
        tips.addAll(another.tips);
        locked = locked || another.locked;
    }

    protected void setLocked(String tips, ID recordId) {
        this.locked = true;
        this.lockedTips = tips;
        this.lockedRecordId = recordId;
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

        if (recordId != null) {
            t = ContentWithFieldVars.replaceWithRecord(t, recordId);
        }
        return t;
    }

    /**
     * 有效
     *
     * @return
     */
    public boolean isValid() {
        return locked || !tips.isEmpty();
    }

    @Override
    public JSON toJSON() {
        List<String[]> tips2 = new ArrayList<>(this.tips);
        if (lockedRecordId != null) {
            for (String[] tip :  tips2) {
                tip[0] = ContentWithFieldVars.replaceWithRecord(tip[0], lockedRecordId);
            }
        }

        return JSONUtils.toJSONObject(
                new String[]{"tips", "locked"},
                new Object[]{tips2, locked});
    }
}
