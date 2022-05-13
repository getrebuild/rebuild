/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.series;

import cn.devezhao.commons.CalendarUtils;

/**
 * 时间系列
 *
 * @author devezhao
 * @since 12/24/2018
 */
public class TimeVar extends SeriesVar {

    /**
     * @param symbols
     */
    protected TimeVar(String symbols) {
        super(symbols);
    }

    @Override
    public String generate() {
        String s = getSymbols().replace("Y", "y");
        // YYYY-MM-DD HH-II-SS > yyyy-MM-dd HH-mm:ss
        s = s.replace("D", "d");
        s = s.replace("I", "m");
        s = s.replace("S", "s");
        return CalendarUtils.format(s, CalendarUtils.now());
    }
}
