<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>待办</title>
<style type="text/css">
.notification-info .badge {
    border-radius: 99px;
    font-weight: normal;
    font-size: 12px;
    line-height: 1.5;
}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-offcanvas-menu">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="待办" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="notifications" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container">
			<div class="tab-container">
				<ul class="nav nav-tabs nav-tabs-classic notification-tab">
					<li class="nav-item">
						<a class="nav-link" href="../notifications"><span class="icon zmdi zmdi-notifications"></span> 通知</a>
					</li>
					<li class="nav-item">
						<a class="nav-link active" href="todo"><span class="icon zmdi zmdi-alarm-check"></span> 待办</a>
					</li>
				</ul>
				<div class="tab-content">
					<div class="tab-pane active">
						<div class="row">
							<div class="col-md-3 col-12">
								<div class="list-group notification-type">
									<a href="#approval" data-type="20" class="list-group-item d-flex list-group-item-action active">
										<span class="text">待我审批</span>
										<span class="badge badge-pill badge-primary hide">0</span>
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
<script>
window.__PageConfig = {
	type: 'Approval'
}
</script>
<script src="${baseUrl}/assets/js/notifications.jsx" type="text/babel"></script>
</body>
</html>
