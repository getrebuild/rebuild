<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>待办</title>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-offcanvas-menu">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="待办" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="notifications" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container">
			<div class="tab-container">
				<ul class="nav nav-tabs nav-tabs-classic notification-tab">
					<li class="nav-item">
						<a class="nav-link" href="../notifications"><span class="icon zmdi zmdi-notifications"></span> 通知</a>
					</li>
					<li class="nav-item">
						<a class="nav-link active" href="todo"><span class="icon zmdi zmdi-alarm-check"></span> 待办</a>
					</li>
				</ul>
				<div class="tab-content">
					<div class="tab-pane active">
						<div class="row">
							<div class="col-3">
								<div class="list-group notification-type">
									<a href="#approval" data-type="20" class="list-group-item d-flex list-group-item-action">
										<span class="text">待我审批</span>
										<span class="badge badge-pill badge-primary">0</span>
									</a>
									<a href="#all" data-type="21" class="list-group-item d-flex list-group-item-action">
										<span class="text">全部代办</span>
									</a>
								</div>
							</div>
							<div class="col-9">
								<div class="rb-notifications notification-list">
									<ul class="list-unstyled">
									</ul>
								</div>
								<div class="notification-page hide">
									<ul class="pagination pagination-rounded mb-0">
										<li class="page-item"><a class="page-link"><i class="icon zmdi zmdi-chevron-left"></i></a></li>
										<li class="page-item"><a class="page-link"><i class="icon zmdi zmdi-chevron-right"></i></a></li>
									</ul>
								</div>
							</div>
						</div>
					</div>
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
	let btns = $('.notification-type>a').click(function(){
		btns.removeClass('active')
		$(this).addClass('active')
		load_list($(this).data('type'), 1)
	})
	let hit = $('a[href="' + (location.hash || '#') + '"]')
	if (hit.length === 0) hit = $('.notification-type>a:eq(0)')
	hit.trigger('click')

	$('.pages a:eq(0)').click(function() {
		if (current_page <= 1) return
		current_page--
		load_list()
	})
	$('.pages a:eq(1)').click(function() {
		if (!has_moresdata) return
		current_page++
		load_list()
	})
})
let current_type = 0
let current_page = 1
let has_moresdata = true
let load_list = function(type, page) {
	type = type || current_type
	page = page || current_page
	$.get(rb.baseUrl + '/notification/todos?type=' + type + '&page=' + page, function(res) {
		if (page == 1) $('.list-nodata').remove()
		$('.notification-list>ul').empty()

		let _data = res.data || []
		$(_data).each((idx, item) => {
			let o = $('<li class="notification"></li>').appendTo('.notification-list>ul')
			o.attr('data-id', item[4])
			if (item[3] == true){
				let unread = o
				o.addClass('notification-unread').attr('title', '点击设为已读').click(() => {
					$.post(rb.baseUrl + '/notification/make-read?id=' + item[4], () => {
						unread.removeClass('notification-unread').removeAttr('title')
					})
				})
			}
			o = $('<a></a>').appendTo(o)
			$('<div class="image"><img src="' + rb.baseUrl + '/account/user-avatar/' + item[0][0] + '" alt="Avatar"></div>').appendTo(o)
			o = $('<div class="notification-info"></div>').appendTo(o)
			$('<div class="text">' + item[1] + '</div>').appendTo(o)
			$('<span class="date">' + item[2] + '</span>').appendTo(o)
		})

		if (page == 1 && _data.length == 0) {
			$('<div class="list-nodata"><span class="zmdi zmdi-alarm-check"></span><p>暂无通知</p></div>').appendTo('.notification-list')
			$('.pages').addClass('hide')
		} else {
			has_moresdata = _data.length >= 40
			if (has_moresdata) $('.pages').removeClass('hide')
		}
	})
}
</script>
</body>
</html>
