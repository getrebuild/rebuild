<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp" %>
<title>用户登录</title>
<style type="text/css">
.rb-login{
	width: 500px;
	margin: 0 auto;
	margin-top: 10%;
	padding: 42px 0;
}
.rb-login > div{
	width:300px;
	margin: 0 auto;
}
.mdl-textfield{font-size:14px;}
.mdl-textfield__input{font-size:14px}
</style>
</head>
<body>
<div class="rb-login mdl-card mdl-shadow--2dp">
	<div><h3 style="font-size:20px;margin:0;padding:0">用户登录</h3></div>
	<div class="mdl-textfield mdl-js-textfield">
		<input class="mdl-textfield__input" type="text" id="user"> 
		<label class="mdl-textfield__label" for="user">用户名</label>
	</div>
	<div class="mdl-textfield mdl-js-textfield">
		<input class="mdl-textfield__input" type="password" id="passwd">
		<label class="mdl-textfield__label" for="passwd">密码</label>
	</div>
	<div>
		<button class="mdl-button mdl-js-button mdl-button--raised mdl-button--colored J_login-btn" style="width:100%;font-size:15px;height:42px;">登录</button>
	</div>
</div>
<script type="text/javascript">
$(document).ready(function(){
	$('.J_login-btn').click(function(){
		let user = $('#user').val(),
			passwd = $('#passwd').val();
		$.post(__baseUrl + '/user/user-login', {user:user, passwd:passwd}, function(res){
			if (res.error_code == 0) location.replace('../dashboard/home');
			else alert(res.error_msg);
		});
	});
});
</script>
</body>
</html>
