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
		<a class="close sm admin-show" href="${baseUrl}/admin/entity/${entityName}/form-design" title="配置布局" target="_blank"><i class="zmdi zmdi-settings"></i></a>
	</span>
</div>
<div class="main-content container-fluid">
	<div class="row">
		<div class="col-sm-9 pr-0">
			<div class="tab-container">
				<ul class="nav nav-tabs">
					<li class="nav-item"><a class="nav-link active" href="#tab-rbview" data-toggle="tab">明细视图</a></li>
				</ul>
				<div class="tab-content">
                    <div class="tab-pane active" id="tab-rbview"></div>
				</div>
			</div>
		</div>
		<div class="col-sm-3 view-operating">
			<div class="view-action row">
				<div class="col-12 col-lg-6">
					<button class="btn btn-secondary J_edit" type="button"><i class="icon zmdi zmdi-border-color"></i> 编辑</button>
				</div>
				<div class="col-12 col-lg-6 btn-group J_mores">
					<button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown">更多 <i class="icon zmdi zmdi-more-vert"></i></button>
					<div class="dropdown-menu dropdown-menu-right">
						<a class="dropdown-item J_delete"><i class="icon zmdi zmdi-delete"></i> 删除</a>
						<div class="dropdown-divider"></div>
						<a class="dropdown-item J_print" target="_blank" href="${baseUrl}/app/entity/print?id=${id}"><i class="icon zmdi zmdi-print"></i> 打印</a>
					</div>
				</div>
			</div>
			<div class="view-date">
				<div class="form-line"><fieldset><legend>日期</legend></fieldset></div>
				<dl class="row">
					<dt class="col-12 col-md-4 pr-0">创建时间</dt>
					<dd class="col-12 col-md-4 pl-0 J_createdOn"></dd>
				</dl>
				<dl class="row">
					<dt class="col-12 col-md-4 pr-0">修改时间</dt>
					<dd class="col-12 col-md-4 pl-0 J_modifiedOn"></dd>
				</dl>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = {
	type: 'SlaveView',
	entity: ['${entityName}','${entityLabel}','${entityIcon}'],
	privileges: ${entityPrivileges},
	recordId: '${id}'
}
</script>
<script src="${baseUrl}/assets/js/rb-forms.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-forms.exts.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-view.jsx" type="text/babel"></script>
</body>
</html>
