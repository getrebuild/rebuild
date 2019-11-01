/* eslint-disable react/no-string-refs */
let __cropper
$(document).ready(function () {
  if (location.hash === '#secure') $('.nav-tabs a:eq(1)').trigger('click')

  $createUploader('#avatar-input',
    function () {
      if (__cropper) return
      renderRbcomp(<DlgCropper disposeOnHide={true} />, null, function () {
        __cropper = this
      })
    }, function (res) {
      __cropper.setImg(res.key)
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
      else RbHighbar.create(res.error_msg)
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
      <div className="form">
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
      </div>
    </RbModal>)
  }
  post() {
    let s = this.state
    if (!s.oldPasswd) { RbHighbar.create('请输入原密码'); return }
    if (!s.newPasswd) { RbHighbar.create('请输入新密码'); return }
    if (s.newPasswd !== s.newPasswdAgain) { RbHighbar.create('两次输入的新密码不一致'); return }
    let btns = $(this.refs['btns']).find('.btn').button('loading')
    $.post(rb.baseUrl + '/account/settings/save-passwd?oldp=' + $encode(s.oldPasswd) + '&newp=' + $encode(s.newPasswd), (res) => {
      btns.button('reset')
      if (res.error_code === 0) {
        this.hide()
        RbHighbar.create('密码修改成功', 'success')
      } else RbHighbar.create(res.error_msg)
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
      <div className="form">
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
      </div>
    </RbModal>)
  }
  sendVCode() {
    let s = this.state
    if (!s.newEmail || !$regex.isMail(s.newEmail)) { RbHighbar.create('请输入有效的邮箱地址'); return }
    this.setState({ vcodeDisabled: true })
    $.post(rb.baseUrl + '/account/settings/send-email-vcode?email=' + $encode(s.newEmail), (res) => {
      if (res.error_code === 0) this.vcodeResend()
      else {
        this.setState({ vcodeDisabled: false })
        RbHighbar.create(res.error_msg)
      }
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
    if (!s.newEmail || !$regex.isMail(s.newEmail)) { RbHighbar.create('请输入有效的邮箱地址'); return }
    if (!s.newEmail || !s.vcode) { RbHighbar.create('请输入邮箱地址和验证码'); return }
    let btns = $(this.refs['btns']).find('.btn').button('loading')
    $.post(rb.baseUrl + '/account/settings/save-email?email=' + $encode(s.newEmail) + '&vcode=' + $encode(s.vcode), (res) => {
      btns.button('reset')
      if (res.error_code === 0) {
        this.hide()
        $('.J_email-account').html('当前绑定邮箱 <b>' + s.newEmail + '</b>')
        RbHighbar.create('邮箱修改成功', 'success')
      } else RbHighbar.create(res.error_msg)
    })
  }
}

// 头像裁剪
class DlgCropper extends RbModalHandler {
  constructor(props) {
    super(props)
    this.state.inLoad = true
  }

  render() {
    return <RbModal title="更改头像" ref={(c) => this._dlg = c} width="500" onHide={() => __cropper = null}>
      <div className={this.state.inLoad ? 'rb-loading rb-loading-active' : null} style={{ height: 400, overflow: 'hide' }}>
        {this.state.img && <img src={`${rb.baseUrl}/filex/img/${this.state.img}?temp=true`} ref={(c) => this._avatar = c} style={{ maxWidth: '100%' }} />}
        {this.state.inLoad && <RbSpinner />}
      </div>
      <div className="mt-3">
        <button className="btn btn-primary w-100" onClick={this.post} ref={(c) => this._btn = c}>更改</button>
      </div>
    </RbModal>
  }

  componentDidMount() {
    if (this.state.img) this.setImg(this.state.img)
  }

  setImg(img) {
    this.setState({ img: img }, () => {
      let that = this
      $(this._avatar).cropper({
        aspectRatio: 1 / 1,
        viewMode: 0,
        checkOrientation: false,
        ready() {
          that.setState({ inLoad: false })
        }
      })
      this.__cropper = $(this._avatar).data('cropper')
    })
  }

  post = () => {
    let data = this.__cropper.getData()
    let xywh = [~~data.x, ~~data.y, ~~data.width, ~~data.height].join(',')

    $(this._btn).button('loading')
    $.post(`${rb.baseUrl}/account/user-avatar-update?avatar=${this.state.img}&xywh=${xywh}`, () => {
      location.reload()
      // $('#avatar-img').attr('src', `${rb.baseUrl}/filex/img/${res.data}?imageView2/2/w/100/interlace/1/q/100`)
      // this.hide()
    })
  }
}