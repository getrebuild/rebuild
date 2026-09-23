/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.engine.ID;
import org.springframework.core.NamedThreadLocal;

/**
 * AI 操作源标记（线程级）。用于在变更历史中标识由 AI 工具触发的数据操作。
 *
 * @author devezhao
 * @since 2026/9/22
 * @see com.rebuild.core.service.general.RevisionHistoryObserver
 * @see com.rebuild.core.service.trigger.RobotTriggerObserver.TriggerSource
 */
public class AiSourceHolder {

    private static final ThreadLocal<ID> AI_SOURCE = new NamedThreadLocal<>("AI source");

    /**
     * 设置 AI 操作源（会话 ID），返回旧值用于恢复
     *
     * @param chatid
     * @return
     */
    public static ID set(ID chatid) {
        ID old = AI_SOURCE.get();
        AI_SOURCE.set(chatid);
        return old;
    }

    /**
     * 获取 AI 操作源
     *
     * @return
     */
    public static ID get() {
        return AI_SOURCE.get();
    }

    /**
     * 清理/恢复 AI 操作源
     *
     * @param restore 传 null 则清理，否则恢复为指定值
     */
    public static void clear(ID restore) {
        if (restore == null) AI_SOURCE.remove();
        else AI_SOURCE.set(restore);
    }
}
