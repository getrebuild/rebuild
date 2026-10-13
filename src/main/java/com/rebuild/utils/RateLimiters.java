/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * 统一限流规则
 *
 * @author ZHAO
 * @since 2020/4/29
 */
public class RateLimiters {

    // 登录 Token（基于用户）
    public static final RequestRateLimiter LOGIN_TOKEN_4USER = createRateLimiter(
            new int[]{60, 600, 3600}, new int[]{5, 15, 30});
    // 注册验证码（基于邮箱）
    public static final RequestRateLimiter SIGNUP_4EMAIL = createRateLimiter(
            new int[]{60, 600, 3600}, new int[]{2, 6, 12});
    // 注册验证码（基于 IP）
    public static final RequestRateLimiter SIGNUP_4IP = createRateLimiter(
            new int[]{60, 600, 3600}, new int[]{20, 60, 120});

    // API 网关（基于 IP）
    public static final RequestRateLimiter API_GATEWAY_4IP = createRateLimiter(
            new int[]{10, 2000}, new int[]{60, 6000});

    // 外部门户表单（基于 IP）
    public static final RequestRateLimiter EXTFORM_4IP = createRateLimiter(
            new int[]{60, 3600}, new int[]{20, 200});

    // MCP 网关（基于用户）
    public static final RequestRateLimiter MCP_4USER = createRateLimiter(
            new int[]{36000}, new int[]{500});
    // 免登录 AI 对话（基于 IP，防止 Token 成本被恶意消耗）
    public static final RequestRateLimiter AIBOT_PUB_4IP = createRateLimiter(
            new int[]{60, 3600}, new int[]{20, 200});

    /**
     * @param seconds
     * @param limit
     * @return
     */
    public static RequestRateLimiter createRateLimiter(int seconds, int limit) {
        return createRateLimiter(new int[]{seconds}, new int[]{limit});
    }

    /**
     * @param seconds
     * @param limits
     * @return
     */
    public static RequestRateLimiter createRateLimiter(int[] seconds, int[] limits) {
        Assert.isTrue(seconds.length == limits.length, "Rule pair does not match");

        Set<RequestLimitRule> rules = new HashSet<>();
        for (int i = 0; i < seconds.length; i++) {
            rules.add(RequestLimitRule.of(Duration.ofSeconds(seconds[i]), limits[i]));
        }
        return new InMemorySlidingWindowRequestRateLimiter(rules);
    }
}
