/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 限流工具
 *
 * @author ZHAO
 * @since 2020/4/29
 */
public class RateLimiters {

    /**
     * 1 分钟
     */
    public static final long MINUTE = 60;

    /**
     * 1 小时
     */
    public static final long HOUR = MINUTE * 60;

    /**
     * for 登陆
     */
    public static final RequestRateLimiter RRL_LOGIN = createRateLimiter(
            new long[] { 30, RateLimiters.MINUTE, RateLimiters.HOUR },
            new int[]  {  5, 10, 30 });

    /**
     * @param seconds
     * @param limit
     * @return
     */
    public static RequestRateLimiter createRateLimiter(long seconds, int limit) {
        Set<RequestLimitRule> rules = Collections.singleton(RequestLimitRule.of(Duration.ofSeconds(seconds), limit));
        return new InMemorySlidingWindowRequestRateLimiter(rules);
    }

    /**
     * @param seconds
     * @param limit
     * @return
     */
    public static RequestRateLimiter createRateLimiter(long[] seconds, int[] limit) {
        Assert.isTrue(seconds.length == limit.length, "Rule pair not matchs");

        Set<RequestLimitRule> rules = new HashSet<>();
        for (int i = 0; i < seconds.length; i++) {
            rules.add(RequestLimitRule.of(Duration.ofSeconds(seconds[i]), limit[i]));
        }
        return new InMemorySlidingWindowRequestRateLimiter(rules);
    }
}
