/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.commons.ThreadPool;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.support.setup.Installer;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.List;

/**
 * @author ZHAO
 * @since 2020/4/6
 */
public class RedisQueueObserverTest extends TestSupport {

    @Test
    public void testProducer() {
        if (!Installer.isUseRedis()) return;

        ThreadPool.submit(new MessageProducer(true));

        // 测试发送
        addRecordOfTestAllFields(SIMPLE_USER);
        addRecordOfTestAllFields(SIMPLE_USER);
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
            JedisPool pool = Application.getCommonsCache().getJedisPool();

            try (Jedis jedis = pool.getResource()) {
                if (useTopic) {
                    jedis.subscribe(new TopicProducer(), RedisQueueObserver.QUEUE_NAME);
                    System.out.println("subscribe ...");
                } else {
                    int reviced = 0;
                    while (reviced < 2) {
                        // BLOCK ...
                        List<String> datas = jedis.blpop(0, RedisQueueObserver.QUEUE_NAME);
                        System.out.println("Reviced queue data : " + datas);
                        reviced++;
                    }
                }
            }
        }
    }

    /**
     * 发布/订阅模式
     *
     * @see RedisQueueObserver#isUseTopic()
     */
    static class TopicProducer extends JedisPubSub {

        @Override
        public void onMessage(String channel, String data) {
            if ("QUIT".equalsIgnoreCase(data)) {
                unsubscribe(RedisQueueObserver.QUEUE_NAME);
                return;
            }

            System.out.println("Reviced topic data : " + channel + " > " + data);
        }
    }
}