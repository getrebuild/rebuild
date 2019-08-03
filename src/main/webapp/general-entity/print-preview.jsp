<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>打印预览</title>
<style type="text/css">
html,body{background-color:#fff;}
.preview-tools{
    background-color: #eee;
    padding: 10px 20px;
    padding-bottom: 5px;
    text-align: right;
}
</style>
</head>
<body>
<div class="preview-tools d-print-none">
    <button class="btn btn-space btn-secondary" onclick="window.print()"><i class="icon zmdi zmdi-print"></i> 打印</button>
    <button class="btn btn-space btn-secondary" onclick="window.close()">关闭</button>
</div>
<div class="preview-content">${contentBody}</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
</script>
<script type="text/babel">
$(document).ready(function() {
})
</script>
</body>
</html>
