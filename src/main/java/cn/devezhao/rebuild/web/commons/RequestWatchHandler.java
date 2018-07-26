package cn.devezhao.rebuild.web.commons;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.RebuildException;
import cn.devezhao.rebuild.utils.AppUtils;

/**
 * @author Zhao Fangfang
 * @version $Id: RequestWatchHandler.java 3313 2017-04-09 05:32:57Z devezhao $
 * @since 1.0, 2013-6-24
 */
public class RequestWatchHandler extends HandlerInterceptorAdapter {

	private static final Log LOG = LogFactory.getLog(RequestWatchHandler.class);
	
	private static final String TIMEOUT_KEY = "ErrorHandler_TIMEOUT";
	
	private static final Set<String> IGNORE_RES = new HashSet<>();
	static {
		IGNORE_RES.add("/user/");
	}
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
			Object handler) throws Exception {
		request.setAttribute(TIMEOUT_KEY, System.currentTimeMillis());
		
		String contentType = request.getContentType();
		contentType = StringUtils.defaultIfBlank(contentType, request.getHeader("content-type"));
		if ("text/html".equalsIgnoreCase(contentType)) {
		}
		
		ID user = AppUtils.getRequestUser(request);
		if (user != null) {
			Application.getCurrentCaller().set(user);
		} else {
			String rUrl = request.getRequestURI();
			boolean isIgnore = false;
			for (String r : IGNORE_RES) {
				if (rUrl.contains(r)) {
					isIgnore = true;
					break;
				}
			}
			
			if (!isIgnore) {
				LOG.warn("Unauthorized access [ " + rUrl + " ] from [ " + ServletUtils.getReferer(request) + " ]");
				if (ServletUtils.isAjaxRequest(request)) {
					ServletUtils.writeJson(response, AppUtils.formatClientMsg(403, "非授权访问"));
				} else {
					response.sendError(403, "登录后继续");
				}
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void afterCompletion(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception exception)
			throws Exception {
		ID requestUser = Application.getCurrentCaller().get();
		Application.getCurrentCaller().clean();
		logProgressTime(request);
		
		if (exception != null) {
			Throwable rootCause = ThrowableUtils.getRootCause(exception);
			String errorMsg = "系统繁忙";
			if (rootCause instanceof RebuildException) {
				errorMsg = ((RebuildException) rootCause).toClientMsgString();
			}
			
			StringBuffer sb = new StringBuffer()
					.append("\n++ EXECUTE REQUEST ERROR(s) STACK TRACE +++++++++++++++++++++++++++++++++++++++++++++")
					.append("\nUser:     ").append(requestUser == null ? "-" : requestUser)
					.append("\nHandler:  ").append(request.getRequestURI() + " [ " + handler + " ]")
					.append("\nIP:       ").append(ServletUtils.getRemoteAddr(request))
					.append("\nReferer:  ").append(StringUtils.defaultIfEmpty(ServletUtils.getReferer(request), "-"))
					.append("\nBrowser:  ").append(StringUtils.defaultIfEmpty(request.getHeader("user-agent"), "-"))
					.append("\nCause:    ").append(rootCause.getClass().getName())
					.append("\nMessage:  ").append(rootCause.getMessage());
			LOG.error(sb, rootCause);
			ServletUtils.writeJson(response, AppUtils.formatClientMsg(BaseControll.CODE_ERROR, errorMsg));
		}
	}
	
	/**
	 * 处理时间 LOG
	 * 
	 * @param request
	 */
	protected void logProgressTime(HttpServletRequest request) {
		Long startTime = (Long) request.getAttribute(TIMEOUT_KEY);
		startTime = System.currentTimeMillis() - startTime;
		if (startTime > 500) {
			String url = request.getRequestURI();
			String qstr = request.getQueryString();
			if (qstr != null) url += '?' + qstr;
			LOG.warn("Method handle time " + startTime + " ms. Request URL [ " + url + " ] from [ " + StringUtils.defaultIfEmpty(ServletUtils.getReferer(request), "-") + " ]");
		}
	}
}
