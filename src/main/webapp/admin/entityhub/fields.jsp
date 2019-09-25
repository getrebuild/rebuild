<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>管理字段</title>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-aside rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="实体管理" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="entities" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<aside class="page-aside">
			<div class="rb-scroller-aside rb-scroller">
				<div class="aside-content">
					<div class="content">
						<div class="aside-header">
							<button class="navbar-toggle collapsed" type="button"><span class="icon zmdi zmdi-caret-down"></span></button>
							<span class="title">${entityLabel}</span>
							<p class="description">${comments}</p>
						</div>
					</div>
					<div class="aside-nav collapse">
						<ul class="nav">
							<li><a href="base">基本信息</a></li>
							<li class="active"><a href="fields">管理字段</a></li>
							<li><a href="form-design">表单布局</a></li>
							<li><a href="advanced">高级配置</a></li>
						</ul>
					</div>
				</div>
			</div>
        </aside>
		<div class="main-content container-fluid">
			<div class="card card-table">
				<div class="card-body">
					<div class="dataTables_wrapper container-fluid">
						<div class="row rb-datatable-header">
							<div class="col-sm-6">
								<div class="dataTables_filter">
									<div class="input-group input-search">
										<input class="form-control" type="text" placeholder="查询字段" maxlength="40">
										<span class="input-group-btn"><button class="btn btn-secondary" type="button"><i class="icon zmdi zmdi-search"></i></button></span>
									</div>
								</div>
							</div>
							<div class="col-sm-6">
								<div class="dataTables_oper">
									<button class="btn btn-primary btn-space J_new-field"><i class="icon zmdi zmdi-plus"></i> 添加</button>
								</div>
							</div>
						</div>
						<div class="row rb-datatable-body">
							<div class="col-sm-12">
								<div class="rb-loading rb-loading-active data-list">
									<table class="table table-hover table-striped" id="dataList">
										<thead>
											<tr>
												<th width="25%" data-filed="fieldLabel">字段名称</th>
												<th width="16%" data-field="fieldName">内部标识</th>
												<th width="16%" data-field="displayType">类型</th>
												<th data-field="comments">备注</th>
												<th width="50"></th>
											</tr>
										</thead>
										<tbody></tbody>
									</table>
									<%@ include file="/_include/spinner.jsp"%>
								</div>
							</div>
						</div>
						<div id="pagination">
							<div class="row rb-datatable-footer">
								<div class="col-sm-3"><div class="dataTables_info"></div></div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>

<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = { isSuperAdmin: ${isSuperAdmin} }
</script>
<script type="text/babel">
let fields_data = null
let name_field = '${nameField}'
$(document).ready(function(){
	$.get('../list-field?entity=${entityName}', function(res){
		fields_data = res.data
		render_list()
	})

	$('.input-search .btn').click(function(){
		render_list($val('.input-search .form-control'))
	})
	$('.input-search .form-control').keydown(function(event){
		if (event.which == 13) $('.input-search .btn').trigger('click')
	})

	$('.J_new-field').click(function(){
		if (window.__PageConfig.isSuperAdmin) RbModal.create('${baseUrl}/admin/p/entityhub/field-new?entity=${entityName}', '添加字段')
		else RbHighbar.error('仅超级管理员可添加字段')
	})
});
const render_list = function(q){
	if (!fields_data) return
	let tbody = $('#dataList tbody').empty()
	let size = 0
	$(fields_data).each(function(idx, item){
		if (!!q){
			if (!(item.fieldName.contains(q) || item.fieldLabel.contains(q))) return
		}
		let tr = $('<tr data-id="' + (item.fieldId || '') + '"></tr>').appendTo(tbody)
		let name = $('<td><a href="field/' + item.fieldName + '" class="column-main">' + item.fieldLabel + '</a></td>').appendTo(tr)
		if (item.fieldName == name_field){
			tr.addClass('primary')
			$('<span class="badge badge-pill badge-secondary thin ml-1">名称</span>').appendTo(name)
		} else if (item.creatable == false){
			tr.addClass('muted')
		} else if (item.nullable == false){
			tr.addClass('danger')
		}
		$('<td><div class="text-muted">' + item.fieldName + '</div></td>').appendTo(tr)
		$('<td><div class="text-muted">' + item.displayType + '</div></td>').appendTo(tr)
		$('<td><div>' + (item.comments || '') + '</div></td>').appendTo(tr)
		$('<td class="actions"><a class="icon J_edit" href="field/' + item.fieldName + '"><i class="zmdi zmdi-settings"></i></a></td>').appendTo(tr)
		size++
	});
	
	$('.dataTables_info').text('共 ' + size + ' 个字段')
	$('#dataList').parent().removeClass('rb-loading-active')
}
</script>
</body>
</html>