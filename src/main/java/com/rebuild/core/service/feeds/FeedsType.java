/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.feeds;

/**
 * 动态类型
 *
 * @author devezhao
 * @since 2019/11/1
 */
public enum FeedsType {

    ACTIVITY(1, "动态"),
    FOLLOWUP(2, "跟进"),
    ANNOUNCEMENT(3, "公告"),
    SCHEDULE(4, "日程"),

    ;

    final private int mask;
    final private String name;

    FeedsType(int mask, String name) {
        this.mask = mask;
        this.name = name;
    }

    /**
     * @return
     */
    public int getMask() {
        return mask;
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @param typeMask
     * @return
     */
    public static FeedsType parse(int typeMask) {
        for (FeedsType t : values()) {
            if (t.getMask() == typeMask) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown mask : " + typeMask);
    }
}
