<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/charts.css">
<title>${entityLabel}列表</title>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-aside ${asideCollapsed ? 'rb-aside-collapsed' : ''}">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="${entityLabel}列表" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="nav_entity-${masterEntity}" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<aside class="page-aside widgets">
			<a class="side-toggle" title="展开/收缩面板"><i class="zmdi zmdi-arrow-left"></i></a>
			<div class="tab-container">
				<ul class="nav nav-tabs">
					<li class="nav-item"><a class="nav-link active" href="#asideFilters" data-toggle="tab">常用查询</a></li>
					<li class="nav-item"><a class="nav-link J_load-chart" href="#asideWidgets" data-toggle="tab">图表</a></li>
				</ul>
				<div class="tab-content rb-scroller">
					<div class="tab-pane active" id="asideFilters">
						<div class="ph-item rb">
							<div class="ph-col-12 p-0">
								<div class="ph-row">
									<div class="ph-col-12 big"></div>
									<div class="ph-col-12 big"></div>
								</div>
							</div>
						</div>
					</div>
					<div class="tab-pane" id="asideWidgets">
						<div class="charts-wrap"></div>
						<div class="text-center"><button class="btn btn-secondary J_add-chart"><i class="icon zmdi zmdi-plus"></i> 选择图表</button></div>
					</div>
				</div>
			</div>
		</aside>
		<div class="main-content container-fluid">
			<ul class="nav nav-tabs nav-tabs-classic">
				<li class="nav-item"><a href="../${masterEntity}/list" class="nav-link"><span class="icon zmdi zmdi-${masterEntityIcon}"></span> ${masterEntityLabel}</a></li>
				<li class="nav-item"><a class="nav-link active"><span class="icon zmdi zmdi-${slaveEntityIcon}"></span> ${slaveEntityLabel}</a></li>
			</ul>
			<div class="card card-table">
				<div class="card-body">
					<div class="dataTables_wrapper container-fluid">
						<div class="row rb-datatable-header">
							<div class="col-12 col-md-6">
								<div class="dataTables_filter">
									<div class="adv-search float-left">
										<div class="btn-group btn-space">
											<button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown"><span class="text-truncate J_name">全部数据</span><i class="icon zmdi zmdi-caret-down"></i></button>
											<div class="dropdown-menu rb-scroller"><div class="dropdown-item" data-id="$ALL$"><a>全部数据</a></div></div>
											<div class="input-group-append"><button title="高级查询" class="btn btn-secondary J_advfilter" type="button"><i class="icon zmdi zmdi-filter-list"></i></button></div>
										</div>
									</div>
									<div class="input-group input-search">
										<input class="form-control" type="text" placeholder="查询${entityLabel}" maxlength="40">
										<span class="input-group-btn"><button class="btn btn-secondary" type="button"><i class="icon zmdi zmdi-search"></i></button></span>
									</div>
								</div>
							</div>
							<div class="col-12 col-md-6">
								<div class="dataTables_oper">
									<button class="btn btn-space btn-secondary J_view" disabled="disabled"><i class="icon zmdi zmdi-folder"></i> 打开</button>
									<button class="btn btn-space btn-secondary J_edit" disabled="disabled"><i class="icon zmdi zmdi-border-color"></i> 编辑</button>
									<div class="btn-group btn-space J_action">
										<button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown">更多 <i class="icon zmdi zmdi-more-vert"></i></button>
										<div class="dropdown-menu dropdown-menu-right">
											<a class="dropdown-item J_delete"><i class="icon zmdi zmdi-delete"></i> 删除</a>
											<div class="dropdown-divider"></div>
											<a class="dropdown-item J_columns"><i class="icon zmdi zmdi-code-setting"></i> 列显示</a>
										</div>
									</div>
								</div>
							</div>
						</div>
						<div id="react-list" class="rb-loading rb-loading-active data-list">
							<%@ include file="/_include/spinner.jsp"%>
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
	type: $pgt.SlaveList,
	entity: ['${entityName}','${entityLabel}','${entityIcon}'],
	privileges: ${entityPrivileges},
	listConfig: ${DataListConfig},
	advFilter: true
}
</script>
<script src="${baseUrl}/assets/js/rb-datalist.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-forms.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-forms.exts.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-advfilter.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-approval.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/lib/charts/echarts.min.js"></script>
<script src="${baseUrl}/assets/js/charts/charts.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/settings-share2.jsx" type="text/babel"></script>
</body>
</html>
