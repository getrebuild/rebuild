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
     * @param seconds
     * @param limit
     * @return
     */
    public static RequestRateLimiter createRateLimiter(int seconds, int limit) {
        Set<RequestLimitRule> rules = Collections.singleton(
                RequestLimitRule.of(Duration.ofSeconds(seconds), limit));
        return new InMemorySlidingWindowRequestRateLimiter(rules);
    }

    /**
     * @param seconds
     * @param limits
     * @return
     */
    public static RequestRateLimiter createRateLimiter(int[] seconds, int[] limits) {
        Assert.isTrue(seconds.length == limits.length, "Rule pair not matchs");

        Set<RequestLimitRule> rules = new HashSet<>();
        for (int i = 0; i < seconds.length; i++) {
            rules.add(RequestLimitRule.of(Duration.ofSeconds(seconds[i]), limits[i]));
        }
        return new InMemorySlidingWindowRequestRateLimiter(rules);
    }
}
