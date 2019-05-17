<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/widget/bootstrap-slider.min.css">
<%@ include file="/_include/Head.jsp"%>
<title>表单回填配置</title>
<style type="text/css">
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-aside rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="字段表单回填配置" name="pageTitle"/>
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
							<li><a href="../../base">基本信息</a></li>
							<li class="active"><a href="../../fields">管理字段</a></li>
							<li><a href="../../form-design">表单布局</a></li>
							<li><a href="../../advanced">高级配置</a></li>
						</ul>
					</div>
				</div>
			</div>
        </aside>
		<div class="page-head">
			<div class="page-head-title">表单回填配置</div>
		</div>
		<div class="main-content container-fluid pt-1">
			<ul class="nav nav-tabs nav-tabs-classic">
				<li class="nav-item"><a href="../${fieldName}" class="nav-link">字段信息</a></li>
				<li class="nav-item"><a href="./auto-fillin" class="nav-link active">表单回填配置</a></li>
			</ul>
			<div class="card mb-0">
				<div class="card-body">
					<div class="dataTables_wrapper container-fluid">
						<div class="row rb-datatable-header pr-0">
							<div class="col-sm-6">
							</div>
							<div class="col-sm-6">
								<div class="dataTables_oper">
									<button class="btn btn-primary J_add-rule"><i class="icon zmdi zmdi-plus"></i> 添加</button>
								</div>
							</div>
						</div>
						<div class="row rb-datatable-body">
							<div class="col-sm-12">
								<div class="rb-loading rb-loading-active data-list">
									<table class="table table-hover table-striped" id="dataList">
										<thead>
											<tr>
												<th>源字段</th>
												<th>目标字段</th>
												<th width="30%">回填规则</th>
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
window.__PageConfig = {
	entityName: '${entityName}',
	fieldName: '${fieldName}',
	fieldLabel: '${fieldLabel}'
}
</script>
<script type="text/babel" src="${baseUrl}/assets/js/entity/auto-fillin.jsx"></script>
</body>
</html>
