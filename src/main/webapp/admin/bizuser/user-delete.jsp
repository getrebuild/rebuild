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
		<p class="alters">我们建议你禁用用户，而非删除用户。<br>如确认删除，你需要先将此用户下的所有业务数据转移给其他用户。</p>
		<div class="mt-6 mb-4">
			<button class="btn btn-space btn-secondary" type="button" onclick="parent.rb.modalHide()">取消</button>
			<button class="btn btn-space btn-primary" type="button">禁用</button>
			<button class="btn btn-space btn-danger" type="button">删除</button>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
$(document).ready(function(){
	let user = $urlp('user')
	let btn = $('.btn-danger').click(function(){
		btn.button('loading')
		$.post(rb.baseUrl + '/admin/bizuser/user-delete?id=' + user, function(res){
			if (res.error_code == 0) parent.location.replace(rb.baseUrl + '/admin/bizuser/role-privileges')
			else rb.notice(res.error_msg, 'danger')
			btn.button('reset')
		});
	});
});
</script>
</body>
</html>