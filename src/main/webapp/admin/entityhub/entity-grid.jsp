<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>实体管理</title>
<style type="text/css">
.card.entity{position:relative;}
.card.entity:hover{background-color:rgba(255,255,255,.7)}
.card.entity .card-body{padding:12px 20px;color:#333;}
.card.entity .card-body .float-left{width:30px;text-align:center;}
.card.entity .icon{font-size:32px;color:#4285f4;}
.card.entity .badge{position:absolute;top:11px;right:11px}
.card.entity span{margin-top:2px;display:block;}
#entityList{margin:0 -10px}
#entityList>div{padding-left:10px;padding-right:10px}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
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
<div class="col-xl-2 col-lg-3 col-md-4 col-sm-6">
	<a class="card entity">
		<div class="card-body">
			<div class="float-left"><i class="icon zmdi"></i></div>
			<div class="ml-7"><span class="text-truncate"></span><p class="text-muted text-truncate m-0"></p></div>
			<div class="clearfix"></div>
		</div>
	</a>
</div>
</script>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = { isSuperAdmin: ${isSuperAdmin} }
</script>
<script type="text/babel">
$(document).ready(function(){
	$.get(rb.baseUrl + '/admin/entity/entity-list', function(res){
		$(res.data).each(function(){ if (this.builtin == true) render_entity(this) })
		$(res.data).each(function(){ if (this.builtin == false) render_entity(this) })
		
		let forNew = render_entity({ icon: 'plus', entityLabel: '新建实体', comments: '新建一个实体' })
		forNew.find('a.card').attr('href', 'javascript:;').click(function(){
			if (window.__PageConfig.isSuperAdmin) rb.modal(rb.baseUrl + '/admin/p/entityhub/entity-new', '新建实体')
			else rb.hberror('仅超级管理员可新建实体')
		})
	})
})
let render_entity = function(item){
	let tmp = $($('#entity-tmpl').html()).appendTo('#entityList')
	tmp.find('a.card').attr('href', 'entity/' + item.entityName + '/base')
	tmp.find('.icon').addClass('zmdi-' + item.icon)
	tmp.find('span').text(item.entityLabel)
	tmp.find('p').text(item.comments || '-')
	if (item.builtin == true) $('<i class="badge badge-pill badge-secondary thin text-muted">内建</i>').appendTo(tmp.find('a.card'))
	if (!!item.slaveEntity) $('<i class="badge badge-pill badge-secondary thin text-muted">明细</i>').appendTo(tmp.find('a.card'))
	return tmp
}
</script>
</body>
</html>
