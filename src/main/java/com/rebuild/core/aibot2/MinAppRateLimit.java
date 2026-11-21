/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.RateLimiters;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微应用免登录访问限流（分钟/小时，按 IP，防止恶意刷量）
 *
 * @author Zixin
 * @since 4.6
 */
public class MinAppRateLimit {

    // 相同规则的微应用共用限流器，访问计数按 页面+IP 隔离
    private static final Map<String, RequestRateLimiter> LIMITERS = new ConcurrentHashMap<>();

    /**
     * 免登录访问频率检查（按 IP）
     *
     * @param pageId
     * @param config
     * @param remoteIp
     * @return true 表示超出频率限制
     */
    public static boolean overLimit(ID pageId, JSONObject config, String remoteIp) {
        if (pageId == null || config == null) return false;

        Integer minuteLimit = limitOf(config, "minuteVisitLimit");
        Integer hourLimit = limitOf(config, "hourVisitLimit");
        if (minuteLimit == null && hourLimit == null) return false;

        RequestRateLimiter limiter = LIMITERS.computeIfAbsent(
                minuteLimit + ":" + hourLimit, k -> RateLimiters.createRateLimiter(minuteLimit, hourLimit, null));
        return limiter.overLimitWhenIncremented(pageId + ":" + remoteIp);
    }

    private static Integer limitOf(JSONObject config, String key) {
        Integer v = config.getInteger(key);
        return (v != null && v > 0) ? v : null;
    }
}
