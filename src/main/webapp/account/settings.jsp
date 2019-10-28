<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.rebuild.server.service.bizz.privileges.User"%>
<%@ page import="com.rebuild.server.Application"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/widget/cropper.min.css">
<title>个人设置</title>
<style type="text/css">
.tab-pane .form-group{border-bottom:1px solid #e6e9f0;}
.avatar{position:relative;width:160px;height:160px;line-height:1;font-size:0;background-color:#eee;border-radius:4px;overflow:hidden;line-height:120px}
.avatar>img{width:100%;}
.avatar>label{position:absolute;left:0;top:0;width:100%;height:100%;background-color:rgba(0,0,0,0.4);display:none;font-size:14px;text-align:center;padding-top:56px;color:#fff !important;line-height:1.5}
.avatar:hover>label{display:block;cursor:pointer;}
.avatar>label i.zmdi{font-size:28px}
.avatar>label input{display:none;}
.userinfo{line-height:1.8}
.userinfo dt{color:#777;font-weight:normal;padding-right:0}
.userinfo dd{padding-left:0}
</style>
</head>
<body>
<% final User theUser = Application.getUserStore().getUser(AppUtils.getRequestUser(request)); %>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-offcanvas-menu">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="个人设置" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="user-account" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container">
			<div class="card">
				<div class="card-body">
					<div class="tab-container">
						<ul class="nav nav-tabs">
							<li class="nav-item"><a class="nav-link active" href="#base" data-toggle="tab">个人信息</a></li>
							<li class="nav-item"><a class="nav-link" href="#secure" data-toggle="tab">安全设置</a></li>
						</ul>
						<div class="tab-content mb-0">
							<div class="tab-pane active" id="base">
								<div class="row">
									<div class="col-12 col-sm-8">
										<form>
											<div class="form-group row">
												<label class="col-sm-4 col-form-label text-left">账号 (登录名)</label>
												<div class="col-sm-8 pl-0">
													<div class="form-control-plaintext"><%=theUser.getName()%></div>
												</div>
											</div>
											<div class="form-group row">
												<label class="col-sm-4 col-form-label text-left">所在部门</label>
												<div class="col-sm-8 pl-0">
													<div class="form-control-plaintext"><%=theUser.getOwningBizUnit() == null ? "<em>未设置</em>" : theUser.getOwningBizUnit().getName()%></div>
												</div>
											</div>
											<div class="form-group row">
												<label class="col-sm-4 col-form-label text-left">系统角色</label>
												<div class="col-sm-8 pl-0">
													<div class="form-control-plaintext"><%=theUser.getOwningRole() == null ? "<em>未设置</em>" : theUser.getOwningRole().getName()%></div>
												</div>
											</div>
											<div class="form-group row">
												<label class="col-sm-4 col-form-label text-left">姓名</label>
												<div class="col-sm-8 pl-0">
													<input class="form-control form-control-sm" type="text" id="fullName" value="<%=theUser.getFullName()%>" data-o="<%=theUser.getFullName()%>">
												</div>
											</div>
											<div class="form-group row border-none">
												<div class="col-sm-8 offset-sm-4 pl-0">
													<button class="btn btn-primary J_save" type="button">确定</button>
												</div>
											</div>
										</form>
									</div>
									<div class="col-12 col-sm-4">
										<div class="avatar float-right">
											<img alt="Avatar" id="avatar-img" src="${baseUrl}/account/user-avatar?w=200">
											<label>
												<i class="zmdi zmdi-camera"></i><br>更改头像
												<input type="file" id="avatar-input" accept="image/png,image/jpeg,image/gif" data-temp="true">
											</label>
										</div>
										<div class="clearfix"></div>
									</div>
								</div>
							</div>
							<div class="tab-pane" id="secure">
								<form>
									<div class="form-group row">
										<label class="col-sm-2 col-form-label text-left">更改邮箱</label>
										<div class="col-sm-7 pl-0">
											<div class="form-control-plaintext text-muted J_email-account"><%=theUser.getEmail() == null ? "你当前未绑定邮箱" : ("当前绑定邮箱 <b>" + theUser.getEmail() + "</b>")%></div>
										</div>
										<div class="col-sm-3 text-right">
											<button class="btn btn-primary bordered J_email" type="button">更改</button>
										</div>
									</div>
									<div class="form-group row">
										<label class="col-sm-2 col-form-label text-left">更改密码</label>
										<div class="col-sm-7 pl-0">
											<div class="form-control-plaintext text-muted">建议90天更改一次密码</div>
										</div>
										<div class="col-sm-3 text-right">
											<button class="btn btn-primary bordered J_passwd" type="button">更改</button>
										</div>
									</div>
								</form>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = { userid: '<%=theUser.getId()%>' }
</script>
<script src="${baseUrl}/assets/lib/widget/cropper.min.js"></script>
<script src="${baseUrl}/assets/js/user-settings.jsx" type="text/babel"></script>
</body>
</html>
