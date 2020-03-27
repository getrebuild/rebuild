<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>${bundle.lang('UserSignup')}</title>
<style type="text/css">
.form-group.row {
	margin-left: -15px !important;
	margin-right: -15px !important;
}
.form-group.row .btn {
	height: 41px;
	width: 100%;
}
</style>
</head>
<body class="rb-splash-screen">
<div class="rb-wrapper rb-login">
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="splash-container">
				<div class="card card-border-color card-border-color-danger">
					<div class="card-header"><a class="logo-img"></a></div>
					<div class="card-body">
						<div class="form-group">
							<input class="form-control" id="sFullName" type="text" placeholder="${bundle.lang('Your', 'FullName')}" autocomplete="off">
                            <p class="form-text">${bundle.lang('AdminWillReviewSignupTip')}</p>
						</div>
						<div class="form-group">
							<input class="form-control" id="sName" type="text" placeholder="${bundle.lang('Username')}" autocomplete="off">
						</div>
						<div class="form-group">
							<input class="form-control" id="sEmail" type="email" placeholder="${bundle.lang('Email')}" autocomplete="off">
						</div>
						<div class="form-group row pt-0">
							<div class="col-7">
								<input class="form-control" type="text" id="sVcode" placeholder="${bundle.lang('EmailVcode')}" autocomplete="off">
							</div>
							<div class="col-5 text-right pl-0">
								<button class="btn btn-primary bordered J_vcode-btn" type="button">${bundle.lang('GetVcode')}</button>
							</div>
						</div>
						<div class="form-group login-submit">
							<button class="btn btn-primary btn-xl J_confirm-btn" type="button">${bundle.lang('RequestSignup')}</button>
						</div>
                        <div class="alert alert-success alert-icon alert-icon-border alert-sm hide">
                            <div class="icon"><span class="zmdi zmdi-check"></span></div>
                            <div class="message"><p>${bundle.lang('SignupSuccessTip')}<br><a href="login">${bundle.lang('ReturnLogin')}</a></p></div>
                        </div>
					</div>
				</div>
				<div class="splash-footer">
					<span><a href="login">${bundle.lang('ReturnLogin')}</a></span>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/signup.js"></script>
</body>
</html>
