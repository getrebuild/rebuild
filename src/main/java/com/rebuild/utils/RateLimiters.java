/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.rebuild.core.support.CommandArgs;
import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
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

    private static final Set<String> DISABLED = new HashSet<>();
    static {
        String v = CommandArgs.getString(CommandArgs._DisabledRateLimiters);
        if (StringUtils.isNotBlank(v)) {
            for (String name : v.split(",")) {
                if (StringUtils.isNotBlank(name)) DISABLED.add(name.trim());
            }
        }
        if (!DISABLED.isEmpty()) log.warn("Disabled rate limiters : {}", DISABLED);
    }

    // 登录 Token（基于用户）
    public static final RequestRateLimiter LOGIN_TOKEN_4USER = disableAware("LOGIN_TOKEN_4USER",
            new int[]{60, 600, 3600}, new int[]{5, 15, 30});
    // 注册验证码（基于邮箱）
    public static final RequestRateLimiter SIGNUP_4EMAIL = disableAware("SIGNUP_4EMAIL",
            new int[]{60, 600, 3600}, new int[]{2, 6, 12});
    // 注册验证码（基于 IP）
    public static final RequestRateLimiter SIGNUP_4IP = disableAware("SIGNUP_4IP",
            new int[]{60, 600, 3600}, new int[]{20, 60, 120});

    // API 网关（基于 IP）
    public static final RequestRateLimiter API_GATEWAY_4IP = disableAware("API_GATEWAY_4IP",
            new int[]{10, 2000}, new int[]{60, 6000});

    // 外部门户表单（基于 IP）
    public static final RequestRateLimiter EXTFORM_4IP = disableAware("EXTFORM_4IP",
            new int[]{60, 3600}, new int[]{20, 200});

    // MCP 网关（基于用户）
    public static final RequestRateLimiter MCP_4USER = disableAware("MCP_4USER",
            new int[]{36000}, new int[]{500});
    // 免登录 AI 对话（基于 IP，防止 Token 成本被恶意消耗）
    public static final RequestRateLimiter AIBOT_PUB_4IP = disableAware("AIBOT_PUB_4IP",
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

    private static RequestRateLimiter disableAware(String name, int[] seconds, int[] limits) {
        return new DisableAwareRateLimiter(name, createRateLimiter(seconds, limits));
    }

    /**
     * 命名限流器，类目出现在 _DisabledRateLimiters 中时直接放行
     */
    private static class DisableAwareRateLimiter implements RequestRateLimiter {
        private final String name;
        private final RequestRateLimiter delegate;

        DisableAwareRateLimiter(String name, RequestRateLimiter delegate) {
            this.name = name;
            this.delegate = delegate;
        }

        @Override
        public boolean overLimitWhenIncremented(String key) {
            return isDisabled() || delegate.overLimitWhenIncremented(key);
        }

        @Override
        public boolean overLimitWhenIncremented(String key, int weight) {
            return isDisabled() || delegate.overLimitWhenIncremented(key, weight);
        }

        @Override
        public boolean geLimitWhenIncremented(String key) {
            return isDisabled() || delegate.geLimitWhenIncremented(key);
        }

        @Override
        public boolean geLimitWhenIncremented(String key, int weight) {
            return isDisabled() || delegate.geLimitWhenIncremented(key, weight);
        }

        @Override
        public boolean resetLimit(String key) {
            return delegate.resetLimit(key);
        }

        boolean isDisabled() {
            return DISABLED.contains(name);
        }
    }
}
