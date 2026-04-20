/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import com.alibaba.fastjson.JSON;
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
    @Getter
    private String lockedTips = null;

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

    protected void setLocked(boolean locked, String tips) {
        this.locked = locked;
        if (locked) this.lockedTips = tips;
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
        return JSONUtils.toJSONObject(
                new String[]{"tips", "locked"},
                new Object[]{tips, locked});
    }
}
