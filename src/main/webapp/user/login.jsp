<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page import="com.rebuild.server.Application"%>
<%@ page import="com.rebuild.server.ServerListener"%>
<%@ page import="com.rebuild.server.helper.ConfigurableItem" %>
<%@ page import="com.rebuild.server.helper.SysConfiguration" %>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<style type="text/css">
#login-form > .row {
	margin-left: -15px !important;
	margin-right: -15px !important
}
.vcode-row {
	height: 41px;
	max-width: 100%;
	cursor: pointer;
}
.splash-footer * {
	color: rgba(255, 255, 255, 0.88) !important;
	line-height: 1.5;
}
.splash-footer a:hover {
	text-decoration: underline;
}
.rb-bgimg {
	position: fixed;
	width: 100%;
	height: 100%;
	overflow: hidden;
	z-index: 1;
	background: url(../assets/img/bg.jpg) no-repeat 0 0;
	background-size: cover;
	opacity: 1;
}
.rb-bgimg::before {
	content: "";
	position: absolute;
	width: 100%;
	height: 100%;
	z-index: 1;
	background: rgba(0, 0, 0, 0.1);
}
.rb-content {
	z-index: 2;
}
</style>
<title>登录</title>
</head>
<body class="rb-splash-screen">
<div class="rb-wrapper rb-login">
	<div class="rb-bgimg"></div>
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="splash-container mb-0">
				<div class="card card-border-color card-border-color-primary">
					<div class="card-header"><a class="logo-img"></a></div>
					<div class="card-body">
						<form id="login-form">
						<div class="form-group">
							<input class="form-control" id="user" type="text" placeholder="用户名 (或邮箱)">
						</div>
						<div class="form-group">
							<input class="form-control" id="passwd" type="password" placeholder="登录密码">
						</div>
						<div class="form-group row pt-0 hide vcode-row" data-state="${sessionScope.needLoginVCode}">
							<div class="col-6 pr-0">
								<input class="form-control" type="text" placeholder="输入右侧验证码">
							</div>
							<div class="col-6 text-right pl-0 pr-0">
								<img style="max-width:100%;margin-right:-15px" alt="验证码" title="点击刷新">
							</div>
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
							<button class="btn btn-primary btn-xl" type="submit" data-loading-text="登录中">登录</button>
							<div class="mt-4 text-center">还没有账号？<a href="signup">立即注册</a></div>
						</div>
						</form>
					</div>
				</div>
				<div class="splash-footer">
					<div class="copyright">
						<span>&copy; 2019 <a href="https://getrebuild.com/" target="_blank">${appName}</a></span>
						<div class="dev-show" style="font-size:11px">Built on <%=ServerListener.getStartupTime()%> (<%=Application.VER%>)</div>
					</div>
				</div>
			</div>
		</div>
	</div>

</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
useLiveWallpaper = <%=SysConfiguration.getBool(ConfigurableItem.LiveWallpaper)%>
</script>
<script type="text/babel">
$(document).ready(function() {
	if (top != self) { parent.location.reload(); return }
	if ($urlp('t') == 99) RbHighbar.create('注册申请已提交，请等待管理员审核', 'success', { timeout: 999999 })

	$('.vcode-row img').click(function(){
		$(this).attr('src', rb.baseUrl + '/user/captcha?' + $random())
	})

	let vcodeState = $('.vcode-row').data('state')
	if (vcodeState) {
		$('.vcode-row').removeClass('hide').find('img').trigger('click')
	}

	$('#login-form').on('submit', function(e) {
		e.preventDefault()
		let user = $val('#user'), 
			passwd = $val('#passwd'),
			vcode = $val('.vcode-row input')
		if (!user || !passwd){ RbHighbar.create('请输入用户名和密码'); return }
		if (vcodeState && !vcode){ RbHighbar.create('请输入验证码'); return }
		
		let btn = $('.login-submit button').button('loading')
		let url = rb.baseUrl + '/user/user-login?user=' + $encode(user) + '&passwd=' + $encode(passwd) + '&autoLogin=' + $val('#autoLogin')
		if (!!vcode) url += '&vcode=' + vcode
		$.post(url, function(res) {
			if (res.error_code == 0){
				location.replace($decode($urlp('nexturl') || '../dashboard/home'))
			} else if (res.error_msg == 'VCODE') {
				vcodeState = true
				$('.vcode-row').removeClass('hide').find('img').trigger('click')
				btn.button('reset')
			} else {
				$('.vcode-row img').trigger('click')
				RbHighbar.create(res.error_msg || '登录失败，请稍后重试')
				btn.button('reset')
			}
		})
	})

	if (useLiveWallpaper) {
		$.get('https://getrebuild.com/api/misc/bgimg?k=IjkMHgq94T7s7WkP', (res) => {
			if (!res.url) return
			let bgimg = new Image()
			bgimg.src = res.url
			bgimg.onload = function() {
				$('.rb-bgimg').animate({ opacity: 0 })
				setTimeout(() => {
					$('.rb-bgimg').css('background-image', 'url(' + res.url + ')').animate({ opacity: 1 })
				}, 400)
				if (res.copyright) $('.rb-bgimg').attr('title', res.copyright + ' (' + res.source + ')')
			}
		})
	}
})
</script>
</body>
</html>
