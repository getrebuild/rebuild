<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>通知</title>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-offcanvas-menu">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="通知" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="notifications" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container">
			<div class="tab-container">
				<ul class="nav nav-tabs nav-tabs-classic notification-tab">
					<li class="nav-item">
						<a class="nav-link active" href="notifications"><span class="icon zmdi zmdi-notifications"></span> 通知</a>
					</li>
					<li class="nav-item">
						<a class="nav-link" href="notifications/todo"><span class="icon zmdi zmdi-alarm-check"></span> 待办</a>
					</li>
					<button class="btn btn-secondary read-all" type="button"><i class="zmdi zmdi-check-all icon"></i> 已读全部</button>
				</ul>
				<div class="tab-content">
					<div class="tab-pane active">
						<div class="row">
							<div class="col-md-3 col-12">
								<div class="list-group notification-type">
									<a href="#unread" data-type="1" class="list-group-item d-flex list-group-item-action active">
										<span class="text">未读</span>
									</a>
									<a href="#read" data-type="2" class="list-group-item d-flex list-group-item-action">
										<span class="text">已读</span>
									</a>
									<a href="#assigns" data-type="10" class="list-group-item d-flex list-group-item-action">
										<span class="text">分派/共享通知</span>
									</a>
									<a href="#approval" data-type="20" class="list-group-item d-flex list-group-item-action">
										<span class="text">审批通知</span>
									</a>
									<a href="#all" data-type="3" class="list-group-item d-flex list-group-item-action">
										<span class="text">全部通知</span>
									</a>
								</div>
							</div>
							<div class="col-md-9 col-12" id="message-list">
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/notifications.jsx" type="text/babel"></script>
</body>
</html>
