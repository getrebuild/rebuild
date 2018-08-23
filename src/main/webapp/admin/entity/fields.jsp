<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>管理字段</title>
</head>
<body>
<div class="rb-wrapper rb-collapsible-sidebar rb-fixed-sidebar rb-aside">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="实体管理" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="entity-list" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<aside class="page-aside">
			<div class="rb-scroller">
				<div class="aside-content">
					<div class="content">
						<div class="aside-header">
							<span class="title">${entityLabel}</span>
							<p class="description">${comments}</p>
						</div>
					</div>
					<div class="aside-nav collapse">
						<ul class="nav">
							<li><a href="base"><i class="icon mdi mdi-inbox"></i>基本信息</a></li>
							<li class="active"><a href="fields"><i class="icon mdi mdi-inbox"></i>管理字段</a></li>
							<li><a href="form-design"><i class="icon mdi mdi-inbox"></i>表单布局</a></li>
						</ul>
					</div>
				</div>
			</div>
        </aside>
		<div class="main-content container-fluid">
			<div class="card card-table">
				<div class="card-body rb-loading rb-loading-active">
					<div class="dataTables_wrapper container-fluid">
						<div class="row rb-datatable-header">
							<div class="col-sm-6">
								<div class="dataTables_filter">
									<div class="input-group input-search">
										<input class="form-control" placeholder="搜索..." type="text"><span class="input-group-btn">
										<button class="btn btn-secondary"><i class="icon zmdi zmdi-search"></i></button></span>
									</div>
								</div>
							</div>
							<div class="col-sm-6">
								<div class="dataTables_oper">
									<button class="btn btn-space btn-primary J_new" data-url="${baseUrl}/admin/entity/field-new.htm?entity=${entityName}"><i class="icon zmdi zmdi-plus"></i> 新建</button>
									<button class="btn btn-space btn-secondary" disabled="disabled"><i class="icon zmdi zmdi-delete"></i> 删除</button>
								</div>
							</div>
						</div>
						<div class="row rb-datatable-body">
							<div class="col-sm-12">
								<table class="table table-hover" id="dataList" data-entity="MetaField">
									<thead>
										<tr>
											<th width="50">
												<label class="custom-control custom-control-sm custom-checkbox">
													<input class="custom-control-input" type="checkbox"><span class="custom-control-label"></span>
												</label>
											</th>
											<th data-filed="fieldLabel">字段名称</th>
											<th data-field="fieldName">内部标识</th>
											<th data-field="displayType">类型</th>
											<th data-field="comments">备注</th>
											<th width="100"></th>
										</tr>
									</thead>
									<tbody></tbody>
								</table>
							</div>
						</div>
						<div class="row rb-datatable-footer">
							<div class="col-sm-5">
								<div class="dataTables_info"></div>
							</div>
							<div class="col-sm-7">
								<div class="dataTables_paginate paging_simple_numbers">
									<ul class="pagination">
										<li class="paginate_button page-item previous disabled"><a class="page-link"><span class="icon zmdi zmdi-chevron-left"></span></a></li>
										<li class="paginate_button page-item active"><a class="page-link">1</a></li>
										<li class="paginate_button page-item next disabled"><a class="page-link"><span class="icon zmdi zmdi-chevron-right"></span></a></li>
									</ul>
								</div>
							</div>
						</div>
					</div>
					<div class="rb-spinner">
						<svg width="40px" height="40px" viewBox="0 0 66 66" xmlns="http://-www.w3.org/2000/svg">
							<circle fill="none" stroke-width="4" stroke-linecap="round" cx="33" cy="33" r="30" class="circle"></circle>
						</svg>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/babel">
const rbModal = renderRbcomp(<RbModal title="新建字段" target=".J_new" />);
</script>
<script type="text/javascript">
$(document).ready(function(){
	$.get('../list-field?entity=${entityName}', function(res){
		let tbody = $('#dataList tbody');
		$(res.data).each(function(){
			let tr = $('<tr></tr>').appendTo(tbody);
			$('<td data-id="' + this.fieldId + '"><label class="custom-control custom-control-sm custom-checkbox"><input class="custom-control-input" type="checkbox"><span class="custom-control-label"></span></label></td>').appendTo(tr);
			$('<td>' + this.fieldLabel + '</td>').appendTo(tr);
			$('<td>' + this.fieldName + '</td>').appendTo(tr);
			$('<td>' + this.displayType + '</td>').appendTo(tr);
			$('<td>' + (this.comments || '--') + '</td>').appendTo(tr);
			let actions = $('<td class="actions"><a class="icon J_edit" href="field/' + this.fieldName + '"><i class="zmdi zmdi-settings"></i></a><a class="icon J_del"><i class="zmdi zmdi-delete"></i></a></td>').appendTo(tr);
			actions.find('.J_del').click(function(){
				confirm('删除？');
			});
		});
		$('.dataTables_info').text('共 ' + res.data.length + ' 个字段');
		$('.rb-loading-active').removeClass('rb-loading-active')
	});
});
</script>
</body>
</html>