<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>新建</title>
<style type="text/css">
</style>
</head>
<body class="dialog">
<div class="main-content rb-loading rb-loading-active">
	<div id="form-container"></div>
	<div class="rb-spinner">
		<svg width="40px" height="40px" viewBox="0 0 66 66" xmlns="http://-www.w3.org/2000/svg">
			<circle fill="none" stroke-width="4" stroke-linecap="round" cx="33" cy="33" r="30" class="circle"></circle>
		</svg>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/babel">(function(){ renderRbform(${FormConfig}) })()</script>
<script type="text/javascript">
$(document).ready(function(){
});
</script>
</body>
</html>
