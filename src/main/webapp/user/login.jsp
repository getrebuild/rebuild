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
						<img class="logo-img" src="../assets/img/logo.png" alt="REBUILD">
					</div>
					<div class="card-body">
						<div class="form-group">
							<input class="form-control" id="user" type="text" placeholder="用户名 (或邮箱)" autocomplete="off">
						</div>
						<div class="form-group">
							<input class="form-control" id="passwd" type="password" placeholder="登录密码">
						</div>
						<div class="form-group row login-tools">
								
							<div class="col-6 login-remember">
								<label class="custom-control custom-checkbox custom-control-inline mb-0">
									<input class="custom-control-input" type="checkbox" id="autoLogin"><span class="custom-control-label"> 记住登录</span>
								</label>
							</div>
							<div class="col-6 login-forgot-password">
								<a href="forgot-passwd">找回密码</a>
							</div>
						</div>
						<div class="form-group login-submit">
							<button class="btn btn-primary btn-xl J_login-btn" data-loading-text="登录中">登录</button>
						</div>
					</div>
				</div>
				<div class="splash-footer">
					<span>&copy; 2018 <a href="https://getrebuild.com/" target="_blank">Rebuild</a></span>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/babel">
$(document).ready(function() {
	if (top != self) { parent.location.reload(); return }

	$('#user,#passwd').keydown(function(e){
		if (e.keyCode == 13) $('.J_login-btn').trigger('click')
	})
	let _btn = $('.J_login-btn').click(function() {
		let user = $val('#user'), passwd = $val('#passwd')
		if (!user || !passwd) return
		
		_btn.button('loading')
		$.post(rb.baseUrl + '/user/user-login?user=' + $encode(user) + '&passwd=' + $encode(passwd) + '&autoLogin=' + $val('#autoLogin'), function(res) {
			if (res.error_code == 0) location.replace($decode($urlp('nexturl') || '../dashboard/home'))
			else{
				rb.notice(res.error_msg || '登录失败，请稍后重试')
				_btn.button('reset')
			}
		})
	})
})
</script>
</body>
</html>
