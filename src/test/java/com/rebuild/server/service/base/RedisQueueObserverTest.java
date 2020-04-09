/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.base;

import cn.devezhao.commons.ThreadPool;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupportWithUser;
import com.rebuild.server.helper.cache.JedisCacheDriver;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.List;

/**
 * @author ZHAO
 * @since 2020/4/6
 */
public class RedisQueueObserverTest extends TestSupportWithUser {

    @Test
    public void testProducer() {
        if (!Application.getCommonCache().isUseRedis()) {
            return;
        }

        ThreadPool.submit(new MessageProducer(true));

        // 测试发送
        addRecordOfTestAllFields();
        addRecordOfTestAllFields();
    }

    /**
     * 队列消息消费者
     */
    static class MessageProducer implements Runnable {

        private boolean useTopic;

        protected MessageProducer(boolean useTopic) {
            this.useTopic = useTopic;
        }

        @Override
        public void run() {
            @SuppressWarnings("rawtypes")
            JedisPool pool = ((JedisCacheDriver) Application.getCommonCache().getCacheTemplate()).getJedisPool();

            try (Jedis jedis = pool.getResource()) {
                if (useTopic) {
                    jedis.subscribe(new TopicProducer(), RedisQueueObserver.QUEUE_NAME);
                    System.out.println("subscribe ...");
                }
                else {
                    while (true) {
                        // BLOCK ...
                        List<String> datas = jedis.blpop(0, RedisQueueObserver.QUEUE_NAME);
                        System.out.println("Reviced queue data : " + datas);
                    }
                }

            }
        }
    }

    /**
     * 发布/订阅模式
     * @see RedisQueueObserver#isUseTopic()
     */
    static class TopicProducer extends JedisPubSub {

        @Override
        public void onMessage(String channel, String data) {
            if("QUIT".equalsIgnoreCase(data)) {
                unsubscribe(RedisQueueObserver.QUEUE_NAME);
                return;
            }

            System.out.println("Reviced topic data : " + channel + " > " + data);
        }
    }
}