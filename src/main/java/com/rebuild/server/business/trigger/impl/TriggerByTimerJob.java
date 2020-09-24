/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.business.trigger.ActionContext;
import com.rebuild.server.business.trigger.ActionFactory;
import com.rebuild.server.business.trigger.TriggerAction;
import com.rebuild.server.business.trigger.TriggerWhen;
import com.rebuild.server.configuration.RobotTriggerManager;
import com.rebuild.server.helper.DistributedJobBean;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.query.AdvFilterParser;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.JobExecutionException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 触发器定时执行，每分钟1次
 *
 * @author devezhao
 * @since 2020/5/27
 */
public class TriggerByTimerJob extends DistributedJobBean {

    /**
     * @throws JobExecutionException
     * @see RobotTriggerManager#getActions(Entity, TriggerWhen...)
     */
    @Override
    public void executeInternalSafe() throws JobExecutionException {
        final Calendar time = CalendarUtils.getInstance();
        final Object[][] timerTriggers = Application.createQueryNoFilter(
                "select when,whenTimer,whenFilter,belongEntity,actionType,actionContent,configId from RobotTriggerConfig" +
                        " where when >= 512 and whenTimer is not null and isDisabled = 'F' order by priority desc")
                .array();

        for (Object[] trigger : timerTriggers) {
            int when = (int) trigger[0];
            if ((when & TriggerWhen.TIMER.getMaskValue()) == 0) {
                continue;
            }

            String whenTimer = (String) trigger[1];
            if (StringUtils.isBlank(whenTimer) || !inTriggerTime(whenTimer, time)) {
                continue;
            }

            Application.getSessionStore().set(UserService.SYSTEM_USER);
            try {
                LOG.info("Trigger [ " + trigger[6] + " ] timer run at : " + time.getTime());
                int a = triggerOne(trigger);
                LOG.info("Trigger [ " + trigger[6] + " ] finished. Affected records : " + a);

            } catch (Exception ex) {
                LOG.error("Timer trigger error : " + trigger[6], ex);
            } finally {
                Application.getSessionStore().clean();
            }
        }
    }

    /**
     * @param trigger
     * @return
     */
    private int triggerOne(Object[] trigger) {
        Entity belongEntity = MetadataHelper.getEntity((String) trigger[3]);
        String sql = String.format("select %s from %s",
                belongEntity.getPrimaryField().getName(), belongEntity.getName());

        String whenFilter = (String) trigger[2];
        if (JSONUtils.wellFormat(whenFilter)) {
            String filterSql = new AdvFilterParser(belongEntity, JSON.parseObject(whenFilter)).toSqlWhere();
            if (filterSql != null) {
                sql += " where " + filterSql;
            }
        }
        sql += " order by modifiedOn desc";

        final String actionType = (String) trigger[4];
        final JSON actionContent = JSON.parseObject((String) trigger[5]);
        final ID configId = (ID) trigger[6];

        int pageNo = 1;
        int pageSize = 1000;

        int affected = 0;
        while (true) {
            Object[][] array = Application.createQueryNoFilter(sql)
                    .setLimit(pageSize, pageNo * pageSize - pageSize)
                    .array();

            for (Object[] o : array) {
                ActionContext ctx = new ActionContext((ID) o[0], belongEntity, actionContent, configId);
                TriggerAction triggerAction = ActionFactory.createAction(actionType, ctx);

                Record record = EntityHelper.forUpdate((ID) o[0], UserService.SYSTEM_USER, false);
                OperatingContext operatingContext = OperatingContext.create(UserService.SYSTEM_USER, BizzPermission.NONE, record, record);

                triggerAction.execute(operatingContext);
                affected++;
            }

            pageNo++;
            if (array.length < pageSize) {
                break;
            }
        }
        return affected;
    }

    /**
     * 是否在执行时间内
     *
     * @param whenTimer
     * @return
     */
    protected boolean inTriggerTime(String whenTimer, Calendar time) {
        String[] timerDef = whenTimer.split(":");
        if (timerDef.length != 2) return false;
        int times = ObjectUtils.toInt(timerDef[1], 1);

        final int hour = time.get(Calendar.HOUR_OF_DAY);
        final int minte = time.get(Calendar.MINUTE);
        final int day = time.get(Calendar.DAY_OF_MONTH);

        if ("H".equalsIgnoreCase(timerDef[0])) {
            return calcInTimes(60, times).contains(minte);
        } else if ("D".equalsIgnoreCase(timerDef[0]) && minte == 0) {
            // 仅 0分 执行
            return calcInTimes(24, times).contains(hour);
        } else if ("M".equalsIgnoreCase(timerDef[0]) && minte == 0 && hour == 0) {
            // 仅 0时0分 执行
            return calcInTimes(30, times).contains(day - 1);
        }
        return false;
    }

    private List<Integer> calcInTimes(int base, int times) {
        int interval = Math.max(base / times, 1);
        List<Integer> in = new ArrayList<>();
        for (int i = 0; i < base; i += interval) {
            in.add(i);
            if (in.size() >= times) break;
        }
        return in;
    }
}
