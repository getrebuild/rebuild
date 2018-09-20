<%@page import="cn.devezhao.commons.CodecUtils"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="cn.devezhao.rebuild.server.Startup"%>
<%@page import="cn.devezhao.rebuild.utils.AppUtils"%>
<%@page import="com.alibaba.fastjson.JSONObject"%>
<%@page import="com.alibaba.fastjson.JSONArray"%>
<%@page import="cn.devezhao.rebuild.server.service.base.NavManager"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<% final String activeNav = request.getParameter("activeNav"); %>
<div class="rb-left-sidebar">
<div class="left-sidebar-wrapper">
	<div class="left-sidebar-spacer">
		<div class="left-sidebar-scroll rb-scroller">
			<div class="left-sidebar-content">
				<ul class="sidebar-elements">
					<li class="<%="dashboard-home".equals(activeNav) ? "active" : ""%>" id="nav_dashboard-home"><a href="${baseUrl}/dashboard/home"><i class="icon zmdi zmdi-home"></i><span>首页</span></a></li>
					<%
					JSONArray navs = NavManager.getNavForPortal();
					for (Object o : navs) {
						JSONObject nav = (JSONObject) o;
						String navName = "nav_entity-" + nav.getString("value");
						boolean isUrlType = "URL".equals(nav.getString("type"));
						String navUrl = nav.getString("value");
						if (!isUrlType) {
							navUrl = Startup.getContextPath() + "/app/" + navUrl + "/list";
						} else {
							navName = "nav_url-" + System.currentTimeMillis();
							navUrl = Startup.getContextPath() + "/common/url-safe?url=" + CodecUtils.urlEncode(navUrl);
						}
						String navIcon = StringUtils.defaultIfBlank(nav.getString("icon"), "texture");
					%>
					<li id="<%=navName%>" class="<%=navName.equals(activeNav) ? "active" : ""%>"><a href="<%=navUrl%>" target="<%=isUrlType ? "_blank" : "_self"%>"><i class="icon zmdi zmdi-<%=navIcon%>"></i><span><%=nav.getString("text")%></span></a></li>
					<%}%>
				</ul>
			</div>
		</div>
	</div>
	<div class="bottom-widget">
		<a class="nav-settings" href="javascript:;"><i class="icon zmdi zmdi-apps"></i> 导航设置</a>
	</div>
</div>
</div>