/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cn.devezhao.rebuild.server;

import java.security.Security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MetadataFactory;
import cn.devezhao.rebuild.server.metadata.MetadataHelper;
import cn.devezhao.rebuild.server.privileges.UserStore;
import cn.devezhao.rebuild.server.service.CommonService;
import cn.devezhao.rebuild.server.service.QueryFactory;
import cn.devezhao.rebuild.server.service.SqlExecutor;
import cn.devezhao.rebuild.web.CurrentCaller;

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
	public static MetadataFactory getMetadataFactory() {
		return getPersistManagerFactory().getMetadataFactory();
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
	public static cn.devezhao.rebuild.server.privileges.SecurityManager getSecurityManager() {
		return getBean(cn.devezhao.rebuild.server.privileges.SecurityManager.class);
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
