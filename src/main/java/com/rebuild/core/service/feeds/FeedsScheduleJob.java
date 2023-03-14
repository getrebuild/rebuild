/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.feeds;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.RegexUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.service.notification.Message;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.distributed.DistributedJobLock;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.i18n.LanguageBundle;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author devezhao
 * @since 2020/2/27
 */
@Component
public class FeedsScheduleJob extends DistributedJobLock {

    @Scheduled(cron = "0 * * * * ?")
    public void executeJob() {
        if (!tryLock()) return;

        Calendar time = CalendarUtils.getInstance();
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);

        Object[][] array = Application.createQueryNoFilter(
                "select createdBy,feedsId,content,contentMore from Feeds where scheduleTime = ? and type = ?")
                .setParameter(1, time.getTime())
                .setParameter(2, FeedsType.SCHEDULE.getMask())
                .array();

        if (array.length > 0) {
            doInternal(array);
        }
    }

    /**
     * @param array
     */
    protected void doInternal(Object[][] array) {
        // 合并同用户的多条消息
        Map<ID, List<Object[]>> map = new HashMap<>();
        for (Object[] o : array) {
            int reminds = JSON.parseObject((String) o[3]).getIntValue("scheduleRemind");
            if (reminds == 0) continue;

            List<Object[]> list = map.computeIfAbsent((ID) o[0], k -> new ArrayList<>());
            list.add(o);
        }

        final LanguageBundle bundle = Language.getSysDefaultBundle();
        // 发送
        for (List<Object[]> list : map.values()) {
            List<Object[]> notifications = new ArrayList<>();
            List<Object[]> emails = new ArrayList<>();
            List<Object[]> smss = new ArrayList<>();

            // 分类
            for (Object[] o : list) {
                int reminds = JSON.parseObject((String) o[3]).getIntValue("scheduleRemind");
                if ((reminds & 1) != 0) notifications.add(o);
                if ((reminds & 2) != 0) emails.add(o);
                if ((reminds & 4) != 0) smss.add(o);
            }

            ID toUser = (ID) list.get(0)[0];

            // 消息通知
            if (!notifications.isEmpty()) {
                String subject = bundle.L("你有 %d 条日程提醒", notifications.size());
                String contents = subject + mergeContents(notifications, false);
                Application.getNotifications().send(
                        MessageBuilder.createMessage(toUser, contents, Message.TYPE_FEEDS));
            }

            // 邮件
            final String emailAddr = Application.getUserStore().getUser(toUser).getEmail();
            if (SMSender.availableMail() && RegexUtils.isEMail(emailAddr) && !emails.isEmpty()) {
                String subject = bundle.L("你有 %d 条日程提醒", emails.size());
                String contents = mergeContents(emails, true);
                contents = MessageBuilder.formatMessage(contents);
                SMSender.sendMailAsync(emailAddr, subject, contents);
            }

            // 短信（考虑短信字数，内容简化了）
            final String mobileAddr = Application.getUserStore().getUser(toUser).getWorkphone();
            if (SMSender.availableSMS() && RegexUtils.isCNMobile(mobileAddr) && !smss.isEmpty()) {
                String subject = bundle.L("你有 %d 条日程提醒", smss.size());
                SMSender.sendSMSAsync(mobileAddr, subject);
            }
        }
    }

    /**
     * @param msgs
     * @param fullUrl
     * @return
     */
    private String mergeContents(List<Object[]> msgs, boolean fullUrl) {
        Object[] first = msgs.get(0);

        String clickUrl = "/app/redirect?id=" + first[1];
        if (fullUrl) clickUrl = RebuildConfiguration.getHomeUrl(clickUrl);
        else clickUrl = AppUtils.getContextPath(clickUrl);

        String content = String.format("\n\n> [%s](%s)",
                CommonsUtils.maxstr((String) first[2], 100), clickUrl);
        if (msgs.size() > 1) {
            content += String.format("\n\n... (%d)", msgs.size() - 1);
        }
        return content;
    }
}
