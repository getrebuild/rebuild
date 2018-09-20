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
		<jsp:param value="entities" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid" id="entityList">
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/babel">
const EntityList = function(props){
	const content = props.data.map((d) =>
		<div class="col-12 col-lg-2 col-sm-4">
			<a class="card entity" href={"entity/" + d.entityName + "/base"}>
				<div class="card-body">
					<div class="float-left"><i class={"icon zmdi zmdi-" + d.icon}></i></div>
					<div class="float-left">
						<span>{d.entityLabel}</span>
						<p class="font-desc">{d.comments || '-'}</p>
					</div>
					<div class="clearfix"></div>
				</div>
			</a>
		</div>
	);
	return <div className="row">{content}</div>;
};
var newEntityModal = null
$(document).ready(function(){
	$.get(rb.baseUrl + '/admin/entity/entity-list', function(res){
		let _data = res.data;
		_data.push({ entityName:'$NEW$', entityLabel:'新建', comments:'新建一个新实体', icon:'plus' });
		ReactDOM.render(<EntityList data={_data} />, $('#entityList')[0]);

		$('.entity[href="entity/$NEW$/base"]').click(function(){
			if (newEntityModal) newEntityModal.show()
			else newEntityModal = rb.modal(rb.baseUrl + '/admin/page/entity/entity-new', '新建实体')
			return false;
		});
	});
});
</script>
</body>
</html>
