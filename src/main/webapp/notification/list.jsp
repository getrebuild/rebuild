<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>消息通知</title>
<style type="text/css">
.card-title{border-bottom:1px solid #ebebeb;}
.card-body.rb-notifications{padding-left:3px;padding-right:3px;padding-bottom:7px}
.card-body.rb-notifications .notification.notification-unread{background-color:#fff;}
.card-body.rb-notifications .notification.notification-unread>a{cursor:pointer;}
.card-body.rb-notifications .notification.notification-unread>a:after{background-color:#4285f4 !important}
.card-body.rb-notifications .notification:hover{background-color:#ebf2fe !important;}
.card-body.rb-notifications .notification>a{cursor:default;padding:13px 6px;}
.card-body.rb-notifications .notification>a .text{color:#404040 !important;font-size:1rem;padding-right:20px;line-height:1.4}
.card-body.rb-notifications .notification>a .date{color:#8a8a8a !important;margin-top:4px;font-size:12px;}
.notification-unread .notification-info .text{font-weight:bold;}
.card-body.rb-notifications .notification .text a.record{text-decoration:underline;padding:0 1px;} 
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-offcanvas-menu">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="消息通知" name="pageTitle"/>
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
							<div class="btn-group btn-space J_show-type">
								<button class="btn btn-secondary J_view-unread" type="button">未读</button>
								<button class="btn btn-secondary J_view-all" type="button">全部</button>
							</div>
						</div>
						<div class="float-right">
							<button class="btn btn-secondary btn-space J_read-all" type="button"><i class="zmdi zmdi-check-all icon"></i> 已读全部</button>
						</div>
						<div class="clearfix"></div>
					</div>
					<div class="card-body rb-notifications J_list" style="min-height:300px">
						<ul class="list-unstyled"></ul>
					</div>
					<div class="text-center J_mores hide"><a href="javascript:;" style="padding-bottom:10px">显示更多 ...</a></div>
				</div>
			</div>
		</div>
	</div>
</div>
<script type="text/palin" id="item-tmpl">
<div class="item">
	<div class="float-left"><a class="avatar"><img src="" alt="Avatar"></a></div>
	<div class="item-body"><div class="text"></div><div class="time"></div></div>
	<div class="clearfix"></div>
</div>
</script>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/babel">
$(document).ready(function(){
	let btns = $('.J_show-type .btn').click(function(){
		btns.removeClass('active')
		$(this).addClass('active')
		current_page = 1
		load_list()
	})
	if (!!$urlp('id', location.hash)) $('.J_view-unread').trigger('click')
	else $('.J_view-all').trigger('click')
	
	$('.J_read-all').click(function(){
		let ids = []
		let unread = $('.J_list .notification-unread').each(function(){
			ids.push($(this).data('id'))
		})
		if (ids.length == 0){ rb.hbsuccess('所有消息已设为读'); return }
		
		unread.off('click')
		$.post(rb.baseUrl + '/notification/toggle-unread?state=read&id=' + ids.join(','), function(res){
			unread.each(function(){
				$(this).removeClass('notification-unread').removeAttr('title')
			})
			rb.hbsuccess('所有消息已设为读')
		})
	})
	
	$('.J_mores a').click(function(){
		current_page++
		load_list()
	})
})
let current_page = 1
let load_list = function(){
	let isAll = $('.J_view-all').hasClass('active')
	$.get(rb.baseUrl + '/notification/list?isAll=' + isAll + '&pageNo=' + current_page, function(res){
		if (current_page == 1){
			$('.list-nodata').remove()
			$('.J_list ul').empty()
		}
		let _data = res.data || []
		$(_data).each((idx, item)=>{
			let o = $('<li class="notification"></li>').appendTo('.J_list ul')
			o.attr('data-id', item[4])
			if (item[3] == true){
				let unread = o
				o.addClass('notification-unread').attr('title', '点击设为已读').click(()=>{
					$.post(rb.baseUrl + '/notification/toggle-unread?state=read&id=' + item[4], ()=>{
						unread.removeClass('notification-unread')
					})
				})
			}
			o = $('<a></a>').appendTo(o)
			$('<div class="image"><img src="' + item[0][1] + '" alt="Avatar"></div>').appendTo(o)
			o = $('<div class="notification-info"></div>').appendTo(o)
			$('<div class="text">' + item[1] + '</div>').appendTo(o)
			$('<span class="date">' + item[2] + '</span>').appendTo(o)
		})

		if (current_page == 1 && _data.length == 0) {
			$('<div class="list-nodata"><span class="zmdi zmdi-info-outline"></span><p>暂无' + (isAll ? '' : '未读') + '消息</p></div>').appendTo('.J_list')
		} else {
			if (_data.length >= 40) $('.J_mores').removeClass('hide')
			else $('.J_mores').addClass('hide')
		}
	})
}
</script>
</body>
</html>
