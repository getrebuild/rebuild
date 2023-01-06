/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global UserSelectorWithField, EditorWithFieldVars */

const wpc = window.__PageConfig

// ~~ 发送通知
// eslint-disable-next-line no-undef
class ContentSendNotification extends ActionContentSpec {
  state = { ...this.props, type: 1, userType: 1 }

  render() {
    return (
      <div className="send-notification">
        <form className="simple">
          <div className="form-group row pt-1">
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('通知类型')}</label>
            <div className="col-12 col-lg-8 pt-1">
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input" name="mtype" type="radio" onChange={() => this.setState({ type: 1 })} checked={this.state.type === 1} disabled={this.state.userType === 2} />
                <span className="custom-control-label">{$L('通知')}</span>
              </label>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input" name="mtype" type="radio" onChange={() => this.setState({ type: 2 })} checked={this.state.type === 2} />
                <span className="custom-control-label">
                  {$L('邮件')} {this.state.serviceMail === false && `(${$L('不可用')})`}
                </span>
              </label>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input" name="mtype" type="radio" onChange={() => this.setState({ type: 3 })} checked={this.state.type === 3} />
                <span className="custom-control-label">
                  {$L('短信')} {this.state.serviceSms === false && `(${$L('不可用')})`}
                </span>
              </label>
            </div>
          </div>

          <div className="form-group row pt-1 mb-0">
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('发送给谁')}</label>
            <div className="col-12 col-lg-8 pt-1">
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input" name="utype" type="radio" onChange={() => this.setUserType(1)} checked={this.state.userType === 1} />
                <span className="custom-control-label">{$L('内部用户')}</span>
              </label>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input" name="utype" type="radio" onChange={() => this.setUserType(2)} checked={this.state.userType === 2} />
                <span className="custom-control-label">
                  {$L('外部人员')} <sup className="rbv" title={$L('增值功能')} />
                </span>
              </label>
            </div>
          </div>
          <div className="form-group row pt-0 mt-0">
            <label className="col-12 col-lg-3 col-form-label text-lg-right" />
            <div className="col-12 col-lg-8">
              <div className={this.state.userType === 1 ? '' : 'hide'}>
                <UserSelectorWithField ref={(c) => (this._sendTo1 = c)} />
              </div>
              <div className={this.state.userType === 2 ? '' : 'hide'}>
                <AccountSelectorWithField ref={(c) => (this._sendTo2 = c)} hideUser hideDepartment hideRole hideTeam />
                <p className="form-text">{$L('选择外部人员的电话（手机）或邮箱字段')}</p>
              </div>
            </div>
          </div>

          {this.state.type === 2 && (
            <div className="form-group row pb-1">
              <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('邮件标题')}</label>
              <div className="col-12 col-lg-8">
                <input type="text" className="form-control form-control-sm" ref={(c) => (this._$title = c)} maxLength="60" placeholder={$L('你有一条新通知')} style={{ maxWidth: '100%' }} />
              </div>
            </div>
          )}

          <div className="form-group row">
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('内容')}</label>
            <div className="col-12 col-lg-8">
              <EditorWithFieldVars entity={wpc.sourceEntity} ref={(c) => (this._content = c)} />
              <p className="form-text" dangerouslySetInnerHTML={{ __html: $L('内容 (及标题) 支持字段变量，字段变量如 `{createdOn}` (其中 createdOn 为源实体的字段内部标识)') }} />
            </div>
          </div>
        </form>
      </div>
    )
  }

  setUserType(type) {
    const s = { userType: type }
    if (type === 2 && this.state.type === 1) s.type = 2
    this.setState(s)
  }

  componentDidMount() {
    $.get('/admin/robot/trigger/sendnotification-atypes', (res) => this.setState({ ...res.data }))

    const content = this.props.content
    if (content) {
      if (content.sendTo) {
        $.post(`/commons/search/user-selector?entity=${this.props.sourceEntity}`, JSON.stringify(content.sendTo), (res) => {
          if (res.error_code === 0 && res.data.length > 0) {
            if (content.userType === 2) {
              this._sendTo2.setState({ selected: res.data })
            } else {
              this._sendTo1.setState({ selected: res.data })
            }
          }
        })
      }

      this.setState(
        {
          type: content.type ? content.type : 1,
          userType: content.userType ? content.userType : 1,
        },
        () => {
          $(this._$title).val(content.title || '')
          this._content.val(content.content || '')
        }
      )

      if (content.whenUpdateFields && content.whenUpdateFields.length > 0) {
        window.whenUpdateFields = content.whenUpdateFields
        const $s = $('.when-update .custom-control-label')
        $s.text(`${$s.text()} (${content.whenUpdateFields.length})`)
      }
    }
  }

  buildContent() {
    if (rb.commercial < 1 && this.state.userType === 2) {
      RbHighbar.error(WrapHtml($L('免费版不支持外部人员功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return false
    }

    const _data = {
      type: this.state.type,
      userType: this.state.userType,
      sendTo: this.state.userType === 2 ? this._sendTo2.getSelected() : this._sendTo1.getSelected(),
      title: $(this._$title).val(),
      content: this._content.val(),
    }

    if ((_data.sendTo || []).length === 0) {
      RbHighbar.create($L('请选择发送给谁'))
      return false
    }
    if (!_data.content) {
      RbHighbar.create($L('内容不能为空'))
      return false
    }

    if (window.whenUpdateFields) _data.whenUpdateFields = window.whenUpdateFields

    return _data
  }
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  renderRbcomp(<ContentSendNotification {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
  })

  // 指定字段
  $('.when-update a.hide').removeClass('hide')
}

class AccountSelectorWithField extends UserSelector {
  constructor(props) {
    super(props)
    this._useTabs.push(['FIELDS', $L('使用字段')])
  }

  componentDidMount() {
    super.componentDidMount()

    this._fields = []
    $.get(`/commons/metadata/fields?deep=2&entity=${this.props.entity || wpc.sourceEntity}`, (res) => {
      res.data &&
        res.data.forEach((item) => {
          if (item.type === 'PHONE' || item.type === 'EMAIL') {
            this._fields.push({ id: item.name, text: item.label })
          }
        })
      this.switchTab()
    })
  }

  switchTab() {
    this.setState({ tabType: 'FIELDS', items: this._fields || [] })
  }
}
