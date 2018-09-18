<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<% final String activeNav = request.getParameter("activeNav"); %>
<div class="rb-left-sidebar">
<div class="left-sidebar-wrapper">
	<div class="left-sidebar-spacer">
		<div class="left-sidebar-scroll rb-scroller">
			<div class="left-sidebar-content">
				<ul class="sidebar-elements">
					<li class="<%="dashboard-home".equals(activeNav) ? "active" : ""%>" id="nav_dashboard-home"><a href="${baseUrl}/dashboard/home"><i class="icon zmdi zmdi-home"></i><span>首页</span></a></li>
					<li class="<%="dashboard-home".equals(activeNav) ? "active" : ""%>" id="nav_dashboard-home"><a href="${baseUrl}/dashboard/home"><i class="icon zmdi zmdi-home"></i><span>首页</span></a></li>
					<li class="<%="dashboard-home".equals(activeNav) ? "active" : ""%>" id="nav_dashboard-home"><a href="${baseUrl}/dashboard/home"><i class="icon zmdi zmdi-home"></i><span>首页</span></a></li>
					<li class="<%="dashboard-home".equals(activeNav) ? "active" : ""%>" id="nav_dashboard-home"><a href="${baseUrl}/dashboard/home"><i class="icon zmdi zmdi-home"></i><span>首页</span></a></li>
					<li class="<%="dashboard-home".equals(activeNav) ? "active" : ""%>" id="nav_dashboard-home"><a href="${baseUrl}/dashboard/home"><i class="icon zmdi zmdi-home"></i><span>首页</span></a></li>
					<li class="<%="dashboard-home".equals(activeNav) ? "active" : ""%>" id="nav_dashboard-home"><a href="${baseUrl}/dashboard/home"><i class="icon zmdi zmdi-home"></i><span>首页</span></a></li>
				</ul>
			</div>
		</div>
	</div>
	<div class="progress-widget">
		<a class="nav-settings" href="#">导航设置</a>
	</div>
</div>
</div>