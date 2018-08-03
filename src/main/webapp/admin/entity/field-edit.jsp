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
			<label class="col-12 col-sm-3 col-form-label text-sm-right">字段名称</label>
			<div class="col-12 col-sm-8 col-lg-6">
				<input class="form-control form-control-sm" type="text">
			</div>
		</div>
		<div class="form-group row">
			<label class="col-12 col-sm-3 col-form-label text-sm-right">类型</label>
			<div class="col-12 col-sm-8 col-lg-6">
				<select class="form-control form-control-sm">
					<option value="text">文本</option>
					<option value="number">数字</option>
				</select>
			</div>
		</div>
		<div class="form-group row footer">
			<label class="col-12 col-sm-3 col-form-label text-sm-right"></label>
			<div class="col-12 col-sm-8 col-lg-6">
            	<button class="btn btn-primary btn-space" type="button">确定</button>
			</div>
		</div>
	</form>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
//$(window).resize(function(){ setTimeoutDelay(function(){ parent.rbModal.loaded() }, 50, 'rbModal-resize') });
$(document).ready(function(){
});
</script>
</body>
</html>
