<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/view-page.css">
<title>${entityLabel}视图</title>
<style type="text/css">
.nav-tabs>li.nav-item a.nav-link{padding:11px 15px;max-width:120px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}
.nav-tabs,.nav-tabs>li.nav-item{position:relative;}
.nav-tabs .vtab-settings{position:absolute;right:0;top:4px;display:inline-block;padding:9px 8px;font-size:15px;opacity:0.6;display:none;}
.nav-tabs .vtab-settings:hover{opacity:0.8}
.nav-tabs:hover .vtab-settings{display:inline-block;}
.nav-tabs .badge{position:absolute;top:-1px;right:-4px;font-size:10px}
.related-list{min-height:200px}
.related-list .card{border:1px solid #ebebeb;box-shadow:0 0 4px 0 rgba(0,0,0,.04);padding:10px 15px;margin-bottom:9px;}
.related-list .card .float-right{color:#aaa;cursor:help;}
</style>
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
					<a class="vtab-settings" title="设置相关项"><i class="zmdi zmdi-settings"></i> </a>
				</ul>
				<div class="tab-content">
                    <div class="tab-pane active" id="tab-rbview"></div>
				</div>
			</div>
		</div>
		<div class="col-sm-2">
			<div class="view-oper">
				<div class="btns">
					<button class="btn btn-secondary J_edit" type="button"><i class="icon zmdi zmdi-border-color"></i> 编辑</button>
					<div class="btn-group J_actions">
						<button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown">更多 <i class="icon zmdi zmdi-more-vert"></i></button>
						<div class="dropdown-menu dropdown-menu-right">
							<a class="dropdown-item J_delete"><i class="icon zmdi zmdi-delete"></i> 删除</a>
							<a class="dropdown-item J_assign"><i class="icon zmdi zmdi-mail-reply-all"></i> 分派</a>
							<a class="dropdown-item J_share"><i class="icon zmdi zmdi-slideshare"></i> 共享</a>
						</div>
					</div>
				</div>
				<div class="btns">
					<div class="btn-group J_actions">
						<button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown"><i class="icon zmdi zmdi-plus"></i> 新建</button>
						<div class="dropdown-menu dropdown-menu-right">
						</div>
					</div>
				</div>
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
	RbViewPage.init('${id}', [ '${entityLabel}', '${entityName}', '${entityIcon}'])
	RbViewPage.initVTabs(${viewTabs})
});
</script>
</body>
</html>
