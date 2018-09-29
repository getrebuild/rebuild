<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>${entityLabel}视图</title>
<style type="text/css">
html,body{overflow:auto;height:100%;}
.main-content{margin-top:62px;}
.tab-container{margin-top:-8px}
.tab-content{padding:0;margin:0;padding-top:13px}
.view-header{padding:15px 20px;height:62px;border-bottom:1px solid #e3e3e3;position:absolute;top:0;left:0;width:100%;z-index:101;background-color:#fff}
.view-header .header-icon{float:left;}
.view-header .title{line-height:1.428571;display:inline-block;margin:0;font-weight:300;font-size:1.538rem}
.view-btns{padding-bottom:15px;}
.view-btns .btn{width:100%;}
.view-btns .btn+.btn{margin-top:6px}
</style>
</head>
<body class="dialog">
<div class="view-header">
	 <span class="header-icon zmdi zmdi-${entityIcon}"></span>
	<h3 class="title">${entityLabel}视图</h3>
</div>
<div class="main-content container-fluid invisible">
	<div class="row">
		<div class="col-sm-10">
			<div class="tab-container">
				<ul class="nav nav-tabs">
					<li class="nav-item"><a class="nav-link active" href="#tab-rbview" data-toggle="tab">视图</a></li>
					<li class="nav-item"><a class="nav-link" href="#tab-salesorder" data-toggle="tab">订单 (0)</a></li>
					<li class="nav-item"><a class="nav-link" href="#tab-contact" data-toggle="tab">联系人 (0)</a></li>
				</ul>
				<div class="tab-content">
                    <div class="tab-pane active" id="tab-rbview"></div>
					<div class="tab-pane" id="tab-salesorder">tab-salesorder</div>
					<div class="tab-pane" id="tab-contact">tab-contact</div>
				</div>
			</div>
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
<script src="${baseUrl}/assets/js/rb-forms.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-view.jsx" type="text/babel"></script>
<script type="text/babel">
var rbFromView
$(document).ready(function(){
	const recordId = '${id}'
	rbFromView = renderRbcomp(<RbViewForm entity="${entityName}" id={recordId}  />, 'tab-rbview')

	$('.J_edit').click(function(){
		renderRbFormModal(recordId, '编辑记录', '${entityName}', '${entityIcon}')
	});
});
</script>
</body>
</html>
