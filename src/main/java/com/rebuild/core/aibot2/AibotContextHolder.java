/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.engine.ID;
import org.springframework.core.NamedThreadLocal;

/**
 * @author devezhao
 * @since 2026/9/22
 * @see com.rebuild.core.service.general.RevisionHistoryObserver
 * @see com.rebuild.core.service.trigger.RobotTriggerObserver
 */
public class AibotContextHolder {

    private static final ThreadLocal<ID> AI_SOURCE = new NamedThreadLocal<>("AI source");

    private static final ThreadLocal<AibotAgent> AGENT = new NamedThreadLocal<>("AI agent");

    private static final ThreadLocal<Boolean> ANONYMOUS = new NamedThreadLocal<>("AI anonymous");

    /**
     * @param chatid
     * @return
     */
    public static ID setSource(ID chatid) {
        ID old = AI_SOURCE.get();
        AI_SOURCE.set(chatid);
        return old;
    }

    /**
     * @return
     */
    public static ID getSource() {
        return AI_SOURCE.get();
    }

    /**
     * @param restore 传 null 则清理，否则恢复为指定值
     */
    public static void clearSource(ID restore) {
        if (restore == null) AI_SOURCE.remove();
        else AI_SOURCE.set(restore);
    }

    /**
     * @param agent
     * @param anonymous
     */
    public static void setAgent(AibotAgent agent, boolean anonymous) {
        AGENT.set(agent);
        ANONYMOUS.set(anonymous);
    }

    /**
     * @return
     */
    public static AibotAgent getAgent() {
        return AGENT.get();
    }

    /**
     * @return
     */
    public static boolean isAnonymous() {
        return Boolean.TRUE.equals(ANONYMOUS.get());
    }

    public static void clear() {
        AI_SOURCE.remove();
        AGENT.remove();
        ANONYMOUS.remove();
    }
}
