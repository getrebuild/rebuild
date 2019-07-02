
// ~~ 发送通知
// eslint-disable-next-line no-unused-vars
// eslint-disable-next-line 
class ContentSendNotification extends ActionContentSpec {
  constructor(props) {
    super(props)
  }
  render() {
    return <div className="send-notification">
      <form className="simple">
        <div className="form-group row pt-1">
          <label className="col-12 col-lg-2 col-form-label text-lg-right">发送给谁</label>
          <div className="col-12 col-lg-8">
            <UserSelectorExt ref={(c) => this._sendTo = c} />
          </div>
        </div>
        <div className="form-group row pb-1">
          <label className="col-12 col-lg-2 col-form-label text-lg-right">发送内容</label>
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
    if (!_data.sendTo || _data.sendTo.length === 0) { rb.highbar('请选择发送给谁'); return false }
    if (!_data.content) { rb.highbar('发送内容不能为空'); return false }
    return _data
  }
}
class UserSelectorExt extends UserSelector {
  constructor(props) {
    super(props)
    this.tabTypes.push(['FIELDS', '使用字段'])
  }
  componentDidMount() {
    super.componentDidMount()

    this.__fields = []
    $.get(`${rb.baseUrl}/commons/metadata/fields?deep=2&entity=${wpc.sourceEntity}`, (res) => {
      $(res.data).each((idx, item) => {
        if (item.type === 'REFERENCE' && item.ref && (item.ref[0] === 'User' || item.ref[0] === 'Department' || item.ref[0] === 'Role')) {
          this.__fields.push({ id: item.name, text: item.label })
        }
      })
    })
  }
  switchTab(type) {
    type = type || this.state.tabType
    if (type === 'FIELDS') {
      const q = this.state.query
      const cacheKey = type + '-' + q
      this.setState({ tabType: type, items: this.cached[cacheKey] }, () => {
        if (!this.cached[cacheKey]) {
          if (!q) this.cached[cacheKey] = this.__fields
          else {
            let fs = []
            $(this.__fields).each(function () {
              if (this.text.contains(q)) fs.push(this)
            })
            this.cached[cacheKey] = fs
          }
          this.switchTab(type)
        }
      })
    } else {
      super.switchTab(type)
    }
  }
}