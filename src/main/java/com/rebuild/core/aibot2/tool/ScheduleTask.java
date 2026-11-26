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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author devezhao
 * @since 2026/8/9
 */
@Slf4j
public class ScheduleTask implements Tool, PersonalGuard {

    private static final List<String> SCHEDULE_TYPES =
            Arrays.asList("once", "daily", "weekly", "monthly", "workdays");

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
            case "update":
                return doUpdate(args);
            case "pause":
                return doPause(args, true);
            case "resume":
                return doPause(args, false);
            case "list":
                return doList();
            case "cancel":
                return doCancel(args);
            default:
                throw new KnownToolException(
                        "无效的操作类型: " + action + "，可选: create, update, pause, resume, list, cancel");
        }
    }

    /**
     * 创建定时任务
     */
    private Object doCreate(JSONObject args) {
        String content = args.getString("content");
        if (StringUtils.isBlank(content)) {
            throw new KnownToolException("任务内容 (content) 不能为空");
        }

        String scheduleType = args.getString("scheduleType");
        if (StringUtils.isBlank(scheduleType)) {
            throw new KnownToolException("调度类型 (scheduleType) 不能为空，可选: once, daily, weekly, monthly, workdays");
        }
        checkScheduleType(scheduleType);

        String time = args.getString("time");
        Integer dayOfWeek = args.getInteger("dayOfWeek");
        Integer dayOfMonth = args.getInteger("dayOfMonth");
        String executeTime = args.getString("executeTime");

        Date nextExecTime = resolveNextExecTime(scheduleType, executeTime, time, dayOfWeek, dayOfMonth);

        String subject = args.getString("subject");
        if (StringUtils.isBlank(subject)) {
            subject = CommonsUtils.maxstr(content, 40);
        }

        final ID user = UserContextHolder.getUser();

        JSONObject config = new JSONObject(true);
        config.put("userId", user.toLiteral());
        config.put("content", content);
        config.put("scheduleType", scheduleType);
        if (executeTime != null) config.put("executeTime", executeTime);
        if (time != null) config.put("time", time);
        if (dayOfWeek != null) config.put("dayOfWeek", dayOfWeek);
        if (dayOfMonth != null) config.put("dayOfMonth", dayOfMonth);
        config.put("status", "active");
        config.put("nextExecTime", formatNextExecTime(nextExecTime));
        config.put("lastExecTime", null);

        Record record = EntityHelper.forNew(EntityHelper.AibotConfig, user);
        record.setString("type", AibotConfigManager.TYPE_AIBOT_SCHEDULE);
        record.setString("name", subject);
        record.setString("config", config.toJSONString());

        record = Application.getBean(AibotConfigService.class).create(record);

        String taskId = record.getPrimary().toLiteral();
        String nextExecStr = formatNextExecTime(nextExecTime);
        String typeDesc = getScheduleTypeDesc(scheduleType, time, dayOfWeek, dayOfMonth);

        return JSONUtils.toJSONObject(
                new String[]{"status", "id", "nextExecTime", "message"},
                new Object[]{"ok", taskId, nextExecStr,
                        String.format("已成功创建定时任务 [%s]，%s，下次执行时间: %s。到点后我会自动执行，结果通过站内信通知你",
                                subject, typeDesc, nextExecStr)});
    }

    /**
     * 修改定时任务
     */
    private Object doUpdate(JSONObject args) {
        final ID user = UserContextHolder.getUser();
        Object[] task = findSingleTask(user, args);

        ID taskId = (ID) task[0];
        String subject = (String) task[1];
        boolean paused = "paused".equalsIgnoreCase(getStatus(task[4]));

        JSONObject config = parseConfig(task[4]);

        String content = StringUtils.defaultIfBlank(args.getString("content"), config.getString("content"));
        if (StringUtils.isBlank(content)) {
            throw new KnownToolException("任务内容 (content) 不能为空");
        }

        String scheduleType = StringUtils.defaultIfBlank(args.getString("scheduleType"), config.getString("scheduleType"));
        checkScheduleType(scheduleType);

        String executeTime = args.containsKey("executeTime")
                ? args.getString("executeTime") : config.getString("executeTime");
        String time = args.containsKey("time") ? args.getString("time") : config.getString("time");
        Integer dayOfWeek = args.containsKey("dayOfWeek")
                ? args.getInteger("dayOfWeek") : config.getInteger("dayOfWeek");
        Integer dayOfMonth = args.containsKey("dayOfMonth")
                ? args.getInteger("dayOfMonth") : config.getInteger("dayOfMonth");

        Date nextExecTime = resolveNextExecTime(scheduleType, executeTime, time, dayOfWeek, dayOfMonth);

        String newSubject = StringUtils.defaultIfBlank(args.getString("subject"), subject);

        config.put("content", content);
        config.put("scheduleType", scheduleType);
        config.remove("executeTime");
        config.remove("time");
        config.remove("dayOfWeek");
        config.remove("dayOfMonth");
        if ("once".equals(scheduleType)) {
            config.put("executeTime", executeTime);
        } else {
            config.put("time", time);
            if ("weekly".equals(scheduleType)) config.put("dayOfWeek", dayOfWeek);
            if ("monthly".equals(scheduleType)) config.put("dayOfMonth", dayOfMonth);
        }
        config.put("nextExecTime", formatNextExecTime(nextExecTime));
        config.put("status", paused ? "paused" : "active");
        config.put("failCount", 0);
        config.remove("lastError");

        Record record = EntityHelper.forUpdate(taskId, user);
        record.setString("name", newSubject);
        record.setString("config", config.toJSONString());
        record.setBoolean("isDisabled", paused);
        Application.getBean(AibotConfigService.class).update(record);

        String typeDesc = getScheduleTypeDesc(scheduleType, time, dayOfWeek, dayOfMonth);
        return JSONUtils.toJSONObject(
                new String[]{"status", "id", "nextExecTime", "message"},
                new Object[]{"ok", taskId.toLiteral(), formatNextExecTime(nextExecTime),
                        String.format("已更新定时任务 [%s]，%s，下次执行时间: %s%s",
                                newSubject, typeDesc, formatNextExecTime(nextExecTime),
                                paused ? "。任务当前为暂停状态，恢复后才会执行" : "")});
    }

    /**
     * 暂停或恢复定时任务
     */
    private Object doPause(JSONObject args, boolean pause) {
        final ID user = UserContextHolder.getUser();
        Object[] task = findSingleTask(user, args);

        ID taskId = (ID) task[0];
        String subject = (String) task[1];
        boolean disabled = Boolean.TRUE.equals(task[3]);
        JSONObject config = parseConfig(task[4]);

        if (pause) {
            if (disabled && "paused".equalsIgnoreCase(config.getString("status"))) {
                return JSONUtils.toJSONObject(
                        new String[]{"status", "message"},
                        new Object[]{"ok", String.format("定时任务 [%s] 已处于暂停状态", subject)});
            }

            config.put("status", "paused");
            updateTask(taskId, user, config, true);
            return JSONUtils.toJSONObject(
                    new String[]{"status", "message"},
                    new Object[]{"ok", String.format("已暂停定时任务 [%s]，恢复后可继续按计划执行", subject)});
        }

        String scheduleType = config.getString("scheduleType");
        checkScheduleType(scheduleType);

        Date nextExecTime;
        if ("once".equals(scheduleType)) {
            if (StringUtils.isBlank(config.getString("executeTime"))) {
                throw new KnownToolException("该任务缺少一次性执行时间，请先通过 update 修改执行时间");
            }
            nextExecTime = CommonsUtils.parseDate(config.getString("executeTime"));
            if (nextExecTime == null) {
                throw new KnownToolException("无法解析一次性执行时间，请先通过 update 修改执行时间");
            }
            if (!nextExecTime.after(CalendarUtils.getInstance().getTime())) {
                throw new KnownToolException("该一次性任务的执行时间已过，请先通过 update 修改为未来时间");
            }
        } else {
            nextExecTime = resolveNextExecTime(scheduleType, null,
                    config.getString("time"), config.getInteger("dayOfWeek"), config.getInteger("dayOfMonth"));
        }

        config.put("status", "active");
        config.put("nextExecTime", formatNextExecTime(nextExecTime));
        config.put("failCount", 0);
        config.remove("lastError");
        updateTask(taskId, user, config, false);

        String nextExecStr = formatNextExecTime(nextExecTime);
        return JSONUtils.toJSONObject(
                new String[]{"status", "nextExecTime", "message"},
                new Object[]{"ok", nextExecStr,
                        String.format("已恢复定时任务 [%s]，下次执行时间: %s", subject, nextExecStr)});
    }

    /**
     * 查询当前用户的所有定时任务
     */
    private Object doList() {
        Object[][] array = queryUserTasks(UserContextHolder.getUser());

        List<JSONObject> tasks = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            Object[] row = array[i];
            String taskId = ((ID) row[0]).toLiteral();
            String configStr = (String) row[1];
            String name = (String) row[2];
            boolean disabled = row[3] != null && (Boolean) row[3];

            JSONObject taskInfo = new JSONObject(true);
            taskInfo.put("no", i + 1);
            taskInfo.put("subject", name);
            taskInfo.put("disabled", disabled);

            if (StringUtils.isNotBlank(configStr)) {
                try {
                    JSONObject config = JSON.parseObject(configStr);
                    taskInfo.put("scheduleType", config.getString("scheduleType"));
                    taskInfo.put("nextExecTime", config.getString("nextExecTime"));
                    taskInfo.put("lastExecTime", config.getString("lastExecTime"));
                    taskInfo.put("lastResult", config.getString("lastResult"));
                    taskInfo.put("lastError", config.getString("lastError"));
                    taskInfo.put("status", config.getString("status"));
                    taskInfo.put("content", config.getString("content"));

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
     * 取消定时任务（按序号或任务 ID 定位）。
     * no 支持单个序号或序号数组，传多个时先按同一份列表快照解析全部序号再统一删除，
     * 避免删除过程中序号重排导致后续序号失效
     */
    private Object doCancel(JSONObject args) {
        final ID user = UserContextHolder.getUser();

        List<ID> taskIds = new ArrayList<>();
        List<Integer> nos = ToolHelper.resolveNos(args.get("no"));
        if (!nos.isEmpty()) {
            Object[][] array = queryUserTasks(user);
            for (Integer no : nos) {
                if (no < 1 || no > array.length) {
                    throw new KnownToolException(String.format(
                            "序号 %d 无效（当前共 %d 个定时任务），请先通过 action=list 查看序号", no, array.length));
                }
                ID id = (ID) array[no - 1][0];
                if (!taskIds.contains(id)) taskIds.add(id);
            }
        } else {
            taskIds.add(resolveTaskId(args));
        }

        // 先完成全部存在性与权限校验再统一删除，避免批量中途中断造成部分已删除
        List<Object[]> tasks = new ArrayList<>();
        for (ID taskId : taskIds) {
            Object[] task = queryTask(taskId);

            if (task == null) {
                if (taskIds.size() == 1) {
                    throw new KnownToolException("定时任务不存在或已删除: " + taskId);
                }
                continue;
            }

            // 权限校验：仅创建者或管理员可取消
            ID createdBy = (ID) task[2];
            if (!user.equals(createdBy) && !UserHelper.isAdmin(user)) {
                throw new KnownToolException("无权取消他人的定时任务");
            }
            tasks.add(task);
        }

        if (tasks.isEmpty()) {
            throw new KnownToolException("定时任务不存在或已删除");
        }

        List<String> names = new ArrayList<>();
        for (Object[] task : tasks) {
            Application.getBean(AibotConfigService.class).delete((ID) task[0]);
            names.add((String) task[1]);
        }

        String message;
        if (names.size() == 1) {
            message = String.format("已成功取消定时任务 [%s]", names.get(0));
        } else {
            StringBuilder joined = new StringBuilder();
            for (String name : names) {
                if (joined.length() > 0) joined.append("、");
                joined.append("[").append(name).append("]");
            }
            message = String.format("已成功取消 %d 个定时任务：%s", names.size(), joined);
        }

        return JSONUtils.toJSONObject(
                new String[]{"status", "message"},
                new Object[]{"ok", message});
    }

    /**
     * 按 no 或 taskId 定位单个任务并校验权限
     */
    private Object[] findSingleTask(ID user, JSONObject args) {
        List<Integer> nos = ToolHelper.resolveNos(args.get("no"));
        if (nos.size() > 1) {
            throw new KnownToolException("一次只能操作一个定时任务，请分别调用");
        }

        ID taskId;
        if (nos.size() == 1) {
            Object[][] array = queryUserTasks(user);
            int no = nos.get(0);
            if (no < 1 || no > array.length) {
                throw new KnownToolException(String.format(
                        "序号 %d 无效（当前共 %d 个定时任务），请先通过 action=list 查看序号", no, array.length));
            }
            taskId = (ID) array[no - 1][0];
        } else {
            taskId = resolveTaskId(args);
        }

        Object[] task = queryTask(taskId);
        if (task == null) {
            throw new KnownToolException("定时任务不存在或已删除: " + taskId);
        }

        ID createdBy = (ID) task[2];
        if (!user.equals(createdBy) && !UserHelper.isAdmin(user)) {
            throw new KnownToolException("无权操作他人的定时任务");
        }
        return task;
    }

    private ID resolveTaskId(JSONObject args) {
        String taskId = args.getString("taskId");
        if (StringUtils.isBlank(taskId)) {
            throw new KnownToolException("请提供任务序号 (no) 或任务 ID (taskId)，可先通过 action=list 查看");
        }
        return ToolHelper.resolveId(taskId, "taskId", EntityHelper.AibotConfig);
    }

    /**
     * 查询单个任务（含配置）
     */
    private Object[] queryTask(ID taskId) {
        return Application.createQueryNoFilter(
                "select configId,name,createdBy,isDisabled,config from AibotConfig where configId = ? and type = ?")
                .setParameter(1, taskId)
                .setParameter(2, AibotConfigManager.TYPE_AIBOT_SCHEDULE)
                .unique();
    }

    private void updateTask(ID taskId, ID user, JSONObject config, boolean disabled) {
        Record record = EntityHelper.forUpdate(taskId, user);
        record.setString("config", config.toJSONString());
        record.setBoolean("isDisabled", disabled);
        Application.getBean(AibotConfigService.class).update(record);
    }

    private JSONObject parseConfig(Object configStr) {
        JSONObject config = configStr == null ? null : JSON.parseObject(configStr.toString());
        return config == null ? new JSONObject(true) : config;
    }

    private String getStatus(Object configStr) {
        JSONObject config = parseConfig(configStr);
        return config.getString("status");
    }

    /**
     * 查询当前用户的定时任务（按创建时间由新到旧，序号与 list 结果一致）
     *
     * @param user
     * @return
     */
    private Object[][] queryUserTasks(ID user) {
        return Application.createQueryNoFilter(
                "select configId,config,name,isDisabled from AibotConfig where type = ? and createdBy = ? order by createdOn desc")
                .setParameter(1, AibotConfigManager.TYPE_AIBOT_SCHEDULE)
                .setParameter(2, user)
                .array();
    }

    private void checkScheduleType(String scheduleType) {
        if (!SCHEDULE_TYPES.contains(scheduleType)) {
            throw new KnownToolException(
                    "无效的调度类型: " + scheduleType + "，可选: once, daily, weekly, monthly, workdays");
        }
    }

    /**
     * 校验时间参数并计算下次执行时间
     */
    private Date resolveNextExecTime(String scheduleType, String executeTime, String time,
                                     Integer dayOfWeek, Integer dayOfMonth) {
        if ("once".equals(scheduleType)) {
            if (StringUtils.isBlank(executeTime)) {
                throw new KnownToolException("一次性任务必须指定执行时间 (executeTime)");
            }
            Date nextExecTime = CommonsUtils.parseDate(executeTime);
            if (nextExecTime == null) {
                throw new KnownToolException("无法解析执行时间: " + executeTime + "，请使用 yyyy-MM-dd HH:mm:ss 格式");
            }
            if (!nextExecTime.after(CalendarUtils.getInstance().getTime())) {
                throw new KnownToolException("一次性任务的执行时间必须是未来时间 : " + executeTime);
            }
            return nextExecTime;
        }

        if (StringUtils.isBlank(time)) {
            throw new KnownToolException("周期任务必须指定执行时间 (time)，格式 HH:mm，如 09:00");
        }
        if ("weekly".equals(scheduleType) && (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7)) {
            throw new KnownToolException("每周任务必须指定 dayOfWeek (1-7，1=周一，7=周日)");
        }
        if ("monthly".equals(scheduleType) && (dayOfMonth == null || dayOfMonth < 1 || dayOfMonth > 31)) {
            throw new KnownToolException("每月任务必须指定 dayOfMonth (1-31)");
        }
        return calculateNextExecTime(scheduleType, time, dayOfWeek, dayOfMonth);
    }

    private String formatNextExecTime(Date date) {
        return CalendarUtils.getUTCDateTimeFormat().format(date);
    }

    /**
     * 计算下次执行时间
     *
     * @param scheduleType once/daily/weekly/monthly/workdays
     * @param time         HH:mm
     * @param dayOfWeek    1-7 (1=周一，7=周日)
     * @param dayOfMonth   1-31
     * @return 下次执行时间
     */
    public static Date calculateNextExecTime(String scheduleType, String time, Integer dayOfWeek, Integer dayOfMonth) {
        int hour;
        int minute;
        try {
            String[] hm = time.split(":");
            hour = Integer.parseInt(hm[0].trim());
            minute = hm.length > 1 ? Integer.parseInt(hm[1].trim()) : 0;
        } catch (NumberFormatException ex) {
            throw new KnownToolException("无法解析执行时间: " + time + "，请使用 HH:mm 格式，如 09:00");
        }
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new KnownToolException("执行时间超出范围: " + time + "，请使用 HH:mm 格式，如 09:00");
        }

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

            case "workdays":
                if (cal.before(now)) {
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
                while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                        || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
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
                int delta = (targetDay - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7;
                cal.add(Calendar.DAY_OF_MONTH, delta);
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
     * 调度类型描述。list 路径读的是历史 config，可能缺少部分键，因此需自防御避免 NPE / 数组越界
     */
    static String getScheduleTypeDesc(String scheduleType, String time, Integer dayOfWeek, Integer dayOfMonth) {
        if (StringUtils.isBlank(scheduleType)) return "未知调度类型";
        time = StringUtils.defaultIfBlank(time, "?");
        switch (scheduleType) {
            case "once":
                return "一次性执行";
            case "daily":
                return "每天 " + time + " 执行";
            case "workdays":
                return "每个工作日 " + time + " 执行";
            case "weekly": {
                String[] weekNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                if (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7) return "每周 " + time + " 执行";
                return "每" + weekNames[dayOfWeek] + " " + time + " 执行";
            }
            case "monthly":
                return "每月" + (dayOfMonth == null ? "?" : dayOfMonth) + "日 " + time + " 执行";
            default:
                return "未知调度类型 : " + scheduleType;
        }
    }
}
