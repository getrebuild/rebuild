<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
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
				<div class="dept-tree"></div>
			</div>
		</aside>
		<div class="main-content container-fluid main-content-list">
			<ul class="nav nav-tabs nav-tabs-classic">
				<li class="nav-item"><a href="users" class="nav-link"><span class="icon zmdi zmdi-account"></span> 用户列表</a></li>
				<li class="nav-item"><a href="departments" class="nav-link active"><span class="icon zmdi zmdi-accounts"></span> ${entityLabel}列表</a></li>
			</ul>
			<div class="card card-table">
				<div class="card-body">
					<div class="dataTables_wrapper">
						<div class="row rb-datatable-header">
							<div class="col-12 col-sm-6">
								<div class="dataTables_filter">
									<div class="input-group input-search">
										<input class="form-control rounded-left J_search-text" placeholder="搜索 ..." type="text">
										<span class="input-group-btn"><button class="btn btn-secondary J_search-btn" type="button"><i class="icon zmdi zmdi-search"></i></button></span>
										<span class="input-group-btn plus"><button class="btn btn-secondary J_qfields" type="button" title="设置查询字段"><i class="icon zmdi zmdi-playlist-plus"></i></button></span>
									</div>
								</div>
							</div>
							<div class="col-12 col-sm-6">
								<div class="dataTables_oper">
									<button class="btn btn-space btn-secondary J_view" disabled="disabled"><i class="icon zmdi zmdi-folder"></i> 打开</button>
									<button class="btn btn-primary btn-space J_new" type="button"><i class="icon zmdi zmdi-accounts-add"></i> 新建${entityLabel}</button>
									<div class="btn-group btn-space">
										<button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown">更多 <i class="icon zmdi zmdi-more-vert"></i></button>
										<div class="dropdown-menu dropdown-menu-right">
											<a class="dropdown-item J_columns"><i class="icon zmdi zmdi-sort-amount-asc"></i> 列显示</a>
										</div>
									</div>
								</div>
							</div>
						</div>
						<div id="react-list" class="rb-loading rb-loading-active data-list">
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
	</div>
</div>

<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/rb-list.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-forms.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-forms-ext.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/bizuser/dept-tree.js"></script>
<script type="text/babel">
RbForm.postAfter = function(){
	location.reload()
}
$(document).ready(function(){
	RbListPage.init($.parseJSON('${DataListConfig}'), ['Department','${entityLabel}','${entityIcon}'], $.parseJSON('${entityPrivileges}'))
	loadDeptTree()
})
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
