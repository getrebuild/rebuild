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

    // 日志首行分析提示词，日志文件可直接发给 AI 分析
    private static final String ANALYZE_PROMPT =
            "你是 AI 会话质量分析专家。本文件是一份 AIBot 会话完整日志（Markdown 格式，"
            + "包含 SYSTEM PROMPT、USER 输入、ASSISTANT 回复、TOOL_CALL 入参与 TOOL_RESULT 结果）。"
            + "请通读并分析该会话，给出优化方向，重点关注："
            + "1) 工具调用：是否存在冗余/重复调用、入参不合理、可合并的步骤；"
            + "2) 结果利用：TOOL_RESULT 是否被充分利用，是否存在超大结果浪费 token；"
            + "3) 提示词：SYSTEM PROMPT 是否有可改进之处；"
            + "4) 回复质量：ASSISTANT 回复是否准确、完整、简洁。"
            + "输出使用 Markdown 格式，按「问题 → 优化建议」逐条列出，简明扼要。";

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
                    "%s%n%n# AIBot Chat Log%n> 会话: %s | 模型: %s | 时间: %s%n%n---%n%n## SYSTEM PROMPT%n%n%s%n",
                    ANALYZE_PROMPT, chatid, model, formatTime(), systemPrompt);
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
        // 工具入参/结果多为 JSON，压缩为单行紧凑格式，避免占用过多日志空间
        if (type.startsWith("TOOL_") && JSONUtils.wellFormat(content)) {
            try {
                content = JSON.toJSONString(JSON.parse(content));
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
