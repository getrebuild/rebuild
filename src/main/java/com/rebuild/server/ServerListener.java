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

import cn.devezhao.commons.CalendarUtils;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.SysConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContextEvent;
import java.util.Date;

/**
 * 服务启动/停止监听
 * 
 * @author devezhao
 * @since 10/13/2018
 */
public class ServerListener extends ContextLoaderListener {

	private static final Log LOG = LogFactory.getLog(ServerListener.class);

	private static String CONTEXT_PATH = "";
	private static Date STARTUP_TIME = CalendarUtils.now();
	
	@Override
	public void contextInitialized(ServletContextEvent event) {
		long at = System.currentTimeMillis();
		LOG.info("Rebuild Booting ...");
		
		CONTEXT_PATH = event.getServletContext().getContextPath();
		LOG.debug("Detecting Rebuild context-path '" + CONTEXT_PATH + "'");

		LOG.info("Initializing Spring context ...");
		try {
			super.contextInitialized(event);
			WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(event.getServletContext());
			new Application(wac).init(at);
			STARTUP_TIME = CalendarUtils.now();

			// 全局 EL 变量
			event.getServletContext().setAttribute("baseUrl", CONTEXT_PATH);
			event.getServletContext().setAttribute("appName", SysConfiguration.get(ConfigurableItem.AppName));
			event.getServletContext().setAttribute("storageUrl", StringUtils.defaultIfEmpty(SysConfiguration.getStorageUrl(), ""));

		} catch (Throwable ex) {
			LOG.fatal("Rebuild Booting failure!!!", ex);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		LOG.info("Rebuild shutdown ...");
		super.contextDestroyed(event);
	}
	
	// --
	
	/**
	 * @return
	 */
	public static String getContextPath() {
		return CONTEXT_PATH;
	}
	
	/**
	 * @return
	 */
	public static Date getStartupTime() {
		return STARTUP_TIME;
	}
}
