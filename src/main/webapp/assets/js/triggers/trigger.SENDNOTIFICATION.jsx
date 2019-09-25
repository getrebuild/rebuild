/* eslint-disable react/jsx-no-undef */
// ~~ 发送通知
// eslint-disable-next-line no-undef
class ContentSendNotification extends ActionContentSpec {
  constructor(props) {
    super(props)
  }

  render() {
    return <div className="send-notification">
      <form className="simple">
        <div className="form-group row pt-1">
          <label className="col-12 col-lg-3 col-form-label text-lg-right">发送给谁</label>
          <div className="col-12 col-lg-8">
            <UserSelectorExt ref={(c) => this._sendTo = c} />
          </div>
        </div>
        <div className="form-group row pb-1">
          <label className="col-12 col-lg-3 col-form-label text-lg-right">发送内容</label>
          <div className="col-12 col-lg-8">
            <textarea className="form-control form-control-sm row3x" ref={(c) => this._content = c} maxLength="600"></textarea>
          </div>
        </div>
      </form>
    </div>
  }

  componentDidMount() {
    if (this.props.content && this.props.content.sendTo) {
      $.post(`${rb.baseUrl}/commons/search/user-selector?entity=${this.props.sourceEntity}`, JSON.stringify(this.props.content.sendTo), (res) => {
        if (res.error_code === 0 && res.data.length > 0) this._sendTo.setState({ selected: res.data })
      })
    }
    $(this._content).val(this.props.content.content || '')
  }

  buildContent() {
    let _data = { sendTo: this._sendTo.getSelected(), content: $(this._content).val() }
    if (!_data.sendTo || _data.sendTo.length === 0) { RbHighbar.create('请选择发送给谁'); return false }
    if (!_data.content) { RbHighbar.create('发送内容不能为空'); return false }
    return _data
  }
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  // eslint-disable-next-line no-undef
  renderRbcomp(<ContentSendNotification {...props} />, 'react-content', function () { contentComp = this })
}