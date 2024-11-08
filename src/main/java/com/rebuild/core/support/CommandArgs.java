/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import cn.devezhao.commons.ObjectUtils;
import com.rebuild.core.BootEnvironmentPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

/**
 * 命令行保留参数
 *
 * @author RB
 * @since 2023/2/4
 * @see ConfigurationItem
 */
@Slf4j
public class CommandArgs {

    public static final String rbdev = "rbdev";
    public static final String rbpass = "rbpass";

    public static final String _ForceTour = "_ForceTour";
    public static final String _HeavyStopWatcher = "_HeavyStopWatcher";
    public static final String _UniPush = "_UniPush";
    public static final String _UseDbFullText = "_UseDbFullText";
    public static final String _StartEntityTypeCode = "_StartEntityTypeCode";

    /**
     * 内部消息同步发送短信
     */
    public static final String _SmsDistributor = "_SmsDistributor";
    /**
     * 内部消息同步发送邮件
     */
    public static final String _EmailDistributor = "_EmailDistributor";
    /**
     * FrontJS在所有页面生效
     */
    public static final String _UseFrontJSAnywhere = "_UseFrontJSAnywhere";
    /**
     * 触发器级联执行深度
     */
    public static final String _TriggerMaxDepth = "_TriggerMaxDepth";
    /**
     * @see com.rebuild.web.admin.ProtectedAdmin
     */
    public static final String _ProtectedAdmin = "_ProtectedAdmin";
    /**
     * 更少的触发器日志输出
     */
    public static final String _TriggerLessLog = "_TriggerLessLog";

    /**
     * @param name
     * @return default `false`
     */
    public static boolean getBoolean(String name) {
        return BooleanUtils.toBoolean(getProperty39(name));
    }

    /**
     * @param name
     * @return default `-1`
     */
    public static int getInt(String name) {
        return ObjectUtils.toInt(getProperty39(name), -1);
    }

    /**
     * @param name
     * @param defaultValue
     * @return
     */
    public static int getInt(String name, int defaultValue) {
        int s = getInt(name);
        return s == -1 ? defaultValue : s;
    }

    /**
     * @param name
     * @return
     */
    public static String getString(String name) {
        return getProperty39(name);
    }

    /**
     * @param name
     * @return
     */
    protected static String getStringWithBootEnvironmentPostProcessor(String name) {
        String s = getProperty39(name);
        if (StringUtils.isEmpty(s)) s = BootEnvironmentPostProcessor.getProperty(name);
        return s;
    }

    // --

    // 引入外部配置文件
    private static Properties CONF39;
    private static String getProperty39(String name) {
        if (CONF39 == null) {
            File rebuildConf = RebuildConfiguration.getFileOfData("rebuild.conf");
            if (rebuildConf.exists()) {
                Properties conf = new Properties();
                try {
                    conf.load(Files.newInputStream(rebuildConf.toPath()));
                    CONF39 = conf;
                } catch (IOException e) {
                    log.warn("Cannot load `rebuild.conf` : {}", rebuildConf);
                }
            }
        }

        String s = CONF39.getProperty(name);
        if (StringUtils.isNotBlank(s)) return s;
        return System.getProperty(name);
    }
}
