<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>通知</title>
<style type="text/css">
.card-title{border-bottom:1px solid #ebebeb;}
.item{border-bottom:1px dotted #eee;padding-bottom:12px}
.item .item-body{margin-left:45px;}
.item .item-body .text{}
.item .item-body .time{color:#777;font-size:12px}
.item+.item{margin-top:12px}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-offcanvas-menu">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="通知" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="notifications" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container">
			<div class="card">
				<div class="card-body">
					<div class="card-title pb-2">
						<div class="float-left">
							<div class="btn-group btn-space">
								<button class="btn btn-secondary active" type="button">未读</button>
								<button class="btn btn-secondary" type="button">全部</button>
							</div>
						</div>
						<div class="float-right">
							<button class="btn btn-secondary btn-space" type="button"><i class="zmdi zmdi-check-all icon"></i> 已读全部</button>
						</div>
						<div class="clearfix"></div>
					</div>
					<div class="card-body J_list">
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<script type="text/palin" id="item-tmpl">
<div class="item">
	<div class="float-left"><a class="avatar "><img src="" alt="Avatar"></a></div>
	<div class="item-body"><div class="time"></div><div class="text"></div></div>
	<div class="clearfix"></div>
</div>
</script>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
$(document).ready(function(){
	load_list()
})
let load_list = function(page){
	$.get(rb.baseUrl + '/app/notification/list', function(res){
		$(res.data).each(function(){
			let tmp = $($('#item-tmpl').html()).appendTo('.J_list')
			tmp.find('img').attr('src', this[0][1])
			tmp.find('.item-body .text').html(this[1])
			tmp.find('.item-body .time').html(this[4])
		})
	})
}
</script>
</body>
</html>
