/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable react/jsx-no-undef */

// ~~ 发送通知
// eslint-disable-next-line no-undef
class ContentSendNotification extends ActionContentSpec {

  constructor(props) {
    super(props)
    this.state = { type: 1 }
  }

  render() {
    return <div className="send-notification">
      <form className="simple">
        <div className="form-group row pt-1">
          <label className="col-12 col-lg-3 col-form-label text-lg-right">通知类型</label>
          <div className="col-12 col-lg-8 pt-1">
            <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
              <input className="custom-control-input" name="mtype" type="radio" onChange={() => this.setState({ type: 1 })} checked={this.state.type === 1} />
              <span className="custom-control-label">消息通知</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
              <input className="custom-control-input" name="mtype" type="radio" onChange={() => this.setState({ type: 2 })} checked={this.state.type === 2} />
              <span className="custom-control-label">邮件 {this.state.serviceMail === false && '(不可用)'}</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1 hide">
              <input className="custom-control-input" name="mtype" type="radio" onChange={() => this.setState({ type: 3 })} checked={this.state.type === 3} />
              <span className="custom-control-label">短信 {this.state.serviceSms === false && '(不可用)'}</span>
            </label>
          </div>
        </div>
        <div className="form-group row pt-1">
          <label className="col-12 col-lg-3 col-form-label text-lg-right">发送给谁</label>
          <div className="col-12 col-lg-8">
            <UserSelectorExt ref={(c) => this._sendTo = c} />
          </div>
        </div>
        {this.state.type === 2 && <div className="form-group row pb-1">
          <label className="col-12 col-lg-3 col-form-label text-lg-right">邮件标题</label>
          <div className="col-12 col-lg-8">
            <input type="text" className="form-control form-control-sm" ref={(c) => this._title = c} maxLength="60" placeholder="你有一条新通知" />
          </div>
        </div>
        }
        <div className="form-group row pb-1">
          <label className="col-12 col-lg-3 col-form-label text-lg-right">内容</label>
          <div className="col-12 col-lg-8">
            <textarea className="form-control form-control-sm row3x" ref={(c) => this._content = c} maxLength="600"></textarea>
            {rb.env === 'dev' && <p className="form-text">内容支持变量，例如 <code>{'{createdOn}'}</code>（其中 createdOn 为触发实体的字段内部标识）</p>}
          </div>
        </div>
      </form>
    </div>
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
    const content = {
      type: this.state.type,
      sendTo: this._sendTo.getSelected(),
      title: $(this._title).val(),
      content: $(this._content).val()
    }
    if (!content.sendTo || content.sendTo.length === 0) { RbHighbar.create('请选择发送给谁'); return false }
    if (!content.content) { RbHighbar.create('内容不能为空'); return false }
    return content
  }
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  // eslint-disable-next-line no-undef
  renderRbcomp(<ContentSendNotification {...props} />, 'react-content', function () { contentComp = this })
}