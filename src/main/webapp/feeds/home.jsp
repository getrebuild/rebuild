<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>动态</title>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/feeds.css">
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="动态" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="nav_entity-Feeds" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="announcement-wrapper"></div>
		<div class="main-content container container-smart">
			<div class="row">
				<div class="col-lg-8 col-12">
                    <div class="rb-loading rb-loading-active" id="rb-feeds">
                        <%@ include file="/_include/spinner.jsp"%>
                    </div>
				</div>
				<div class="col-lg-4 col-12 side-wrapper-parent pl-lg-0">
					<div class="side-wrapper">
						<div class="accordion m-0">
							<div class="card">
								<div class="card-header" id="headingSearch">
									<button class="btn" data-toggle="collapse" data-target="#collapseSearch" aria-expanded="true"><i class="icon zmdi zmdi-chevron-right"></i> 筛选</button>
								</div>
								<div class="collapse show" id="collapseSearch">
									<div class="card-body">
										<div class="input-group input-group-sm">
											<input type="text" class="form-control search J_search-key" placeholder="关键词" />
											<span class="append">
												<a><i class="icon zmdi zmdi-close"></i></a>
												<i class="icon zmdi zmdi-search"></i>
											</span>
										</div>
										<div class="input-group input-group-sm">
											<input type="text" class="form-control search J_date-begin" placeholder="发布时间 (起)" />
											<span class="append">
												<a><i class="icon zmdi zmdi-close"></i></a>
												<i class="icon zmdi zmdi-calendar"></i>
											</span>
										</div>
										<div class="input-group input-group-sm">
											<input type="text" class="form-control search J_date-end" placeholder="发布时间 (止)" />
											<span class="append">
												<a><i class="icon zmdi zmdi-close"></i></a>
												<i class="icon zmdi zmdi-calendar"></i>
											</span>
										</div>
									</div>
								</div>
							</div>
							<div class="card">
								<div class="card-header" id="headingFeedsType">
									<button class="btn" data-toggle="collapse" data-target="#collapseFeedsType"><i class="icon zmdi zmdi-chevron-right"></i> 类型</button>
								</div>
								<div class="collapse" id="collapseFeedsType">
									<div class="card-body">
										<div class="dept-tree rb-scroller">
											<ul class="list-unstyled">
												<li data-type="1"><a>动态</a></li>
												<li data-type="2"><a>跟进</a></li>
												<li data-type="3"><a>公告</a></li>
											</ul>
										</div>
									</div>
								</div>
							</div>
							<div class="card">
								<div class="card-header" id="headingGroup">
									<button class="btn" data-toggle="collapse" data-target="#collapseGroup"><i class="icon zmdi zmdi-chevron-right"></i> 团队</button>
									<a class="add-group admin-show" href="${baseUrl}/admin/bizuser/teams" target="_blank" title="管理团队"><i class="icon zmdi zmdi-settings"></i></a>
								</div>
								<div class="collapse" id="collapseGroup">
									<div class="card-body">
                                        <div class="search-member"><input type="text" placeholder="搜索团队" /></div>
										<div class="dept-tree rb-scroller">
										</div>
									</div>
								</div>
							</div>
							<div class="card">
								<div class="card-header" id="headingUser">
									<button class="btn" data-toggle="collapse" data-target="#collapseUser"><i class="icon zmdi zmdi-chevron-right"></i> 用户</button>
								</div>
								<div class="collapse" id="collapseUser">
									<div class="card-body pb-3">
                                        <div class="search-member"><input type="text" placeholder="搜索用户" /></div>
										<div class="dept-tree rb-scroller">
										</div>
									</div>
								</div>
							</div>
						</div>
					</div>
                </div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/lib/jquery.textarea.js"></script>
<script src="${baseUrl}/assets/js/feeds/announcement.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/feeds/feeds-post.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/feeds/feeds-list.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/feeds/feeds.jsx" type="text/babel"></script>
</body>
</html>
