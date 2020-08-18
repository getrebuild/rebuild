<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>查询${entityLabel}</title>
<style type="text/css">
    .rb-datatable-header {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        background-color: #f7f7f7;
        z-index: 10;
    }
    #react-list {
        margin-top: 68px;
    }
</style>
</head>
<body class="dialog">
<div class="main-content container-fluid p-0">
    <div class="card card-table">
        <div class="card-body">
            <div class="dataTables_wrapper container-fluid">
                <div class="row rb-datatable-header">
                    <div class="col-6">
                        <div class="dataTables_filter">
                            <div class="input-group input-search">
                                <input class="form-control" type="text" placeholder="查询${entityLabel}" maxlength="40">
                                <span class="input-group-btn"><button class="btn btn-secondary" type="button"><i class="icon zmdi zmdi-search"></i></button></span>
                            </div>
                        </div>
                    </div>
                    <div class="col-6">
                        <div class="dataTables_oper">
                            <button class="btn btn-space btn-primary J_select"><i class="icon zmdi zmdi-check"></i> 确定</button>
                        </div>
                    </div>
                </div>
                <div id="react-list" class="rb-loading rb-loading-active data-list">
                    <%@ include file="/_include/Spinner.jsp"%>
                </div>
            </div>
        </div>
    </div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = {
    type: 'RecordList',
    entity: ['${entityName}','${entityLabel}','${entityIcon}'],
    listConfig: ${DataListConfig},
    advFilter: false,
    protocolFilter: '${referenceFilter}'
}
</script>
<script src="${baseUrl}/assets/js/rb-datalist.jsx" type="text/babel"></script>
<script type="text/babel">
RbList.renderAfter = function() {
    parent &&  parent.referenceSearch__dialog && parent.referenceSearch__dialog.resize()
}
$(document).ready(function () {
    $('.J_select').click(function () {
        const ss = RbListPage._RbList.getSelectedIds()
        if (ss.length > 0 && parent && parent.referenceSearch__call) parent.referenceSearch__call(ss)
    })
})
</script>
</body>
</html>
