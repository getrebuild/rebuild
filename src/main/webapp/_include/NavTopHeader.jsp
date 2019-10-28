<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.rebuild.utils.AppUtils"%>
<%@ page import="com.rebuild.server.Application"%>
<%@ page import="com.rebuild.server.service.bizz.privileges.User"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%
final User currentUser = Application.getUserStore().getUser(AppUtils.getRequestUser(request));
%>
<nav class="navbar navbar-expand fixed-top rb-top-header">
	<div class="container-fluid">
		<div class="rb-navbar-header">
			<a class="navbar-brand" href="${baseUrl}/dashboard/home"></a>
		</div>
		<div class="rb-right-navbar">
			<ul class="nav navbar-nav float-right rb-user-nav">
				<li class="nav-item dropdown">
					<a class="nav-link dropdown-toggle" href="${baseUrl}/account/settings" data-toggle="dropdown">
						<img src="${baseUrl}/account/user-avatar" alt="Avatar">
						<span class="user-name"><%=currentUser.getFullName()%></span>
					</a>
					<div class="dropdown-menu">
						<div class="user-info">
							<div class="user-name"><%=currentUser.getFullName()%></div>
							<div class="user-id"><%=StringUtils.defaultIfBlank(currentUser.getEmail(), "邮箱未设置")%></div>
						</div>
						<a class="dropdown-item" href="${baseUrl}/account/settings"><span class="icon zmdi zmdi-account-box"></span>个人设置</a>
						<a class="dropdown-item" href="${baseUrl}/user/logout"><span class="icon zmdi zmdi-power"></span>退出</a>
					</div>
				</li>
			</ul>
		</div>
	</div>
</nav>