/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.service.general.OperatingContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * @author RB
 * @since 2022/07/04
 */
@Slf4j
public class TriggerSource {

    private final List<Object[]> sources = new ArrayList<>();
    private boolean skipOnce = false;

    protected TriggerSource(OperatingContext origin, TriggerWhen originAction) {
        addNext(origin, originAction);
        if (Application.devMode()) log.warn("[dev] New trigger-source : {}", this);
    }

    public void addNext(OperatingContext next, TriggerWhen nextAction) {
        sources.add(new Object[] { next, nextAction });
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
        return ((OperatingContext) last[0]).getAnyRecord().getPrimary()
                + ":" + ((TriggerWhen) last[1]).name().charAt(0);
    }

    public int getSourceDepth() {
        return sources.size();
    }

    public void setSkipOnce() {
        skipOnce = true;
    }

    public boolean isSkipOnce() {
        boolean skipOnceHold = skipOnce;
        skipOnce = false;
        return skipOnceHold;
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
