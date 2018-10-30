<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>ICON</title>
<style type="text/css">
#icons a{display:inline-block;width:34px;height:34px;text-align:center;border-radius:2px;}
#icons a i{font-size:21px;line-height:34px;}
#icons a:hover{background-color:#4285f4}
#icons a:hover i{color:#fff;}
</style>
</head>
<body class="dialog">
<div class="main-content">
	<div id="icons" class="text-center"></div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/zmdi-icons.js"></script>
<script type="text/javascript">
$(document).ready(function(){
	let call = parent.clickIcon || function(icon){ alert(icon) };
	$(ZMDI_ICONS).each(function(){
		let a = $('<a data-icon="' + this + '" title="' + this.toUpperCase() + '"><i class="zmdi zmdi-' + this + '"></a>').appendTo('#icons');
		a.click(function(){
			call($(this).data('icon'))
		});
	});
});
</script>
</body>
</html>
