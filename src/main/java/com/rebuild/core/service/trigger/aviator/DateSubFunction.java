/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.aviator;

import java.util.Date;

/**
 * Usage: DATESUB(date, interval[H|D|M|Y])
 * Return: Date
 *
 * @author devezhao
 * @since 2021/4/12
 */
public class DateSubFunction extends DateAddFunction {
    private static final long serialVersionUID = -8857066285235050207L;

    @Override
    protected Date dateAdd(Date date, int interval, int field) {
        return super.dateAdd(date, -interval, field);
    }

    @Override
    public String getName() {
        return "DATESUB";
    }
}
