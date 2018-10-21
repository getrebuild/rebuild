<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>新建实体</title>
</head>
<body class="dialog">
<div class="main-content">
	<form>
		<div class="form-group row">
			<label class="col-sm-3 col-form-label text-sm-right">实体名称</label>
			<div class="col-sm-7 col-lg-4">
				<input class="form-control form-control-sm" type="text" id="entityLabel" maxlength="40">
			</div>
		</div>
		<div class="form-group row">
			<label class="col-sm-3 col-form-label text-sm-right">备注</label>
			<div class="col-sm-7 col-lg-4">
				<textarea class="form-control form-control-sm row2x" id="comments" maxlength="100" placeholder="可选"></textarea>
			</div>
		</div>
		<div class="form-group row footer">
			<div class="col-sm-7 offset-sm-3">
				<button class="btn btn-primary" type="button" data-loading-text="请稍后">确定</button>
				<a class="btn btn-link" onclick="parent.newEntityModal.hide()">取消</a>
			</div>
		</div>
	</form>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
$(document).ready(function(){
	let btn = $('.btn-primary').click(function(){
		let entityLabel = $val('#entityLabel'),
			comments = $val('#comments');
		if (!entityLabel){
			rb.notice('请输入实体名称'); return;
		}
		let _data = { label:entityLabel, comments:comments }
		_data = JSON.stringify(_data)
		
		btn.button('loading');
		$.post(rb.baseUrl + '/admin/entity/entity-new', _data, function(res){
			if (res.error_code == 0) parent.location.href = rb.baseUrl + '/admin/entity/' +res.data + '/base';
			else rb.notice(res.error_msg, 'danger')
		});
	});
});
</script>
</body>
</html>
