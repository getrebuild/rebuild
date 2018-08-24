<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>${entityLabel}列表</title>
<style type="text/css">
</style>
</head>
<body>
<div class="rb-wrapper rb-collapsible-sidebar">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="${entityLabel}列表" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="${entityName}-list" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="card card-table">
				<div class="card-body rb-loading rb-loading-active1">
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
									<button class="btn btn-space btn-primary J_new" data-url="${baseUrl}/entity/${entity}/new"><i class="icon zmdi zmdi-plus"></i> 新建</button>
									<button class="btn btn-space btn-secondary J_del" disabled="disabled"><i class="icon zmdi zmdi-delete"></i> 删除</button>
									<div class="btn-group btn-space">
										<button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown">更多 <span class="icon-dropdown zmdi zmdi-chevron-down"></span></button>
										<div class="dropdown-menu dropdown-menu-right">
											<a class="dropdown-item">列显示</a>
											<div class="dropdown-divider"></div>
											<a class="dropdown-item">导入</a>
											<a class="dropdown-item">导出</a>
										</div>
									</div>
								</div>
							</div>
						</div>
						<div class="row rb-datatable-body">
							<div class="col-sm-12">
								<table class="table" id="dataList" data-entity="User">
									<thead>
										<tr>
											<th width="50">
												<label class="custom-control custom-control-sm custom-checkbox">
													<input class="custom-control-input" type="checkbox"><span class="custom-control-label"></span>
												</label>
											</th>
											<th data-filed="loginName">用户</th>
											<th data-field="email">邮箱</th>
											<th data-field="createdOn">创建时间</th>
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
										<li class="paginate_button page-item previous disabled"><a href="#" class="page-link"><span class="icon zmdi zmdi-chevron-left"></span></a></li>
										<li class="paginate_button page-item active"><a href="#" class="page-link">1</a></li>
										<li class="paginate_button page-item next"><a href="#" class="page-link"><span class="icon zmdi zmdi-chevron-right"></span></a></li>
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
<script src="${baseUrl}/assets/js/rb-list.js" type="text/javascript"></script>
<script type="text/babel">const rbModal = renderRbcomp(<RbModal title="新建${entityLabel}" target=".J_new" />)</script>
<script type="text/javascript">
$(document).ready(function(){
	$('.dropdown-toggle').dropdown()
});
</script>
</body>
</html>
