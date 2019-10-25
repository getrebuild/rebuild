<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/view-page.css">
<title>${entityLabel}视图</title>
</head>
<body class="view-body">
<div class="view-header">
	<i class="header-icon zmdi zmdi-${entityIcon}"></i>
	<h3 class="title">${entityLabel}视图</h3>
	<span>
		<a class="close J_close"><i class="zmdi zmdi-close"></i></a>
		<a class="close sm J_reload"><i class="zmdi zmdi-refresh"></i></a>
	</span>
</div>
<div class="main-content container-fluid">
	<div class="alert alert-warning alert-icon alert-dismissible min hide J_tips">
		<div class="icon"><span class="zmdi zmdi-info-outline"></span></div>
		<div class="message"><a class="close" data-dismiss="alert"><span class="zmdi zmdi-close"></span></a><p></p></div>
	</div>
	<div class="row">
		<div class="col-sm-9 pr-0">
			<div class="tab-container">
				<ul class="nav nav-tabs">
					<li class="nav-item"><a class="nav-link active" href="#tab-rbview" data-toggle="tab">${entityLabel}信息</a></li>
				</ul>
				<div class="tab-content">
                    <div class="tab-pane active" id="tab-rbview"></div>
				</div>
			</div>
		</div>
		<div class="col-sm-3 view-operating">
			<div class="view-action row admin-show admin-verified">
				<div class="col-12 col-lg-6">
					<button class="btn btn-secondary J_edit" type="button"><i class="icon zmdi zmdi-border-color"></i> 编辑</button>
				</div>
				<div class="col-12 col-lg-6 btn-group J_mores">
					<button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown">更多 <i class="icon zmdi zmdi-more-vert"></i></button>
					<div class="dropdown-menu dropdown-menu-right">
						<a class="dropdown-item J_delete"><i class="icon zmdi zmdi-delete"></i> 删除</a>
						<a class="dropdown-item J_enable"><i class="icon zmdi zmdi-check-circle"></i> 启用</a>
						<a class="dropdown-item J_disable"><i class="icon zmdi zmdi-block"></i> 停用</a>
						<div class="dropdown-divider"></div>
						<a class="dropdown-item J_changeDept"><i class="icon zmdi zmdi-accounts"></i> 变更部门</a>
						<a class="dropdown-item J_changeRole"><i class="icon zmdi zmdi-lock"></i> 变更角色</a>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = {
	type: $pgt.RecordView,
	entity: ['User','${entityLabel}','${entityIcon}'],
	recordId: '${id}'
}
</script>
<script src="${baseUrl}/assets/js/rb-forms.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-forms.exts.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-view.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/bizuser/user-view.jsx" type="text/babel"></script>
</body>
</html>
