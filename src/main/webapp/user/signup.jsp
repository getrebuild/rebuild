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
							<input class="form-control" id="sFullName" type="text" placeholder="姓名" autocomplete="off">
						</div>
						<div class="form-group">
							<input class="form-control" id="sName" type="text" placeholder="用户名" autocomplete="off">
						</div>
						<div class="form-group">
							<input class="form-control" id="sEmail" type="email" placeholder="注册邮箱" autocomplete="off">
						</div>
						<div class="form-group pt-0 row">
							<div class="col-7">
								<input class="form-control" type="text" id="sVcode" placeholder="输入邮箱验证码" autocomplete="off">
							</div>
							<div class="col-5 text-right pl-0">
								<button class="btn btn-secondary J_vcode-btn" style="height:41px;width:100%" type="button">获取验证码</button>
							</div>
						</div>
						<div class="form-group login-submit">
							<button class="btn btn-primary btn-xl J_confirm-btn" type="button">注册</button>
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
<script>
$(document).ready(function() {
	$('.J_vcode-btn').click(function() {
		let email = $val('#sEmail')
		if (!email){ rb.highbar('请输入注册邮箱'); return }
		
		let _btn = $(this).button('loading')
		$.post(rb.baseUrl + '/user/signup-email-vcode?email=' + $encode(email), function(res) {
			if (res.error_code == 0) resend_countdown(true)
			else{
				rb.highbar(res.error_msg)
				_btn.button('reset')
			}
		})
	})
	
	$('.J_confirm-btn').click(function() {
		let fullName = $val('#sFullName'), 
			name = $val('#sName'),
			email = $val('#sEmail'),
			vcode = $val('#sVcode')
		if (!fullName){ rb.highbar('请输入姓名'); return }
		if (!name){ rb.highbar('请输入登录名'); return }
		if (!email){ rb.highbar('请输入注册邮箱'); return }
		if (!vcode){ rb.highbar('请输入邮箱验证码'); return }
		let _data = { loginName: name, fullName: fullName, email: email, vcode: vcode }
		
		let _btn = $(this).button('loading')
		$.post(rb.baseUrl + '/user/signup-confirm', JSON.stringify(_data), function(res) {
			if (res.error_code == 0){
				_btn.text('注册成功')
				setTimeout(function(){ location.href = './login?x=99' }, 1000)
			}else{
				rb.highbar(res.error_msg)
				_btn.button('reset')
			}
		})
	})
})
let countdown_timer
let countdown_seconds = 60
let resend_countdown = function(first){
	if (first === true) {
		$('.J_vcode-btn').attr('disabled', true)
		if (countdown_timer) clearTimeout(countdown_timer)
		countdown_seconds = 60
	}
	$('.J_vcode-btn').text('重新获取 (' + (--countdown_seconds) + ')')
	if (countdown_seconds == 0) {
		$('.J_vcode-btn').attr('disabled', false).text('重新获取')
	} else {
		countdown_timer = setTimeout(resend_countdown, 1000)
	}
}
</script>
</body>
</html>
