/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.support.setup.Installer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 通过 redis 实现的消息队列。
 * 第三方系统可以订阅（通过 pop）实现业务。
 *
 * @author ZHAO
 * @since 2020/4/6
 * @deprecated Use trigger HookUrl
 */
@Deprecated
public class RedisQueueObserver extends OperatingObserver {

    /**
     * 队列名称
     */
    protected static final String QUEUE_NAME = "rebuild:queue";

    // 是否使用发布/订阅模式
    private boolean useTopic = false;

    /**
     * @return
     */
    public boolean isUseTopic() {
        return useTopic;
    }

    /**
     * @param useTopic
     */
    public void setUseTopic(boolean useTopic) {
        this.useTopic = useTopic;
    }

    @Override
    protected void updateByAction(OperatingContext ctx) {
        if (!Installer.isUseRedis()) return;

        JSONObject data = new JSONObject();
        data.put("action", ctx.getAction().getName());
        data.put("recordId", ctx.getAnyRecord().getPrimary());
        if (ctx.getAfterRecord() != null) {
            data.put("recordData", ctx.getAfterRecord());
        }

        JedisPool pool = Application.getCommonsCache().getJedisPool();
        try (Jedis jedis = pool.getResource()) {
            if (isUseTopic()) {
                jedis.publish(QUEUE_NAME, data.toJSONString());
            } else {
                jedis.rpush(QUEUE_NAME, data.toJSONString());
            }
        }
    }

    @Override
    protected boolean isAsync() {
        return true;
    }
}
