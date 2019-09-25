<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>回收站</title>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
<jsp:include page="/_include/NavTop.jsp">
    <jsp:param value="回收站" name="pageTitle"/>
</jsp:include>
<jsp:include page="/_include/NavLeftAdmin.jsp">
    <jsp:param value="recycle-bin" name="activeNav"/>
</jsp:include>
<div class="rb-content">
    <div class="main-content container-fluid">
        <div class="card card-table">
            <div class="card-body">
                <div class="dataTables_wrapper container-fluid">
                    <div class="row rb-datatable-header">
                        <div class="col-12 col-md-6">
                            <div class="dataTables_filter">
                                <div class="float-left mr-2 select2-sm">
                                    <select class="form-control form-control-sm" id="belongEntity" style="width:220px">
                                        <option value="$ALL$">全部实体</option>
                                    </select>
                                </div>
                                <div class="input-group input-search">
                                    <input class="form-control" type="text" placeholder="查询记录名称/ID" maxlength="40">
                                    <span class="input-group-btn"><button class="btn btn-secondary" type="button"><i class="icon zmdi zmdi-search"></i></button></span>
                                </div>
                            </div>
                        </div>
                        <div class="col-12 col-md-6">
                            <div class="dataTables_oper">
                                <button class="btn btn-space btn-danger J_restore"><i class="icon zmdi zmdi-undo"></i> 恢复</button>
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
<script src="${baseUrl}/assets/js/rb-datalist.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/entityhub/recycle-bin.jsx" type="text/babel"></script>
</body>
</html>
