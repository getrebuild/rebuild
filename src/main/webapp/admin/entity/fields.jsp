<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
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
							<li><a href="form-design">设计布局</a></li>
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
										<input class="form-control rounded" placeholder="搜索 字段名称/内部标识" type="text">
										<span class="input-group-btn"><button class="btn btn-secondary"><i class="icon zmdi zmdi-search"></i></button></span>
									</div>
								</div>
							</div>
							<div class="col-sm-6">
								<div class="dataTables_oper">
									<button class="btn btn-primary btn-space J_new-field"><i class="icon zmdi zmdi-plus"></i> 新建</button>
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
									<div class="rb-spinner">
										<svg width="40px" height="40px" viewBox="0 0 66 66" xmlns="http://-www.w3.org/2000/svg">
											<circle fill="none" stroke-width="4" stroke-linecap="round" cx="33" cy="33" r="30" class="circle"></circle>
										</svg>
									</div>
								</div>
							</div>
						</div>
						<div id="pagination"></div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>

<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/rb-list.jsx" type="text/babel"></script>
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
		rb.modal('${baseUrl}/admin/p/entity/field-new?entity=${entityName}', '新建字段')
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
			$('<span class="badge badge-pill badge-secondary thin ml-1">主显</span>').appendTo(name)
		} else if (item.builtin == true){
			tr.addClass('muted')
		} else if (item.nullable == false){
			tr.addClass('danger')
		}
		$('<td><div class="text-muted">' + item.fieldName + '</div></td>').appendTo(tr)
		$('<td>' + item.displayType + '</td>').appendTo(tr)
		$('<td><div>' + (item.comments || '') + '</div></td>').appendTo(tr)
		$('<td class="actions"><a class="icon J_edit" href="field/' + item.fieldName + '"><i class="zmdi zmdi-settings"></i></a></td>').appendTo(tr)
		size++
	});
	
	rb.RbListPagination({ rowTotal:size, pageSize:1000, pageNo:1 })
	$('#dataList').parent().removeClass('rb-loading-active')
}
</script>
</body>
</html>