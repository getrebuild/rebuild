/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.StandardRecord;
import cn.devezhao.persist4j.query.QueryedRecord;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.Lisence;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.cache.CommonCache;
import com.rebuild.server.helper.cache.EhcacheTemplate;
import com.rebuild.server.helper.cache.RecentlyUsedCache;
import com.rebuild.server.helper.cache.RecordOwningCache;
import com.rebuild.server.metadata.DynamicMetadataFactory;
import com.rebuild.server.service.CommonService;
import com.rebuild.server.service.EntityService;
import com.rebuild.server.service.SQLExecutor;
import com.rebuild.server.service.ServiceSpec;
import com.rebuild.server.service.base.GeneralEntityService;
import com.rebuild.server.service.bizz.privileges.UserStore;
import com.rebuild.server.service.notification.NotificationService;
import com.rebuild.server.service.query.QueryFactory;
import com.rebuild.utils.RbDateCodec;
import com.rebuild.utils.RbRecordCodec;
import com.rebuild.web.OnlineSessionStore;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 后台类入口
 * 
 * @author zhaofang123@gmail.com
 * @since 05/18/2018
 */
public final class Application {
	
	/** Rebuild Version
	 */
	public static final String VER = "1.6.1";
	
	/** Logging for Global
	 */
	public static final Log LOG = LogFactory.getLog(Application.class);
	
	// 调试模式
	private static boolean debugMode = false;
	// 服务启动正常
	private static boolean serverReady = false;
	
	// SPRING
	private static ApplicationContext APPLICATION_CTX;
	// 实体对应的服务类
	private static Map<Integer, ServiceSpec> SSS = null;
	
	/**
	 * @param ctx
	 */
	protected Application(ApplicationContext ctx) {
		APPLICATION_CTX = ctx;
	}

	/**
	 * 初始化
	 *
	 * @param startAt
	 * @throws Exception
	 */
	synchronized
	protected void init(long startAt) throws Exception {
		serverReady = ServerStatus.checkAll();
		if (!serverReady) {
			LOG.fatal("\n###################################################################\n"
					+ "\n  REBUILD BOOTING FAILURE DURING THE STATUS CHECKS."
					+ "\n  PLEASE VIEW BOOTING LOGS."
					+ "\n  Version : " + VER
					+ "\n  OS      : " + SystemUtils.OS_NAME + " " + SystemUtils.OS_ARCH
					+ "\n  Report an issue : "
					+ "\n  https://github.com/getrebuild/rebuild/issues/new?title=error-boot"
					+ "\n\n###################################################################");
			return;
		}

		// for fastjson Serialize
		SerializeConfig.getGlobalInstance().put(ID.class, ToStringSerializer.instance);
		SerializeConfig.getGlobalInstance().put(Date.class, RbDateCodec.instance);
		SerializeConfig.getGlobalInstance().put(StandardRecord.class, RbRecordCodec.instance);
		SerializeConfig.getGlobalInstance().put(QueryedRecord.class, RbRecordCodec.instance);

		Lisence.SN();
		// 更新刷新配置缓存
		for (ConfigurableItem item : ConfigurableItem.values()) {
			SysConfiguration.get(item, true);
		}

		// 升级数据库
		UpgradeDatabase.getInstance().upgradeQuietly();

		// 自定义实体
		LOG.info("Loading customized/business entities ...");
		((DynamicMetadataFactory) APPLICATION_CTX.getBean(PersistManagerFactory.class).getMetadataFactory()).refresh(false);

		// 实体对应的服务类
		SSS = new HashMap<>();
		for (Map.Entry<String, ServiceSpec> e : APPLICATION_CTX.getBeansOfType(ServiceSpec.class).entrySet()) {
			ServiceSpec ss = e.getValue();
			if (ss.getEntityCode() > 0) {
				SSS.put(ss.getEntityCode(), ss);
				LOG.info("Service specification : " + ss);
			}
		}

		// 若使用 Ehcache 则添加持久化钩子
		final CommonCache ccache = APPLICATION_CTX.getBean(CommonCache.class);
		if (!ccache.isUseRedis()) {
			addShutdownHook(new Thread("ehcache-persistent") {
				@Override
				public void run() {
					((EhcacheTemplate<?>) ccache.getCacheTemplate()).shutdown();
				}
			});
		}

		LOG.info("Rebuild Boot successful in " + (System.currentTimeMillis() - startAt) + " ms");
	}

	/**
	 * FOR TESTING ONLY
	 *
	 * @return
	 * @throws Exception
	 */
	protected static ApplicationContext debug() throws Exception {
		if (APPLICATION_CTX == null) {
			debugMode = true;
			LOG.info("Rebuild Booting in DEBUG mode ...");
			long at = System.currentTimeMillis();
			ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] { "application-ctx.xml" });
			new Application(ctx).init(at);
		}
		return APPLICATION_CTX;
	}
	
	/**
	 * 是否开发模式
	 * 
	 * @return
	 */
	public static boolean devMode() {
		return BooleanUtils.toBoolean(System.getProperty("rbdev")) || debugMode;
	}
	
	/**
	 * 各项服务是否正常启动
	 * 
	 * @return
	 */
	public static boolean serversReady() {
		return serverReady;
	}
	
	/**
	 * 添加服务停止钩子
	 * 
	 * @param hook
	 */
	public static void addShutdownHook(Thread hook) {
		LOG.warn("Add shutdown hook : " + hook.getName());
		Runtime.getRuntime().addShutdownHook(hook);
	}
	
	/**
	 * SPRING Context
	 * 
	 * @return
	 */
	public static ApplicationContext getApplicationContext() {
		if (APPLICATION_CTX == null) {
			throw new IllegalStateException("Rebuild unstarted");
		}
		return APPLICATION_CTX;
	}
	
	public static <T> T getBean(Class<T> beanClazz) {
		return getApplicationContext().getBean(beanClazz);
	}

	public static OnlineSessionStore getSessionStore() {
		return getBean(OnlineSessionStore.class);
	}
	
	public static ID getCurrentUser() {
		return getSessionStore().get();
	}

	public static PersistManagerFactory getPersistManagerFactory() {
		return getBean(PersistManagerFactory.class);
	}
	
	public static DynamicMetadataFactory getMetadataFactory() {
		return (DynamicMetadataFactory) getPersistManagerFactory().getMetadataFactory();
	}

	public static UserStore getUserStore() {
		return getBean(UserStore.class);
	}
	
	public static RecordOwningCache getRecordOwningCache() {
		return getBean(RecordOwningCache.class);
	}
	
	public static RecentlyUsedCache getRecentlyUsedCache() {
		return getBean(RecentlyUsedCache.class);
	}
	
	public static CommonCache getCommonCache() {
		return getBean(CommonCache.class);
	}

	public static com.rebuild.server.service.bizz.privileges.SecurityManager getSecurityManager() {
		return getBean(com.rebuild.server.service.bizz.privileges.SecurityManager.class);
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

	public static SQLExecutor getSQLExecutor() {
		return getBean(SQLExecutor.class);
	}
	
	public static NotificationService getNotifications() {
		return getBean(NotificationService.class);
	}

	/**
	 * @param entityCode
	 * @return
	 */
	public static ServiceSpec getService(int entityCode) {
		if (SSS != null && SSS.containsKey(entityCode)) {
			return SSS.get(entityCode);
		} else {
			return getGeneralEntityService();
		}
	}
	
	/**
	 * @param entityCode
	 * @return
	 * @see #getGeneralEntityService()
	 */
	public static EntityService getEntityService(int entityCode) {
		ServiceSpec spec = getService(entityCode);
		if (EntityService.class.isAssignableFrom(spec.getClass())) {
			return (EntityService) spec;
		}
		throw new RebuildException("Non EntityService implements : " + entityCode);
	}
	
	/**
	 * @return
	 */
	public static GeneralEntityService getGeneralEntityService() {
		return (GeneralEntityService) getApplicationContext().getBean("generalEntityService");
	}
	
	/**
	 * @return
	 */
	public static CommonService getCommonService() {
		return getBean(CommonService.class);
	}
}
