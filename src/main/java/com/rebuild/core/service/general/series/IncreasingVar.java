/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.series;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Field;
import com.rebuild.core.Application;
import com.rebuild.core.support.KVStorage;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数字自增系列
 *
 * @author devezhao
 * @since 12/24/2018
 */
@Slf4j
public class IncreasingVar extends SeriesVar {

    private static final Object INCREASINGS_LOCK = new Object();
    private static final Map<String, AtomicLong> INCREASINGS = new ConcurrentHashMap<>();

    private Field field;
    private String zeroFlag;

    /**
     * @param symbols
     * @param field
     * @param zeroFlag
     */
    protected IncreasingVar(String symbols, Field field, String zeroFlag) {
        super(symbols);
        this.field = field;
        this.zeroFlag = zeroFlag;
    }

    /**
     * @param field
     */
    protected IncreasingVar(Field field) {
        super(null);
        this.field = field;
    }

    private String getNameKey() {
        Assert.notNull(this.field, "[this.field] cannot be null");
        return String.format("Series-%s.%s", field.getOwnEntity().getName(), field.getName());
    }

    @Override
    public String generate() {
        // Preview mode
        if (field == null) {
            return StringUtils.leftPad("1", getSymbols().length(), '0');
        }

        final String nameKey = getNameKey();
        synchronized (INCREASINGS_LOCK) {
            AtomicLong incr = INCREASINGS.get(nameKey);
            if (incr == null) {
                String val = KVStorage.getCustomValue(nameKey);
                long init = val == null ? countFromDb() : ObjectUtils.toLong(val);

                incr = new AtomicLong(init);
                INCREASINGS.put(nameKey, incr);
            }

            long nextValue = incr.incrementAndGet();
            RebuildConfiguration.setCustomValue(nameKey, nextValue, Boolean.TRUE);

            return StringUtils.leftPad(nextValue + "", getSymbols().length(), '0');
        }
    }

    /**
     * 重置序号
     *
     * @param reset
     */
    protected void clean(long reset) {
        Assert.isTrue(reset >= 0, "[reset] must be greater than 0");
        final String nameKey = getNameKey();
        synchronized (INCREASINGS_LOCK) {
            INCREASINGS.remove(nameKey);
            RebuildConfiguration.setCustomValue(nameKey, reset, Boolean.TRUE);
        }
    }

    /**
     * @return
     */
    public long getCurrentValue() {
        final String nameKey = getNameKey();
        synchronized (INCREASINGS_LOCK) {
            String val = KVStorage.getCustomValue(nameKey);
            return val == null ? 0L : ObjectUtils.toLong(val);
        }
    }

    /**
     * NOTE
     * 例如有100条记录，序号也为100。
     * 但是删除了10条后，调用此方法所生产的序号只有 90（直接采用 count 记录数）
     *
     * @return
     */
    private long countFromDb() {
        String dateLimit = null;
        if ("Y".equals(zeroFlag)) {
            dateLimit = CalendarUtils.format("yyyy", CalendarUtils.now()) + "-01-01";
        } else if ("M".equals(zeroFlag)) {
            dateLimit = CalendarUtils.format("yyyy-MM", CalendarUtils.now()) + "-01";
        } else if ("D".equals(zeroFlag)) {
            dateLimit = CalendarUtils.format("yyyy-MM-dd", CalendarUtils.now());
        }

        if (dateLimit != null) {
            dateLimit = "createdOn >= '" + dateLimit + " 00:00:00'";
        } else {
            dateLimit = "(1=1)";
        }

        String sql = String.format("select count(%s) from %s where %s",
                field.getName(), field.getOwnEntity().getName(), dateLimit);
        Object[] count = Application.createQueryNoFilter(sql).unique();
        return ObjectUtils.toLong(count[0]);
    }
}
