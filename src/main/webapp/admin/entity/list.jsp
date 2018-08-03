<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>实体管理</title>
<style type="text/css">
.card.entity .card-body{padding:14px 20px;color:#333;}
.card.entity .icon{font-size:32px;margin-right:12px;color:#4285f4;}
.card.entity span{margin-top:2px;display:block;}
.card.entity p{margin:0}
</style>
</head>
<body>
<div class="rb-wrapper rb-collapsible-sidebar rb-fixed-sidebar">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="实体管理" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="entity-list" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="row">
				<div class="col-sm-2">
					<a class="card entity" href="manage.htm?entity=User">
						<div class="card-body">
							<div class="float-left"><i class="icon zmdi zmdi-account"></i></div>
							<div class="float-left">
								<span>用户</span>
								<p class="font-desc">系统内建</p>
							</div>
							<div class="clearfix"></div>
						</div>
					</a>
				</div>
				<div class="col-sm-2">
					<a class="card entity" href="manage.htm?entity=Department">
						<div class="card-body">
							<div class="float-left"><i class="icon zmdi zmdi-accounts"></i></div>
							<div class="float-left">
								<span>部门</span>
								<p class="font-desc">系统内建</p>
							</div>
							<div class="clearfix"></div>
						</div>
					</a>
				</div>
				<div class="col-sm-2">
					<a class="card entity J_entity-new">
						<div class="card-body">
							<div class="float-left"><i class="icon zmdi zmdi-plus"></i></div>
							<div class="float-left">
								<span>新建实体</span>
								<p class="font-desc">新建一个业务实体</p>
							</div>
							<div class="clearfix"></div>
						</div>
					</a>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/babel">
const rbModal = ReactDOM.render(<RbModal title="新建实体" />, $('<div id="react-comps"></div>').appendTo(document.body)[0]);
</script>
<script type="text/javascript">
$(document).ready(function(){
	$('.J_entity-new').click(function(){
		rbModal.show('entity-new.htm');
	})
})
</script>
</body>
</html>
