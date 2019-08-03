<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>登陆日志</title>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="登陆日志" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="login-logs" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="card card-table">
				<div class="card-body">
					<div class="dataTables_wrapper container-fluid">
						<div class="row rb-datatable-header">
							<div class="col-12 col-md-6">
								<div class="dataTables_filter">
									<div class="input-group input-search" data-qfields="&user,user.loginName,user.email,ipAddr">
										<input class="form-control" type="text" placeholder="查询${entityLabel}" maxlength="40">
										<span class="input-group-btn"><button class="btn btn-secondary" type="button"><i class="icon zmdi zmdi-search"></i></button></span>
									</div>
								</div>
							</div>
							<div class="col-12 col-md-6">
								<div class="dataTables_oper">
									<div class="btn-group btn-space">
										<button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown">更多 <i class="icon zmdi zmdi-more-vert"></i></button>
										<div class="dropdown-menu dropdown-menu-right">
											<a class="dropdown-item J_columns"><i class="icon zmdi zmdi-code-setting"></i> 列显示</a>
										</div>
									</div>
								</div>
							</div>
						</div>
						<div id="react-list" class="rb-loading rb-loading-active data-list">
							<%@ include file="/_include/spinner.jsp"%>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = {
	type: 'RecordList',
	entity: ['LoginLog','登陆日志','assignment-account'],
	listConfig: ${DataListConfig},
	advFilter: false
}
</script>
<script src="${baseUrl}/assets/js/rb-datalist.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-forms.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-forms.exts.jsx" type="text/babel"></script>
<script type="text/babel">
RbList.renderAfter = function() {
    let ipAddrIndex = -1
    $('.rb-datatable-body th.sortable').each(function(idx) {
        if ($(this).data('field') === 'ipAddr') {
            ipAddrIndex = idx
            return false
        }
    })
    if (ipAddrIndex === -1) return

    ipAddrIndex += 1
    $('.rb-datatable-body tbody>tr').each(function() {
        let ipAddr = $(this).find('td:eq(' + ipAddrIndex + ') div')
        let ip = (ipAddr.text() || '').split('(')[0].trim()
        if (ip.contains('0:0:0:0:0:0:0') || /^(127\.0\.0\.1)|(localhost)$/.test(ip)) {
            ipAddr.text(ip + ' (本机)')
        } else if (/^(10\.\d{1,3}\.\d{1,3}\.\d{1,3})|(172\.((1[6-9])|(2\d)|(3[01]))\.\d{1,3}\.\d{1,3})|(192\.168\.\d{1,3}\.\d{1,3})$/.test(ip)) {
            ipAddr.text(ip + ' (局域网)')
        } else {
            //$.getJSON('http://ip.taobao.com/service/getIpInfo.php?ip=' + ip + '&jsoncallback=?', function(res) {
            //    console.log(res)
            //})
            //return false
        }
    })
}
</script>
</body>
</html>
