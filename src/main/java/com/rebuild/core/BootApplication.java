/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
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
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * SpringBoot 启动类
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
@EnableScheduling
@Slf4j
public class BootApplication extends SpringBootServletInitializer {

    private static String CONTEXT_PATH;
    private static String TOMCAT_PORT;

    /**
     * @return
     */
    public static String getContextPath() {
        if (CONTEXT_PATH == null) {
            // USE BOOT
            CONTEXT_PATH = BootEnvironmentPostProcessor.getProperty("server.servlet.context-path", "");
        }
        return CONTEXT_PATH;
    }

    /**
     * @return
     */
    protected static String getLocalUrl() {
        if (TOMCAT_PORT == null) {
            // USE BOOT
            TOMCAT_PORT = BootEnvironmentPostProcessor.getProperty("server.port", "18080");
        }
        return String.format("http://localhost:%s%s", TOMCAT_PORT, getContextPath());
    }

    /**
     * @return
     */
    public static boolean devMode() {
        return BooleanUtils.toBoolean(System.getProperty("rbdev"));
    }

    // ---------------------------------------- USE BOOT

    public static void main(String[] args) {
        if (devMode()) System.setProperty("spring.profiles.active", "dev");

        // kill -15 `cat ~/.rebuild/rebuild.pid`
        File pidFile = RebuildConfiguration.getFileOfData("rebuild.pid");

        log.info("Initializing SpringBoot context {}...", devMode() ? "(dev) " : "");
        SpringApplication spring = new SpringApplication(BootApplication.class);
        spring.addListeners(new ApplicationPidFileWriter(pidFile), new Application());
        spring.run(args);
    }

    // ---------------------------------------- USE TOMCAT

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        if (devMode()) System.setProperty("spring.profiles.active", "dev");

        try {
            initTomcatPort();
        } catch (Exception ex) {
            log.debug("Cannot to get Tomcat port : " + ex.getLocalizedMessage());
        }

        log.info("Initializing SpringBoot context {}...", devMode() ? "(dev) " : "");
        SpringApplicationBuilder spring = builder.sources(BootApplication.class);
        spring.listeners(new Application());
        return spring;
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        CONTEXT_PATH = StringUtils.defaultIfBlank(servletContext.getContextPath(), "");
        super.onStartup(servletContext);
    }

    private static void initTomcatPort() throws Exception {
        List<MBeanServer> mbeans = MBeanServerFactory.findMBeanServer(null);
        if (mbeans.isEmpty()) return;

        MBeanServer tomcat = mbeans.get(0);
        Set<ObjectName> connector = tomcat.queryNames(
                new ObjectName("Catalina:type=Connector,*"),
                Query.match(Query.attr("protocol"), Query.value("HTTP/1.1")));

        Iterator<ObjectName> iter = connector.iterator();
        if (iter.hasNext()) {
            ObjectName name = iter.next();
            TOMCAT_PORT = tomcat.getAttribute(name, "port").toString();
        }
    }
}
