<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.rebuild.server.Application"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>REBUILD 安装程序</title>
<style type="text/css">
.splash-container.rb-install {
	max-width: 680px;
}
.splash-footer {
	padding-top: 20px;
	padding-bottom: 15px;
	text-align: left;
}
.splash-footer .btn.btn-link {
	color: #555;
}
.splash-container .card .card-header {
	padding: 30px 0;
}
.card-body h3 {
	text-align: center;
	color: #666;
	margin: 30px 0;
	padding: 0;
}
.rb-welcome li>a {
	display: block;
	border: 1px solid #eee;
	padding: 15px 20px;
	margin-bottom: 10px;
	border-radius: 2px;
}
.rb-welcome li>a:hover {
	border: 1px solid #4285f4;
	background-color: #E6EFF8;
}
.rb-finish {
	padding: 40px 0;
}
.rb-finish .zmdi.icon {
	font-size: 48px;
	color: #999;
}
.card-body .progress {
	border-radius: 0;
	height: 2px;
	margin-top: 20px;
	background-color: #eee;
}
</style>
</head>
<body class="rb-splash-screen">
<div class="rb-wrapper">
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="splash-container rb-install">
				<div class="card shadow-lg">
					<div class="card-header bg-primary m-0"><a class="logo-img white"></a></div>
					<div class="card-body pt-0">
						<h2 class="text-center text-muted">请稍后 ...</h2>
					</div>
				</div>
				<div class="copyright">
					&copy; REBUILD (<%=Application.VER%>) &nbsp;·&nbsp; <a href="https://getrebuild.com/docs/admin/install" target="_blank" class="link">安装手册</a>
					<div class="mt-1 license">
						REBUILD 使用开源 <a href="https://getrebuild.com/license/LICENSE.txt" target="_blank" class="link">GPL-3.0</a> 和 <a href="https://getrebuild.com/license/COMMERCIAL.txt" target="_blank" class="link">商用</a> 双重授权许可，安装/使用即表示你已阅读并同意许可内容。</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = {
	defaultDataDirectory: '${defaultDataDirectory}',
	defaultAppName: '${defaultAppName}',
	defaultHomeURL: '${defaultHomeURL}'
}
</script>
<script src="${baseUrl}/assets/js/setup/install.jsx" type="text/babel"></script>
</body>
</html>