/* eslint-disable react/no-string-refs */
$(document).ready(function () {
  if (location.hash === '#secure') $('.nav-tabs a:eq(1)').trigger('click')

  $('#avatar-input').html5Uploader({
    name: 'avatar-input',
    postUrl: rb.baseUrl + '/filex/upload?cloud=auto&type=image',
    onClientLoad: function (e, file) {
      if (file.type.substr(0, 5) !== 'image') {
        rb.highbar('请上传图片')
        return false
      }
    },
    onSuccess: function (d) {
      d = JSON.parse(d.currentTarget.response)
      if (d.error_code === 0) {
        let aUrl = `${rb.baseUrl}/cloud/img/${d.data}?imageView2/2/w/100/interlace/1/q/100`
        $('.avatar img').attr({ 'src': aUrl, 'data-src': d.data })
      } else rb.hberror(d.error_msg || '上传失败，请稍后重试')
    }
  })

  $('.J_email').click(() => { renderRbcomp(<DlgChangeEmail />) })
  $('.J_passwd').click(() => { renderRbcomp(<DlgChangePasswd />) })

  $('.J_save').click(function () {
    let fullName = $val('#fullName'),
      avatarUrl = $('.avatar img').attr('data-src')
    if (!fullName && !avatarUrl) { location.reload(); return }

    let _data = { metadata: { entity: 'User', id: window.__PageConfig.userid } }
    if (fullName) _data.fullName = fullName
    if (avatarUrl) _data.avatarUrl = avatarUrl
    $.post(rb.baseUrl + '/app/entity/record-save', JSON.stringify(_data), function (res) {
      if (res.error_code === 0) location.reload()
      else rb.highbar(res.error_msg)
    })
  })
})

// 修改密码
class DlgChangePasswd extends RbFormHandler {
  constructor(props) {
    super(props)
  }
  render() {
    return (<RbModal title="更改密码" ref="dlg" disposeOnHide={true}>
      <form>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">原密码</label>
          <div className="col-sm-7">
            <input type="password" className="form-control form-control-sm" data-id="oldPasswd" onChange={this.handleChange} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">新密码</label>
          <div className="col-sm-7">
            <input type="password" className="form-control form-control-sm" data-id="newPasswd" onChange={this.handleChange} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">重复新密码</label>
          <div className="col-sm-7">
            <input type="password" className="form-control form-control-sm" data-id="newPasswdAgain" onChange={this.handleChange} />
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3" ref="btns">
            <button className="btn btn-primary btn-space" type="button" onClick={() => this.post()}>确定</button>
            <a className="btn btn-link btn-space" onClick={() => this.hide()}>取消</a>
          </div>
        </div>
      </form>
    </RbModal>)
  }
  post() {
    let s = this.state
    if (!s.oldPasswd) { rb.highbar('请输入原密码'); return }
    if (!s.newPasswd) { rb.highbar('请输入新密码'); return }
    if (s.newPasswd !== s.newPasswdAgain) { rb.highbar('两次输入的新密码不一致'); return }
    let btns = $(this.refs['btns']).find('.btn').button('loading')
    $.post(rb.baseUrl + '/settings/account/save-passwd?oldp=' + $encode(s.oldPasswd) + '&newp=' + $encode(s.newPasswd), (res) => {
      btns.button('reset')
      if (res.error_code === 0) {
        this.hide()
        rb.highbar('密码修改成功', 'success')
      } else rb.highbar(res.error_msg)
    })
  }
}
// 修改邮箱
class DlgChangeEmail extends RbFormHandler {
  constructor(props) {
    super(props)
    this.state = { ...this.state, vcodeDisabled: false, vcodeCountdown: '获取验证码' }
  }
  render() {
    return (<RbModal title="更改邮箱" ref="dlg" disposeOnHide={true}>
      <form>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">邮箱地址</label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" data-id="newEmail" onChange={this.handleChange} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">验证码</label>
          <div className="col-sm-4 pr-0">
            <input type="text" className="form-control form-control-sm" data-id="vcode" onChange={this.handleChange} />
          </div>
          <div className="col-sm-3">
            <button type="button" className="btn btn-primary bordered w-100 J_vcode" onClick={() => this.sendVCode()} disabled={this.state.vcodeDisabled}>{this.state.vcodeCountdown}</button>
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3" ref="btns">
            <button className="btn btn-primary btn-space" type="button" onClick={() => this.post()}>确定</button>
            <a className="btn btn-link btn-space" onClick={() => this.hide()}>取消</a>
          </div>
        </div>
      </form>
    </RbModal>)
  }
  sendVCode() {
    let s = this.state
    if (!s.newEmail || !$regex.isMail(s.newEmail)) { rb.highbar('请输入有效的邮箱地址'); return }
    this.setState({ vcodeDisabled: true })
    $.post(rb.baseUrl + '/settings/account/send-email-vcode?email=' + $encode(s.newEmail), (res) => {
      if (res.error_code === 0) this.vcodeResend()
      else rb.highbar(res.error_msg)
    })
  }
  vcodeResend() {
    let countdown = 60
    let countdownTimer = setInterval(() => {
      this.setState({ vcodeCountdown: '重新获取 (' + (--countdown) + ')' })
      if (countdown <= 0) {
        clearInterval(countdownTimer)
        this.setState({ vcodeCountdown: '重新获取', vcodeDisabled: false })
      }
    }, 1000)
  }
  post() {
    let s = this.state
    if (!s.newEmail || !$regex.isMail(s.newEmail)) { rb.highbar('请输入有效的邮箱地址'); return }
    if (!s.newEmail || !s.vcode) { rb.highbar('请输入邮箱地址和验证码'); return }
    let btns = $(this.refs['btns']).find('.btn').button('loading')
    $.post(rb.baseUrl + '/settings/account/save-email?email=' + $encode(s.newEmail) + '&vcode=' + $encode(s.vcode), (res) => {
      btns.button('reset')
      if (res.error_code === 0) {
        this.hide()
        $('.J_email-account').html('当前绑定邮箱 <b>' + s.newEmail + '</b>')
        rb.highbar('邮箱修改成功', 'success')
      } else rb.highbar(res.error_msg)
    })
  }
}
