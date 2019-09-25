<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>图标</title>
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
<script type="text/babel">
$(document).ready(function(){
	let call = parent.clickIcon || function(icon){ console.log(icon) }
	$(ZMDI_ICONS).each(function(){
		if (ZMDI_ICONS_IGNORE.contains(this + '') == false) {
			let a = $('<a data-icon="' + this + '" title="' + this.toUpperCase() + '"><i class="zmdi zmdi-' + this + '"></a>').appendTo('#icons')
			a.click(function(){ call($(this).data('icon')) })
		}
	})
	parent.RbModal.resize()
})
</script>
</body>
</html>
