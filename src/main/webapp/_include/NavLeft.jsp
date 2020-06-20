<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.alibaba.fastjson.JSONObject"%>
<%@ page import="com.alibaba.fastjson.JSONArray"%>
<%@ page import="com.rebuild.utils.AppUtils" %>
<%@ page import="com.rebuild.server.service.bizz.privileges.ZeroEntry" %>
<%@ page import="com.rebuild.server.configuration.portals.NavBuilder" %>
<%
final String activeNav = request.getParameter("activeNav");
final JSONArray navArray = NavBuilder.instance.getNavPortal(request);
%>
<div class="rb-left-sidebar">
<div class="left-sidebar-wrapper">
	<a class="left-sidebar-toggle">导航</a>
	<div class="left-sidebar-spacer">
		<div class="left-sidebar-scroll rb-scroller">
			<div class="left-sidebar-content no-divider">
				<ul class="sidebar-elements">
					<li class="<%="dashboard-home".equals(activeNav) ? "active" : ""%>"><a href="${baseUrl}/dashboard/home"><i class="icon zmdi zmdi-home"></i><span>首页</span></a></li>
					<% for (Object o : navArray) out.print(NavBuilder.instance.renderNavItem((JSONObject) o, activeNav)); %>
				</ul>
			</div>
		</div>
	</div>
	<% if (AppUtils.allow(request, ZeroEntry.AllowCustomNav)) { %>
	<div class="bottom-widget">
		<a class="nav-settings" title="设置导航菜单"><i class="icon zmdi zmdi-apps"></i></a>
	</div>
	<% } %>
</div>
</div>