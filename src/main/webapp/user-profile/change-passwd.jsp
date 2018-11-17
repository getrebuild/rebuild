<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>更改密码</title>
</head>
<body class="dialog">
<div class="main-content">
	<form>
		<div class="form-group row">
			<label class="col-sm-3 col-form-label text-sm-right">原密码</label>
			<div class="col-sm-7 col-lg-4">
				<input type="password" class="form-control form-control-sm" id="oldPasswd">
			</div>
		</div>
		<div class="form-group row">
			<label class="col-sm-3 col-form-label text-sm-right">新密码</label>
			<div class="col-sm-7 col-lg-4">
				<input type="password" class="form-control form-control-sm" id="newPasswd">
			</div>
		</div>
		<div class="form-group row">
			<label class="col-sm-3 col-form-label text-sm-right">重复新密码</label>
			<div class="col-sm-7 col-lg-4">
				<input type="password" class="form-control form-control-sm" id="newPasswd2">
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
<script type="text/babel">
$(document).ready(function(){
	$('.J_save').click(function(){
		let op = $val('#oldPasswd'),
			np = $val('#newPasswd'), np2 = $val('#newPasswd2')
		if (!op){ rb.notice('请输入原密码'); return }
		if (!np || np != np2){ rb.notice('请输入新密码，且新密码两次输入需一致'); return }
		if (np.length < 6){ rb.notice('新密码不等低于6位字符'); return }
		
		$.post(rb.baseUrl + '/settings/save-passwd?oldp=' + $encode(op) + '&newp=' + $encode(np), function(res){
			if (res.error_code == 0){
				rb.notice('密码修改成功', success)
				parent.rb.modalHide()
				$('#oldPasswd, #newPasswd, #newPasswd2').val('')
			} else rb.notice(res.error_msg)
		})
	})
});
</script>
</body>
</html>