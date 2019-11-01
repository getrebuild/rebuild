<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>文件</title>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/feeds.css">
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="文件" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="nav_entity-Attachment" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container">
			<div class="row">
				<div class="col-xs-9 col-12">CONTENTS</div>
				<div class="col-xs-3 col-12">TOOLS</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/feeds.jsx" type="text/babel"></script>
</body>
</html>
