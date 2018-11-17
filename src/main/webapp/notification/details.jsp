<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>通知</title>
<style type="text/css">
.card-title{border-bottom:1px solid #ebebeb;}
</style>
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
			<div class="card">
				<div class="card-body">
					<div class="card-title pb-2">
						<div class="float-left">
							<button class="btn btn-secondary btn-space" onclick="location.href='${baseUrl}/app/notifications'" type="button"><i class="zmdi zmdi-long-arrow-left icon"></i> 返回列表</button>
						</div>
						<div class="float-right">
							<button class="btn btn-secondary btn-space" type="button"><i class="zmdi zmdi-notifications-active icon"></i> 设为未读</button>
						</div>
						<div class="clearfix"></div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/babel">
</script>
</body>
</html>
