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

import java.util.Calendar;

/**
 * 会话摘要后台生成。定期压缩空闲的用户会话为摘要落库，供后续跨会话上下文使用
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

        Object[][] array = Application.createQueryNoFilter(
                "select chatId,contents,summary from AibotChat where source is null and contents is not null and modifiedOn <= ? and createdOn >= ? order by modifiedOn desc")
                .setParameter(1, idle.getTime())
                .setParameter(2, recent.getTime())
                .setLimit(BATCH)
                .array();

        for (Object[] row : array) {
            // 逐会话处理，单个失败不影响其他
            try {
                summarizeIfNeed((ID) row[0], (String) row[1], (String) row[2]);
            } catch (Exception e) {
                log.error("Failed to summarize chat: {}", row[0], e);
            }
        }
    }

    /**
     * @param chatid
     * @param contents
     * @param summary
     */
    private void summarizeIfNeed(ID chatid, String contents, String summary) {
        JSONArray messages = JSONArray.parseArray(contents);
        int msgCount = messages == null ? 0 : messages.size();
        if (msgCount < MIN_MESSAGES) return;

        if (StringUtils.isNotBlank(summary)) {
            JSONObject old = JSON.parseObject(summary);
            if (msgCount - old.getIntValue("rounds") < REFRESH_MESSAGES) return;
        }

        String text = ChatManager.summarizeChat(chatid);
        if (StringUtils.isBlank(text)) return;

        JSONObject json = new JSONObject();
        json.put("rounds", msgCount);
        json.put("text", text);

        // 只写 summary 字段，不触碰 contents，避免与缓存中的会话对象竞争
        Record record = EntityHelper.forUpdate(chatid, UserService.SYSTEM_USER);
        record.setString("summary", json.toJSONString());
        Application.getCommonsService().createOrUpdate(record);

        log.info("Chat summary generated : {} ({} messages)", chatid, msgCount);
    }
}
