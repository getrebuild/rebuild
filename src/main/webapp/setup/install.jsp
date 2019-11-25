<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.rebuild.server.Application" %>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<style type="text/css">
.splash-container.rb-install {
	max-width: 680px;
}
.splash-footer {
	border-top: 1px solid #eee;
	margin-top: 10px;
	padding-top: 20px;
	padding-bottom: 15px;
	text-align: left;
}
.card-body {
	min-height: 242px;
}
.rb-welcome li>a {
	display: block;
	border: 1px solid #eee;
	padding: 15px 20px;
	margin-bottom: 10px;
}
.rb-welcome li>a:hover {
	border: 1px solid #4285f4;
	outline: 1px solid #4285f4;
}
.rb-finish {
	padding-top: 40px;
}
.rb-finish .zmdi.icon {
	font-size: 48px;
	color: #999;
}
</style>
<title>安装程序</title>
</head>
<body class="rb-splash-screen">
<div class="rb-wrapper">
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="splash-container rb-install">
				<div class="card border-danger">
					<div class="card-header"><a class="logo-img"></a></div>
					<div class="card-body pt-0">
						<div class="rb-loading rb-loading-active must-center">
							<jsp:include page="../_include/spinner.jsp"/>
						</div>
					</div>
				</div>
				<div class="copyright">&copy; 2019 <a href="https://getrebuild.com/" target="_blank">REBUILD</a> (<%=Application.VER%>)</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/setup/install.jsx" type="text/babel"></script>
</body>
</html>