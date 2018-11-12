<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>首页</title>
<style type="text/css">
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="首页" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="dashboard-home" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="row">
				<div class="col-sm-6">
				</div>
				<div class="col-sm-6 text-right">
					<button type="button" class="btn btn-link"><i class="zmdi zmdi-plus icon"></i> 添加图表</button>
				</div>
			</div>
			<h3 class="text-center">首页可配置仪表盘</h3>
			<div id="advfilter"></div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/rb-advfilter.jsx" type="text/babel"></script>
<script type="text/babel">
$(document).ready(function(){
	renderRbcomp(<AdvFilter entity="ceshiziduan" trigger="" />, 'advfilter')
	//renderRbcomp(<RbModal><AdvFilter entity="kehu" inModal={true} /></RbModal>)
})
</script>
</body>
</html>
