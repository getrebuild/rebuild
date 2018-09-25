package com.rebuild.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 05/21/2017
 */
public class Startup extends HttpServlet {
	private static final long serialVersionUID = 5783774294311348578L;
	
	private static String contextPath = "/";
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		contextPath = config.getServletContext().getContextPath();
		
		Application.LOG.warn("Rebuild Booting ...");
		try {
			ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
			new Application(ctx);
		} catch (Throwable ex) {
			Application.LOG.fatal("Booting FAIL!", ex);
			System.exit(-1);
		}
	}
	
	/**
	 * 获取部署路径
	 * 
	 * @return
	 */
	public static String getContextPath() {
		return contextPath;
	}
}
