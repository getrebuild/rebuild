<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>用户首页</title>
<style type="text/css">
.card-title{border-bottom:1px solid #ebebeb;}
.item{border-bottom:1px dotted #eee;padding-bottom:6px;}
.item .item-body{margin-left:40px;padding:6px;border-radius:3px;position:relative;}
.item .item-body .time{color:#777;font-size:12px;line-height:1}
.item .item-body .text{margin-bottom:3px;padding-right:10px}
.item+.item{margin-top:9px}
.item:hover .item-body{background-color:#f5f5f5}
.item.unread .item-body .text{font-weight:bold;}
.unread-flag{top:7px;cursor:help;width:8px;height:8px;}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-offcanvas-menu">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="用户首页" name="pageTitle"/>
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
								<button class="btn btn-secondary active" type="button">未读</button>
								<button class="btn btn-secondary J_is-all" type="button">全部</button>
							</div>
						</div>
						<div class="float-right">
							<button class="btn btn-secondary btn-space J_read-all" type="button"><i class="zmdi zmdi-check-all icon"></i> 已读全部</button>
						</div>
						<div class="clearfix"></div>
					</div>
					<div class="card-body J_list" style="min-height:180px">
					</div>
					<div class="text-center J_mores hide"><a href="javascript:;">显示更多 ...</a></div>
				</div>
			</div>
		</div>
	</div>
</div>
<script type="text/palin" id="item-tmpl">
<div class="item">
	<div class="float-left"><a class="avatar "><img src="" alt="Avatar"></a></div>
	<div class="item-body"><div class="text"></div><div class="time"></div></div>
	<div class="clearfix"></div>
</div>
</script>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
$(document).ready(function(){
	let btns = $('.J_show-type .btn').click(function(){
		btns.removeClass('active')
		$(this).addClass('active')
		current_page = 1
		load_list()
	})
	
	$('.J_read-all').click(function(){
		let ids = []
		let unread = $('.J_list .unread').each(function(){
			ids.push($(this).data('id'))
		})
		if (ids.length == 0){ rb.notice('所有消息已设为读', 'success'); return }
		
		unread.off('click')
		$.post(rb.baseUrl + '/app/notification/toggle-unread?state=read&id=' + ids.join(','), function(res){
			unread.each(function(){
				$(this).removeClass('unread')
				$(this).find('.unread-flag').remove()
			})
			rb.notice('所有消息已设为读', 'success')
		})
	})
	
	$('.J_mores a').click(function(){
		current_page++
		load_list()
	})
	
	load_list()
})
let current_page = 1
let load_list = function(){
	let isAll = $('.J_is-all').hasClass('active')
	$.get(rb.baseUrl + '/app/notification/list?isAll=' + isAll + '&page=' + current_page, function(res){
		if (current_page == 1) $('.J_list').empty()
		$(res.data).each(function(){
			let tmp = $($('#item-tmpl').html()).appendTo('.J_list')
			tmp.attr('data-id', this[3])
			tmp.find('img').attr('src', this[0][1])
			tmp.find('.item-body .text').html(this[1])
			tmp.find('.item-body .time').html(this[4])
			if (this[2] == true) {
				$('<span class="unread-flag" title="未读消息"></span>').appendTo(tmp.find('.item-body'))
				tmp.addClass('unread').click(function(){
					tmp.off('click')
					$.post(rb.baseUrl + '/app/notification/toggle-unread?state=read&id=' + tmp.data('id'), function(){
						tmp.removeClass('unread')
						tmp.find('.unread-flag').remove()
					})
				})
			}
		})
		
		if (current_page == 1 && (!res.data || res.data.length == 0)) {
			$('<div class="list-nodata"><span class="zmdi zmdi-info-outline"></span><p>暂无' + (isAll ? '' : '未读') + '消息</p></div>').appendTo('.J_list')
		} else {
			if (res.data.length >= 20) $('.J_mores').removeClass('hide')
			else $('.J_mores').addClass('hide')
		}
	})
}
</script>
</body>
</html>
