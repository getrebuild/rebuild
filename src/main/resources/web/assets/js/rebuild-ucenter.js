/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// eslint-disable-next-line no-unused-vars
const UCenter = {
  query: function (c) {
    $.get('/settings/ucenter/bind-query', (res) => {
      typeof c === 'function' && c(res.data || null)
    })
  },

  bind: function () {
    renderRbcomp(<UCenterBind />)
  },
}

class UCenterBind extends RbFormHandler {
  render() {
    return (
      <RbModal title={$L('绑定 REBUILD 云账号')} ref={(c) => (this._dlg = c)} disposeOnHide>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('云账号')}</label>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" data-id="cloudAccount" onChange={this.handleChange} defaultValue="" />
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('云账号密码')}</label>
            <div className="col-sm-7">
              <input type="password" className="form-control form-control-sm" data-id="cloudPasswd" onChange={this.handleChange} defaultValue="" />
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={() => this.bind()}>
                {$L('绑定')}
              </button>
              <a className="btn btn-link link ml-3" target="_blank" href="https://getrebuild.com/ucenter/signup">
                {$L('注册 REBUILD 云账号')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  bind() {
    const s = this.state
    if (!s.cloudAccount) return RbHighbar.create($L('请输入账号'))
    if (!s.cloudPasswd) return RbHighbar.create($L('请输入密码'))

    const $btns = $(this._btns).find('.btn').button('loading')
    $.post('/settings/ucenter/bind', JSON.stringify(s), (res) => {
      if (res.error_code === 0) {
        this.hide()
        RbHighbar.success($L('绑定成功'))
        setTimeout(() => location.reload(), 1500)
      } else {
        $btns.button('reset')
        RbHighbar.create(res.error_msg)
      }
    })
  }
}
