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
			<div class="row">
				<div class="col-md-8 col-12 p-0">
                    <div class="rb-loading rb-loading-active">
                        <%@ include file="/_include/spinner.jsp"%>
                    </div>
                    <div id="feedsPost"></div>
                    <div id="feedsList"></div>
				</div>
				<div class="col-md-4 col-12 pr-0">
					<div class="side-wrapper">
						<div>
							<h5 class="mt-0">User Profile</h5>
						</div>
						<div>
							<h5>群组</h5>
						</div>
						<div>
							<h5>人员</h5>
						</div>
					</div>
                </div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/feeds/feeds-post.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/feeds/feeds-list.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/feeds/feeds.jsx" type="text/babel"></script>
</body>
</html>
