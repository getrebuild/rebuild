<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>实体管理</title>
<style type="text/css">
.card.entity .card-body{padding:14px 20px}
.card.entity .icon{font-size:40px;}
.card.entity h5,.card.entity p{margin:3px 0;}
.card.entity p{color:#777;font-size:0.9rem;}
</style>
</head>
<body>
<div class="rb-wrapper rb-collapsible-sidebar">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="实体管理" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="entity-manage" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="row">
				<div class="col-sm-2">
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
</script>
</body>
</html>
