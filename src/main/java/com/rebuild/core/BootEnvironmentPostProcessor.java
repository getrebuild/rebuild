/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import ch.qos.logback.classic.LoggerContext;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.setup.InstallState;
import com.rebuild.core.support.setup.Installer;
import com.rebuild.utils.AES;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @author zhaofang123@gmail.com
 * @since 08/28/2020
 */
@Component
public class BootEnvironmentPostProcessor implements EnvironmentPostProcessor, InstallState {

    private static final Logger LOG = LoggerFactory.getLogger(BootEnvironmentPostProcessor.class);

    private static final String V2_PREFIX = "rebuild.";

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
        File file = getInstallFile();
        if (file != null && file.exists()) {
            LOG.info("Use installation file : " + file);

            try {
                Properties temp = PropertiesLoaderUtils.loadProperties(new FileSystemResource(file));
                Properties filePs = new Properties();
                // 兼容 V1
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
                throw new IllegalStateException("Load file of install failed : " + file.toString(), ex);
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

        // `application-bean.xml` 占位符必填
        if (env.getProperty("db.url") == null) confPs.put("db.url", "jdbc:mysql://127.0.0.1:3306/rebuild20");
        if (env.getProperty("db.user") == null) confPs.put("db.user", "rebuild");
        if (env.getProperty("db.passwd") == null) confPs.put("db.passwd", "rebuild");

        aesDecrypt(confPs);
        env.getPropertySources().addFirst(new PropertiesPropertySource(".configuration", confPs));

        ENV_HOLD = env;
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
                    LOG.warn("Decrypting error (Use blank string) : " + name);
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
        if (ConfigurationItem.DataDirectory.name().equalsIgnoreCase(name)) {
            value = System.getProperty("DataDirectory");

        } else if (ENV_HOLD != null) {
            if (!(name.startsWith(V2_PREFIX) || name.contains("."))) {
                name = V2_PREFIX + name;
            }

            value = ENV_HOLD.getProperty(name);
        }
        return value == null ? defaultValue : value;
    }
}
