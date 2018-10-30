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
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="${entityLabel}列表" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="nav_entity-${entityName}" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="card card-table">
				<div class="card-body">
					<div class="dataTables_wrapper container-fluid">
						<div class="row rb-datatable-header">
							<div class="col-12 col-sm-6">
								<div class="dataTables_filter">
									<div class="adv-search float-left">
										<button class="btn btn-secondary" type="button"><span class="text-truncate">所有数据</span><i class="icon zmdi zmdi-caret-down"></i></button>
									</div>
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
									<button class="btn btn-space btn-secondary J_edit" disabled="disabled"><i class="icon zmdi zmdi-border-color"></i> 编辑</button>
									<button class="btn btn-space btn-primary J_new" data-url="${baseUrl}/entity/${entity}/new"><i class="icon zmdi zmdi-plus"></i> 新建</button>
									<div class="btn-group btn-space J_action">
										<button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown">更多 <i class="icon zmdi zmdi-more-vert"></i></button>
										<div class="dropdown-menu dropdown-menu-right">
											<a class="dropdown-item J_delete"><i class="icon zmdi zmdi-delete"></i> 删除</a>
											<a class="dropdown-item J_assign"><i class="icon zmdi zmdi-mail-reply-all"></i> 分派</a>
											<a class="dropdown-item J_share"><i class="icon zmdi zmdi-slideshare"></i> 共享</a>
											<div class="dropdown-divider"></div>
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
<script src="${baseUrl}/assets/js/rb-advfilter.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/assign-share.jsx" type="text/babel"></script>
<script type="text/babel">
$(document).ready(function(){
	RbListPage.init(${DataListConfig}, [ '${entityLabel}', '${entityName}', '${entityIcon}' ], ${entityPrivileges})
});
</script>
</body>
</html>
