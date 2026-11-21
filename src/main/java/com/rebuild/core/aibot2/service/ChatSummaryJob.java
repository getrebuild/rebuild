/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.service;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.aibot2.ChatManager;
import com.rebuild.core.aibot2.Config;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.distributed.DistributedJobLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 会话摘要后台生成。定期压缩空闲的用户会话为摘要落库，引用会话时优先复用
 *
 * @author Zixin
 * @since 2026/11/21
 */
@Slf4j
@Component
public class ChatSummaryJob extends DistributedJobLock {

    // 单轮最多处理的会话数
    private static final int BATCH = 20;
    // 生成摘要的最小消息数（不足两轮不值得压缩）
    private static final int MIN_MESSAGES = 4;
    // 消息数落后存量摘要达到该值才刷新
    private static final int REFRESH_MESSAGES = 6;
    // 会话空闲判定（分钟），避开正在进行的会话
    private static final int IDLE_MINUTES = 10;
    // 只处理最近 N 天的会话
    private static final int RECENT_DAYS = 30;

    @Scheduled(cron = "0 */10 * * * ?")
    public void executeJob() {
        if (!tryLock()) return;
        if (!Config.availableAiBot()) return;

        Calendar idle = CalendarUtils.getInstance();
        idle.add(Calendar.MINUTE, -IDLE_MINUTES);

        Calendar recent = CalendarUtils.getInstance();
        recent.add(Calendar.DAY_OF_MONTH, -RECENT_DAYS);

        // 未生成摘要的优先，避免被已处理的会话长期占位
        List<Object[]> chats = new ArrayList<>();
        for (Object[] row : queryChats(true, idle, recent, BATCH)) chats.add(row);
        if (chats.size() < BATCH) {
            for (Object[] row : queryChats(false, idle, recent, BATCH - chats.size())) chats.add(row);
        }

        for (Object[] row : chats) {
            // 逐会话处理，单个失败不影响其他
            try {
                summarizeIfNeed((ID) row[0], (String) row[1], (String) row[2]);
            } catch (Exception e) {
                log.error("Failed to summarize chat: {}", row[0], e);
            }
        }
    }

    /**
     * @param pending true 查未生成摘要的
     * @param idle
     * @param recent
     * @param limit
     * @return
     */
    private Object[][] queryChats(boolean pending, Calendar idle, Calendar recent, int limit) {
        // 用户会话与公开访问会话，排除系统调用产生的会话
        return Application.createQueryNoFilter(
                "select chatId,contents,summary from AibotChat where " + ChatManager.VISIBLE_WHERE
                        + " and contents is not null"
                        + " and " + (pending ? "summary is null" : "summary is not null")
                        + " and modifiedOn <= ? and modifiedOn >= ? order by modifiedOn desc")
                .setParameter(1, idle.getTime())
                .setParameter(2, recent.getTime())
                .setLimit(limit)
                .array();
    }

    /**
     * @param chatid
     * @param contents
     * @param summary
     */
    private void summarizeIfNeed(ID chatid, String contents, String summary) {
        JSONArray messages = JSONArray.parseArray(contents);
        int msgCount = messages == null ? 0 : messages.size();

        if (StringUtils.isNotBlank(summary)) {
            JSONObject old = JSON.parseObject(summary);
            if (msgCount < MIN_MESSAGES) return;
            if (msgCount - old.getIntValue("messages") < REFRESH_MESSAGES) return;
        } else if (msgCount < MIN_MESSAGES) {
            // 消息过少，写入标记避免每轮重复选中（后续消息达到刷新条件时再处理）
            writeSummary(chatid, msgCount, null);
            return;
        }

        String text = ChatManager.summarizeChat(chatid, true);
        if (StringUtils.isBlank(text)) return;

        writeSummary(chatid, msgCount, text);
        log.info("Chat summary generated : {} ({} messages)", chatid, msgCount);
    }

    /**
     * @param chatid
     * @param msgCount
     * @param text 为空时仅写标记
     */
    private void writeSummary(ID chatid, int msgCount, String text) {
        JSONObject json = new JSONObject(true);
        json.put("messages", msgCount);
        if (text != null) json.put("text", text);

        // 只写 summary 字段，不触碰 contents 与修改时间（避免影响会话列表排序）
        Record record = EntityHelper.forUpdate(chatid, UserService.SYSTEM_USER, false);
        record.setString("summary", json.toJSONString());
        Application.getCommonsService().createOrUpdate(record);
    }
}
