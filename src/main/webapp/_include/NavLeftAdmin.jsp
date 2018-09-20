<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<% final String activeNav = request.getParameter("activeNav"); %>
<div class="rb-left-sidebar">
<div class="left-sidebar-wrapper">
	<div class="left-sidebar-spacer">
		<div class="left-sidebar-scroll rb-scroller">
			<div class="left-sidebar-content">
				<ul class="sidebar-elements">
					<li class="divider">通用</li>
					<li class="<%="systems".equals(activeNav) ? "active" : ""%>" id="nav_systems"><a href="${baseUrl}/admin/systems"><i class="icon zmdi zmdi-settings"></i><span>系统配置</span></a></li>
					<li class="divider">实体</li>
					<li class="<%="entities".equals(activeNav) ? "active" : ""%>" id="nav_entities"><a href="${baseUrl}/admin/entities"><i class="icon zmdi zmdi-widgets"></i><span>实体管理</span></a></li>
					<li class="divider">用户</li>
					<li class="<%="users".equals(activeNav) ? "active" : ""%>" id="nav_user-list"><a href="${baseUrl}/admin/bizuser/users"><i class="icon zmdi zmdi-account"></i><span>用户管理</span></a></li>
					<li class="<%="departments".equals(activeNav) ? "active" : ""%>" id="nav_dept-list"><a href="${baseUrl}/admin/bizuser/departments"><i class="icon zmdi zmdi-accounts"></i><span>部门管理</span></a></li>
					<li class="<%="role-privileges".equals(activeNav) ? "active" : ""%>" id="nav_role-list"><a href="${baseUrl}/admin/bizuser/role-privileges"><i class="icon zmdi zmdi-lock"></i><span>角色权限</span></a></li>
				</ul>
			</div>
		</div>
	</div>
</div>
</div>