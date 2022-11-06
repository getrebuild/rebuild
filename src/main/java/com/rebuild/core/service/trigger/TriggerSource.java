/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.service.general.OperatingContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 触发源
 *
 * @author RB
 * @since 2022/07/04
 */
@Slf4j
public class TriggerSource {

    private static final AtomicLong TSNO = new AtomicLong(0);

    private String id;
    // [OperatingContext, TriggerWhen]
    private final List<Object[]> sources = new ArrayList<>();

    private boolean skipOnce = false;

    // 触发次数
    private int triggerTimes = 0;

    protected TriggerSource(OperatingContext origin, TriggerWhen originAction) {
        this.id = TSNO.incrementAndGet() + "-";
        addNext(origin, originAction);
        System.out.println("[dev] New trigger-source : " + this);

        // Clear
        if (this.id.length() > 4) TSNO.set(0);
    }

    public void addNext(OperatingContext next, TriggerWhen nextAction) {
        sources.add(new Object[] { next, nextAction });
    }

    public String getSourceId() {
        return this.id + sources.size();
    }

    public OperatingContext getOrigin() {
        return (OperatingContext) sources.get(0)[0];
    }

    public ID getOriginRecord() {
        return getOrigin().getAnyRecord().getPrimary();
    }

    public OperatingContext getLast() {
        return (OperatingContext) sources.get(sources.size() - 1)[0];
    }

    public String getLastSourceKey() {
        Object[] last = sources.get(sources.size() - 1);
        return ((OperatingContext) last[0]).getAnyRecord().getPrimary() + ":" + ((TriggerWhen) last[1]).name().charAt(0);
    }

    public void setSkipOnce() {
        skipOnce = true;
    }

    public boolean isSkipOnce() {
        boolean skipOnceHold = skipOnce;
        skipOnce = false;
        return skipOnceHold;
    }

    public int incrTriggerTimes() {
        return ++triggerTimes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Object[] s : sources) {
            sb.append(((TriggerWhen) s[1]).name().charAt(0)).append("#").append(s[0]).append(" >> ");
        }
        return sb.append("END").toString();
    }
}
