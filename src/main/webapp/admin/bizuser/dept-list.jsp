<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>${entityLabel}管理</title>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-aside rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="${entityLabel}管理" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="users" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<aside class="page-aside">
			<div class="rb-scroller">
				<div class="dept-tree">
					<div class="ph-item rb">
						<div class="ph-col-12 p-0">
							<div class="ph-row">
								<div class="ph-col-12 big"></div>
								<div class="ph-col-12 big"></div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</aside>
		<div class="main-content container-fluid">
			<ul class="nav nav-tabs nav-tabs-classic">
				<li class="nav-item"><a href="users" class="nav-link"><span class="icon zmdi zmdi-account"></span> 用户</a></li>
				<li class="nav-item"><a href="departments" class="nav-link active"><span class="icon zmdi zmdi-accounts"></span> ${entityLabel}</a></li>
			</ul>
			<div class="card card-table">
				<div class="card-body">
					<div class="dataTables_wrapper container-fluid">
						<div class="row rb-datatable-header">
							<div class="col-12 col-lg-6">
								<div class="dataTables_filter">
									<div class="input-group input-search">
										<input class="form-control" type="text" placeholder="查询${entityLabel}" maxlength="40">
										<span class="input-group-btn"><button class="btn btn-secondary" type="button"><i class="icon zmdi zmdi-search"></i></button></span>
									</div>
								</div>
							</div>
							<div class="col-12 col-lg-6">
								<div class="dataTables_oper">
									<button class="btn btn-space btn-secondary J_view" disabled="disabled"><i class="icon zmdi zmdi-folder"></i> 打开</button>
									<button class="btn btn-primary btn-space J_new" type="button"><i class="icon zmdi zmdi-accounts-add"></i> 新建${entityLabel}</button>
									<div class="btn-group btn-space">
										<button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown">更多 <i class="icon zmdi zmdi-more-vert"></i></button>
										<div class="dropdown-menu dropdown-menu-right">
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
	type: $pgt.RecordList,
	entity: ['Department','${entityLabel}','${entityIcon}'],
	privileges: ${entityPrivileges},
	listConfig: ${DataListConfig},
	advFilter: false
}
</script>
<script src="${baseUrl}/assets/js/rb-datalist.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-forms.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-forms.exts.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/bizuser/dept-tree.js"></script>
<script type="text/babel">
RbForm.postAfter = loadDeptTree
$(document).ready(loadDeptTree)
clickDept = function(depts) {
	if (depts[0] == '$ALL$') depts = []
	let exp = { items: [], values: {} }
	exp.items.push({ op: 'in', field: 'deptId', value:'{2}' })
	exp.values['2'] = depts
	RbListPage._RbList.search(exp)
}
</script>
</body>
</html>
