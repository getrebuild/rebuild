<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/view-page.css">
<title>${entityLabel}视图</title>
<style type="text/css">
</style>
</head>
<body class="view-body">
<div class="view-header">
	<i class="header-icon zmdi zmdi-${entityIcon}"></i>
	<h3 class="title">${entityLabel}视图</h3>
	<span>
		<a class="close J_close"><i class="zmdi zmdi-close"></i></a>
		<a class="close s J_for-admin" href="${baseUrl}/admin/entity/${entityName}/form-design" title="配置布局" target="_blank"><i class="zmdi zmdi-settings"></i></a>
	</span>
</div>
<div class="main-content container-fluid">
	<div class="row">
		<div class="col-sm-9 pr-0">
			<div class="tab-container">
				<ul class="nav nav-tabs">
					<li class="nav-item"><a class="nav-link active" href="#tab-rbview" data-toggle="tab">视图</a></li>
					<a class="vtab-settings J_for-admin hide" title="配置显示项"><i class="zmdi zmdi-settings"></i></a>
				</ul>
				<div class="tab-content">
                    <div class="tab-pane active" id="tab-rbview"></div>
				</div>
			</div>
		</div>
		<div class="col-sm-3 view-metas">
			<div class="view-action row">
				<div class="col-6 pr-1 mb-2">
					<button class="btn btn-secondary J_edit" type="button"><i class="icon zmdi zmdi-border-color"></i> 编辑</button>
				</div>
				<div class="col-6 pl-1 mb-2 btn-group J_action">
					<button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown"><i class="icon zmdi zmdi-more-vert"></i> 更多</button>
					<div class="dropdown-menu dropdown-menu-right">
						<a class="dropdown-item J_delete"><i class="icon zmdi zmdi-delete"></i> 删除</a>
						<a class="dropdown-item J_assign"><i class="icon zmdi zmdi-mail-reply-all"></i> 分派</a>
						<a class="dropdown-item J_share"><i class="icon zmdi zmdi-slideshare"></i> 共享</a>
					</div>
				</div>
				<div class="col-6 pr-1 mb-2 btn-group J_new">
					<button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown"><i class="icon zmdi zmdi-plus"></i> 新建相关</button>
					<div class="dropdown-menu">
						<div class="dropdown-divider J_for-admin hide"></div>
						<a class="dropdown-item J_for-admin hide"><i class="icon zmdi zmdi-settings"></i> 配置新建</a>
					</div>
				</div>
			</div>
			<div class="view-user">
				<div class="form-line"><fieldset><legend>用户</legend></fieldset></div>
				<dl class="row">
					<dt class="col-4 pr-0">所属用户</dt>
					<dd class="col-8 pl-0 J_owningUser"></dd>
				</dl>
				<dl class="row">
					<dt class="col-4 pr-0">共享用户</dt>
					<dd class="col-8 pl-0 J_shareTo"></dd>
				</dl>
			</div>
			<div class="view-date">
				<div class="form-line"><fieldset><legend>日期</legend></fieldset></div>
				<dl class="row">
					<dt class="col-4 pr-0">创建时间</dt>
					<dd class="col-8 pl-0 J_createdOn"></dd>
				</dl>
				<dl class="row">
					<dt class="col-4 pr-0">修改时间</dt>
					<dd class="col-8 pl-0 J_modifiedOn"></dd>
				</dl>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/rb-forms.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-view.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/assign-share.jsx" type="text/babel"></script>
<script type="text/babel">
$(document).ready(function(){
	RbViewPage.init('${id}', [ '${entityLabel}', '${entityName}', '${entityIcon}' ], ${entityPrivileges})
	RbViewPage.initVTabs(${ViewTabs})
	RbViewPage.initRecordMeta()
});
</script>
</body>
</html>
