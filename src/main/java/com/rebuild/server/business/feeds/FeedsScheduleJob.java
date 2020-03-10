/*
Copyright (c) REBUILD <https://getrebuild.com/>. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.feeds;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.helper.SMSender;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.service.notification.Message;
import com.rebuild.server.service.notification.MessageBuilder;
import com.rebuild.utils.AppUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author devezhao
 * @since 2020/2/27
 */
public class FeedsScheduleJob extends QuartzJobBean {

    private static final Log LOG = LogFactory.getLog(FeedsScheduleJob.class);

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
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
        // 合并同用户
        Map<ID, List<Object[]>> map = new HashMap<>();
        for (Object[] o : array) {
            int reminds = JSON.parseObject((String) o[3]).getIntValue("scheduleRemind");
            if (reminds == 0) {
                continue;
            }

            List<Object[]> list = map.computeIfAbsent((ID) o[0], k -> new ArrayList<>());
            list.add(o);
        }

        // 发送
        for (List<Object[]> list : map.values()) {
            List<Object[]> notifications = new ArrayList<>();
            List<Object[]> emails = new ArrayList<>();

            // 分类
            for (Object[] o : list) {
                int reminds = JSON.parseObject((String) o[3]).getIntValue("scheduleRemind");
                if ((reminds & 1) != 0) notifications.add(o);
                if ((reminds & 2) != 0) emails.add(o);
            }

            final ID toUser = (ID) list.get(0)[0];

            if (!notifications.isEmpty()) {
                String contents = mergeContents(notifications, false);
                Message m = MessageBuilder.createMessage(null, toUser, contents, Message.TYPE_FEEDS);
                Application.getNotifications().send(m);
            }

            String emailAddr = Application.getUserStore().getUser(toUser).getEmail();
            if (!SMSender.availableMail() || StringUtils.isBlank(emailAddr)) {
                LOG.warn("Mail service unavailable or no email-address : " + toUser);
                continue;
            }

            if (!emails.isEmpty()) {
                String subject = "你有 " + emails.size() + " 条动态日程提醒";
                String contents = mergeContents(emails, true);
                contents = MessageBuilder.formatMessage(contents, true, false);
                SMSender.sendMailAsync(emailAddr, subject, contents);
            }
        }
    }

    /**
     * @param list
     * @param isMail
     * @return
     */
    private String mergeContents(List<Object[]> list, boolean isMail) {
        StringBuilder sb = new StringBuilder();
        if (!isMail) {
            sb.append("你有 ").append(list.size()).append(" 条动态日程提醒");
        }

        int nums = 0;
        for (Object[] o : list) {
            sb.append("\n- [");

            String c = (String) o[2];
            if (c.length() > 100) {
                c = c.substring(0, 100) + " ...";
            }
            sb.append(c);

            String url = "/app/list-and-view?id=" + o[1];
            if (isMail) {
                url = SysConfiguration.getHomeUrl(url);
            } else {
                url = AppUtils.getContextPath() + url;
            }
            sb.append("](").append(url).append(")");

            nums++;
            // 最多列出 N 条
            if (nums >= 5) {
                break;
            }
        }

        if (list.size() > nums) {
            sb.append("\n- 等共计 ").append(list.size()).append(" 条");
        }

        return sb.toString();
    }
}
