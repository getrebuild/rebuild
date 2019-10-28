<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/triggers.css">
<title>触发器</title>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
<jsp:include page="/_include/NavTop.jsp">
	<jsp:param value="触发器" name="pageTitle"/>
</jsp:include>
<jsp:include page="/_include/NavLeftAdmin.jsp">
	<jsp:param value="robot-trigger" name="activeNav"/>
</jsp:include>
<div class="rb-content">
	<div class="page-head">
		<div class="page-head-title">触发器<span class="sub-title">${name != null ? name : "未命名"}</span></div>
		<div class="clearfix"></div>
	</div>
	<div class="main-content container-fluid pt-0">
		<div class="card mb-0">
			<div class="card-body">
				<ul class="timeline spare">
					<li class="timeline-item">
						<div class="timeline-date"><span>当发生动作</span></div>
						<div class="timeline-content">
						<form class="simple">
							<div class="form-group row">
								<label class="col-md-12 col-lg-3 col-form-label text-lg-right">源实体</label>
								<div class="col-md-12 col-lg-9">
									<div class="form-control-plaintext text-bold">${sourceEntityLabel}</div>
								</div>
							</div>
							<div class="form-group row pt-0 pb-0">
								<label class="col-md-12 col-lg-3 col-form-label text-lg-right">触发动作</label>
								<div class="col-md-12 col-lg-9 pt-1 J_when">
									<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
										<input class="custom-control-input" type="checkbox" value="1"><span class="custom-control-label"> 新建时</span>
									</label>
									<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
										<input class="custom-control-input" type="checkbox" value="4"><span class="custom-control-label"> 更新时</span>
									</label>
									<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
										<input class="custom-control-input" type="checkbox" value="2"><span class="custom-control-label"> 删除时</span>
									</label>
									<div class="mt-1">
										<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
											<input class="custom-control-input" type="checkbox" value="16" ><span class="custom-control-label"> 分派时</span>
										</label>
										<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
											<input class="custom-control-input" type="checkbox" value="32"><span class="custom-control-label"> 共享时</span>
										</label>
										<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
											<input class="custom-control-input" type="checkbox" value="64"><span class="custom-control-label"> 取消共享时</span>
										</label>
									</div>
								</div>
							</div>
							<div class="form-group row">
								<label class="col-md-12 col-lg-3 col-form-label text-lg-right">附加过滤条件</label>
								<div class="col-md-12 col-lg-9 J_whenFilter">
									<a class="btn btn-link pl-0 text-left">点击设置</a>
									<p class="form-text mb-0 mt-0">符合过滤条件的数据才会被触发</p>
								</div>
							</div>
						</form>
						</div>
					</li>
					<li class="timeline-item">
						<div class="timeline-date"><span>就执行操作</span></div>
						<div class="timeline-content">
						<form class="simple">
							<div class="form-group row">
								<label class="col-md-12 col-lg-3 col-form-label text-lg-right">执行操作</label>
								<div class="col-md-12 col-lg-9">
									<div class="form-control-plaintext text-bold">${actionTypeLabel}</div>
								</div>
							</div>
							<div class="form-group row">
								<label class="col-md-12 col-lg-3 col-form-label text-lg-right">操作内容</label>
								<div class="col-md-12 col-lg-9">
									<div id="react-content">加载中...</div>
								</div>
							</div>
							<div class="form-group row">
								<label class="col-md-12 col-lg-3 col-form-label text-lg-right">执行优先级</label>
								<div class="col-md-12 col-lg-9">
									<input type="number" class="form-control form-control-sm" id="priority" value="${priority}" data-o="${priority}" style="max-width:200px" />
									<p class="form-text mb-0">优先级高的会被先执行</p>
								</div>
							</div>
						</form>
						</div>
					</li>
					<li class="timeline-item last">
						<div class="timeline-content">
						<form class="simple">
							<div class="form-group row footer">
								<label class="col-md-12 col-lg-3 col-form-label text-lg-right"></label>
								<div class="col-md-12 col-lg-9">
									<button class="btn btn-primary J_save" type="button">保存</button>
								</div>
							</div>
						</form>
						</div>
					</li>
				</ul>
			</div>
		</div>
	</div>
</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = {
	configId: '${configId}',
	actionType: '${actionType}',
	sourceEntity: '${sourceEntity}',
	when: ${when},
	whenFilter: ${whenFilter},
	actionContent: ${actionContent}
}
</script>
<script src="${baseUrl}/assets/js/rb-advfilter.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/triggers/trigger-design.jsx" type="text/babel"></script>
<c:if test="${actionType == 'FIELDAGGREGATION'}"><script src="${baseUrl}/assets/js/triggers/trigger.FIELDAGGREGATION.jsx" type="text/babel"></script></c:if>
<c:if test="${actionType == 'SENDNOTIFICATION'}"><script src="${baseUrl}/assets/js/triggers/trigger.SENDNOTIFICATION.jsx" type="text/babel"></script></c:if>
<c:if test="${actionType == 'AUTOASSIGN'}"><script src="${baseUrl}/assets/js/triggers/trigger.AUTOASSIGN.jsx" type="text/babel"></script></c:if>
<c:if test="${actionType == 'AUTOSHARE'}"><script src="${baseUrl}/assets/js/triggers/trigger.AUTOSHARE.jsx" type="text/babel"></script></c:if>
</body>
</html>
