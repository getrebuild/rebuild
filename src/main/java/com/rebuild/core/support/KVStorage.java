/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.BootEnvironmentPostProcessor;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * K/V 对存储
 *
 * @author devezhao
 * @since 2019/11/22
 */
@Slf4j
public class KVStorage {

    public static final Object SETNULL = new Object();

    private static final String CUSTOM_PREFIX = "custom.";

    /**
     * 取
     * @param key 会自动加 `custom.` 前缀
     * @return
     */
    public static String getCustomValue(String key) {
        return getValue(CUSTOM_PREFIX + key, false, null);
    }

    /**
     * 存
     * @param key
     * @param value
     */
    public static void setCustomValue(String key, Object value) {
        setValue(CUSTOM_PREFIX + key, value);
    }

    /**
     * 存（异步）
     * @param key
     * @param value
     * @param throttled 是否节流
     */
    public static void setCustomValue(String key, Object value, boolean throttled) {
        if (throttled) {
            synchronized (THROTTLED_QUEUE_LOCK) {
                THROTTLED_QUEUE.put(key, value);
            }
        } else {
            setCustomValue(key, value);
        }
    }

    /**
     * 删
     * @param key
     */
    public static void removeCustomValue(String key) {
        setCustomValue(key, SETNULL);
    }

    // -- RAW

    /**
     * @param key
     * @param value
     */
    protected static void setValue(final String key, Object value) {
        Object[] exists = Application.createQueryNoFilter(
                "select configId from SystemConfig where item = ?")
                .setParameter(1, key)
                .unique();

        // 删除
        if (value == SETNULL) {
            if (exists != null) {
                Application.getCommonsService().delete((ID) exists[0]);
                Application.getCommonsCache().evict(key);
            }
            return;
        }

        Record record;
        if (exists == null) {
            record = EntityHelper.forNew(EntityHelper.SystemConfig, UserService.SYSTEM_USER, false);
            record.setString("item", key);
        } else {
            record = EntityHelper.forUpdate((ID) exists[0], UserService.SYSTEM_USER, false);
        }
        record.setString("value", String.valueOf(value));

        Application.getCommonsService().createOrUpdate(record);
        Application.getCommonsCache().evict(key);
    }

    /**
     * @param key
     * @param noCache
     * @param defaultValue
     * @return
     */
    protected static String getValue(final String key, boolean noCache, Object defaultValue) {
        String value = null;

        if (Application.isReady()) {
            // 0. 从缓存
            if (!noCache) {
                value = Application.getCommonsCache().get(key);
                if (value != null) {
                    return value;
                }
            }

            // 1. 从数据库
            Object[] fromDb = Application.createQueryNoFilter(
                    "select value from SystemConfig where item = ?")
                    .setParameter(1, key)
                    .unique();
            value = fromDb == null ? null : StringUtils.defaultIfBlank((String) fromDb[0], null);
        }

        // 2. 从配置文件/命令行加载
        if (value == null) {
            value = BootEnvironmentPostProcessor.getProperty(key);
        }

        // 3. 默认值
        if (value == null && defaultValue != null) {
            value = defaultValue.toString();
        }

        if (Application.isReady()) {
            if (value == null) {
                Application.getCommonsCache().evict(key);
            } else {
                Application.getCommonsCache().put(key, value);
            }
        }

        return value;
    }

    // -- ASYNC

    private static final Object THROTTLED_QUEUE_LOCK = new Object();
    private static final Map<String, Object> THROTTLED_QUEUE = new ConcurrentHashMap<>();
    private static final Timer THROTTLED_TIMER = new Timer("KVStorage-Timer");

    static {
        final TimerTask localTimerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    synchronized (THROTTLED_QUEUE_LOCK) {
                        if (THROTTLED_QUEUE.isEmpty()) return;

                        final Map<String, Object> queue = new HashMap<>(THROTTLED_QUEUE);
                        THROTTLED_QUEUE.clear();

                        log.info("Synchronize KV pairs ... {}", queue);
                        for (Map.Entry<String, Object> e : queue.entrySet()) {
                            RebuildConfiguration.setCustomValue(e.getKey(), e.getValue());
                        }
                    }
                } catch (Throwable ex) {
                    log.error(null, ex);
                }
            }
        };

        THROTTLED_TIMER.scheduleAtFixedRate(localTimerTask, 2000, 2000);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("The KVStorage shutdown hook is enabled");
            localTimerTask.run();
        }));
    }
}
