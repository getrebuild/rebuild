<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>实体管理</title>
<style type="text/css">
.card.entity:hover{background-color:rgba(255,255,255,.7)}
.card.entity .card-body{padding:14px 20px;color:#333;}
.card.entity .icon{font-size:32px;color:#4285f4;}
.card.entity .card-body .float-left{width:30px;text-align:center;}
.card.entity span{margin-top:2px;display:block;}
.card.entity p{margin:0}
.title{margin-left:44px}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="实体管理" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="entities" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="row" id="entityList">
			</div>
		</div>
	</div>
</div>
<script type="text/palin" id="entity-tmpl">
<div class="col-xl-2 col-lg-3 col-sm-6">
	<a class="card entity">
		<div class="card-body">
			<div class="float-left"><i class="icon zmdi"></i></div>
			<div class="title">
				<span class="text-truncate">新建实体</span>
				<p class="text-muted">新建一个新实体</p>
			</div>
			<div class="clearfix"></div>
		</div>
	</a>
</div>
</script>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
var newEntityModal = null
$(document).ready(function(){
	$.get(rb.baseUrl + '/admin/entity/entity-list', function(res){
		$(res.data).each(function(){
			let tmp = $($('#entity-tmpl').html()).appendTo('#entityList')
			tmp.find('a.card').attr('href', 'entity/' + this.entityName + '/base')
			tmp.find('.icon').addClass('zmdi-' + this.icon)
			tmp.find('.title span').text(this.entityLabel)
			tmp.find('.title p').text(this.comments || '-')
		});
		
		let tmp = $($('#entity-tmpl').html()).appendTo('#entityList')
		tmp.find('.icon').addClass('zmdi-plus')
		tmp.click(function(){
			if (newEntityModal) newEntityModal.show()
			else newEntityModal = rb.modal(rb.baseUrl + '/admin/page/entity/entity-new', '新建实体')
		})
	})
})
</script>
</body>
</html>
