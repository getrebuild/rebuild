/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * 会话完整日志（仅开发模式）
 *
 * @author devezhao
 * @since 2026/8/17
 */
@Slf4j
public class ChatLogger {

    private final ID chatid;

    private final Object writeLock = new Object();

    /**
     * @param chatid
     */
    public ChatLogger(ID chatid) {
        this.chatid = chatid;
    }

    /**
     * 是否启用（仅开发模式有效）
     *
     * @return
     */
    public boolean enabled() {
        return chatid != null && Application.devMode();
    }

    /**
     * 记录会话头
     *
     * @param model
     * @param systemPrompt
     */
    public void logSession(String model, String systemPrompt) {
        if (!enabled()) return;

        File file = logFile();
        synchronized (writeLock) {
            if (file.exists() && file.length() > 0) return;

            String header = String.format(
                    "# AIBot Chat Log%n> 会话: %s | 模型: %s | 时间: %s%n%n---%n%n## SYSTEM PROMPT%n%n%s%n",
                    chatid, model, formatTime(), systemPrompt);
            write(file, header);
        }
    }

    /**
     * 记录一条日志
     *
     * @param type USER / ASSISTANT / EVENT / TOOL_CALL {工具} / TOOL_RESULT {工具}
     * @param detail
     */
    public void log(String type, String detail) {
        if (!enabled()) return;

        String content = StringUtils.defaultIfBlank(detail, "(empty)");
        // 工具入参/结果多为 JSON，格式化后便于阅读
        if (type.startsWith("TOOL_") && JSONUtils.wellFormat(content)) {
            try {
                content = JSONUtils.prettyPrint(JSON.parse(content));
            } catch (Exception ignored) {
            }
        }

        String entry = String.format("%n---%n%n## %s | %s%n%n%s%n", type, formatTime(), content);
        write(logFile(), entry);
    }

    /**
     * 记录一条会话流程事件（如中断、断连、工具轮次）
     *
     * @param event
     */
    public void logEvent(String event) {
        log("EVENT", event);
    }

    /**
     * @return
     */
    public File logFile() {
        File logd = RebuildConfiguration.getFileOfData("_log");
        return new File(logd, "aibot-chat-" + chatid + ".log");
    }

    /**
     * @return
     */
    private String formatTime() {
        return CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now());
    }

    /**
     * @param file
     * @param content
     */
    private void write(File file, String content) {
        synchronized (writeLock) {
            try {
                Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ex) {
                log.warn("Cannot write chat log : {}", file, ex);
            }
        }
    }
}
