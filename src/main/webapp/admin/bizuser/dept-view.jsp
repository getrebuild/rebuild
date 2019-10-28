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
	entity: ['Department','${entityLabel}','${entityIcon}'],
	recordId: '${id}'
}
</script>
<script src="${baseUrl}/assets/js/rb-forms.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-forms.exts.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-view.jsx" type="text/babel"></script>
<script type="text/babel">
let RbForm_postAfter = RbForm.postAfter
RbForm.postAfter = function() {
	RbForm_postAfter()
	if (parent && parent.loadDeptTree) parent.loadDeptTree()
}
$(document).ready(function() {
	$('.J_delete').off('click').click(function() {
		$.get(rb.baseUrl + '/admin/bizuser/delete-checks?id=${id}', function(res) {
			if (res.data.hasMember == 0 && res.data.hasChild == 0){
				RbAlert.create('此部门可以被安全的删除', '删除部门', { type: 'danger', confirmText: '删除', confirm: function(){ deleteDept(this) } })
			} else {
				let msg = '此部门下有 '
				if (res.data.hasMember > 0) msg += '<b>' + res.data.hasMember + '</b> 个用户' + (res.data.hasMember > 0 ? '和 ' : ' ')
				if (res.data.hasMember > 0) msg += '<b>' + res.data.hasMember + '</b> 个子部门'
				msg += '<br>需要先将他们转移至其他部门，然后才能安全删除'
				RbAlert.create(msg, '无法删除', { type: 'warning', html: true })
			}
		})
	})
})
let deleteDept = function(dlg){
	dlg.disabled(true)
	$.post(rb.baseUrl + '/admin/bizuser/dept-delete?transfer=&id=${id}', function(res){
		if (res.error_code == 0) {
			parent.location.hash = '!/View/'
			parent.location.reload()
		} else RbHighbar.error(res.error_msg)
	})
}
</script>
</body>
</html>
