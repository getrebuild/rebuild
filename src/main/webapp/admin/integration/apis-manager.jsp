<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>API 秘钥管理</title>
<style type="text/css">
.syscfg h5{background-color:#eee;margin:0;padding:10px;}
.syscfg .table td{padding:10px;}
.syscfg .table td p{margin:0;color:#999;font-weight:normal;font-size:12px;}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="API 秘钥管理" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="apis-manager" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
	    <div class="page-head">
            <div class="float-left"><div class="page-head-title">API 秘钥管理</div></div>
            <div class="float-right pt-1">
                <button class=" btn btn-primary J_add" type="button"><i class="icon zmdi zmdi-plus"></i> 添加</button>
            </div>
            <div class="clearfix"></div>
        </div>
		<div class="main-content container-fluid pt-0">
		    <div class="card card-table">
                <div class="card-body">
                    <div class="dataTables_wrapper container-fluid">
                        <div class="row rb-datatable-body">
                            <div class="col-sm-12">
                                <div class="rb-loading rb-loading-active data-list">
                                    <table class="table table-hover table-striped table-fixed">
                                        <thead>
                                            <tr>
                                                <th width="10%">APP ID</th>
                                                <th width="30%">APP SECRET</th>
                                                <th>绑定用户</th>
                                                <th width="15%">创建时间</th>
                                                <th>使用统计 (30日)</th>
                                                <th width="50"></th>
                                            </tr>
                                        </thead>
                                        <tbody id="appList"></tbody>
                                    </table>
                                    <%@ include file="/_include/spinner.jsp"%>
                                    <div class="list-nodata hide"><span class="zmdi zmdi-key"></span><p>没有 API 秘钥</p></div>
                                </div>
                            </div>
                        </div>
                        <div id="pagination">
                            <div class="row rb-datatable-footer">
                                <div class="col-sm-3"><div class="dataTables_info"></div></div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/babel" src="${baseUrl}/assets/js/admin/apis-manager.jsx"></script>
</body>
</html>