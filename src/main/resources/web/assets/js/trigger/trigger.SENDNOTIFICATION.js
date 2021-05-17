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
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('NotifyType')}</label>
            <div className="col-12 col-lg-8 pt-1">
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input" name="mtype" type="radio" onChange={() => this.setState({ type: 1 })} checked={this.state.type === 1} />
                <span className="custom-control-label">{$L('Notification')}</span>
              </label>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input" name="mtype" type="radio" onChange={() => this.setState({ type: 2 })} checked={this.state.type === 2} />
                <span className="custom-control-label">
                  {$L('Mail')} {this.state.serviceMail === false && `(${$L('Unavailable')})`}
                </span>
              </label>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input" name="mtype" type="radio" onChange={() => this.setState({ type: 3 })} checked={this.state.type === 3} />
                <span className="custom-control-label">
                  {$L('Sms')} {this.state.serviceSms === false && `(${$L('Unavailable')})`}
                </span>
              </label>
            </div>
          </div>
          <div className="form-group row pt-1">
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('SendToWho')}</label>
            <div className="col-12 col-lg-8">
              <UserSelectorWithField ref={(c) => (this._sendTo = c)} />
            </div>
          </div>
          {this.state.type === 2 && (
            <div className="form-group row pb-1">
              <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('EmailSubject')}</label>
              <div className="col-12 col-lg-8">
                <input type="text" className="form-control form-control-sm" ref={(c) => (this._title = c)} maxLength="60" placeholder={$L('YouHave1Notify')} />
              </div>
            </div>
          )}
          <div className="form-group row pb-1">
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('Content')}</label>
            <div className="col-12 col-lg-8">
              <textarea className="form-control form-control-sm row3x" ref={(c) => (this._content = c)} maxLength="600"></textarea>
              <p className="form-text" dangerouslySetInnerHTML={{ __html: $L('NotifyContentTips') }}></p>
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
      RbHighbar.create($L('请选择,SendToWho'))
      return false
    }
    if (!_data.content) {
      RbHighbar.create($L('SomeNotEmpty,Content'))
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
