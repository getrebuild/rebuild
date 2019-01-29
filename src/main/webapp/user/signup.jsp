<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>用户注册</title>
</head>
<body class="rb-splash-screen">
<div class="rb-wrapper rb-login">
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="splash-container">
				<div class="card card-border-color card-border-color-primary">
					<div class="card-header"><a class="logo-img"></a></div>
					<div class="card-body">
						<div class="form-group">
							<input class="form-control" id="user" type="text" placeholder="用户名" autocomplete="off">
						</div>
						<div class="form-group">
							<input class="form-control" id="user" type="text" placeholder="注册邮箱" autocomplete="off">
						</div>
						<div class="form-group">
							<input class="form-control" id="passwd" type="password" placeholder="登录密码">
						</div>
						<div class="form-group login-submit">
							<button class="btn btn-primary btn-xl J_login-btn" disabled="disabled">未开放注册</button>
						</div>
					</div>
				</div>
				<div class="splash-footer">
					<span><a href="login">返回登录</a></span>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/babel">
$(document).ready(function() {
});
</script>
</body>
</html>
