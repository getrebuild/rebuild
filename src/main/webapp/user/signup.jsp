<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
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
<script src="${baseUrl}/assets/js/signup.js"></script>
</body>
</html>
