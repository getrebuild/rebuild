<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp" %>
<title>安全提示</title>
<style type="text/css">
.safe-tips {
    background-color: #fff;
    max-width: 601px;
    border-radius: 4px;
    margin: 0 auto;
    padding: 30px 40px;
}
.safe-tips .url {
    border-radius: 3px;
    border: 1px solid #ddd;
    background: #fafafa;
    padding: 8px;
    text-align: left;
    color: #4285f4;
    margin-bottom: 20px;
    cursor: default;
}
.safe-tips .url .icon {
    width: 40px;
    height: 40px;
    line-height: 40px;
    background-color: #bcc6d8;
    border-radius: 2px;
    text-align: center;
    font-size: 28px;
    color: #f3f3f3;
    float: left;
    margin-right: 10px;
}
.safe-tips .url .text {
    height: 40px;
    display: table-cell;
    vertical-align: middle;
    word-break: break-all;
}
</style>
</head>
<body class="rb-splash-screen">
<div class="rb-wrapper rb-error">
    <div class="rb-content m-0">
        <div class="main-content container">
            <div class="error-container">
                <div class="safe-tips">
                    <h3>即将跳转到外部网站</h3>
                    <p>安全性未知，请谨慎访问</p>
                    <div class="url">
                        <span class="icon"><i class="zmdi zmdi-link"></i></span>
                        <span class="text">${outerUrl}</span>
                    </div>
                    <div class="clearfix"></div>
                    <a class="btn btn-danger bordered" href="${outerUrl}" rel="noopener noreferrer">继续访问</a>
                    <a class="btn btn-link" onclick="window.close()">关闭</a>
                </div>
            </div>
        </div>
    </div>
</div>
<div class="copyright fixed">&copy; ${appName}</div>
<%@ include file="/_include/Foot.jsp" %>
</body>
</html>
