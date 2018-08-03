<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>用户管理</title>
<style type="text/css">
.footer{padding-bottom:0 !important;}
</style>
</head>
<body class="dialog">
<div class="main-content">
	<form>
		<div class="form-group row">
			<label class="col-12 col-sm-3 col-form-label text-sm-right">实体名称</label>
			<div class="col-12 col-sm-8 col-lg-4">
				<input class="form-control form-control-sm" type="text" id="entityLabel" maxlength="20">
			</div>
		</div>
		<div class="form-group row">
			<label class="col-12 col-sm-3 col-form-label text-sm-right">描述</label>
			<div class="col-12 col-sm-8 col-lg-4">
				<textarea class="form-control form-control-sm row2" id="description" maxlength="100"></textarea>
			</div>
		</div>
		<div class="form-group row footer">
			<label class="col-12 col-sm-3"></label>
			<div class="col-12 col-sm-8 col-lg-4">
				<button class="btn btn-primary btn-space" type="button" data-loading-text="请稍后">确定</button>
			</div>
		</div>
	</form>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
$(document).ready(function(){
	let btn = $('.btn-primary').click(function(){
		let entityLabel = $('#entityLabel').val(),
			desc = $('#description').val();
		
		btn.button('loading');
		$.post('entity-new', { label:entityLabel, desc:desc }, function(res){
			if (res.error_code == 0) parent.location.href = 'manage.htm?entity=' + res.data;
			else{
				alert(res);
				btn.button('reset');
			}
		});
	});
});
</script>
</body>
</html>
