<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>列表选项</title>
<style type="text/css">
.unset-list .dd-handle {
	font-style: italic;
	color: #aaa
}
.unset-list .dd-item a.action {
	position: absolute;
	right: 24px;
	top: 1px;
	font-style: normal;
}
.unset-list .dd-item:hover a.action {
	color: #fff
}
</style>
</head>
<body class="dialog">
<div class="main-content">
	<div class="row m-0">
		<div class="col-6">
			<h5 class="sortable-box-title">选项列表</h5>
			<div class="sortable-box rb-scroller">
				<ol class="dd-list J_config"></ol>
			</div>
			<form>
				<div class="input-group input-group-sm">
					<input class="form-control J_text" type="text" maxlength="50">
					<div class="input-group-append">
						<button class="btn btn-secondary J_confirm" type="submit" style="min-width:0">添加</button>
					</div>
				</div>
			</form>
		</div>
		<div class="col-6">
			<h5 class="sortable-box-title">已禁用的</h5>
			<div class="sortable-box rb-scroller">
				<ol class="dd-list unset-list"></ol>
			</div>
		</div>
	</div>
	<div class="dialog-footer">
		<button class="btn btn-primary J_save" type="button">保存</button>
		<button class="btn btn-secondary" onclick="parent.RbModal.hide()" type="button">取消</button>
	</div>
</div>

<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/sortable.js"></script>
<script src="${baseUrl}/assets/js/entityhub/picklist-editor.js" type="text/babel"></script>
</body>
</html>
