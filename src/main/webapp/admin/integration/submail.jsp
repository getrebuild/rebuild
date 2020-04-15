<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>短信/邮件配置</title>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="短信/邮件配置" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="integration-submail" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid syscfg">
			<div class="row">
				<div class="col-lg-9 col-12">
					<div class="card">
						<div class="card-header card-header-divider">
                            赛邮 SUBMAIL
                            <a href="#modfiy" class="float-right"><i class="icon zmdi zmdi-edit"></i> 修改</a>
                        </div>
						<div class="card-body">
							<h5>邮件服务</h5>
                            <table class="table">
                            <tbody>
                                <tr>
                                    <td width="40%">APPID</td>
                                    <td data-id="MailUser">${mailAccount == null ? "未配置" : mailAccount[0]}</td>
                                </tr>
                                <tr>
                                    <td>APPKEY</td>
                                    <td data-id="MailPassword">${mailAccount == null ? "未配置" : mailAccount[1]}</td>
                                </tr>
                                <tr>
                                    <td>发件人地址<p>地址域名需与 SUBMAIL 中配置的域名匹配</p></td>
                                    <td data-id="MailAddr">${mailAccount == null ? "未配置" : mailAccount[2]}</td>
                                </tr>
                                <tr>
                                    <td>发件人名称</td>
                                    <td data-id="MailName">${mailAccount == null ? "未配置" : mailAccount[3]}</td>
                                </tr>
                                <tr class="show-on-edit">
                                    <td></td>
                                    <td><button class="btn btn-primary bordered J_test-email">发送测试</button></td>
                                </tr>
                            </tbody>
                            </table>
                            <c:if test="${mailAccount == null}">
                            <div class="alert alert-danger alert-icon mt-6 mb-6">
                                <div class="icon"><span class="zmdi zmdi-close-circle-o"></span></div>
                                <div class="message">邮件账号未配置，邮件相关功能不可用</div>
                            </div>
                            </c:if>
							<h5>短信服务</h5>
                            <table class="table">
                            <tbody>
                                <tr>
                                    <td width="40%">APPID</td>
                                    <td data-id="SmsUser">${smsAccount == null ? "未配置" : smsAccount[0]}</td>
                                </tr>
                                <tr>
                                    <td>APPKEY</td>
                                    <td data-id="SmsPassword">${smsAccount == null ? "未配置" : smsAccount[1]}</td>
                                </tr>
                                <tr>
                                    <td>短信签名</td>
                                    <td data-id="SmsSign">${smsAccount == null ? "未配置" : smsAccount[2]}</td>
                                </tr>
                                <tr class="show-on-edit">
                                    <td></td>
                                    <td><button class="btn btn-primary bordered J_test-sms">发送测试</button></td>
                                </tr>
                            </tbody>
                            </table>
                            <c:if test="${smsAccount == null}">
                            <div class="alert alert-danger alert-icon mt-6">
                                <div class="icon"><span class="zmdi zmdi-close-circle-o"></span></div>
                                <div class="message">短信账号未配置，短信相关功能不可用</div>
                            </div>
                            </c:if>
                            <div class="edit-footer">
                                <button class="btn btn-link">取消</button>
                                <button class="btn btn-primary">保存</button>
                            </div>
						</div>
					</div>
				</div>
				<div class="col-lg-3 col-12">
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/admin/syscfg.jsx" type="text/babel"></script>
<script type="text/babel">
$(document).ready(() => {
    $('.J_test-email').click(() => renderRbcomp(<TestSend type="email" />) )
    $('.J_test-sms').click(() => renderRbcomp(<TestSend type="sms" />))
})
class TestSend extends RbAlert {
    constructor(props) {
        super(props)
        this.state = { ...props }
    }
    renderContent() {
        const typeName = this.props.type === 'email' ? '邮箱' : '手机'
        return (
            <form style={{ maxWidth: 400, margin: '0 auto' }}>
                <div className="form-group">
                    <label>输入接收{typeName}</label>
                    <input type="text" className="form-control form-control-sm" placeholder={typeName} ref={(c) => this._input = c} />
                </div>
                <div className="form-group mb-1">
                    <button type="button" className="btn btn-space btn-primary" onClick={()=>this.confirm()} ref={(c) => this._btn = c} >发送</button>
                </div>
            </form>
        )
    }
    confirm() {
        let receiver = $(this._input).val()
        if (!receiver) return

        let conf = {}
        $('.syscfg table td[data-id]').each(function () {
            let $this = $(this)
            conf[$this.data('id')] = $this.find('input').val()
        })

        $(this._btn).button('loading')
        $.post('./submail/test?type=' + this.props.type + '&receiver=' + $encode(receiver), JSON.stringify(conf), (res) => {
            if (res.error_code === 0) {
                RbHighbar.success('测试发送成功')
                // this.hide()
            } else RbHighbar.create(res.error_msg || '测试发送失败')
            $(this._btn).button('reset')
        })
    }
}
</script>
</body>
</html>
