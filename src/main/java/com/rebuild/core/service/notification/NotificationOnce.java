/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.notification;

import cn.devezhao.persist4j.engine.ID;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 消息合并发送，用于批量操作时合并消息。
 * Using:
 * <tt>begin</tt>
 * <tt>[didBegin]</tt>
 * <tt>[getMergeSet]</tt>
 * <tt>end</tt>
 *
 * @author ZHAO
 * @since 2019/12/2
 */
public class NotificationOnce {

    private static final ThreadLocal<Set<ID>> STATE = new ThreadLocal<>();

    /**
     * 启动合并，此后线程中的消息发送将忽略
     */
    public static void begin() {
        STATE.set(new LinkedHashSet<>());
    }

    /**
     * 是否开始了
     *
     * @return
     */
    public static boolean didBegin() {
        return STATE.get() != null;
    }

    /**
     * 合并结束
     *
     * @return
     */
    public static Set<ID> end() {
        Set<ID> set = getMergeSet();
        STATE.remove();
        return Collections.unmodifiableSet(set);
    }

    /**
     * 被合并到一次发送的记录
     *
     * @return
     */
    protected static Set<ID> getMergeSet() {
        return STATE.get();
    }
}
