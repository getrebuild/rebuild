/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

    /**
     * @param m 每分钟次数
     * @param h 每小时次数
     * @param d 每日次数
     * @return
     */
    public static RequestRateLimiter createRateLimiter(Integer m, Integer h, Integer d) {
        Set<RequestLimitRule> rules = new HashSet<>();
        if (m != null && m > 0) rules.add(RequestLimitRule.of(Duration.ofSeconds(60), m));
        if (h != null && h > 0) rules.add(RequestLimitRule.of(Duration.ofHours(1), h));
        if (d != null && d > 0) rules.add(RequestLimitRule.of(Duration.ofDays(1), d));

        return rules.isEmpty() ? NO_LIMITS : new InMemorySlidingWindowRequestRateLimiter(rules);
    }

    // 无规则时的不限流实例
    private static final RequestRateLimiter NO_LIMITS = new RequestRateLimiter() {
        @Override
        public boolean overLimitWhenIncremented(String key) {
            return false;
        }
        @Override
        public boolean overLimitWhenIncremented(String key, int weight) {
            return false;
        }
        @Override
        public boolean geLimitWhenIncremented(String key) {
            return false;
        }
        @Override
        public boolean geLimitWhenIncremented(String key, int weight) {
            return false;
        }
        @Override
        public boolean resetLimit(String key) {
            return false;
        }
    };
}
