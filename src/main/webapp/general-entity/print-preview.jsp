<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>打印预览</title>
<style type="text/css">
html, body{
    background-color:#fff;
}
.preview-tools {
    padding: 10px 20px;
    padding-bottom: 5px;
    text-align: right;
}
.table th {
    background-color: #eee !important;
    width: 17.5%;
}
.table th, .table td {
    vertical-align: text-top;
    vertical-align: top;
    word-break: break-all;
    word-wrap: break-word;
}
.table td img {
    max-height: 80px;
    max-width: 80px;
}
</style>
</head>
<body>
<div class="preview-tools d-print-none">
    <button class="btn btn-space btn-secondary" onclick="window.print()"><i class="icon zmdi zmdi-print"></i> 打印</button>
    <button class="btn btn-space btn-secondary" onclick="window.close()">关闭</button>
</div>
<div class="preview-content" id="preview-table">
</div>
<div class="font-italic">
    时间 ${printTime} - 编号 ${recordId}
</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = {
    record: '${recordId}',
    content: ${contentBody}
}
</script>
<script src="${baseUrl}/assets/js/entity/print-preview.jsx" type="text/babel"></script>
</body>
</html>
