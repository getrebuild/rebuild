/*
rebuild - Building your system freely.
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

import java.security.Security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.privileges.UserStore;
import com.rebuild.server.service.CommonService;
import com.rebuild.server.service.QueryFactory;
import com.rebuild.server.service.SqlExecutor;
import com.rebuild.web.CurrentCaller;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;

/**
 * @author zhaofang123@gmail.com
 * @since 05/18/2018
 */
public class Application {
	
	/**
	 * Logging */
	public static final Log LOG = LogFactory.getLog(Application.class);
	
	private static ApplicationContext APPLICATION_CTX;
	
	/**
	 * @param ctx
	 */
	protected Application(ApplicationContext ctx) {
		Security.addProvider(new BouncyCastleProvider());
		APPLICATION_CTX = ctx;
//		APPLICATION_CTX.getBeansOfType(Object.class);
		
		// 自定义实体
		LOG.info("Loading customized entity ...");
		MetadataHelper.refreshMetadata();
		
		//
		
		LOG.warn("Rebuild Booting successful.");
	}
	
	/**
	 * 添加服务停止钩子
	 * 
	 * @param hook
	 */
	public static void addShutdownHook(Thread hook) {
		Runtime.getRuntime().addShutdownHook(hook);
	}
	
	/**
	 * @return
	 */
	public static ApplicationContext context() {
		if (APPLICATION_CTX == null) {
			ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] { "application-ctx.xml" });
			new Application(ctx);
		}
		return APPLICATION_CTX;
	}
	
	/**
	 * @param itemKey
	 */
	public static String getConfigItem(String itemKey) {
		return getBean(AesPreferencesConfigurer.class).getItem(itemKey);
	}
	
	/**
	 * @return
	 */
	public static CurrentCaller getCurrentCaller() {
		return getBean(CurrentCaller.class);
	}
	
	/**
	 * @return
	 */
	public static ID getCurrentCallerUser() {
		return getCurrentCaller().get();
	}
	
	/**
	 * @return
	 */
	public static PersistManagerFactory getPersistManagerFactory() {
		return getBean(PersistManagerFactory.class);
	}
	
	/**
	 * @return
	 */
	public static UserStore getUserStore() {
		return getBean(UserStore.class);
	}
	
	/**
	 * @return
	 */
	public static com.rebuild.server.privileges.SecurityManager getSecurityManager() {
		return getBean(com.rebuild.server.privileges.SecurityManager.class);
	}
	
	/**
	 * @return
	 */
	public static QueryFactory getQueryFactory() {
		return getBean(QueryFactory.class);
	}
	
	/**
	 * @return
	 */
	public static Query createQuery(String ajql) {
		return getQueryFactory().createQuery(ajql);
	}
	
	/**
	 * @return
	 */
	public static Query createNoFilterQuery(String ajql) {
		return getQueryFactory().createQueryUnfiltered(ajql);
	}
	
	/**
	 * @return
	 */
	public static SqlExecutor getSqlExecutor() {
		return getBean(SqlExecutor.class);
	}
	
	/**
	 * @param beanClazz
	 * @return
	 */
	public static <T> T getBean(Class<T> beanClazz) {
		return context().getBean(beanClazz);
	}
	
	/**
	 * @return
	 */
	public static CommonService getCommonService() {
		return getBean(CommonService.class);
	}
}
