<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map"%>
<%@ page import="java.util.List"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>角色权限</title>
<style type="text/css">
.priv{width:36px;height:18px;display:inline-block;background:url(${baseUrl}/assets/img/role/role_0.gif) no-repeat center center;}
.priv:hover{cursor:pointer;opacity:0.8}
.R1{background-image:url(${baseUrl}/assets/img/role/role_1.gif)}
.R2{background-image:url(${baseUrl}/assets/img/role/role_2.gif)}
.R3{background-image:url(${baseUrl}/assets/img/role/role_3.gif)}
.R4{background-image:url(${baseUrl}/assets/img/role/role_4.gif)}
.table-priv.table{table-layout:fixed;border:0 none;border-bottom:1px solid #dee2e6;}
.table-priv.table td, .table-priv.table th{padding:9px 6px;vertical-align:middle;line-height:1}
.table-priv.table th{border-top:0 none;font-weight:normal;color:#777}
.table-priv.table th a:hover, .table-priv.table .name a:hover{opacity:0.8}
.legend{border:1px solid #dee2e6;border-radius:3px;display:inline-block;padding:9px 15px;}
.legend label{margin:0 6px;}
.legend .priv{width:22px;float:left;}
.legend-warp{padding:10px 0;padding-bottom:5px;text-align:right;}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-aside">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="角色权限" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="role-privileges" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<aside class="page-aside">
			<div class="rb-scroller">
				<div class="dept-tree">
					<ul class="list-unstyled"></ul>
				</div>
			</div>
		</aside>
		<div class="main-content container-fluid">
			<div class="float-right" style="margin-top:-1px">
				<button class="btn btn-secondary btn-space J_new-role" type="button"><i class="icon zmdi zmdi-plus"></i> 新建角色</button>
				<button class="btn btn-primary btn-space J_save mr-0" type="button" disabled="disabled">保存</button>
			</div>
			<div class="tab-container">
				<ul class="nav nav-tabs nav-tabs-classic">
					<li class="nav-item"><a data-toggle="tab" href="#priv-entity" class="nav-link active">实体权限</a></li>
					<li class="nav-item"><a data-toggle="tab" href="#priv-zero" class="nav-link">扩展权限</a></li>
				</ul>
				<div class="tab-content mb-0" style="border-top:0px solid #4285f4;">
					<div class="tab-pane active" id="priv-entity">
						<table class="table table-priv">
						<thead>
							<tr>
								<th width="25%">业务实体</th>
								<th class="text-center unselect"><a data-action="C">新建</a></th>
								<th class="text-center unselect"><a data-action="R">读取</a></th>
								<th class="text-center unselect"><a data-action="U">修改</a></th>
								<th class="text-center unselect"><a data-action="D">删除</a></th>
								<th class="text-center unselect"><a data-action="A">分派</a></th>
								<th class="text-center unselect"><a data-action="S">共享</a></th>
							</tr>
						</thead>
						<tbody>
						<%
						List<String[]> entities = (List<String[]>) request.getAttribute("Entities");
						for (String[] e : entities) {
							boolean noAS = "/User/Department/Role/".contains("/" + e[0] + "/");
						%>
							<tr>
								<td class="name"><a data-name="<%=e[0]%>"><%=e[1]%></a></td>
								<td class="text-center"><i data-action="C" class="priv R0"></i></td>
								<td class="text-center"><i data-action="R" class="priv R0"></i></td>
								<td class="text-center"><i data-action="U" class="priv R0"></i></td>
								<td class="text-center"><i data-action="D" class="priv R0"></i></td>
								<% if (noAS) { %>
								<td class="text-center text-muted">-</td>
								<td class="text-center text-muted">-</td>
								<%} else { %>
								<td class="text-center"><i data-action="A" class="priv R0"></i></td>
								<td class="text-center"><i data-action="S" class="priv R0"></i></td>
								<%} %>
							</tr>
						<%} %>
						</tbody>
						</table>
						<div class="legend-warp">
							<div class="legend">
								图例
								<label><i class="priv R0"></i> 无权限</label>
								<label><i class="priv R1"></i> 本人</label>
								<label><i class="priv R2"></i> 本部门</label>
								<label><i class="priv R3"></i> 本部门及子级部门</label>
								<label><i class="priv R4"></i> 全部</label>
							</div>
						</div>
					</div>
					<div class="tab-pane" id="priv-zero">
						<table class="table table-priv">
						<thead>
							<tr>
								<th width="25%">权限项目</th>
								<th class="text-center"><span data-action="Z">允许</span></th>
								<th>前置条件</th>
								<th></th>
								<th></th>
								<th></th>
								<th></th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<td class="name"><a data-name="0-AllowLogin">允许登录</a></td>
								<td class="text-center"><i data-action="Z" class="priv R4"></i></td>
								<td colspan="5" class="text-muted">无</td>
							</tr>
							<tr>
								<td class="name"><a data-name="0-AllowBatchUpdate">允许批量修改</a></td>
								<td class="text-center"><i data-action="Z" class="priv R0"></i></td>
								<td colspan="5">需具备相应实体的修改权限</td>
							</tr>
							<tr>
								<td class="name"><a data-name="0-AllowDataImport">允许导入数据</a></td>
								<td class="text-center"><i data-action="Z" class="priv R0"></i></td>
								<td colspan="5">需具备相应实体的创建权限</td>
							</tr>
							<tr>
								<td class="name"><a data-name="0-AllowDataOutput">允许导出数据</a></td>
								<td class="text-center"><i data-action="Z" class="priv R0"></i></td>
								<td colspan="5">需具备相应实体的读取权限</td>
							</tr>
							<tr>
								<td class="name"><a data-name="0-AllowCustomListColumn">允许自定义列显示</a></td>
								<td class="text-center"><i data-action="Z" class="priv R0"></i></td>
								<td colspan="5" class="text-muted">无</td>
							</tr>
							<tr>
								<td class="name"><a data-name="0-AllowCustomQuickField">允许自定义查询字段</a></td>
								<td class="text-center"><i data-action="Z" class="priv R0"></i></td>
								<td colspan="5" class="text-muted">无</td>
							</tr>
						</tbody>
						</table>
						<div class="legend-warp">
							<div class="legend">
								图例
								<label><i class="priv R4"></i> 是</label>
								<label><i class="priv R0"></i> 否</label>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/rb-forms.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-forms-ext.jsx" type="text/babel"></script>
<script type="text/babel">
RbForm.postAfter = function(data){
	location.href = rb.baseUrl + '/admin/bizuser/role/' + data.id
}
var currentRoleId
$(document).ready(function(){
	$('.J_new-role').click(function(){
		rb.RbFormModal({ title: '新建角色', entity: 'Role', icon: 'lock' })
	})
	
	currentRoleId = '${RoleId}'
	if (!!currentRoleId) {
		$('.J_save').attr('disabled', false).click(updatePrivileges)
		loadPrivileges()
	}
	loadRoles()
	
	// ENTITY
		
	// 单个操作
	$('#priv-entity tbody .priv').click(function(){
		let _this = $(this)
		clickPriv(_this, _this.data('action'))
	})
	// 批量操作
	$('#priv-entity thead th>a').click(function(){
		let _this = $(this)
		let action = _this.data('action')
		let privAll = $('#priv-entity tbody .priv[data-action="' + action + '"]')
		clickPriv(privAll, action)
	})
	// 批量操作
	$('#priv-entity tbody .name>a').click(function(){
		let privAll = $(this).parent().parent().find('.priv')
		let clz = 'R0'
		if (privAll.eq(0).hasClass('R0')) clz = 'R4'
		privAll.removeClass('R0 R1 R2 R3 R4').addClass(clz)
	})
	
	// ZERO
	
	$('#priv-zero tbody .priv').click(function(){
		clickPriv($(this), 'Z')
	})
	$('#priv-zero tbody .name>a').click(function(){
		let el = $(this).parent().next().find('i.priv')
		clickPriv(el, 'Z')
	})
	
})
const clickPriv = function(elements, action) {
	if (action == 'C' || action == 'Z') {
		elements.toggleClass('R0')
		elements.toggleClass('R4')
	} else {
		let clz = 'R0'
		if (elements.hasClass('R0')) clz = 'R1'
		else if (elements.hasClass('R1')) clz = 'R2'
		else if (elements.hasClass('R2')) clz = 'R3'
		else if (elements.hasClass('R3')) clz = 'R4'
		elements.removeClass('R0 R1 R2 R3 R4').addClass(clz)
	}
}
const loadRoles = function() {
	$.get(rb.baseUrl + '/admin/bizuser/role-list', function(res){
		$('.dept-tree ul').empty()
		$(res.data).each(function(){
			let item = $('<li><a class="text-truncate" href="' + rb.baseUrl + '/admin/bizuser/role/' + this.id + '">' + this.name + '</a></li>').appendTo('.dept-tree ul')
			if (currentRoleId == this.id) item.addClass('active')
		})
	})
}
const loadPrivileges = function() {
	$.get(rb.baseUrl + '/admin/bizuser/privileges-list?role=' + currentRoleId, function(res){
		if (res.error_code == 0){
			$(res.data).each(function(){
				let etr = $('.table-priv tbody td.name>a[data-name="' + this.name + '"]')
				etr = etr.parent().parent()
				let defi = JSON.parse(this.definition)
				for (let k in defi) {
					etr.find('.priv[data-action="' + k + '"]').removeClass('R0 R1 R2 R3 R4').addClass('R' + defi[k])
				}
			})
		}else{
			$('.J_save').attr('disabled', true)
			rb.notice(res.error_msg)
		}
	})
}
const updatePrivileges = function() {
	let privEntity = {}
	$('#priv-entity tbody>tr').each(function(){
		let etr = $(this)
		let name = etr.find('td.name a').data('name')
		let definition = {}
		etr.find('i.priv').each(function(){
			let _this = $(this)
			let action = _this.data('action')
			let deep = 0
			if (_this.hasClass('R1')) deep = 1
			else if (_this.hasClass('R2')) deep = 2
			else if (_this.hasClass('R3')) deep = 3
			else if (_this.hasClass('R4')) deep = 4
			definition[action] = deep
		})
		privEntity[name] = definition
	})
	let privZero = {}
	$('#priv-zero tbody>tr').each(function(){
		let etr = $(this)
		let name = etr.find('td.name a').data('name')
		let definition = etr.find('i.priv').hasClass('R0') ? {Z:0} : {Z:4}
		privZero[name] = definition
	})
	
	let priv = { entity: privEntity, zero: privZero }
	console.log(JSON.stringify(priv))
	$.post(rb.baseUrl + '/admin/bizuser/privileges-update?role=' + currentRoleId, JSON.stringify(priv), function(){
		rb.notice('保存成功', 'success')
	})
}
</script>
</body>
</html>
