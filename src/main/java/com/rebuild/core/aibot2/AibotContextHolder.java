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

    private static final ThreadLocal<Boolean> NON_INTERACTIVE = new NamedThreadLocal<>("AI non-interactive");

    private static final ThreadLocal<Boolean> INTERNAL_ASK = new NamedThreadLocal<>("AI internal ask");

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
     * 替换当前 Agent，返回原值（用于嵌套调用恢复）
     *
     * @param agent
     * @return
     */
    public static AibotAgent swapAgent(AibotAgent agent) {
        AibotAgent old = AGENT.get();
        restoreAgent(agent);
        return old;
    }

    /**
     * @param agent
     */
    public static void restoreAgent(AibotAgent agent) {
        if (agent == null) AGENT.remove();
        else AGENT.set(agent);
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

    /**
     * 非交互执行标记。定时任务等无用户交互场景下，仅用于提示词引导，不影响二次确认
     *
     * @param nonInteractive
     * @return 原值
     */
    public static boolean setNonInteractive(boolean nonInteractive) {
        boolean old = Boolean.TRUE.equals(NON_INTERACTIVE.get());
        if (nonInteractive) NON_INTERACTIVE.set(true);
        else NON_INTERACTIVE.remove();
        return old;
    }

    /**
     * @return
     */
    public static boolean isNonInteractive() {
        return Boolean.TRUE.equals(NON_INTERACTIVE.get());
    }

    /**
     * 内部调用标记（如文件内容解析）。仅影响提示词构建：不注入用户上下文、记忆等
     *
     * @param internalAsk
     * @return 原值
     */
    public static boolean setInternalAsk(boolean internalAsk) {
        boolean old = Boolean.TRUE.equals(INTERNAL_ASK.get());
        if (internalAsk) INTERNAL_ASK.set(true);
        else INTERNAL_ASK.remove();
        return old;
    }

    /**
     * @return
     */
    public static boolean isInternalAsk() {
        return Boolean.TRUE.equals(INTERNAL_ASK.get());
    }

    public static void clear() {
        AI_SOURCE.remove();
        AGENT.remove();
        ANONYMOUS.remove();
        NON_INTERACTIVE.remove();
        INTERNAL_ASK.remove();
    }
}
