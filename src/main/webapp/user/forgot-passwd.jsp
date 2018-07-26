<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>重置密码</title>
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
							<input class="form-control" id="email" type="text" placeholder="邮箱" autocomplete="off">
						</div>
						<div class="form-group login-submit">
							<button class="btn btn-primary btn-xl J_forgot-btn">重置密码</button>
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
<script type="text/javascript">
$(document).ready(function() {
	$('.J_forgot-btn').click(function() {
		let email = $('#email').val();
		if (!email){ alert('请输入邮箱'); return; }
		
		let reqdata = { email: email };
		$.post(__baseUrl + '/user/user-forgot-passwd', reqdata, function(res) {
			if (res.error_code == 0) location.replace('../dashboard/home');
			else alert(res.error_msg);
		});
	});
});
</script>
</body>
</html>
