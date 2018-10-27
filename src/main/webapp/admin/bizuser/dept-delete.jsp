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
	let dept = $urlp('dept')
	$.get(rb.baseUrl + '/admin/bizuser/check-has-member?id=' + dept, function(res){
		if (res.data == 0){
			$('p.alters').text('此部门可以被安全的删除。')
		} else {
			let url = rb.baseUrl + '/admin/bizuser/users#dept=' + dept
			$('p.alters').html('此部门下有 <a href="' + url + '" target="_blank" class="text-bold">' + res.data + '</a> 个用户。<br>你需要先将这些用户转移到其他部门，然后才能删除。')
			$('.btn-danger').remove()
		}
	});
	
	let btn = $('.btn-danger').click(function(){
		btn.button('loading')
		$.post(rb.baseUrl + '/admin/bizuser/dept-delete?transfer=&id=' + dept, function(res){
			if (res.error_code == 0) parent.parent.location.reload()
			else rb.notice(res.error_msg, 'danger')
			btn.button('reset')
		});
	});
});
</script>
</body>
</html>