<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>用户管理</title>
<style type="text/css">
</style>
</head>
<body>
<div class="rb-wrapper rb-collapsible-sidebar">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="用户管理" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="user-list" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="card card-table">
				<div class="card-body rb-loading">
					<div class="dataTables_wrapper container-fluid">
						<div class="row rb-datatable-header">
							<div class="col-sm-6">
								<div class="dataTables_filter">
									<div class="input-group input-search">
										<input class="form-control" placeholder="搜索..." type="text"><span class="input-group-btn">
										<button class="btn btn-secondary"><i class="icon zmdi zmdi-search"></i></button></span>
									</div>
								</div>
							</div>
							<div class="col-sm-6">
								<div class="dataTables_oper">
									<button class="btn btn-space btn-primary" onclick="rbModal.show('user-edit.htm')"><i class="icon zmdi zmdi-plus"></i> 新建</button>
									<button class="btn btn-space btn-secondary" disabled="disabled"><i class="icon zmdi zmdi-delete"></i> 删除</button>
									<div class="btn-group btn-space">
										<button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown">更多 <i class="icon zmdi zmdi-chevron-down"></i></button>
										<div class="dropdown-menu">
											<a class="dropdown-item" href="#">Action</a>
											<a class="dropdown-item" href="#">Another action</a>
											<a class="dropdown-item" href="#">Something else here</a>
											<div class="dropdown-divider"></div>
											<a class="dropdown-item" href="#">Separated link</a>
										</div>
									</div>
								</div>
							</div>
						</div>
						<div class="row rb-datatable-body">
							<div class="col-sm-12">
								<table class="table" id="dataList" data-entity="User">
									<thead>
										<tr>
											<th width="50">
												<label class="custom-control custom-control-sm custom-checkbox">
													<input class="custom-control-input" type="checkbox"><span class="custom-control-label"></span>
												</label>
											</th>
											<th data-filed="loginName">用户</th>
											<th data-field="email">邮箱</th>
											<th data-field="createdOn">创建时间</th>
										</tr>
									</thead>
									<tbody></tbody>
								</table>
							</div>
						</div>
						<div class="row rb-datatable-footer">
							<div class="col-sm-5">
								<div class="dataTables_info"></div>
							</div>
							<div class="col-sm-7">
								<div class="dataTables_paginate paging_simple_numbers">
									<ul class="pagination">
										<li class="paginate_button page-item previous disabled"><a href="#" class="page-link"><span class="icon zmdi zmdi-chevron-left"></span></a></li>
										<li class="paginate_button page-item active"><a href="#" class="page-link">1</a></li>
										<li class="paginate_button page-item next"><a href="#" class="page-link"><span class="icon zmdi zmdi-chevron-right"></span></a></li>
									</ul>
								</div>
							</div>
						</div>
					</div>
					<div class="rb-spinner">
						<svg width="40px" height="40px" viewBox="0 0 66 66" xmlns="http://-www.w3.org/2000/svg">
							<circle fill="none" stroke-width="4" stroke-linecap="round" cx="33" cy="33" r="30" class="circle"></circle>
						</svg>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>

<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/rb-list.js" type="text/javascript"></script>
<script type="text/babel">
const rbModal = ReactDOM.render(<RbModal title="新建用户" />, $('<div id="react-comps"></div>').appendTo(document.body)[0]);
</script>
<script type="text/javascript">
$(document).ready(function(){
	let query = {
		entity: 'User', pageNo: 1, pageSize: 20,
		fields: ['loginName', 'email', 'createdOn']
	};
	$.post(__baseUrl + '/entity/list', JSON.stringify(query), function(res){
		let tbody = $('#dataList tbody');
		$(res.data.data).each(function(){
			let tr = $('<tr></tr>').appendTo(tbody);
			let rowLen = this.length;
			$('<td data-id="' + this[rowLen - 1] + '"><label class="custom-control custom-control-sm custom-checkbox"><input class="custom-control-input" type="checkbox"><span class="custom-control-label"></span></label></td>').appendTo(tr);
			$(this).each(function(idx, item){
				$('<td>' + (item || '-') + '</td>').appendTo(tr);
				if (idx == rowLen - 2) return false;
			});
		});
	});
});
</script>
</body>
</html>
