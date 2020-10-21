/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import com.rebuild.core.support.RebuildConfiguration;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ImportResource;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;

/**
 * 启动类
 *
 * @author devezhao
 * @since 2020/9/22
 */
@SpringBootApplication(scanBasePackages = {"com.rebuild"}, exclude = {
        DataSourceAutoConfiguration.class,
        JdbcRepositoriesAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        RedisAutoConfiguration.class,
        CacheAutoConfiguration.class})
@ImportResource("classpath:application-bean.xml")
public class BootApplication extends SpringBootServletInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(BootApplication.class);

    private static boolean DEBUG = false;

    private static String CONTEXT_PATH = null;

    public static String getContextPath() {
        if (CONTEXT_PATH == null) {
            // IN BOOT
            CONTEXT_PATH = BootEnvironmentPostProcessor.getProperty("server.servlet.context-path", "");
        }
        return CONTEXT_PATH;
    }

    public static boolean devMode() {
        return DEBUG || BooleanUtils.toBoolean(System.getProperty("rbdev"));
    }

    // ---------------------------------------- USE BOOT

    public static void main(String[] args) {
        DEBUG = args.length > 0 && args[0].contains("rbdev=true");
        if (devMode()) System.setProperty("spring.profiles.active", "dev");

        // kill -15 `cat ~/.rebuild/rebuild.pid`
        File pidFile = RebuildConfiguration.getFileOfData("rebuild.pid");

        LOG.info("Initializing SpringBoot context {}...", devMode() ? "(dev) " : "");
        SpringApplication spring = new SpringApplication(BootApplication.class);
        spring.addListeners(new ApplicationPidFileWriter(pidFile), new Application());
        spring.run(args);
    }

    // ---------------------------------------- USE TOMCAT

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        if (devMode()) System.setProperty("spring.profiles.active", "dev");

        LOG.info("Initializing SpringBoot context {}...", devMode() ? "(dev) " : "");
        SpringApplicationBuilder spring = builder.sources(BootApplication.class);
        spring.listeners(new Application());
        return spring;
    }
    
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        CONTEXT_PATH = StringUtils.defaultIfBlank(servletContext.getContextPath(), "");
        super.onStartup(servletContext);
    }
}
