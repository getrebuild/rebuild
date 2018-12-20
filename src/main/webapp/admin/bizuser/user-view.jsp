<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/view-page.css">
<title>${entityLabel}视图</title>
</head>
<body class="view-body">
<div class="view-header">
	<span class="header-icon zmdi zmdi-${entityIcon}"></span>
	<h3 class="title">${entityLabel}视图</h3>
	<span>
		<a class="close J_close"><i class="zmdi zmdi-close"></i></a>
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
		<div class="col-sm-3 view-metas">
			<div class="view-action row">
				<div class="col-6 pr-1 mb-2">
					<button class="btn btn-secondary J_edit" type="button"><i class="icon zmdi zmdi-border-color"></i> 编辑</button>
				</div>
				<div class="col-6 pl-1 mb-2 btn-group J_action">
					<button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown">更多 <i class="icon zmdi zmdi-more-vert"></i></button>
					<div class="dropdown-menu dropdown-menu-right">
						<a class="dropdown-item J_delete"><i class="icon zmdi zmdi-delete"></i> 删除</a>
						<a class="dropdown-item J_enable"><i class="icon zmdi zmdi-minus-circle-outline"></i> 启用</a>
						<a class="dropdown-item J_disable"><i class="icon zmdi zmdi-minus-circle-outline"></i> 停用</a>
						<div class="dropdown-divider"></div>
						<a class="dropdown-item J_changeDept"><i class="icon zmdi zmdi-accounts"></i> 指定新部门</a>
						<a class="dropdown-item J_changeRole"><i class="icon zmdi zmdi-lock"></i> 指定新角色</a>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/rb-forms.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-forms-ext.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-view.jsx" type="text/babel"></script>
<script>
window.__PageConfig = {
	type: 'RecordView',
	entity: ['User','${entityLabel}','${entityIcon}'],
	recordId: '${id}'
}
</script>
<script type="text/babel">
$(document).ready(function(){
	if (rb.isAdminUser != true || rb.isAdminVerified == false) $('.view-action').remove()

	$('.J_delete').off('click').click(function(){
		rb.alert('<b>暂不支持删除用户</b><br>我们建议你停用用户，而非将其删除', null, { html: true, confirmText: '停用', confirm:()=>{ toggleDisabled(true) } })
	})
	$('.J_enable').click(()=>{ toggleDisabled(false) })
	$('.J_disable').click(()=>{ toggleDisabled(true) })

	$('.J_changeDept').click(function(){
		rb.modal(rb.baseUrl + '/p/admin/bizuser/change-dept?user=${id}', '指定新部门', { width:580 } )
	})
	$('.J_changeRole').click(function(){
		rb.modal(rb.baseUrl + '/p/admin/bizuser/change-role?user=${id}', '指定新角色', { width:580 } )
	})
	
	if (rb.isAdminVerified){
		$.get(rb.baseUrl + '/admin/bizuser/check-user-status?id=${id}', (res) => {
			if (res.data.system == true){
				$('.J_tips').removeClass('hide').find('.message p').text('系统内建用户，不允许修改')
				$('.view-action').remove()
				return
			}

			if (res.data.disabled == true) $('.J_disable').remove()
			else $('.J_enable').remove()

			if (res.data.active == true) return
			let reason = []
			if (!res.data.role) reason.push('未指定角色')
			if (!res.data.dept) reason.push('未指定部门')
			if (res.data.disabled == true) reason.push('已停用')
			$('.J_tips').removeClass('hide').find('.message p').text('当前用户处于未激活状态，因为其 ' + reason.join(' / '))
		})
	}
})
let toggleDisabled = function(disabled){
	let _data = { isDisabled: disabled }
	_data.metadata = { entity: 'User', id: '${id}' }
	$.post(rb.baseUrl + '/app/entity/record-save', JSON.stringify(_data), function(res){
		if (res.error_code == 0){
			rb.notice('用户已' + (disabled ? '停用' : '启用'), 'success')
			setTimeout(()=>{ location.reload() }, 500)
		}
	})
}
</script>
</body>
</html>
