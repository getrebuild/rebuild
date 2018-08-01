<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<% final String activeNav = request.getParameter("activeNav"); %>
<div class="rb-left-sidebar">
<div class="left-sidebar-wrapper">
	<div class="left-sidebar-spacer">
		<div class="left-sidebar-scroll rb-scroller">
			<div class="left-sidebar-content">
				<ul class="sidebar-elements">
					<li class="divider">通用</li>
					<li class="<%="general".equals(activeNav) ? "active" : ""%>" id="nav_general"><a href="${baseUrl}/admin/general.htm"><i class="icon zmdi zmdi-settings"></i><span>通用配置</span></a></li>
					<li class="divider">实体</li>
					<li class="<%="entity-manage".equals(activeNav) ? "active" : ""%>" id="nav_entity-manage"><a href="${baseUrl}/admin/entity/entity-manage.htm"><i class="icon zmdi zmdi-apps"></i><span>实体管理</span></a></li>
					<li class="divider">用户</li>
					<li class="<%="user-list".equals(activeNav) ? "active" : ""%>" id="nav_user-list"><a href="${baseUrl}/admin/bizuser/user-list.htm"><i class="icon zmdi zmdi-account"></i><span>用户管理</span></a></li>
					<li class="<%="dept-list".equals(activeNav) ? "active" : ""%>" id="nav_dept-list"><a href="${baseUrl}/admin/entity/dept-list.htm"><i class="icon zmdi zmdi-accounts"></i><span>部门管理</span></a></li>
					<li class="<%="role-list".equals(activeNav) ? "active" : ""%>" id="nav_role-list"><a href="${baseUrl}/admin/entity/role-list.htm"><i class="icon zmdi zmdi-lock"></i><span>角色权限</span></a></li>
				</ul>
			</div>
		</div>
	</div>
</div>
</div>