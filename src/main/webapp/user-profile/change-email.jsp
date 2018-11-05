<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>更改邮箱</title>
</head>
<body class="dialog">
<div class="main-content">
	<form>
		<div class="form-group row">
			<label class="col-sm-3 col-form-label text-sm-right">新邮箱</label>
			<div class="col-sm-7">
				<input type="text" class="form-control form-control-sm" id="newEmail">
			</div>
		</div>
		<div class="form-group row">
			<label class="col-sm-3 col-form-label text-sm-right">验证码</label>
			<div class="col-sm-4 pr-0">
				<input type="text" class="form-control form-control-sm" id="emailCode">
			</div>
			<div class="col-sm-3">
				<button type="button" class="btn btn-primary bordered w-100 J_vcode">获取验证码</button>
			</div>
		</div>
		<div class="form-group row footer">
			<div class="col-sm-7 offset-sm-3">
            	<button class="btn btn-primary J_save" type="button" data-loading-text="请稍后">确定</button>
            	<a class="btn btn-link" onclick="parent.rb.modalHide()">取消</a>
			</div>
		</div>
	</form>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
$(document).ready(function(){
	let btn = $('.J_vcode').click(function(){
		let email = $val('#newEmail')
		if (!email || !$regex.isMail(email)){ rb.notice('请输入有效的邮箱地址'); return }
		$.post(rb.baseUrl + '/settings/send-email-vcode?email=' + $encode(email), function(res){
			if (res.error_code == 0){
				btn.attr('disabled', true)
				show_countdown(60)
			} else rb.notice(res.error_msg)
		})
	})
	
	$('.J_save').click(function(){
		let email = $val('#newEmail')
			vcode = $val('#emailCode')
		if (!email || !vcode) return
		$.post(rb.baseUrl + '/settings/save-email?email=' + $encode(email) + '&vcode=' + $encode(vcode), function(res){
			if (res.error_code == 0){
				parent.$('.J_email-account').text(email)
				rb.notice('邮箱修改成功', 'success')
				parent.rb.modalHide()
				$('#newEmail, #emailCode').val('')
			} else rb.notice(res.error_msg)
		})
	})
	
	$('#newEmail').attr('placeholder', parent.$('.J_email-account').text())
})
let show_countdown = function(s){
	$('.J_vcode').text(s + '秒后重发送')
	if (s == 0) $('.J_vcode').attr('disabled', false).text('重新发送')
	else setTimeout(function(){ show_countdown(s - 1) }, 1000)
}
</script>
</body>
</html>