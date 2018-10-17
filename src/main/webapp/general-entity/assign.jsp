<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>分派</title>
</head>
<body class="dialog">
<div class="main-content">
	<form>
		<div class="form-group row">
			<label class="col-sm-3 col-form-label text-sm-right">分派哪些记录</label>
			<div class="col-sm-7 col-lg-4">
				<div class="form-control-plaintext" id="records">选中的记录</div>
			</div>
		</div>
		<div class="form-group row">
			<label class="col-sm-3 col-form-label text-sm-right">分派给谁</label>
			<div class="col-sm-7 col-lg-4">
				<select class="form-control form-control-sm" id="toUser" multiple="multiple">
				</select>
			</div>
		</div>
		<div class="form-group row J_click-cass">
			<div class="col-sm-7 offset-sm-3">
				<a href="javascript:;">同时分派关联记录</a>
			</div>
		</div>
		<div class="form-group row J_cass hide">
			<label class="col-sm-3 col-form-label text-sm-right">同时分派关联记录</label>
			<div class="col-sm-7 col-lg-4">
				<select class="form-control form-control-sm" id="cascades" multiple="multiple">
				</select>
			</div>
		</div>
		<div class="form-group row footer">
			<div class="col-sm-7 offset-sm-3">
            	<button class="btn btn-primary J_submit" type="button" data-loading-text="请稍后">确定</button>
            	<a class="btn btn-link J_cancel">取消</a>
			</div>
		</div>
	</form>
</div>

<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/assign-share.js"></script>
</body>
</html>
