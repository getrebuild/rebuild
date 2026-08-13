/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.aibot2.service.AibotConfigManager;
import com.rebuild.core.aibot2.service.AibotConfigService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * AI定时任务管理工具。支持创建、查询、取消操作
 *
 * @author devezhao
 * @since 2026/8/9
 */
@Slf4j
public class ScheduleTask implements Tool {

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String action = args.getString("action");
        if (StringUtils.isBlank(action)) {
            action = "create";
        }

        switch (action) {
            case "create":
                return doCreate(args);
            case "list":
                return doList();
            case "cancel":
                return doCancel(args);
            default:
                throw new ToolException("无效的操作类型: " + action + "，可选: create, list, cancel");
        }
    }

    /**
     * 创建定时任务
     */
    private Object doCreate(JSONObject args) throws Exception {
        String content = args.getString("content");
        if (StringUtils.isBlank(content)) {
            throw new ToolException("任务内容 (content) 不能为空");
        }

        String scheduleType = args.getString("scheduleType");
        if (StringUtils.isBlank(scheduleType)) {
            throw new ToolException("调度类型 (scheduleType) 不能为空，可选: once, daily, weekly, monthly");
        }

        if (!"once".equals(scheduleType) && !"daily".equals(scheduleType)
                && !"weekly".equals(scheduleType) && !"monthly".equals(scheduleType)) {
            throw new ToolException("无效的调度类型: " + scheduleType + "，可选: once, daily, weekly, monthly");
        }

        final ID user = UserContextHolder.getUser();

        String time = args.getString("time");
        Integer dayOfWeek = args.getInteger("dayOfWeek");
        Integer dayOfMonth = args.getInteger("dayOfMonth");
        String executeTime = args.getString("executeTime");

        // 参数校验
        if ("once".equals(scheduleType)) {
            if (StringUtils.isBlank(executeTime)) {
                throw new ToolException("一次性任务必须指定执行时间 (executeTime)");
            }
        } else {
            if (StringUtils.isBlank(time)) {
                throw new ToolException("周期任务必须指定执行时间 (time)，格式 HH:mm，如 09:00");
            }
            if ("weekly".equals(scheduleType) && (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7)) {
                throw new ToolException("每周任务必须指定 dayOfWeek (1-7，1=周一，7=周日)");
            }
            if ("monthly".equals(scheduleType) && (dayOfMonth == null || dayOfMonth < 1 || dayOfMonth > 31)) {
                throw new ToolException("每月任务必须指定 dayOfMonth (1-31)");
            }
        }

        // 计算下次执行时间
        Date nextExecTime;
        if ("once".equals(scheduleType)) {
            nextExecTime = CommonsUtils.parseDate(executeTime);
            if (nextExecTime == null) {
                throw new ToolException("无法解析执行时间: " + executeTime + "，请使用 yyyy-MM-dd HH:mm:ss 格式");
            }
        } else {
            nextExecTime = calculateNextExecTime(scheduleType, time, dayOfWeek, dayOfMonth);
        }

        String subject = args.getString("subject");
        if (StringUtils.isBlank(subject)) {
            subject = CommonsUtils.maxstr(content, 40);
        }

        // 构建 config JSON
        JSONObject config = new JSONObject(true);
        config.put("userId", user.toLiteral());
        config.put("content", content);
        config.put("scheduleType", scheduleType);
        if (executeTime != null) config.put("executeTime", executeTime);
        if (time != null) config.put("time", time);
        if (dayOfWeek != null) config.put("dayOfWeek", dayOfWeek);
        if (dayOfMonth != null) config.put("dayOfMonth", dayOfMonth);
        config.put("status", "active");
        config.put("nextExecTime", CalendarUtils.getUTCDateTimeFormat().format(nextExecTime));
        config.put("lastExecTime", null);

        // 创建 AibotConfig 记录
        Record record = EntityHelper.forNew(EntityHelper.AibotConfig, user);
        record.setString("type", AibotConfigManager.TYPE_AIBOT_SCHEDULE);
        record.setString("name", subject);
        record.setString("config", config.toJSONString());

        record = Application.getBean(AibotConfigService.class).create(record);

        String taskId = record.getPrimary().toLiteral();
        String nextExecStr = CalendarUtils.getUTCDateTimeFormat().format(nextExecTime);
        String typeDesc = getScheduleTypeDesc(scheduleType, time, dayOfWeek, dayOfMonth);

        return JSONUtils.toJSONObject(
                new String[]{"status", "taskId", "nextExecTime", "message"},
                new Object[]{"ok", taskId, nextExecStr,
                        String.format("已成功创建定时任务 [%s]，%s，下次执行时间: %s",
                                subject, typeDesc, nextExecStr)});
    }

    /**
     * 查询当前用户的所有定时任务
     */
    private Object doList() {
        Object[][] array = Application.createQueryNoFilter(
                "select configId,config,name,isDisabled from AibotConfig where type = ? and createdBy = ? order by createdOn desc")
                .setParameter(1, AibotConfigManager.TYPE_AIBOT_SCHEDULE)
                .setParameter(2, UserContextHolder.getUser())
                .array();

        List<JSONObject> tasks = new ArrayList<>();
        for (Object[] row : array) {
            String taskId = ((ID) row[0]).toLiteral();
            String configStr = (String) row[1];
            String name = (String) row[2];
            boolean disabled = row[3] != null && (Boolean) row[3];

            JSONObject taskInfo = new JSONObject(true);
            taskInfo.put("taskId", taskId);
            taskInfo.put("subject", name);
            taskInfo.put("disabled", disabled);

            if (StringUtils.isNotBlank(configStr)) {
                try {
                    JSONObject config = JSON.parseObject(configStr);
                    taskInfo.put("scheduleType", config.getString("scheduleType"));
                    taskInfo.put("nextExecTime", config.getString("nextExecTime"));
                    taskInfo.put("lastExecTime", config.getString("lastExecTime"));
                    taskInfo.put("status", config.getString("status"));
                    taskInfo.put("content", CommonsUtils.maxstr(config.getString("content"), 60));

                    String time = config.getString("time");
                    Integer dayOfWeek = config.getInteger("dayOfWeek");
                    Integer dayOfMonth = config.getInteger("dayOfMonth");
                    taskInfo.put("scheduleDesc", getScheduleTypeDesc(
                            config.getString("scheduleType"), time, dayOfWeek, dayOfMonth));
                } catch (Exception e) {
                    log.warn("Failed to parse task config: {}", taskId, e);
                }
            }
            tasks.add(taskInfo);
        }

        return JSONUtils.toJSONObject(
                new String[]{"status", "count", "tasks"},
                new Object[]{"ok", tasks.size(), tasks});
    }

    /**
     * 取消定时任务
     */
    private Object doCancel(JSONObject args) throws Exception {
        ID taskId = ToolHelper.resolveId(args.getString("taskId"), "taskId");
        final ID user = UserContextHolder.getUser();

        // 验证任务存在
        Object[] task = Application.createQueryNoFilter(
                "select config,name,createdBy from AibotConfig where configId = ? and type = ?")
                .setParameter(1, taskId)
                .setParameter(2, AibotConfigManager.TYPE_AIBOT_SCHEDULE)
                .unique();

        if (task == null) {
            throw new ToolException("定时任务不存在或已删除: " + taskIdStr);
        }

        // 权限校验：仅创建者或管理员可取消
        ID createdBy = (ID) task[2];
        if (!user.equals(createdBy) && !UserHelper.isAdmin(user)) {
            throw new ToolException("无权取消他人的定时任务");
        }

        String name = (String) task[1];

        // 删除任务
        Application.getBean(AibotConfigService.class).delete(taskId);

        return JSONUtils.toJSONObject(
                new String[]{"status", "message"},
                new Object[]{"ok", String.format("已成功取消定时任务 [%s]", name)});
    }

    /**
     * 计算下次执行时间
     *
     * @param scheduleType once/daily/weekly/monthly
     * @param time         HH:mm
     * @param dayOfWeek    1-7 (1=周一，7=周日)
     * @param dayOfMonth   1-31
     * @return 下次执行时间
     */
    public static Date calculateNextExecTime(String scheduleType, String time, Integer dayOfWeek, Integer dayOfMonth) {
        String[] hm = time.split(":");
        int hour = Integer.parseInt(hm[0]);
        int minute = hm.length > 1 ? Integer.parseInt(hm[1]) : 0;

        Calendar cal = CalendarUtils.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);

        Calendar now = CalendarUtils.getInstance();

        switch (scheduleType) {
            case "daily":
                if (cal.before(now)) {
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
                break;

            case "weekly":
                // 1=周一 -> Calendar.MONDAY(2), ..., 7=周日 -> Calendar.SUNDAY(1)
                int targetDay = (dayOfWeek % 7) + 1;
                // 若时间已过，从明天开始找
                if (cal.before(now)) {
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
                while (cal.get(Calendar.DAY_OF_WEEK) != targetDay) {
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
                break;

            case "monthly":
                if (cal.before(now)) {
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
                int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                int targetDom = Math.min(dayOfMonth, maxDay);
                if (cal.get(Calendar.DAY_OF_MONTH) > targetDom) {
                    cal.add(Calendar.MONTH, 1);
                    maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                    targetDom = Math.min(dayOfMonth, maxDay);
                }
                cal.set(Calendar.DAY_OF_MONTH, targetDom);
                break;
        }

        return cal.getTime();
    }

    /**
     * 调度类型描述
     */
    static String getScheduleTypeDesc(String scheduleType, String time, Integer dayOfWeek, Integer dayOfMonth) {
        switch (scheduleType) {
            case "once":
                return "一次性执行";
            case "daily":
                return "每天 " + time + " 执行";
            case "weekly":
                String[] weekNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                return "每" + weekNames[dayOfWeek] + " " + time + " 执行";
            case "monthly":
                return "每月" + dayOfMonth + "日 " + time + " 执行";
            default:
                return scheduleType;
        }
    }
}

