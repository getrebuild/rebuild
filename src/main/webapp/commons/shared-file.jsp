<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>分享文件</title>
<style type="text/css">
.preview-modal .preview-header .float-right a:first-child {
    display: none;
}
.preview-modal {
    background-color: transparent;
}
.preview-modal .preview-header {
    background-color: #28a745;
}
.sharebox .zmdi {
    color: #aaa;
    font-size: 81px;
    opacity: 0.6;
}
</style>
</head>
<body>
<div class="sharebox must-center">
    <i class="zmdi zmdi-share"></i>
</div>
<div class="copyright fixed">&copy; ${appName}</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = { publicUrl: '${publicUrl}' }
</script>
<script type="text/babel">
$(document).ready(function() {
    renderRbcomp(<RbPreview urls={[window.__PageConfig.publicUrl]} currentIndex="0" unclose="true" />)
})
</script>
</body>
</html>
