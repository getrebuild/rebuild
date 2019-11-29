<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>${bundle.lang('ForgotPassword')}</title>
</head>
<body class="rb-splash-screen">
<div class="rb-wrapper rb-login">
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="splash-container">
				<div class="card card-border-color card-border-color-primary">
					<div class="card-header"><a class="logo-img"></a></div>
					<div class="card-body J_step1">
						<div class="form-group">
							<input class="form-control" id="email" type="text" placeholder="${bundle.lang('Email')}" autocomplete="off">
							<p class="form-text">${bundle.lang('UnsetEmailTip')}</p>
						</div>
						<div class="form-group login-submit">
							<button class="btn btn-primary btn-xl J_forgot-btn">${bundle.lang('ResetPassword')}</button>
						</div>
					</div>
					<div class="card-body J_step2 hide">
						<div class="alert alert-warning alert-icon alert-icon-border alert-sm">
							<div class="icon"><span class="zmdi zmdi-info-outline"></span></div>
							<div class="message"><p>${bundle.lang('VcodeEmailSent')} <b class="J_email ml-1"></b></p></div>
						</div>
						<div class="form-group">
							<div class="row">
								<div class="col-7">
									<input class="form-control" id="vcode" type="text" placeholder="${bundle.lang('InputVcodePls')}" autocomplete="off">
								</div>
								<div class="col-5 pl-0">
									<button type="button" class="btn btn-primary bordered J_vcode-resend" style="height:41px;width:100%">${bundle.lang('GetVcode')}</button>
								</div>
							</div>
						</div>
						<div class="form-group">
							<input class="form-control" id="newpwd" type="password" placeholder="${bundle.lang('NewPassword')}" autocomplete="off">
						</div>
						<div class="form-group">
							<input class="form-control" id="newpwd2" type="password" placeholder="${bundle.lang('RepeatNewPassword')}" autocomplete="off">
						</div>
						<div class="form-group login-submit">
							<button class="btn btn-primary btn-xl J_confirm-btn">${bundle.lang('ConfirmReset')}</button>
						</div>
					</div>
				</div>
				<div class="splash-footer">
					<span><a href="login">${bundle.lang('ReturnHome')}</a></span>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/babel">
$(document).ready(function() {
	let email = null
	$('.J_forgot-btn, .J_vcode-resend').click(function() {
		email = $val('#email')
		if (!email){ RbHighbar.create($lang('InputEmailPls')); return }
		$('.J_email').text(email)
		let _btn = $(this).button('loading')
		$.post(rb.baseUrl + '/user/user-forgot-passwd?email=' + $encode(email), function(res) {
			if (res.error_code == 0){
				$('.J_step1').hide()
				$('.J_step2').removeClass('hide')
				resend_countdown(true)
			}else RbHighbar.create(res.error_msg)
			_btn.button('reset')
		})
	})
	
	$('.J_confirm-btn').click(function() {
		let vcode = $val('#vcode')
		let newpwd = $val('#newpwd')
		let newpwd2 = $val('#newpwd2')
		if (!vcode) { RbHighbar.create($lang('InputVcodePls')); return }
		if (!newpwd) { RbHighbar.create($lang('InputNewPasswordPls')); return }
    	if (newpwd !== newpwd2) { RbHighbar.create($lang('PasswordNotMatch')); return }
    	
    	let _data = { email: email, vcode: vcode, newpwd: newpwd }
    	let _btn = $(this).button('loading')
		$.post(rb.baseUrl + '/user/user-confirm-passwd', JSON.stringify(_data), function(res) {
			if (res.error_code == 0){
				_btn.text($lang('ActionSuccess'))
				setTimeout(()=>{ location.href = './login' }, 1000)
			} else {
				RbHighbar.create(res.error_msg)
				_btn.button('reset')
			}
		})
	})
})
let countdown_timer
let countdown_seconds = 60
let resend_countdown = function(first){
	if (first === true) {
		$('.J_vcode-resend').attr('disabled', true)
		if (countdown_timer) clearTimeout(countdown_timer)
		countdown_seconds = 60
	}
	$('.J_vcode-resend').text($lang('GetVcode2') + ' (' + (--countdown_seconds) + ')')
	if (countdown_seconds == 0) {
		$('.J_vcode-resend').attr('disabled', false).text($lang('GetVcode2'))
	} else {
		countdown_timer = setTimeout(resend_countdown, 1000)
	}
}
</script>
</body>
</html>
