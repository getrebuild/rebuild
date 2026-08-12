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
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.aibot2.ChatManager;
import com.rebuild.core.aibot2.Config;
import com.rebuild.core.aibot2.tool.ScheduleTask;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.notification.Message;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.core.support.distributed.DistributedJobLock;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 定时任务调度。每分钟检查到期任务并执行，执行时调用 AI 处理内容并将结果通过站内信通知用户
 *
 * @author devezhao
 * @since 2026/8/9
 */
@Slf4j
@Component
public class AiBotScheduleJob extends DistributedJobLock {

    // 正在执行中的任务，防止重复执行
    private static final Set<String> IN_PROGRESS = ConcurrentHashMap.newKeySet();

    @Scheduled(cron = "0 * * * * ?")
    public void executeJob() {
        if (!tryLock()) return;

        // 查询所有AI定时任务（未禁用的）
        Object[][] array = Application.createQueryNoFilter(
                "select configId,config,name from AibotConfig where type = ? and isDisabled = 'F'")
                .setParameter(1, AibotConfigManager.TYPE_AIBOT_SCHEDULE)
                .array();

        if (array.length == 0) return;

        Calendar nowCal = CalendarUtils.getInstance();
        nowCal.set(Calendar.SECOND, 0);
        nowCal.set(Calendar.MILLISECOND, 0);

        // {taskId, config, name}
        List<Object[]> dueTasks = new ArrayList<>();

        for (Object[] row : array) {
            ID taskId = (ID) row[0];
            String configStr = (String) row[1];
            String name = (String) row[2];
            if (StringUtils.isBlank(configStr)) continue;

            try {
                JSONObject task = JSON.parseObject(configStr);
                if (!"active".equals(task.getString("status"))) continue;

                if (IN_PROGRESS.contains(taskId.toLiteral())) continue;

                String nextExecTimeStr = task.getString("nextExecTime");
                if (StringUtils.isBlank(nextExecTimeStr)) continue;

                Date nextExecTime = CommonsUtils.parseDate(nextExecTimeStr);
                if (nextExecTime == null) continue;

                Calendar nextCal = CalendarUtils.getInstance();
                nextCal.setTime(nextExecTime);
                nextCal.set(Calendar.SECOND, 0);
                nextCal.set(Calendar.MILLISECOND, 0);

                // 到期（当前分钟或之前）
                if (!nextCal.after(nowCal)) {
                    dueTasks.add(new Object[]{taskId, task, name});
                }
            } catch (Exception e) {
                log.warn("Failed to parse scheduled task: {}", taskId, e);
            }
        }

        // 逐任务执行，单个任务异常不影响其他任务
        for (Object[] dueTask : dueTasks) {
            ID taskId = (ID) dueTask[0];
            JSONObject task = (JSONObject) dueTask[1];
            String name = (String) dueTask[2];
            try {
                executeTask(taskId, task, name);
            } catch (Exception e) {
                log.error("Failed to execute scheduled task: {}", taskId, e);
            }
        }
    }

    /**
     * 执行单个定时任务
     */
    private void executeTask(ID taskId, JSONObject task, String subject) {
        String taskIdStr = taskId.toLiteral();
        String userIdStr = task.getString("userId");
        String content = task.getString("content");
        String scheduleType = task.getString("scheduleType");

        ID user = ID.valueOf(userIdStr);
        ID oldUser = null;
        boolean success = false;

        try {
            IN_PROGRESS.add(taskIdStr);
            oldUser = UserContextHolder.setUser(user);

            log.info("Executing AI scheduled task: {} ({})", subject, taskIdStr);

            // 检查AI是否可用
            if (!Config.availableAiBot()) {
                log.warn("AI bot not available, skip task: {}", taskIdStr);
                Application.getNotifications().send(
                        MessageBuilder.createMessage(user,
                                String.format("**[%s]**\n\n定时任务无法执行：AI 助手未配置", subject),
                                Message.TYPE_DEFAULT));
                return;
            }

            // 调用AI处理内容
            String aiResponse = ChatManager.ask(content);

            // 发送站内信通知
            String message = String.format("**[%s]**\n\n%s", subject, aiResponse);
            Application.getNotifications().send(
                    MessageBuilder.createMessage(user, message, Message.TYPE_DEFAULT));

            success = true;
            log.info("AI scheduled task completed: {} ({})", subject, taskIdStr);

        } catch (Exception e) {
            log.error("AI scheduled task failed: {} ({})", subject, taskIdStr, e);

            try {
                String errorMessage = String.format("**[%s]**\n\n定时任务执行失败: %s",
                        subject, CommonsUtils.maxstr(e.getMessage(), 200));
                Application.getNotifications().send(
                        MessageBuilder.createMessage(user, errorMessage, Message.TYPE_DEFAULT));
            } catch (Exception ex) {
                log.error("Failed to send error notification for task: {}", taskIdStr, ex);
            }
        } finally {
            if (oldUser != null) {
                UserContextHolder.setUser(oldUser);
            } else {
                UserContextHolder.clearUser();
            }
            // 在 IN_PROGRESS.remove 之前更新任务状态，避免竞态窗口
            updateTaskStatus(taskId, task, scheduleType, user, success);
            IN_PROGRESS.remove(taskIdStr);
        }
    }

    /**
     * 更新任务状态到 AibotConfig。执行失败时不标记完成，避免任务未执行即丢失
     *
     * @param success 本次是否执行成功
     */
    private void updateTaskStatus(ID taskId, JSONObject task, String scheduleType, ID user, boolean success) {
        try {
            String nowStr = CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now());
            task.put("lastExecTime", nowStr);

            boolean disable = false;

            if (success) {
                if ("once".equals(scheduleType)) {
                    // 一次性任务标记为完成
                    task.put("status", "done");
                    disable = true;
                } else {
                    // 周期任务计算下次执行时间
                    task.put("nextExecTime", formatNextExecTime(task, scheduleType));
                }
            } else if ("once".equals(scheduleType)) {
                // 一次性任务失败保留任务待重试，连续失败超限后终止
                int failCount = task.getIntValue("failCount") + 1;
                task.put("failCount", failCount);
                if (failCount >= 3) {
                    task.put("status", "failed");
                    disable = true;
                }
            } else {
                // 周期任务失败仍顺延到下次执行时间，避免逐分钟重试
                task.put("nextExecTime", formatNextExecTime(task, scheduleType));
            }

            ID oldUser = UserContextHolder.setUser(user);
            try {
                Record record = EntityHelper.forUpdate(taskId, user);
                record.setString("config", task.toJSONString());
                if (disable) {
                    record.setBoolean("isDisabled", true);
                }
                Application.getBean(AibotConfigService.class).update(record);
            } finally {
                if (oldUser != null) {
                    UserContextHolder.setUser(oldUser);
                } else {
                    UserContextHolder.clearUser();
                }
            }
        } catch (Exception e) {
            log.error("Failed to update scheduled task status: {}", taskId.toLiteral(), e);
        }
    }

    /**
     * @return 格式化后的下次执行时间（UTC）
     */
    private String formatNextExecTime(JSONObject task, String scheduleType) {
        String time = task.getString("time");
        Integer dayOfWeek = task.getInteger("dayOfWeek");
        Integer dayOfMonth = task.getInteger("dayOfMonth");
        Date nextExecTime = ScheduleTask.calculateNextExecTime(scheduleType, time, dayOfWeek, dayOfMonth);
        return CalendarUtils.getUTCDateTimeFormat().format(nextExecTime);
    }
}
