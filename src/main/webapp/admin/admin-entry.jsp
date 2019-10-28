<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>管理员入口</title>
<style type="text/css">
</style>
</head>
<body>
<div class="rb-wrapper rb-nosidebar-left rb-color-header">
	<jsp:include page="/_include/NavTopHeader.jsp" />
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="splash-container">
				<div class="card card-border-color card-border-color-primary">
					<div class="card-header">
						<h4>需要验证你的管理员身份</h4>
					</div>
					<div class="card-body">
						<form>
						<div class="form-group">
							<input class="form-control" id="admin-passwd" type="password" placeholder="输入登录密码" autocomplete="off">
						</div>
						<div class="form-group login-submit">
							<button class="btn btn-primary btn-xl J_verify-btn" type="submit">验证</button>
						</div>
						</form>
					</div>
				</div>
				<div class="splash-footer">
					<span><a href="javascript:history.back()">返回</a></span>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/babel">
$(document).ready(function(){
	let nexturl = $decode($urlp('nexturl') || '../admin/systems')
	$('.J_verify-btn').click(function(){
		let passwd = $val('#admin-passwd')
		if (!!!passwd) return false
		
		let but = $(this).button('loading')
		$.post('admin-verify?passwd=' + passwd, function(res) {
			if (res.error_code == 0) location.replace(nexturl)
			else{
				RbHighbar.create(res.error_msg)
				but.button('reset')
			}
		})
		return false
	})
	if (self != top && $(window).width() < 800){
		$('.splash-footer, .navbar').remove()
		$('.rb-wrapper').addClass('pt-0')
	}
})
</script>
</body>
</html>
