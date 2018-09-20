<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<nav class="navbar navbar-expand fixed-top rb-top-header">
	<div class="container-fluid">
		<div class="rb-navbar-header">
			<a class="navbar-brand" href="${baseUrl}/dashboard/home"></a>
			<a class="rb-toggle-left-sidebar" title="展开/收缩菜单"><span class="icon zmdi zmdi-menu"></span></a>
		</div>
		<div class="rb-right-navbar">
			<ul class="nav navbar-nav float-right rb-user-nav">
				<li class="nav-item dropdown">
					<a class="nav-link dropdown-toggle" href="${baseUrl}/my/profile" data-toggle="dropdown"><img src="${baseUrl}/assets/img/avatar.png" alt="Avatar"><span class="user-name">admin</span></a>
					<div class="dropdown-menu">
						<div class="user-info">
							<div class="user-name">admin</div>
							<div class="user-id">hello@getrebuild.com</div>
						</div>
						<a class="dropdown-item" href="${baseUrl}/my/profile"><span class="icon zmdi zmdi-face"></span>个人信息</a>
						<a class="dropdown-item" href="${baseUrl}/user/logout"><span class="icon zmdi zmdi-power"></span>退出</a>
					</div>
				</li>
			</ul>
			<div class="page-title"><span><%=request.getParameter("pageTitle")%></span></div>
			<ul class="nav navbar-nav float-right rb-icons-nav">
				<li class="nav-item dropdown">
					<a class="nav-link rb-toggle-right-sidebar" href="${baseUrl}/admin/systems" title="系统设置"><span class="icon zmdi zmdi-settings"></span></a>
				</li>
				<li class="nav-item dropdown">
					<a class="nav-link dropdown-toggle" href="${baseUrl}/app/messages" title="消息中心"><span class="icon zmdi zmdi-notifications"></span></a>
				</li>
			</ul>
		</div>
	</div>
</nav>
<div class="rb-scroll-top"></div>