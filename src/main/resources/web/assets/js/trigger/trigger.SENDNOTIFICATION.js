/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global UserSelectorWithField */

// ~~ 发送通知
// eslint-disable-next-line no-undef
class ContentSendNotification extends ActionContentSpec {
  state = { ...this.props, type: 1 }

  render() {
    return (
      <div className="send-notification">
        <form className="simple">
          <div className="form-group row pt-1">
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('通知类型')}</label>
            <div className="col-12 col-lg-8 pt-1">
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input" name="mtype" type="radio" onChange={() => this.setState({ type: 1 })} checked={this.state.type === 1} />
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

          <div className="form-group row pt-1">
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('发送给谁')}</label>
            <div className="col-12 col-lg-8">
              <UserSelectorWithField ref={(c) => (this._sendTo = c)} />
            </div>
          </div>

          {this.state.type === 2 && (
            <div className="form-group row pb-1">
              <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('邮件标题')}</label>
              <div className="col-12 col-lg-8">
                <input type="text" className="form-control form-control-sm" ref={(c) => (this._title = c)} maxLength="60" placeholder={$L('你有一条新通知')} />
              </div>
            </div>
          )}

          <div className="form-group row pb-1">
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('内容')}</label>
            <div className="col-12 col-lg-8">
              <textarea className="form-control form-control-sm row3x" ref={(c) => (this._content = c)} maxLength="600" />
              <p
                className="form-text"
                dangerouslySetInnerHTML={{ __html: $L('内容支持内置变量，内置变量如 `{createdOn}` (其中 createdOn 为触发实体的字段内部标识)') }}
              />
            </div>
          </div>
        </form>
      </div>
    )
  }

  componentDidMount() {
    $.get('/admin/robot/trigger/sendnotification-atypes', (res) => this.setState({ ...res.data }))

    const content = this.props.content
    if (content) {
      if (content.sendTo) {
        $.post(`/commons/search/user-selector?entity=${this.props.sourceEntity}`, JSON.stringify(content.sendTo), (res) => {
          if (res.error_code === 0 && res.data.length > 0) this._sendTo.setState({ selected: res.data })
        })
      }

      this.setState({ type: content.type ? content.type : 1 }, () => {
        $(this._title).val(content.title || '')
        $(this._content).val(content.content || '')
      })
    }
  }

  buildContent() {
    const _data = {
      type: this.state.type,
      sendTo: this._sendTo.getSelected(),
      title: $(this._title).val(),
      content: $(this._content).val(),
    }
    if (!_data.sendTo || _data.sendTo.length === 0) {
      RbHighbar.create($L('请选择发送给谁'))
      return false
    }
    if (!_data.content) {
      RbHighbar.create($L('内容不能为空'))
      return false
    }
    return _data
  }
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  renderRbcomp(<ContentSendNotification {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
  })
}
