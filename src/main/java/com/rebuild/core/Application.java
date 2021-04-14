/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.excel.Cell;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.StandardRecord;
import cn.devezhao.persist4j.query.QueryedRecord;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
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
import com.rebuild.utils.JSONable;
import com.rebuild.utils.RebuildBanner;
import com.rebuild.utils.codec.RbDateCodec;
import com.rebuild.utils.codec.RbRecordCodec;
import com.rebuild.web.OnlineSessionStore;
import com.rebuild.web.RebuildWebConfigurer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.OrderComparator;

import java.util.*;

/**
 * 后台入口类
 *
 * @author zhaofang123@gmail.com
 * @since 05/18/2018
 */
@Slf4j
public class Application implements ApplicationListener<ApplicationStartedEvent> {

    /**
     * Rebuild Version
     */
    public static final String VER = "2.3.0-dev";
    /**
     * Rebuild Build
     */
    public static final int BUILD = 20300;

    static {
        // Driver for DB
        try {
            Class.forName(com.mysql.cj.jdbc.Driver.class.getName());
            Class.forName(org.h2.Driver.class.getName());

            // fix https://github.com/alibaba/druid/issues/3991
            System.setProperty("druid.mysql.usePingMethod", "false");
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

        JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.DisableCircularReferenceDetect.getMask();
        JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.WriteMapNullValue.getMask();
    }

    // 系统状态
    private static boolean _READY;
    // 业务组件已装载
    private static boolean _WAITLOAD = true;

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
        boolean started = false;

        final Timer timer = new Timer("Boot-Timer");

        try {
            if (Installer.isInstalled()) {
                started = init();

                if (started) {
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            String banner = RebuildBanner.formatSimple(
                                    "Rebuild (" + VER + ") start successfully in " + (System.currentTimeMillis() - time) + " ms.",
                                    "License   : " + License.queryAuthority(false).values(),
                                    "Local URL : " + BootApplication.getLocalUrl());
                            log.info(banner);
                        }
                    }, 1500);
                }

            } else {
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        log.warn(RebuildBanner.formatBanner(
                                "REBUILD IS WAITING FOR INSTALL ...",
                                "Install : " + BootApplication.getLocalUrl() + "/setup/install"));
                    }
                }, 1500);
            }

        } catch (Exception ex) {
            _READY = false;
            log.error(RebuildBanner.formatBanner("REBUILD INITIALIZATION FILAED !!!"), ex);

        } finally {
            if (!started) {
                // 某些资源未成功启动仍需初始化
                try {
                    _CONTEXT.getBean(RebuildWebConfigurer.class).init();
                    _CONTEXT.getBean(Language.class).init();
                } catch (Exception ex) {
                    log.error("REBUILD STARTUP FAILED", ex);
                }
            }

            _WAITLOAD = false;
        }
    }

    /**
     * 系统初始化
     *
     * @throws Exception
     */
    public static boolean init() throws Exception {
        if (_READY) throw new IllegalStateException("REBUILD ALREADY STARTED");
        log.info("Initializing Rebuild context [ {} ] ...", _CONTEXT.getClass().getSimpleName());

        if (!(_READY = ServerStatus.checkAll())) {
            log.error(RebuildBanner.formatBanner(
                    "REBUILD STARTUP FILAED DURING THE STATUS CHECK.", "PLEASE VIEW LOGS FOR MORE DETAILS."));
            return false;
        }

        // 升级数据库
        new UpgradeDatabase().upgradeQuietly();

        // 版本升级会清除缓存
        int lastBuild = ObjectUtils.toInt(RebuildConfiguration.get(ConfigurationItem.AppBuild, true), 0);
        if (lastBuild < BUILD) {
            log.warn("Clean up the cache once when upgrading : " + BUILD);
            Installer.clearAllCache();
            RebuildConfiguration.set(ConfigurationItem.AppBuild, BUILD);
        }

        // 刷新配置缓存
        for (ConfigurationItem item : ConfigurationItem.values()) {
            RebuildConfiguration.get(item, true);
        }

        // 加载自定义实体
        log.info("Loading customized/business entities ...");
        ((DynamicMetadataFactory) _CONTEXT.getBean(PersistManagerFactory.class).getMetadataFactory()).refresh(false);

        // 实体对应的服务类
        _ESS = new HashMap<>();
        for (Map.Entry<String, ServiceSpec> e : _CONTEXT.getBeansOfType(ServiceSpec.class).entrySet()) {
            ServiceSpec s = e.getValue();
            if (s.getEntityCode() > 0) {
                _ESS.put(s.getEntityCode(), s);
                if (devMode()) {
                    log.info("Service specification : {} for [ {} ]", s.getClass().getName(), s.getEntityCode());
                }
            }
        }

        // 初始化业务组件
        List<Initialization> ordered = new ArrayList<>(_CONTEXT.getBeansOfType(Initialization.class).values());
        OrderComparator.sort(ordered);
        for (Initialization bean : ordered) {
            bean.init();
        }
        License.isRbvAttached();

        return true;
    }

    public static boolean devMode() {
        return BootApplication.devMode();
    }

    public static boolean isReady() {
        return _READY && _CONTEXT != null;
    }

    public static boolean isWaitLoad() {
        return _WAITLOAD && _CONTEXT != null;
    }

    public static ApplicationContext getContext() {
        if (_CONTEXT == null) throw new IllegalStateException("Rebuild unstarted");
        else return _CONTEXT;
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

    public static NotificationService getNotifications() {
        return getBean(NotificationService.class);
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

    /**
     * 非业务实体使用
     * @see #getCommonsService()
     */
    public static ServiceSpec getService(int entityCode) {
        if (_ESS != null && _ESS.containsKey(entityCode)) {
            ServiceSpec es = _ESS.get(entityCode);
            if (EntityService.class.isAssignableFrom(es.getClass())) {
                log.warn("Use the #getEntityService is recommended");
            }
            return es;

        } else {
            // default
            return getCommonsService();
        }
    }

    /**
     * 业务实体使用
     * @see #getGeneralEntityService()
     */
    public static EntityService getEntityService(int entityCode) {
        ServiceSpec es = null;
        if (_ESS != null && _ESS.containsKey(entityCode)) {
            es = _ESS.get(entityCode);
        }

        if (es == null) {
            // default
            return getGeneralEntityService();
        }

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
}
