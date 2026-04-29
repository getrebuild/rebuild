/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;

import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 流式输出支持
 *
 * @author devezhao
 * @since 2025/4/12
 */
public class StreamEcho {

    // 流式输出中断
    private static final Set<ID> INTERRUPTED_CHATS = new CopyOnWriteArraySet<>();

    /**
     * @param text
     * @param writer
     */
    public static void text(String text, PrintWriter writer) {
        echo(text, writer, null);
    }

    /**
     * @param error
     * @param writer
     */
    public static void error(String error, PrintWriter writer) {
        echo(error, writer, null, true);
    }

    /**
     * @param content
     * @param writer
     * @param type `_reasoning` `_chatid`
     */
    public static void echo(String content, PrintWriter writer, String type) {
        echo(content, writer, type, false);
    }

    /**
     * @param content
     * @param writer
     * @param type
     * @param isError
     */
    static void echo(String content, PrintWriter writer, String type, boolean isError) {
        JSONObject o = JSONUtils.toJSONObject(isError ? "error" : "content", content);
        if (type != null) o.put("type", type);

        String echo = String.format("data: %s\n\n", o);
        writer.write(echo);
        writer.flush();
    }

    /**
     * @param chatid
     * @return
     */
    public static boolean isInterrupted(ID chatid) {
        return INTERRUPTED_CHATS.remove(chatid);
    }

    /**
     * @param chatid
     * @return
     */
    public static boolean setInterrupt(ID chatid) {
        return INTERRUPTED_CHATS.add(chatid);
    }
}
