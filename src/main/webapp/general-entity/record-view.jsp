<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>${entityLabel}视图</title>
<style type="text/css">
.view-btns{padding-bottom:15px;}
.view-btns .btn{width:100%;}
.view-btns .btn+.btn{margin-top:6px}
</style>
</head>
<body class="dialog">
<div class="main-content">
	<div class="row">
		<div class="col-sm-10">
			${id}
		</div>
		<div class="col-sm-2">
			<div class="view-btns">
				<button class="btn btn-secondary J_edit" type="button">编辑</button>
			</div>
			<div class="view-btns">
				<button class="btn btn-secondary J_assgin" type="button">分配</button>
				<button class="btn btn-secondary J_share" type="button">共享</button>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
$(document).ready(function(){
})
</script>
</body>
</html>
