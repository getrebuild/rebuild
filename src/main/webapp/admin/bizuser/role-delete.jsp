<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>删除角色</title>
<style type="text/css">
.alters{margin-left:48px;margin-right:48px}
</style>
</head>
<body class="dialog">
<div class="main-content">
	<div class="text-center">
		<div class="text-danger">
			<span class="modal-main-icon zmdi zmdi-alert-triangle"></span>
		</div>
		<h4>删除确认</h4>
		<p class="alters">-</p>
		<div class="mt-6 mb-4">
			<button class="btn btn-space btn-secondary" type="button" onclick="parent.rb.modalHide()">取消</button>
			<button class="btn btn-space btn-danger" type="button">删除</button>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
$(document).ready(function(){
	let role = $urlp('role')
	$.get(rb.baseUrl + '/admin/bizuser/check-has-member?id=' + role, function(res){
		if (res.data == 0){
			$('p.alters').text('此角色可以被安全的删除。')
		} else {
			let url = rb.baseUrl + '/admin/bizuser/users#role=' + role
			$('p.alters').html('有 <a href="' + url + '" target="_blank" class="text-bold">' + res.data + '</a> 个用户应用了此角色。<br>删除将导致这些用户被禁用，直到你为他们指定了新的角色。')
		}
	});
	
	let btn = $('.btn-danger').click(function(){
		btn.button('loading')
		$.post(rb.baseUrl + '/admin/bizuser/role-delete?transfer=&id=' + role, function(res){
			if (res.error_code == 0) parent.location.replace(rb.baseUrl + '/admin/bizuser/role-privileges')
			else rb.notice(res.error_msg, 'danger')
			btn.button('reset')
		});
	});
});
</script>
</body>
</html>