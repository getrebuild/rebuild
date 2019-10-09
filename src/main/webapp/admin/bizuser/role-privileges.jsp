<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
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
.legend-wrap{padding:10px 0;padding-bottom:5px;text-align:right;}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-aside rb-color-header">
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
					<div class="ph-item rb">
						<div class="ph-col-12 p-0">
							<div class="ph-row">
								<div class="ph-col-12 big"></div>
								<div class="ph-col-12 big"></div>
							</div>
						</div>
					</div>
					<ul class="list-unstyled"></ul>
				</div>
			</div>
		</aside>
		<div class="main-content container-fluid">
			<div class="alert alert-warning alert-icon alert-dismissible min hide J_tips">
				<div class="icon"><span class="zmdi zmdi-info-outline"></span></div>
				<div class="message"><a class="close" data-dismiss="alert"><span class="zmdi zmdi-close"></span></a><p>1</p></div>
			</div>
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
						<c:forEach items="${Entities}" var="e">
							<tr>
								<td class="name"><a data-name="${e[0]}">${e[1]}</a></td>
								<td class="text-center"><i data-action="C" class="priv R0"></i></td>
								<td class="text-center"><i data-action="R" class="priv R0"></i></td>
								<td class="text-center"><i data-action="U" class="priv R0"></i></td>
								<td class="text-center"><i data-action="D" class="priv R0"></i></td>
								<td class="text-center"><i data-action="A" class="priv R0"></i></td>
								<td class="text-center"><i data-action="S" class="priv R0"></i></td>
							</tr>
						</c:forEach>
						</tbody>
						</table>
						<div class="legend-wrap">
							<div class="legend">
								图例
								<label><i class="priv R0"></i> 无权限</label>
								<label><i class="priv R1"></i> 本人</label>
								<label><i class="priv R2"></i> 本部门</label>
								<label><i class="priv R3"></i> 本部门及子部门</label>
								<label><i class="priv R4"></i> 全部</label>
							</div>
						</div>
					</div>
					<div class="tab-pane" id="priv-zero">
						<table class="table table-priv">
						<thead>
							<tr>
								<th width="25%">权限项</th>
								<th class="text-center unselect"><a data-action="Z">允许</a></th>
								<th>前置条件</th>
								<th></th>
								<th></th>
								<th></th>
								<th></th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<td class="name"><a data-name="AllowLogin">允许登录</a></td>
								<td class="text-center"><i data-action="Z" class="priv R4"></i></td>
								<td colspan="5" class="text-muted">无</td>
							</tr>
                            <tr>
								<td class="name"><a data-name="AllowCustomNav">允许自定义导航菜单</a></td>
								<td class="text-center"><i data-action="Z" class="priv R4"></i></td>
								<td colspan="5" class="text-muted">无</td>
							</tr>
                            <tr>
								<td class="name"><a data-name="AllowCustomDataList">允许自定义列表列显示</a></td>
								<td class="text-center"><i data-action="Z" class="priv R4"></i></td>
								<td colspan="5" class="text-muted">需具备相应实体的读取权限</td>
							</tr>
							<!--
							<tr>
								<td class="name"><a data-name="AllowBatchUpdate">允许批量修改</a></td>
								<td class="text-center"><i data-action="Z" class="priv R0"></i></td>
								<td colspan="5">需具备相应实体的修改权限</td>
							</tr>
							<tr>
								<td class="name"><a data-name="AllowDataImport">允许导入数据</a></td>
								<td class="text-center"><i data-action="Z" class="priv R0"></i></td>
								<td colspan="5">需具备相应实体的创建权限</td>
							</tr>
							<tr>
								<td class="name"><a data-name="AllowDataOutput">允许导出数据</a></td>
								<td class="text-center"><i data-action="Z" class="priv R0"></i></td>
								<td colspan="5">需具备相应实体的读取权限</td>
							</tr>
							-->
						</tbody>
						</table>
						<div class="legend-wrap">
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
<script>
window.__PageConfig = { recordId: '${id}' }
</script>
<script src="${baseUrl}/assets/js/rb-forms.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-forms.exts.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/bizuser/roles.jsx" type="text/babel"></script>
</body>
</html>
