<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>用户登录</title>
<style type="text/css">
</style>
</head>
<body class="rb-splash-screen">
<div class="rb-wrapper rb-login">
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="splash-container">
				<div class="card card-border-color card-border-color-primary">
					<div class="card-header">
						<h1 style="margin:0;font-weight:bold;">REBUILD</h1>
					</div>
					<div class="card-body">
						<div class="form-group">
							<input class="form-control" id="user" type="text" placeholder="用户名 (或邮箱)" autocomplete="off">
						</div>
						<div class="form-group">
							<input class="form-control" id="passwd" type="password" placeholder="密码">
						</div>
						<div class="form-group row login-tools">
							<div class="col-6 login-remember">
								<label class="custom-control custom-checkbox">
									<input class="custom-control-input" type="checkbox">
									<span class="custom-control-label">记住登录</span>
								</label>
							</div>
							<div class="col-6 login-forgot-password">
								<a href="forgot-passwd">找回密码</a>
							</div>
						</div>
						<div class="form-group login-submit">
							<button class="btn btn-primary btn-xl J_login-btn">登录</button>
						</div>
					</div>
				</div>
				<div class="splash-footer">
					<span>&copy; 2018 <a href="https://github.com/devezhao/re-build/" target="_blank">Rebuild</a></span>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
$(document).ready(function() {
	$('.J_login-btn').click(function() {
		let user = $('#user').val(),
			passwd = $('#passwd').val();
		if (!user || !passwd){ alert('请输入用户名和密码'); return; }
		
		let reqdata = { user: user, passwd: passwd };
		$.post(__baseUrl + '/user/user-login', reqdata, function(res) {
			if (res.error_code == 0) location.replace('../dashboard/home.htm');
			else alert(res.error_msg);
		});
	});
});
</script>
</body>
</html>
