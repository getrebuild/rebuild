<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<% final String activeNav = request.getParameter("activeNav"); %>
<div class="rb-left-sidebar">
<div class="left-sidebar-wrapper">
	<div class="left-sidebar-spacer">
		<div class="left-sidebar-scroll rb-scroller">
			<div class="left-sidebar-content">
				<ul class="sidebar-elements">
					<li class="<%="dashboard-home".equals(activeNav) ? "active" : ""%>" id="nav_dashboard-home"><a href="${baseUrl}/dashboard/home"><i class="icon zmdi zmdi-home"></i><span>首页</span></a></li>
					<li class="<%="entitylist-User".equals(activeNav) ? "active" : ""%>" id="nav_entitylist-User"><a href="${baseUrl}/entity/User/list"><i class="icon zmdi zmdi-account"></i><span>首页</span></a></li>
					<li class="<%="entitylist-ceshiziduanleixing".equals(activeNav) ? "active" : ""%>" id="nav_entitylist-ceshiziduanleixing"><a href="${baseUrl}/entity/ceshiziduanleixing/list"><i class="icon zmdi zmdi-account"></i><span>首页</span></a></li>
				</ul>
			</div>
		</div>
	</div>
</div>
</div>