<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/view-page.css">
<title>${entityLabel}视图</title>
</head>
<body class="dialog">
<div class="view-header">
	 <span class="header-icon zmdi zmdi-${entityIcon}"></span>
	<h3 class="title">${entityLabel}视图</h3>
</div>
<div class="main-content container-fluid invisible">
	<div class="row">
		<div class="col-sm-10">
			<div class="tab-container">
				<ul class="nav nav-tabs">
					<li class="nav-item"><a class="nav-link active" href="#tab-rbview" data-toggle="tab">视图</a></li>
					<li class="nav-item"><a class="nav-link" href="#tab-salesorder" data-toggle="tab">订单 (0)</a></li>
					<li class="nav-item"><a class="nav-link" href="#tab-contact" data-toggle="tab">联系人 (0)</a></li>
				</ul>
				<div class="tab-content">
                    <div class="tab-pane active" id="tab-rbview"></div>
					<div class="tab-pane" id="tab-salesorder">tab-salesorder</div>
					<div class="tab-pane" id="tab-contact">tab-contact</div>
				</div>
			</div>
		</div>
		<div class="col-sm-2">
			<div class="view-btns">
				<button class="btn btn-secondary J_edit" type="button">编辑</button>
			</div>
			<div class="view-btns">
				<button class="btn btn-secondary J_assgin" type="button">分配</button>
				<button class="btn btn-secondary J_share" type="button">共享</button>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/rb-forms.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-view.jsx" type="text/babel"></script>
<script type="text/babel">
var rbFromView
$(document).ready(function(){
	const recordId = '${id}'
	rbFromView = rb.RbViewForm({ entity:'${entityName}', id:recordId })

	$('.J_edit').click(function(){
		rb.RbFormModal({ id:recordId, title:'编辑${entityLabel}',entity:'${entityName}', icon:'${entityIcon}' })
	});
});
</script>
</body>
</html>
