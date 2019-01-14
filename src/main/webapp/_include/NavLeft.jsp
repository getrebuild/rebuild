<%@ page import="com.alibaba.fastjson.JSONObject"%>
<%@ page import="com.alibaba.fastjson.JSONArray"%>
<%@ page import="com.rebuild.server.helper.manager.NavManager"%>
<%
final String activeNav = request.getParameter("activeNav");
final JSONArray navArray = NavManager.getNavForPortal(request);
%>
<div class="rb-left-sidebar">
<div class="left-sidebar-wrapper">
	<a class="left-sidebar-toggle">IN-MIN</a>
	<div class="left-sidebar-spacer">
		<div class="left-sidebar-scroll rb-scroller">
			<div class="left-sidebar-content no-divider">
				<ul class="sidebar-elements">
					<li class="<%="dashboard-home".equals(activeNav) ? "active" : ""%>" id="nav_dashboard-home"><a href="${baseUrl}/dashboard/home"><i class="icon zmdi zmdi-home"></i><span>首页</span></a></li>
					<% for (Object o : navArray) { out.print(NavManager.renderNavItem((JSONObject) o, activeNav, true)); } %>
				</ul>
			</div>
		</div>
	</div>
	<div class="bottom-widget">
		<a class="nav-settings" href="javascript:;" title="设置导航菜单"><i class="icon zmdi zmdi-apps"></i></a>
	</div>
</div>
</div>