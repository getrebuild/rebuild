/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 会话完整日志（仅开发模式）
 * 将一次会话的完整交互（用户输入、模型回复、工具调用及结果）输出到
 * ${DataDirectory}/temp/aibot-chat-{chatid}.log，便于排查问题时还原现场
 *
 * @author devezhao
 * @since 2026/8/17
 */
@Slf4j
public class ChatLogger {

    private ChatLogger() {}

    /**
     * 是否启用（仅开发模式有效）
     *
     * @return
     */
    public static boolean enabled() {
        return Application.devMode();
    }

    /**
     * 记录会话头（仅首次写入时，含模型与系统提示词）
     *
     * @param chatid
     * @param model
     * @param systemPrompt
     */
    public static void logSession(ID chatid, String model, String systemPrompt) {
        if (!enabled() || chatid == null) return;

        File file = logFile(chatid);
        if (file.exists() && file.length() > 0) return;

        String header = String.format("会话: %s | 模型: %s%n%s%n====== SYSTEM PROMPT ======%n%s%n",
                chatid, model, formatTime(), systemPrompt);
        write(file, header);
    }

    /**
     * 记录一条日志
     *
     * @param chatid
     * @param type USER / ASSISTANT / TOOL_CALL {工具} / TOOL_RESULT {工具}
     * @param detail
     */
    public static void log(ID chatid, String type, String detail) {
        if (!enabled() || chatid == null) return;

        String entry = String.format("%n====== %s | %s ======%n%s%n",
                formatTime(), type, StringUtils.defaultIfBlank(detail, "(empty)"));
        write(logFile(chatid), entry);
    }

    /**
     * @param chatid
     * @return
     */
    public static File logFile(ID chatid) {
        return RebuildConfiguration.getFileOfTemp("aibot-chat-" + chatid + ".log");
    }

    // --

    private static String formatTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }

    private static void write(File file, String content) {
        // 串行追加，避免并发写入截断条目
        synchronized (ChatLogger.class) {
            try {
                FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8, true);
            } catch (Exception ex) {
                log.warn("Cannot write chat log : {}", file, ex);
            }
        }
    }
}
