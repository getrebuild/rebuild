/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.support.i18n.Language;

import java.util.Calendar;
import java.util.Date;

/**
 * 智能体 token 限额（日/月，按智能体全局计量）
 *
 * @author Zixin
 * @since 4.6
 */
public class AgentQuota {

    private static final String CK_PREFIX = "AgentQuota-";

    /**
     * 检查智能体是否超出限额
     *
     * @param agent
     * @return 超限提示文案
     */
    public static String check(AibotAgent agent) {
        if (agent == null || agent.getAgentId() == null) return null;

        Long dailyLimit = agent.dailyTokenLimit();
        Long monthlyLimit = agent.monthlyTokenLimit();
        if (dailyLimit == null && monthlyLimit == null) return null;

        long[] usage = usage(agent.getAgentId());
        if (dailyLimit != null && usage[0] >= dailyLimit) {
            return Language.L("今日用量已达限额");
        }
        if (monthlyLimit != null && usage[1] >= monthlyLimit) {
            return Language.L("本月用量已达限额");
        }
        return null;
    }

    /**
     * 智能体当期用量（结果缓存 1 分钟，check 与编辑页展示共用）
     *
     * @param agentId
     * @return [0]=今日 [1]=本月，按会话创建时间归属（与 stats 统计口径一致）
     */
    public static long[] usage(ID agentId) {
        if (agentId == null) return new long[]{0, 0};

        long[] cached = (long[]) Application.getCommonsCache().getx(CK_PREFIX + agentId);
        if (cached != null) return cached;

        Calendar month = CalendarUtils.getInstance();
        month.set(Calendar.DAY_OF_MONTH, 1);

        long todayUsage = sumToken(agentId, CalendarUtils.clearTime(CalendarUtils.now()));
        long monthUsage = sumToken(agentId, CalendarUtils.clearTime(month.getTime()));

        long[] usage = new long[]{todayUsage, monthUsage};
        Application.getCommonsCache().putx(CK_PREFIX + agentId, usage, CommonsCache.TS_MINTE);
        return usage;
    }

    private static long sumToken(ID agentId, Date since) {
        Object[] o = Application.createQueryNoFilter(
                "select sum(token) from AibotChat where agentId = ? and createdOn > ?")
                .setParameter(1, agentId)
                .setParameter(2, since)
                .unique();
        return (o == null || o[0] == null) ? 0L : ((Number) o[0]).longValue();
    }
}
