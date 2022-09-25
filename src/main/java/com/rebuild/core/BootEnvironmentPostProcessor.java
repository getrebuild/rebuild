/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import ch.qos.logback.classic.LoggerContext;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.setup.InstallState;
import com.rebuild.core.support.setup.Installer;
import com.rebuild.utils.AES;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * 配置参数
 *
 * @author Zixin (RB)
 * @since 08/28/2020
 */
@Component
@Slf4j
public class BootEnvironmentPostProcessor implements EnvironmentPostProcessor, InstallState {

    private static final String V2_PREFIX = "rebuild.";
    private static final String MYSQL_J8_TIMEZONE = "serverTimezone=GMT%2B08:00";

    private static ConfigurableEnvironment ENV_HOLD;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        if (env == null) env = ENV_HOLD;

        try {
            // LogbackLoggingSystem#beforeInitialize
            ((LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory()).resetTurboFilterList();
        } catch (Exception ignored) {
        }

        // 从安装文件
        File installed;
        try {
            installed = getInstallFile();
        } catch (RebuildException init) {
            throw new IllegalStateException("GET INSTALL FILE ERROR!", init);
        }

        if (installed != null && installed.exists()) {
            log.info("Use installed file : {}", installed);

            try {
                Properties temp = PropertiesLoaderUtils.loadProperties(new FileSystemResource(installed));
                Properties filePs = new Properties();
                // compatible: v1.x
                for (String name : temp.stringPropertyNames()) {
                    String value = temp.getProperty(name);
                    if (name.endsWith(".aes")) {
                        name = name.substring(0, name.length() - 4);
                        if (StringUtils.isNotBlank(value)) {
                            value = "AES(" + value + ")";
                        }
                    }

                    if (name.startsWith(Installer.CONF_PREFIX)) {
                        filePs.put(name, value);
                    } else {
                        filePs.put(Installer.CONF_PREFIX + name, value);
                    }
                }

                aesDecrypt(filePs);
                env.getPropertySources().addFirst(new PropertiesPropertySource(".rebuild", filePs));

            } catch (IOException ex) {
                throw new IllegalStateException("READ INSTALL FILE FAILED : " + installed, ex);
            }
        }

        Properties confPs = new Properties();
        for (ConfigurationItem item : ConfigurationItem.values()) {
            String name = V2_PREFIX + item.name();
            String value = env.getProperty(name);
            if (value != null) confPs.put(name, value);

            name = Installer.CONF_PREFIX + item.name();
            value = env.getProperty(name);
            if (value != null) confPs.put(name, value);
        }

        String dbUrl = env.getProperty("db.url");

        // `application-bean.xml` 占位符必填
        if (dbUrl == null) {
            dbUrl = "jdbc:mysql://127.0.0.1:3306/rebuild30?characterEncoding=UTF8";
            confPs.put("db.url", dbUrl);
        }
        if (env.getProperty("db.user") == null) confPs.put("db.user", "rebuild");
        if (env.getProperty("db.passwd") == null) confPs.put("db.passwd", "rebuild");

        // fix: v2.1
        if (dbUrl.contains("jdbc:mysql") && !dbUrl.contains("serverTimezone")) {
            confPs.put("db.url", dbUrl + "&" + MYSQL_J8_TIMEZONE);
        }
        // fix: v2.2 ~ v2.6.1
        else if (dbUrl.contains("serverTimezone=UTC")) {
            confPs.put("db.url", dbUrl.replace("serverTimezone=UTC", MYSQL_J8_TIMEZONE));
            log.info("Fix MYSQL_J8_TIMEZONE : {}", confPs.getProperty("db.url"));
        }

        aesDecrypt(confPs);
        env.getPropertySources().addFirst(new PropertiesPropertySource(".configuration", confPs));

        ENV_HOLD = env;

        log.info("Use RB data directory : {}", RebuildConfiguration.getFileOfData("/"));
    }

    /**
     * 解密配置 `AES(xxx)`
     *
     * @param ps
     * @see AES
     */
    private void aesDecrypt(Properties ps) {
        for (String name : ps.stringPropertyNames()) {
            String value = ps.getProperty(name);
            if ((value.startsWith("AES(") || value.startsWith("aes(")) && value.endsWith(")")) {
                value = value.substring(4, value.length() - 1);
                String newValue = AES.decryptQuietly(value);
                if (newValue == null) {
                    newValue = StringUtils.EMPTY;
                    log.warn("Decrypting error (Use blank string) : " + name);
                }
                ps.put(name, newValue);
            }
        }
    }

    /**
     * @param name
     * @return
     */
    public static String getProperty(String name) {
        return getProperty(name, null);
    }

    /**
     * @param name
     * @param defaultValue
     * @return
     */
    public static String getProperty(String name, String defaultValue) {
        String value = null;
        // in CLI
        if (ConfigurationItem.DataDirectory.name().equalsIgnoreCase(name)
                || ConfigurationItem.RedisDatabase.name().equalsIgnoreCase(name)
                || ConfigurationItem.MobileUrl.name().equalsIgnoreCase(name)
                || ConfigurationItem.RbStoreUrl.name().equalsIgnoreCase(name)
                || ConfigurationItem.TriggerMaxDepth.name().equalsIgnoreCase(name)
                || ConfigurationItem.SecurityEnhanced.name().equalsIgnoreCase(name)
                || ConfigurationItem.SN.name().equalsIgnoreCase(name)) {
            value = StringUtils.defaultIfBlank(System.getProperty(name), System.getProperty(V2_PREFIX + name));

        } else if (ENV_HOLD != null) {
            if (!(name.startsWith(V2_PREFIX) || name.contains("."))) {
                name = V2_PREFIX + name;
            }
            value = ENV_HOLD.getProperty(name);
        }

        return StringUtils.isEmpty(value) ? defaultValue : value;
    }
}
