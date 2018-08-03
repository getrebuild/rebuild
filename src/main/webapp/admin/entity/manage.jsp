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
							<p class="description">系统内建</p>
						</div>
					</div>
					<div class="aside-nav collapse">
						<ul class="nav">
							<li class="active"><a href="manage.htm"><i class="icon mdi mdi-inbox"></i>基本信息</a></li>
							<li><a href="manage-fields.htm"><i class="icon mdi mdi-inbox"></i>管理字段</a></li>
							<li><a href="manage-layout.htm"><i class="icon mdi mdi-inbox"></i>管理布局</a></li>
						</ul>
					</div>
				</div>
			</div>
        </aside>
		<div class="page-head">
			<div class="page-head-title">基本信息</div>
		</div>
		<div class="main-content container-fluid" style="padding-top:3px">
			<div class="card">
				<div class="card-body">
					<form>
						<div class="form-group row">
							<label class="col-12 col-sm-2 col-form-label text-sm-right">实体名称</label>
							<div class="col-12 col-sm-8 col-lg-4">
								<input class="form-control form-control-sm" type="text">
							</div>
						</div>
						<div class="form-group row">
							<label class="col-12 col-sm-2 col-form-label text-sm-right">系统标识</label>
							<div class="col-12 col-sm-8 col-lg-4">
								<input class="form-control form-control-sm" type="text" readonly="readonly">
							</div>
						</div>
						<div class="form-group row">
							<label class="col-12 col-sm-2 col-form-label text-sm-right">描述</label>
							<div class="col-12 col-sm-8 col-lg-4">
								<textarea class="form-control form-control-sm row2"></textarea>
							</div>
						</div>
						<div class="form-group row footer">
							<label class="col-12 col-sm-2 col-form-label text-sm-right"></label>
							<div class="col-12 col-sm-8 col-lg-4">
								<button class="btn btn-primary btn-space" type="button">确定</button>
							</div>
						</div>
					</form>
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
