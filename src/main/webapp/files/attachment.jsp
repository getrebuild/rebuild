<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>附件管理</title>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/files.css">
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-aside">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="附件管理" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="nav_entity-Attachment" name="activeNav"/>
	</jsp:include>
	<div class="rb-content rb-no-padding">
		<aside class="page-aside widgets">
			<a class="side-toggle" title="展开/收缩面板"><i class="zmdi zmdi-arrow-left"></i></a>
			<div class="tab-container">
				<ul class="nav nav-tabs">
					<li class="nav-item"><a class="nav-link active" href="attachment">附件</a></li>
					<li class="nav-item"><a class="nav-link" href="docs">文档</a></li>
				</ul>
				<div class="tab-content rb-scroller">
					<div class="tab-pane active" id="navTree">
						<div class="ph-item rb">
							<div class="ph-col-12 p-0">
								<div class="ph-row">
									<div class="ph-col-12 big"></div>
									<div class="ph-col-12 big"></div>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</aside>
		<div class="main-content container-fluid">
			<div class="file-header">
				<div class="file-path">
					<ol class="breadcrumb">
						<li class="breadcrumb-item active">全部附件</li>
					</ol>
				</div>
				<div class="file-operator">
					<div class="row">
					<div class="col-lg-6 col-12">
						<div class="input-group input-search">
							<input class="form-control" type="text" placeholder="搜索 ..." maxlength="40">
							<span class="input-group-btn"><button class="btn btn-secondary" type="button"><i class="icon zmdi zmdi-search"></i></button></span>
						</div>
					</div>
					<div class="col-lg-6 col-12 text-lg-right mt-lg-0 mt-md-2 mt-sm-2">
						<div class="btn-group btn-space mb-0 mr-0 J_sort">
							<button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown"><span>默认排序</span> <i class="icon zmdi zmdi-chevron-down"></i></button>
							<div class="dropdown-menu dropdown-menu-right">
								<a class="dropdown-item" data-sort="newer">最近上传</a>
								<a class="dropdown-item" data-sort="older">最早上传</a>
							</div>
						</div>
					</div>
				</div>
				</div>
			</div>
			<div class="file-viewport">
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/files/files.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/files/files-attachment.jsx" type="text/babel"></script>
</body>
</html>
