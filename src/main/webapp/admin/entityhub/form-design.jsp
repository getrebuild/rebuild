<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/form-design.css">
<title>表单布局</title>
</head>
<body class="open-right-sidebar">
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-aside rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="实体管理" name="pageTitle"/>
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
							<li><a href="base">基本信息</a></li>
							<li><a href="fields">管理字段</a></li>
							<li class="active"><a href="form-design">表单布局</a></li>
							<li><a href="advanced">高级配置</a></li>
						</ul>
					</div>
				</div>
			</div>
        </aside>
        <div class="rb-right-sidebar field-aside">
        	<div class="rb-content">
	        	<div class="field-head">
		        	<h4 class="float-left">字段列表</h4>
		        	<div class="float-right"><span class="not-nullable">必填字段</span><span class="readonly">只读字段</span></div>
				</div>
	        	<div class="rb-scroller">
	        		<div class="field-list dd-list">
						<div class="nodata">全部字段已布局</div>
					</div>
	        	</div>
        	</div>
        </div>
		<div class="main-content container-fluid">
			<div style="min-width:400px">
				<div class="float-right" style="margin-top:-1px">
					<a class="btn btn-link J_add-divider">+ 添加分栏</a>
					<button class="btn btn-primary J_save" type="button">保存配置</button>
				</div>
				<div class="tab-container">
               		<ul class="nav nav-tabs nav-tabs-classic">
						<li class="nav-item"><a class="nav-link active" href="#form-design">表单/视图布局</a></li>
					</ul>
	                <div class="tab-content">
						<div class="tab-pane active">
							<div class="form-preview view-preview dd-list">
								<div class="nodata">点击右侧字段添加到布局</div>
							</div>
							<div class="clearfix"></div>
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
	formConfig: ${FormConfig} || [],
	entityName: '${entityName}'
}
</script>
<script src="${baseUrl}/assets/js/entityhub/form-design.jsx" type="text/babel"></script>
</body>
</html>