<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<% final String activeNav = request.getParameter("activeNav"); %>
<div class="rb-left-sidebar">
<div class="left-sidebar-wrapper">
	<a class="left-sidebar-toggle">MIN</a>
	<div class="left-sidebar-spacer">
		<div class="left-sidebar-scroll rb-scroller">
			<div class="left-sidebar-content">
				<ul class="sidebar-elements">
					<li class="divider">系统</li>
					<li class="<%="systems".equals(activeNav) ? "active" : ""%>" id="nav_systems"><a href="${baseUrl}/admin/systems"><i class="icon zmdi zmdi-settings"></i><span>通用配置</span></a></li>
					<li class="parent">
						<a href="javascript:;"><i class="icon zmdi zmdi-cloud-done"></i><span>第三方服务集成</span></a>
						<ul class="sub-menu">
							<li class="title">第三方服务集成</li>
							<li class="nav-items">
								<div class="rb-scroller">
									<div class="content">
										<ul>
											<li class="<%="plugins-storage".equals(activeNav) ? "active" : ""%>" id="nav_plugins-storage"><a href="${baseUrl}/admin/plugins/storage">云存储</a></li>
											<li class="<%="plugins-cache".equals(activeNav) ? "active" : ""%>" id="nav_plugins-cache"><a href="${baseUrl}/admin/plugins/cache">缓存系统</a></li>
											<li class="<%="plugins-sms".equals(activeNav) ? "active" : ""%>" id="nav_plugins-sms"><a href="${baseUrl}/admin/plugins/sms">短信服务</a></li>
											<li class="<%="plugins-mail".equals(activeNav) ? "active" : ""%>" id="nav_plugins-mail"><a href="${baseUrl}/admin/plugins/mail">邮件服务</a></li>
										</ul>
									</div>
								</div>
							</li>
						</ul>
					</li>
					<li class="divider">业务/实体</li>
					<li class="<%="entities".equals(activeNav) ? "active" : ""%>" id="nav_entities"><a href="${baseUrl}/admin/entities"><i class="icon zmdi zmdi-widgets"></i><span>实体管理</span></a></li>
					<li class="<%="audit-logging".equals(activeNav) ? "active" : ""%>" id="nav_audit-logging"><a href="${baseUrl}/admin/audit-logging"><i class="icon zmdi zmdi-assignment-check"></i><span>审计日志</span></a></li>
					<li class="divider">用户</li>
					<li class="<%="users".equals(activeNav) ? "active" : ""%>" id="nav_user-list"><a href="${baseUrl}/admin/bizuser/users"><i class="icon zmdi zmdi-accounts"></i><span>部门用户</span></a></li>
					<li class="<%="role-privileges".equals(activeNav) ? "active" : ""%>" id="nav_role-list"><a href="${baseUrl}/admin/bizuser/role-privileges"><i class="icon zmdi zmdi-lock"></i><span>角色权限</span></a></li>
				</ul>
			</div>
		</div>
	</div>
</div>
</div>