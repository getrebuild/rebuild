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
			<a class="navbar-brand" href="${baseUrl}/dashboard/home" title="返回首页"></a>
			<a class="rb-toggle-left-sidebar" title="展开/收缩菜单"><span class="icon zmdi zmdi-menu"></span></a>
		</div>
		<div class="rb-right-navbar">
			<ul class="nav navbar-nav float-right rb-user-nav">
				<li class="nav-item dropdown">
					<a class="nav-link dropdown-toggle" href="${baseUrl}/me/profile" data-toggle="dropdown">
						<img src="<%=currentUser.getAvatarUrl(true)%>" alt="Avatar">
						<span class="user-name"><%=showName%></span>
					</a>
					<div class="dropdown-menu">
						<div class="user-info">
							<div class="user-name"><%=showName%></div>
							<div class="user-id"><%=StringUtils.defaultIfBlank(currentUser.getEmail(), "邮箱未设置")%></div>
						</div>
						<a class="dropdown-item" href="${baseUrl}/settings/account"><i class="icon zmdi zmdi-account-box"></i>个人设置</a>
						<a class="dropdown-item" href="${baseUrl}/user/logout"><i class="icon zmdi zmdi-power"></i>退出</a>
					</div>
				</li>
			</ul>
			<div class="page-title"><span><%=request.getParameter("pageTitle")%></span></div>
			<ul class="nav navbar-nav float-right rb-icons-nav">
				<% if (currentUser.isAdmin()) { %>
				<li class="nav-item dropdown J_admin-settings">
					<a class="nav-link" href="${baseUrl}/admin/systems"><i class="icon zmdi zmdi-settings"></i></a>
				</li>
				<%} %>
				<li class="nav-item dropdown">
					<a class="nav-link dropdown-toggle" data-toggle="dropdown--disable" href="${baseUrl}/app/notifications"><i class="icon zmdi zmdi-notifications"></i><span class="indicator hide"></span></a>
					<ul class="dropdown-menu rb-notifications">
					<li>
						<div class="title">未读 <span class="badge badge-pill">0</span></div>
						<div class="list">
							<div class="rb-scroller-notifications">
								<div class="content">
									<ul>
										<li class="notification notification-unread">
											<a href="${baseUrl}/app/notification/123">
												<div class="image"><img src="${baseUrl}/assets/img/avatar.png" alt="Avatar"></div>
												<div class="notification-info">
													<div class="text"><span class="user-name">Jessica Caruso</span> accepted your invitation to join the team.</div>
													<span class="date">2 min ago</span>
												</div>
											</a>
										</li>
									</ul>
								</div>
							</div>
						</div>
						<div class="footer"><a href="${baseUrl}/app/notifications">查看全部</a></div>
					</li>
					</ul>
				</li>
			</ul>
		</div>
	</div>
</nav>
<div class="rb-scroll-top"></div>