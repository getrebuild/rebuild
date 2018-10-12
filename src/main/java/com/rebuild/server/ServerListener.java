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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * 服务监听
 * 
 * @author devezhao
 * @since 10/13/2018
 */
public class ServerListener implements ServletContextListener {

	private static final Log LOG = LogFactory.getLog(ServerListener.class);

	private static String CONTEXT_PATH = "";
	
	@Override
	public void contextInitialized(ServletContextEvent event) {
		LOG.info("Rebuild Booting ...");
		
		CONTEXT_PATH = event.getServletContext().getContextPath();
		LOG.info("Detecting Rebuild context path '" + CONTEXT_PATH + "'");

		try {
			ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(event.getServletContext());
			new Application(ctx);
		} catch (Throwable ex) {
			LOG.fatal("Booting FAIL!", ex);
			System.exit(-1);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		LOG.info("Rebuild shutdown.");
	}
	
	/**
	 * 获取部署路径
	 * 
	 * @return
	 */
	public static String getContextPath() {
		return CONTEXT_PATH;
	}
}
