<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
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
}
.vcode-row img {
	cursor: pointer;
}
.splash-footer *,
.copyright,
.copyright * {
	color: rgba(255, 255, 255, 0.9) !important;
    text-shadow: 0px 1px 1px #444;
	max-width: 680px;
	margin: 0 auto;
	text-align: center;
}
.copyright.dev-show a {
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
.select-lang>a {
	display: inline-block;
	padding: 5px 4px;
    font-size: 0;
    line-height: 1;
    border-radius: 2px;
}
.select-lang>a:hover {
    background-color: #E6EFF8;
}
</style>
<title>${bundle.lang('UserLogin')}</title>
</head>
<body class="rb-splash-screen">
<div class="rb-wrapper rb-login">
	<div class="rb-bgimg"></div>
	<div class="rb-content">
		<div class="announcement-wrapper"></div>
		<div class="main-content container-fluid">
			<div class="splash-container mb-1">
				<div class="card card-border-color card-border-color-primary">
					<div class="card-header"><a class="logo-img"></a></div>
					<div class="card-body">
						<form id="login-form">
						<div class="form-group">
							<input class="form-control" id="user" type="text" placeholder="${bundle.lang('UsernameOrEmail')}">
						</div>
						<div class="form-group">
							<input class="form-control" id="passwd" type="password" placeholder="${bundle.lang('LoginPassword')}">
						</div>
						<div class="form-group row pt-0 mb-3 hide vcode-row" data-state="${sessionScope.needLoginVCode}">
							<div class="col-6 pr-0">
								<input class="form-control" type="text" placeholder="${bundle.lang('InputCaptcha')}">
							</div>
							<div class="col-6 text-right pl-0 pr-0">
								<img class="mw-100 mr-zero" alt="${bundle.lang('Captcha')}" title="${bundle.lang('ClickReload')}">
							</div>
						</div>
						<div class="form-group row login-tools">
							<div class="col-6 login-remember">
								<label class="custom-control custom-checkbox custom-control-inline mb-0">
									<input class="custom-control-input" type="checkbox" id="autoLogin">
                                    <span class="custom-control-label"> ${bundle.lang('RememberMe')}</span>
								</label>
							</div>
							<div class="col-6 login-forgot-password">
								<a href="forgot-passwd">${bundle.lang('ForgotPassword')}</a>
							</div>
						</div>
						<div class="form-group login-submit mb-2">
							<button class="btn btn-primary btn-xl" type="submit" data-spinner>${bundle.lang('Login')}</button>
							<div class="mt-4 text-center">${bundle.lang('NoAccountYet')}&nbsp;<a href="signup">${bundle.lang('SignupNow')}</a></div>
						</div>
						<div class="select-lang text-center mb-2">
							<a href="?locale=zh_CN" title="中文"><img src="${baseUrl}/assets/img/flag/zh-CN.png" /></a>
							<a href="?locale=en" title="English"><img src="${baseUrl}/assets/img/flag/en-US.png" /></a>
							<a href="?locale=ja" title="日本語"><img src="${baseUrl}/assets/img/flag/ja-JP.png" /></a>
						</div>
						</form>
					</div>
				</div>
				<div class="splash-footer">
					<div class="copyright">
						&copy; ${appName}
					</div>
				</div>
			</div>
			<div class="copyright">${bundle.lang('RightsTip')}</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/feeds/announcement.jsx" type="text/babel"></script>
<script type="text/babel">
$(document).ready(function() {
	if (top != self) { parent.location.reload(); return }

	$('.copyright a').attr('target', '_blank').addClass('link')
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
		if (!user || !passwd){ RbHighbar.create($lang('InputUserOrPasswordPls')); return }
		if (vcodeState && !vcode){ RbHighbar.create($lang('InputCaptchaPls')); return }

		let btn = $('.login-submit button').button('loading')
		let url = '/user/user-login?user=' + $encode(user) + '&passwd=******&autoLogin=' + $val('#autoLogin')
		if (!!vcode) url += '&vcode=' + vcode
		$.post(url, $encode(passwd), function(res) {
			if (res.error_code == 0){
				location.replace($decode($urlp('nexturl') || '../dashboard/home'))
			} else if (res.error_msg == 'VCODE') {
				vcodeState = true
				$('.vcode-row').removeClass('hide').find('img').trigger('click')
				btn.button('reset')
			} else {
				$('.vcode-row img').trigger('click')
				RbHighbar.create(res.error_msg)
				btn.button('reset')
			}
		})
	})

    $.get('/user/live-wallpaper', (res) => {
        if (res.error_code != 0 || !res.data) return
        let bgimg = new Image()
        bgimg.src = res.data
        bgimg.onload = function() {
            $('.rb-bgimg').animate({ opacity: 0 })
            setTimeout(() => $('.rb-bgimg').css('background-image', 'url(' + res.data + ')').animate({ opacity: 1 }), 400)
        }
    })
})
</script>
</body>
</html>
