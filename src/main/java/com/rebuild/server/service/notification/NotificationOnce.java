/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.service.notification;

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
