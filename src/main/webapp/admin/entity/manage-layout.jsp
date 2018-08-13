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
<div class="rb-wrapper rb-collapsible-sidebar rb-fixed-sidebar rb-aside">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="实体管理" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="entity-list" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<aside class="page-aside">
			<div class="rb-scroller">
				<div class="aside-content">
					<div class="content">
						<div class="aside-header">
							<span class="title">用户</span>
							<p class="description">管理用户</p>
						</div>
					</div>
					<div class="aside-nav collapse">
						<ul class="nav">
							<li><a href="manage.htm"><i class="icon mdi mdi-inbox"></i>基本信息</a></li>
							<li><a href="manage-fields.htm"><i class="icon mdi mdi-inbox"></i>管理字段</a></li>
							<li class="active"><a href="manage-layout.htm"><i class="icon mdi mdi-inbox"></i>管理布局</a></li>
						</ul>
					</div>
				</div>
			</div>
        </aside>
		<div class="main-content container-fluid">
			<h3 class="text-center">暂未开放</h3>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
$(document).ready(function(){
	let entity = $urlp('entity');
	$('.aside-nav a').each(function(){
		let that = $(this);
		that.attr('href', that.attr('href') + '?entity=' + entity);
	});
	
});
</script>
</body>
</html>
