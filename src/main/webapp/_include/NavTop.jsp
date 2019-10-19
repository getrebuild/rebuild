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
			<a class="rb-toggle-left-sidebar" title="展开/收缩导航菜单"><span class="icon zmdi zmdi-menu"></span></a>
		</div>
        <div class="search-container">
            <input class="form-control form-control-sm search-input" type="text" name="search" maxlength="100" placeholder="搜索" />
            <div class="search-models animated fadeIn faster"></div>
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
						<a class="dropdown-item" href="${baseUrl}/account/settings"><i class="icon zmdi zmdi-account-box"></i>个人设置</a>
						<a class="dropdown-item" href="${baseUrl}/user/logout"><i class="icon zmdi zmdi-power"></i>退出</a>
					</div>
				</li>
			</ul>
			<div class="page-title hide"><span>${param['pageTitle']}</span></div>
			<ul class="nav navbar-nav float-right rb-icons-nav">
				<li class="nav-item dropdown admin-show admin-settings">
					<a class="nav-link" href="${baseUrl}/admin/systems"><i class="icon zmdi zmdi-settings"></i></a>
				</li>
				<li class="nav-item dropdown J_notifications-top">
					<a class="nav-link dropdown-toggle" data-toggle="dropdown" href="${baseUrl}/notifications"><i class="icon zmdi zmdi-notifications"></i><span class="indicator hide"></span></a>
					<ul class="dropdown-menu rb-notifications">
					<li>
						<div class="title">未读 <span class="badge badge-pill">0</span></div>
						<div class="list">
							<div class="rb-scroller">
								<div class="content">
									<ul></ul>
								</div>
							</div>
						</div>
						<div class="footer"><a href="${baseUrl}/notifications">查看全部</a></div>
					</li>
					</ul>
				</li>
			</ul>
		</div>
	</div>
</nav>
<div class="rb-scroll-top"></div>