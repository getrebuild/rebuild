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
import com.rebuild.core.service.TransactionManual;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.TransactionStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import static com.rebuild.core.support.ConfigurationItem.DataDirectory;
import static com.rebuild.core.support.ConfigurationItem.RedisDatabase;

/**
 * K/V 对存储
 *
 * @author devezhao
 * @since 2019/11/22
 */
@Slf4j
public class KVStorage {

    // 设为空值
    public static final Object SETNULL = new Object();
    // Key 前缀
    private static final String CUSTOM_PREFIX = "custom.";

    /**
     * @param key 会自动加 `custom.` 前缀
     * @return
     */
    public static String getCustomValue(String key) {
        return getValue(CUSTOM_PREFIX + key, false, null);
    }

    /**
     * @param key
     * @param value
     */
    public static void setCustomValue(String key, Object value) {
        setValue(CUSTOM_PREFIX + key, value);
    }

    /**
     * 异步存。注意`非关键值`再使用此方法（因为此方案存在值丢失隐患）
     *
     * @param key
     * @param value
     */
    public static void setCustomValueAsync(String key, Object value) {
        synchronized (THROTTLED_QUEUE_LOCK) {
            THROTTLED_QUEUE.put(CUSTOM_PREFIX + key, value);
        }
    }

    /**
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
    synchronized
    protected static void setValue(final String key, Object value) {
        final Object[] e = Application.createQueryNoFilter(
                "select configId,value from SystemConfig where item = ?")
                .setParameter(1, key)
                .unique();

        // 删除
        if (value == SETNULL) {
            if (e != null) {
                Application.getCommonsService().delete((ID) e[0]);
                Application.getCommonsCache().evict(key);
            }
            return;
        }

        final Object previousValue = e == null || e[0] == null ? SETNULL : e[1];

        Record kv;
        if (e == null) {
            kv = EntityHelper.forNew(EntityHelper.SystemConfig, UserService.SYSTEM_USER, false);
            kv.setString("item", key);
        } else {
            kv = EntityHelper.forUpdate((ID) e[0], UserService.SYSTEM_USER, false);
        }
        kv.setString("value", String.valueOf(value));

        // v4.4 使用单独事务，避免锁死影响其他
        TransactionStatus tx = TransactionManual.newTransaction(false);
        try {
            Application.getCommonsService().createOrUpdate(kv);
            TransactionManual.commit(tx);
        } catch (Throwable ex) {
            TransactionManual.rollback(tx);
            throw ex;
        }
        Application.getCommonsCache().put(key, String.valueOf(value));

        // v4.4 若失败了则还原
        TransactionManual.registerAfterRollback(() -> {
            log.error("Rollback KV : {} -> {}", key, previousValue);
            setValue(key, previousValue);
        });
    }

    /**
     * @param key
     * @param noCache 慎用!!!
     * @param defaultValue
     * @return
     */
    protected static String getValue(final String key, boolean noCache, Object defaultValue) {
        String value = null;

        // 0.1. 从命令行
        if (ConfigurationItem.SN.name().equals(key)) {
            value = BootEnvironmentPostProcessor.getProperty(key);
        } else if (ConfigurationItem.inJvmArgs(key)) {
            if (DataDirectory.name().equalsIgnoreCase(key) || RedisDatabase.name().equalsIgnoreCase(key)) {
                return BootEnvironmentPostProcessor.getProperty(key);
            }
            return CommandArgs.getStringWithBootEnvironmentPostProcessor(key);
        }

        if (Application.isStateReady()) {
            // 1.0. 从缓存
            value = Application.getCommonsCache().get(key);
            if (noCache) value = null;
            if (value != null) return value;

            // 1.1. 从数据库
            Object[] fromDb = Application.createQueryNoFilter(
                    "select value from SystemConfig where item = ?")
                    .setParameter(1, key)
                    .unique();
            value = fromDb == null ? null : StringUtils.defaultIfBlank((String) fromDb[0], null);

            // 1.2. 从异步池
            if (value == null && THROTTLED_QUEUE.containsKey(key)) {
                value = String.valueOf(THROTTLED_QUEUE.get(key));
            }
        }

        // 2. 从配置文件/命令行加载
        if (value == null) {
            value = BootEnvironmentPostProcessor.getProperty(key);
        }

        // 3. 默认值
        if (value == null && defaultValue != null) {
            value = defaultValue.toString();
        }

        // 10. 存如缓存
        if (Application.isStateReady() && value != null) {
            Application.getCommonsCache().put(key, value);
        }

        return value;
    }

    // -- ASYNC 同步K/V值到数据库。注意如果系统异常停止可能导致同步数据丢失!!!

    private static final Object THROTTLED_QUEUE_LOCK = new Object();
    private static final Map<String, Object> THROTTLED_QUEUE = new ConcurrentHashMap<>();
    private static final Timer THROTTLED_TIMER = new Timer("KVStorage-Syncer");

    static {
        final TimerTask localTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (THROTTLED_QUEUE.isEmpty()) return;

                synchronized (THROTTLED_QUEUE_LOCK) {
                    final Map<String, Object> queue = new HashMap<>(THROTTLED_QUEUE);
                    THROTTLED_QUEUE.clear();

                    log.info("Synchronize KV pairs ... {}", queue);
                    for (Map.Entry<String, Object> e : queue.entrySet()) {
                        try {
                            setCustomValue(e.getKey(), e.getValue());
                        } catch (Throwable ex) {
                            log.error("Synchronize KV error : {}", e, ex);

                            // Retry next-time
                            THROTTLED_QUEUE.put(e.getKey(), e.getValue());
                        }
                    }
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
