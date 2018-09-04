<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>${entityLabel}列表</title>
<style type="text/css">
#react-list{background-color:#fff;min-height:200px}
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
				<div class="card-body">
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
									<button class="btn btn-space btn-secondary J_column">列显示 <i class="icon zmdi zmdi-more-vert"></i></button>
								</div>
							</div>
						</div>
						<div id="react-list" class="rb-loading rb-loading-active">
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
<script type="text/babel">
$(document).ready(function(){
	var listConfig = JSON.parse('${DataListConfig}');
	var rbList = renderRbcomp(<RbList config={listConfig} />, 'react-list');

	var rbFormModal = null;
	$('.J_new').click(function(){
		if (rbFormModal) rbFormModal.show()
		else rbFormModal = renderRbcomp(<RbFormModal title="新建${entityLabel}" entity="${entityName}" />, 'react-forms')
	});
});
</script>
</body>
</html>
