<%@page import="com.rebuild.server.Application"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page import="com.rebuild.server.ServerListener"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>用户登录</title>
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
							<input class="form-control" id="user" type="text" placeholder="用户名 (或邮箱)" autocomplete="off">
						</div>
						<div class="form-group">
							<input class="form-control" id="passwd" type="password" placeholder="登录密码">
						</div>
						<c:if test="${sessionScope.needVcode != null}">
						<div class="form-group row pt-0 mt-2">
							<div class="col-sm-6">
								<input class="form-control" id="vcode" type="text" placeholder="验证码">
							</div>
							<div class="col-sm-6">
								<img style="height:41px" class="J_captcha">
							</div>
						</div>
						</c:if>
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
					<div class="mb-1">还没有账号? <a href="signup">立即注册</a></div>
					<div class="text-muted">
						<span>&copy; 2019 <a class="text-muted" href="https://getrebuild.com/" target="_blank">REBUILD</a></span>
						<div class="dev-show" style="font-size:11px">Built on <%=ServerListener.getStartupTime()%> (<%=Application.VER%>)</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/babel">
$(document).ready(function() {
	if (top != self) { parent.location.reload(); return }
	$('.J_captcha').click(function(){
		$(this).attr('src', rb.baseUrl + '/user/captcha?' + $random())
	}).trigger('click')

	$('#user,#passwd,#vcode').keydown(function(e){
		if (e.keyCode == 13) $('.J_login-btn').trigger('click')
	})
	const btn = $('.J_login-btn').click(function() {
		let user = $val('#user'), 
			passwd = $val('#passwd'),
			vcode = $val('#vcode')
		if (!user || !passwd){ rb.highbar('请输入用户名和密码'); return }
		if ($('.J_captcha').length > 0 && !vcode){ rb.highbar('请输入验证码'); return }
		
		btn.button('loading')
		let url = rb.baseUrl + '/user/user-login?user=' + $encode(user) + '&passwd=' + $encode(passwd) + '&autoLogin=' + $val('#autoLogin')
		if (!!vcode) url += '&vcode=' + vcode
		$.post(url, function(res) {
			if (res.error_code == 0) location.replace($decode($urlp('nexturl') || '../dashboard/home'))
			else if (res.error_msg == 'VCODE') location.reload()
			else{
				$('.J_captcha').trigger('click')
				rb.highbar(res.error_msg || '登录失败，请稍后重试')
				btn.button('reset')
			}
		})
	})
})
</script>
</body>
</html>
