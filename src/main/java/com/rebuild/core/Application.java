/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.ReflectUtils;
import cn.devezhao.commons.excel.Cell;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.StandardRecord;
import cn.devezhao.persist4j.query.QueryedRecord;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.rebuild.core.cache.CommonsCache;
import com.rebuild.core.metadata.impl.DynamicMetadataFactory;
import com.rebuild.core.privileges.PrivilegesManager;
import com.rebuild.core.privileges.RecordOwningCache;
import com.rebuild.core.privileges.UserStore;
import com.rebuild.core.service.CommonsService;
import com.rebuild.core.service.ServiceSpec;
import com.rebuild.core.service.SqlExecutor;
import com.rebuild.core.service.general.EntityService;
import com.rebuild.core.service.general.GeneralEntityService;
import com.rebuild.core.service.notification.NotificationService;
import com.rebuild.core.service.query.QueryFactory;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.License;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.setup.Installer;
import com.rebuild.core.support.setup.UpgradeDatabase;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONable;
import com.rebuild.utils.RebuildBanner;
import com.rebuild.utils.codec.RbDateCodec;
import com.rebuild.utils.codec.RbRecordCodec;
import com.rebuild.web.OnlineSessionStore;
import com.rebuild.web.RebuildWebConfigurer;
import org.h2.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 后台入口类
 *
 * @author zhaofang123@gmail.com
 * @since 05/18/2018
 */
public class Application implements ApplicationListener<ApplicationStartedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    /**
     * Rebuild Version
     */
    public static final String VER = "2.0.0";
    /**
     * Rebuild Build
     */
    public static final int BUILD = 20000;

    static {
        // Driver for DB
        try {
            Class.forName(com.mysql.jdbc.Driver.class.getName());
            Class.forName(Driver.class.getName());
        } catch (ClassNotFoundException ex) {
            throw new RebuildException(ex);
        }

        // for fastjson Serialize
        SerializeConfig.getGlobalInstance().put(JSONable.class, ToStringSerializer.instance);
        SerializeConfig.getGlobalInstance().put(ID.class, ToStringSerializer.instance);
        SerializeConfig.getGlobalInstance().put(Cell.class, ToStringSerializer.instance);
        SerializeConfig.getGlobalInstance().put(Date.class, RbDateCodec.instance);
        SerializeConfig.getGlobalInstance().put(StandardRecord.class, RbRecordCodec.instance);
        SerializeConfig.getGlobalInstance().put(QueryedRecord.class, RbRecordCodec.instance);
    }

    // 服务启动状态
    private static boolean _READY;
    // 等待系统组件装载
    private static boolean _WAITLOADS = true;
    // 增值模块
    private static boolean _RBV;

    // SPRING
    private static ApplicationContext _CONTEXT;

    // 实体对应的服务类
    private static Map<Integer, ServiceSpec> _ESS;

    protected Application() {
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        if (_CONTEXT != null) throw new IllegalStateException("Rebuild already started");

        _CONTEXT = event.getApplicationContext();

        long time = System.currentTimeMillis();

        String localUrl = String.format("http://localhost:%s%s",
                BootEnvironmentPostProcessor.getProperty("server.port", "18080"),
                AppUtils.getContextPath());

        boolean started = false;
        try {
            if (Installer.isInstalled()) {
                started = init();

                if (started) {
                    String banner = RebuildBanner.formatSimple(
                            "Rebuild (" + VER + ") start successfully in " + (System.currentTimeMillis() - time) + " ms.",
                            "License   : " + License.queryAuthority(false).values(),
                            "Local URL : " + localUrl);
                    LOG.info(banner);
                }

            } else {
                LOG.warn(RebuildBanner.formatBanner(
                        "REBUILD IS WAITING FOR INSTALL ...", "Install : " + localUrl + "/setup/install"));
            }

        } catch (Exception ex) {
            _READY = false;
            LOG.error(RebuildBanner.formatBanner("REBUILD INITIALIZATION FILAED !!!"), ex);

        } finally {
            if (!started) {
                // 某些资源未成功启动仍需初始化
                try {
                    _CONTEXT.getBean(RebuildWebConfigurer.class).init();
                    _CONTEXT.getBean(Language.class).init();
                } catch (Exception ex) {
                    LOG.error("STARTUP FAILED", ex);
                }
            }

            _WAITLOADS = false;
        }
    }

    /**
     * 系统初始化
     *
     * @throws Exception
     */
    public static boolean init() throws Exception {
        if (_READY) throw new IllegalStateException("Rebuild already started");
        LOG.info("Initializing Rebuild context [ {} ] ...", _CONTEXT.getClass().getSimpleName());

        if (!(_READY = ServerStatus.checkAll())) {
            LOG.error(RebuildBanner.formatBanner(
                    "REBUILD STARTUP FILAED DURING THE STATUS CHECK.", "PLEASE VIEW LOGS FOR MORE DETAILS."));
            return false;
        }

        try {
            Object RBV = ReflectUtils.classForName("com.rebuild.Rbv").getDeclaredConstructor().newInstance();
            LOG.info("Loaded " + RBV);
            _RBV = true;
        } catch (Exception ignore) {
        }

        // 升级数据库
        new UpgradeDatabase().upgradeQuietly();

        // 版本升级会清除缓存
        int lastBuild = ObjectUtils.toInt(RebuildConfiguration.get(ConfigurationItem.AppBuild, true), 0);
        if (lastBuild < BUILD) {
            LOG.warn("Clear all cache after the first upgrade : " + BUILD);
            Installer.clearAllCache();
            RebuildConfiguration.set(ConfigurationItem.AppBuild, BUILD);
        }

        // 刷新配置缓存
        for (ConfigurationItem item : ConfigurationItem.values()) {
            RebuildConfiguration.get(item, true);
        }

        // 加载自定义实体
        LOG.info("Loading customized/business entities ...");
        ((DynamicMetadataFactory) _CONTEXT.getBean(PersistManagerFactory.class).getMetadataFactory()).refresh(false);

        // 实体对应的服务类
        _ESS = new HashMap<>();
        for (Map.Entry<String, ServiceSpec> e : _CONTEXT.getBeansOfType(ServiceSpec.class).entrySet()) {
            ServiceSpec s = e.getValue();
            if (s.getEntityCode() > 0) {
                _ESS.put(s.getEntityCode(), s);
                if (devMode()) {
                    LOG.info("Service specification : " + s.getClass().getName() + " for <" + s.getEntityCode() + ">");
                }
            }
        }

        // 初始化业务组件
        for (Initialization bean : _CONTEXT.getBeansOfType(Initialization.class).values()) {
            bean.init();
        }

        return true;
    }

    /**
     * 是否开发模式
     * @return
     */
    public static boolean devMode() {
        return BootApplication.devMode();
    }

    /**
     * RBV 可用
     * @return
     */
    public static boolean rbvLoaded() {
        return License.getCommercialType() > 0 && _RBV;
    }

    /**
     * 已启动（不含组件装载）
     * @return
     */
    public static boolean isReady() {
        return _READY && _CONTEXT != null;
    }

    /**
     * 等待系统组件装载完毕
     * @return
     */
    public static boolean isWaitLoads() {
        return _WAITLOADS && _CONTEXT != null;
    }

    public static ApplicationContext getContext() {
        if (_CONTEXT == null) throw new IllegalStateException("Rebuild unstarted");
        return _CONTEXT;
    }

    public static <T> T getBean(Class<T> beanClazz) {
        return getContext().getBean(beanClazz);
    }

    public static Language getLanguage() {
        return getBean(Language.class);
    }

    public static OnlineSessionStore getSessionStore() {
        return getBean(OnlineSessionStore.class);
    }

    public static PersistManagerFactory getPersistManagerFactory() {
        return getBean(PersistManagerFactory.class);
    }

    public static UserStore getUserStore() {
        return getBean(UserStore.class);
    }

    public static RecordOwningCache getRecordOwningCache() {
        return getBean(RecordOwningCache.class);
    }

    public static CommonsCache getCommonsCache() {
        return getBean(CommonsCache.class);
    }

    public static PrivilegesManager getPrivilegesManager() {
        return getBean(PrivilegesManager.class);
    }

    public static QueryFactory getQueryFactory() {
        return getBean(QueryFactory.class);
    }

    public static Query createQuery(String ajql) {
        return getQueryFactory().createQuery(ajql);
    }

    public static Query createQuery(String ajql, ID user) {
        return getQueryFactory().createQuery(ajql, user);
    }

    public static Query createQueryNoFilter(String ajql) {
        return getQueryFactory().createQueryNoFilter(ajql);
    }

    public static SqlExecutor getSqlExecutor() {
        return getBean(SqlExecutor.class);
    }

    public static ServiceSpec getService(int entityCode) {
        if (_ESS != null && _ESS.containsKey(entityCode)) {
            return _ESS.get(entityCode);
        } else {
            return getGeneralEntityService();
        }
    }

    public static EntityService getEntityService(int entityCode) {
        ServiceSpec es = getService(entityCode);
        if (EntityService.class.isAssignableFrom(es.getClass())) {
            return (EntityService) es;
        }
        throw new RebuildException("Non EntityService implements : " + entityCode);
    }

    public static GeneralEntityService getGeneralEntityService() {
        return (GeneralEntityService) getContext().getBean("generalEntityService");
    }

    public static CommonsService getCommonsService() {
        return getBean(CommonsService.class);
    }

    public static NotificationService getNotifications() {
        return getBean(NotificationService.class);
    }
}
