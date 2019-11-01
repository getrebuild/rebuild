<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>动态</title>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/feeds.css">
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="动态" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="nav_entity-Feeds" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container feeds-container">
			<div class="row bg-white">
				<div class="col-md-8 col-12 contents">
					<div class="postbox">
						<h4>发布动态</h4>
						<textarea class="form-control"></textarea>
					</div>
				</div>
				<div class="col-md-4 col-12 tools">TOOLS</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/feeds.jsx" type="text/babel"></script>
</body>
</html>
