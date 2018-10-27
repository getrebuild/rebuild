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
			<div id="tab-rbview"></div>
		</div>
		<div class="col-sm-2">
			<div class="view-oper">
				<div class="btns">
					<button class="btn btn-secondary J_edit" type="button"><i class="icon zmdi zmdi-border-color"></i> 编辑</button>
					<button class="btn btn-secondary J_delete2" type="button"><i class="icon zmdi zmdi-delete"></i> 删除</button>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/rb-forms.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-forms-ext.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-view.jsx" type="text/babel"></script>
<script type="text/babel">
var rbFromView
$(document).ready(function(){
	RbViewPage.init('${id}', [ '${entityLabel}', 'User', '${entityIcon}'])
	$('.J_delete2').click(function(){
		rb.modal(rb.baseUrl + '/page/admin/bizuser/user-delete?user=${id}', '删除用户', { width:580 } )
	})
});
</script>
</body>
</html>
