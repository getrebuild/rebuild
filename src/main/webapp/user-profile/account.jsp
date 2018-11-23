<%@page import="org.apache.commons.lang.StringUtils"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.rebuild.server.bizz.privileges.User"%>
<%@ page import="com.rebuild.server.Application"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>个人设置</title>
<style type="text/css">
form .form-group{border-bottom:1px solid #e6e9f0;}
.avatar{position:relative;width:120px;height:120px;line-height:1;font-size:0;background-color:#eee;border-radius:4px;overflow:hidden;line-height:120px}
.avatar>img{width:100%;}
.avatar>label{position:absolute;left:0;top:0;width:100%;height:100%;background-color:rgba(0,0,0,0.4);display:none;font-size:14px;text-align:center;padding-top:36px;color:#fff !important;line-height:1.5}
.avatar:hover>label{display:block;cursor:pointer;}
.avatar>label i.zmdi{font-size:28px}
.avatar>label input{display:none;}
.userinfo{line-height:1.8}
.userinfo dt{color:#777;font-weight:normal;padding-right:0}
.userinfo dd{padding-left:0}
</style>
</head>
<body>
<%
User theUser = Application.getUserStore().getUser(AppUtils.getRequestUser(request));
%>
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
						<div class="tab-content">
							<div class="tab-pane active" id="base">
								<div class="row">
									<div class="col-sm-6">
										<form>
											<div class="form-group row">
												<label class="col-sm-4 col-form-label text-left">姓名</label>
												<div class="col-sm-8 pl-0">
													<input class="form-control form-control-sm" type="text" id="fullName" value="<%=theUser.getFullName()%>" data-o="<%=theUser.getFullName()%>">
												</div>
											</div>
											<div class="form-group row border-none">
												<div class="col-sm-8 offset-sm-4 pl-0">
													<button class="btn btn-primary J_save" type="button">更新</button>
												</div>
											</div>
										</form>
									</div>
									<div class="col-sm-5 offset-sm-1">
										<div class="avatar float-left">
											<img alt="Avatar" src="<%=theUser.getAvatarUrl(true)%>">
											<label>
												<i class="zmdi zmdi-camera"></i><br>上传头像
												<input type="file" id="avatar-input" accept="image/*">
											</label>
										</div>
										<div>
											<dl class="row userinfo">
												<dt class="col-3 offset-1">账号</dt>
												<dd class="col-8"><%=theUser.getName()%></dd>
												<dt class="col-3 offset-1">部门</dt>
												<dd class="col-8"><%=theUser.getOwningBizUnit() == null ? "<em>未设置</em>" : theUser.getOwningBizUnit().getName()%></dd>
												<dt class="col-3 offset-1">角色</dt>
												<dd class="col-8"><%=theUser.getOwningRole() == null ? "<em>未设置</em>" : theUser.getOwningRole().getName()%></dd>
											</dl>
										</div>
										<div class="clearfix"></div>
									</div>
								</div>
							</div>
							<div class="tab-pane" id="secure">
								<form>
									<div class="form-group row">
										<label class="col-sm-2 col-form-label text-left">更改邮箱</label>
										<div class="col-sm-8 pl-0">
											<div class="form-control-plaintext text-muted"><%=theUser.getEmail() == null ? "你当前未绑定邮箱" : ("当前绑定邮箱 <b class='J_email-account'>" + theUser.getEmail() + "</b>")%></div>
										</div>
										<div class="col-sm-2 text-right">
											<button class="btn btn-primary bordered J_email" type="button">更改</button>
										</div>
									</div>
									<div class="form-group row">
										<label class="col-sm-2 col-form-label text-left">更改密码</label>
										<div class="col-sm-8 pl-0">
											<div class="form-control-plaintext text-muted">建议90天更改一次密码</div>
										</div>
										<div class="col-sm-2 text-right">
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
<script type="text/babel">
$(document).ready(function(){
	if (location.hash == '#secure') $('.nav-tabs a:eq(1)').trigger('click')
	
	$('#avatar-input').html5Uploader({
        name: 'avatar-input',
        postUrl: rb.baseUrl + '/filex/upload?cloud=auto&type=image',
        onClientLoad: function(e, file){
            if (file.type.substr(0, 5) != 'image'){
                rb.notice('请上传图片')
                return false
            }
        },
        onSuccess:function(d){
            d = JSON.parse(d.currentTarget.response)
            if (d.error_code == 0){
            	let aUrl = rb.storageUrl + d.data + '?imageView2/2/w/100/interlace/1/q/100'
            	$('.avatar img').attr({ 'src': aUrl, 'data-src': d.data })
            } else rb.notice(d.error_msg || '上传失败，请稍后重试', 'danger')
        }
    })
    
    $('.J_email').click(function(){
    	rb.modal(rb.baseUrl + '/p/user-profile/change-email', '更改邮箱')
    })
    $('.J_passwd').click(function(){
    	rb.modal(rb.baseUrl + '/p/user-profile/change-passwd', '更改密码')
    })
    
    $('.J_save').click(function(){
    	let fullName = $val('#fullName'),
    		avatarUrl = $('.avatar img').attr('data-src')
    	if (!fullName && !avatarUrl){ location.reload(); return }
    	
    	let _data = { metadata: { entity: 'User', id: '<%=theUser.getId()%>' } }
    	if (fullName) _data.fullName = fullName
    	if (avatarUrl) _data.avatarUrl = avatarUrl
    	$.post(rb.baseUrl + '/app/entity/record-save', JSON.stringify(_data), function(res){
    		location.reload()
    	})
    })
})
</script>
</body>
</html>
