<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.rebuild.web.admin.AdminEntryControll"%>
<%@ page import="com.rebuild.utils.AppUtils"%>
<%@ page import="com.rebuild.server.Application"%>
<%@ page import="com.rebuild.server.bizz.privileges.User"%>
<%@ page import="com.rebuild.server.bizz.UserHelper"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%
final User currentUser = Application.getUserStore().getUser(AppUtils.getRequestUser(request));
final String showName = UserHelper.getShowName(currentUser);
%>
<nav class="navbar navbar-expand fixed-top rb-top-header">
	<div class="container-fluid">
		<div class="rb-navbar-header">
			<a class="navbar-brand" href="${baseUrl}/dashboard/home"></a>
			<a class="rb-toggle-left-sidebar" title="展开/收缩菜单"><span class="icon zmdi zmdi-menu"></span></a>
		</div>
		<div class="rb-right-navbar">
			<ul class="nav navbar-nav float-right rb-user-nav">
				<li class="nav-item dropdown">
					<a class="nav-link dropdown-toggle" href="${baseUrl}/me/profile" data-toggle="dropdown">
						<img src="<%=UserHelper.getAvatarUrl(currentUser)%>" alt="Avatar">
						<span class="user-name"><%=showName%></span>
					</a>
					<div class="dropdown-menu">
						<div class="user-info">
							<div class="user-name"><%=showName%></div>
							<div class="user-id"><%=StringUtils.defaultIfBlank(currentUser.getEmail(), "邮箱未设置")%></div>
						</div>
						<a class="dropdown-item" href="${baseUrl}/me/profile"><i class="icon zmdi zmdi-account-box"></i>个人设置</a>
						<a class="dropdown-item" href="${baseUrl}/user/logout"><i class="icon zmdi zmdi-power"></i>退出</a>
					</div>
				</li>
			</ul>
			<div class="page-title"><span><%=request.getParameter("pageTitle")%></span></div>
			<ul class="nav navbar-nav float-right rb-icons-nav">
				<% if (currentUser.isAdmin()) { %>
				<li class="nav-item dropdown J_admin-settings" data-verified="<%=AdminEntryControll.isAdminVerified(request)%>">
					<a class="nav-link" href="${baseUrl}/admin/systems" title="系统配置"><i class="icon zmdi zmdi-settings"></i></a>
				</li>
				<%} %>
				<li class="nav-item dropdown">
					<a class="nav-link" href="${baseUrl}/app/notifications" title="通知"><i class="icon zmdi zmdi-notifications"></i></a>
				</li>
			</ul>
		</div>
	</div>
</nav>
<div class="rb-scroll-top"></div>