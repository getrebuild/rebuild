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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 05/21/2017
 */
public class Startup extends HttpServlet {
	private static final long serialVersionUID = 5783774294311348578L;
	
	private static final Log LOG = LogFactory.getLog(Startup.class);
	
	private static String CONTEXT_PATH = "";
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		Startup.CONTEXT_PATH = config.getServletContext().getContextPath();
		LOG.info("Detecting Rebuild context path '" + Startup.CONTEXT_PATH + "'");
		
		LOG.info("Rebuild Booting ...");
		try {
			ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
			new Application(ctx);
		} catch (Throwable ex) {
			LOG.fatal("Booting FAIL!", ex);
			System.exit(-1);
		}
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
