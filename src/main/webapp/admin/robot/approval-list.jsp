<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>审批流程</title>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header rb-aside">
<jsp:include page="/_include/NavTop.jsp">
	<jsp:param value="审批流程" name="pageTitle"/>
</jsp:include>
<jsp:include page="/_include/NavLeftAdmin.jsp">
	<jsp:param value="robot-approval" name="activeNav"/>
</jsp:include>
<div class="rb-content">
	<aside class="page-aside">
		<div class="rb-scroller">
			<div class="dept-tree">
				<h5 class="config-title">应用实体</h5>
				<ul class="list-unstyled">
					<li class="active"><a>所有实体</a></li>
				</ul>
			</div>
		</div>
	</aside>
	<div class="page-head">
		<div class="float-left"><div class="page-head-title">审批流程</div></div>
		<div class="float-right pt-1">
			<button class=" btn btn-primary J_add" type="button"><i class="icon zmdi zmdi-plus"></i> 添加</button>
		</div>
		<div class="float-right pt-1 mr-3">
			<div class="input-group input-search">
				<input class="form-control" type="text" placeholder="查询" maxlength="40">
				<span class="input-group-btn"><button class="btn btn-secondary" type="button"><i class="icon zmdi zmdi-search"></i></button></span>
			</div>
		</div>
		<div class="clearfix"></div>
	</div>
	<div class="main-content container-fluid pt-0">
		<div class="card card-table">
			<div class="card-body">
				<div class="dataTables_wrapper container-fluid">
					<div class="row rb-datatable-body">
						<div class="col-sm-12">
							<div class="rb-loading rb-loading-active data-list">
								<table class="table table-hover table-striped table-fixed">
									<thead>
									<tr>
										<th>名称</th>
										<th>应用实体</th>
										<th width="80">启用</th>
										<th width="160">更新时间</th>
										<th width="80"></th>
									</tr>
									</thead>
									<tbody id="dataList"></tbody>
								</table>
								<%@ include file="/_include/spinner.jsp"%>
								<div class="list-nodata hide"><span class="zmdi zmdi-assignment-check"></span><p>暂无审批流程</p></div>
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
<script type="text/babel" src="${baseUrl}/assets/js/admin/config-comps.jsx"></script>
<script type="text/babel" src="${baseUrl}/assets/js/entityhub/approval-list.jsx"></script>
</body>
</html>
