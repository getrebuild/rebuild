<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>用户管理</title>
<style type="text/css">
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-aside">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="用户管理" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="users" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<aside class="page-aside ">
			<div class="rb-scroller">
				<div class="dept-tree">
				</div>
			</div>
		</aside>
		<div class="main-content container-fluid main-content-list">
			<ul class="nav nav-tabs nav-tabs-classic">
				<li class="nav-item"><a href="users" class="nav-link active"><span class="icon zmdi zmdi-account"></span> 用户列表</a></li>
				<li class="nav-item"><a href="departments" class="nav-link"><span class="icon zmdi zmdi-accounts"></span> 部门列表</a></li>
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
									<div class="btn-group btn-space">
										<button class="btn btn-primary J_new-user" type="button"><i class="icon zmdi zmdi-account-add"></i> 新建用户</button>
										<button class="btn btn-primary dropdown-toggle auto" type="button" data-toggle="dropdown"><span class="icon zmdi zmdi-chevron-down"></span></button>
										<div class="dropdown-menu dropdown-menu-right">
											<a class="dropdown-item J_new-dept"><i class="icon zmdi zmdi-accounts-add"></i> 新建部门</a>
                      					</div>
									</div>
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
<script src="${baseUrl}/assets/js/rb-advfilter.jsx" type="text/babel"></script>
<script type="text/babel">
var rbList, columnsModal
$(document).ready(function(){
	rbList = rb.RbList({ config: JSON.parse('${DataListConfig}') })
	
	$('.J_view').click(function(){
		let s = rbList.getSelectedRows()
		if (s.length == 1) {
			s = s[0]
			rb.RbViewModal({ id: s[0], entity: s[2][0] })
		}
	})

	$('.J_new-user').click(function(){
		rb.RbFormModal({ title: '新建用户', entity: 'User', icon: 'account' })
		formPostType = 1
	})
	$('.J_new-dept').click(function(){
		rb.RbFormModal({ title: '新建部门', entity: 'Department', icon: 'accounts' })
		formPostType = 2
	})

	$('.J_columns').click(function(){
		if (columnsModal) columnsModal.show()
		else columnsModal = rb.modal('${baseUrl}/page/general-entity/show-columns?entity=User', '设置列显示')
	})

	QuickFilter.init('.input-search', 'User')

	loadDeptTree()
})
var formPostType = 1
RbForm.postAfter = function(){
	if (formPostType == 1) rbList.reload()
	else loadDeptTree()
}
const loadDeptTree = function(){
	$.get(rb.baseUrl + '/admin/bizuser/dept-tree', function(res){
		$('.dept-tree').empty()
		let root = $('<ul class="list-unstyled"></ul>').appendTo('.dept-tree')
		renderDeptTree({ id:'$ALL', name:'所有部门' }, root).addClass('active')
		$(res.data).each(function(){
			renderDeptTree(this, root)
		})
	})
}
const renderDeptTree = function(dept, target) {
	let child = $('<li data-id="' + dept.id + '"><a class="text-truncate">' + dept.name + '</a></li>').appendTo(target)
	child.click(function(){
		$('.dept-tree li').removeClass('active')
		child.addClass('active')
		return false
	})
	if (dept.children && dept.children.length > 0) {
		let parent = $('<ul class="list-unstyled"></ul>').appendTo(child)
		$(dept.children).each(function(){
			renderDeptTree(this, parent)
		})
	}
	return child
}
</script>
</body>
</html>
