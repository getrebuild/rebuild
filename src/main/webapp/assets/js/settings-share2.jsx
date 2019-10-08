/* eslint-disable react/prop-types */
class _ChangeHandler extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  handleChange = (e) => {
    let target = e.target
    let val = target.type === 'checkbox' ? target.checked : target.value
    let s = {}
    s[target.name] = val
    this.setState(s)
  }
}

const SHARE_ALL = 'ALL'
const SHARE_SELF = 'SELF'

// ~~ 共享组件
class Share2 extends _ChangeHandler {
  constructor(props) {
    super(props)
    if (props.shareTo && props.shareTo !== SHARE_SELF) this.state.shared = true
  }

  render() {
    if (!rb.isAdminUser) return null
    return <React.Fragment>
      <div className="float-left">
        <div className="btn-group">
          <button type="button" className="btn btn-link" data-toggle="dropdown"><i className="zmdi zmdi-settings icon"></i></button>
          <div className="dropdown-menu">
            <a className="dropdown-item" onClick={this.showSwitch}>切换{this.props.title || '配置'}</a>
            <a className="dropdown-item" href="?id=NEW">新增{this.props.title || '配置'}</a>
          </div>
        </div>
      </div>
      <label className="custom-control custom-checkbox custom-control-inline">
        <input className="custom-control-input" type="checkbox" checked={this.state.shared === true} name="shared" onChange={this.handleChange} />
        {(this.state.shareTo && this.state.shareTo.length > 10) ?
          <span className="custom-control-label">共享给 <a href="javascript:;" onClick={() => { return this.showSettings() }}>指定用户({this.state.shareTo.split(',').length})</a></span>
          :
          <span className="custom-control-label">共享给全部用户或 <a href="javascript:;" onClick={() => { return this.showSettings() }}>指定用户</a></span>
        }
      </label>
    </React.Fragment>
  }

  showSwitch = () => {
    let that = this
    if (that.__switch) that.__switch.show()
    else renderRbcomp(<Share2Switch modalClazz="select-list" list={this.props.list} />, null, function () { that.__switch = this })
  }

  showSettings() {
    let that = this
    if (that.__settings) that.__settings.show()
    else renderRbcomp(<Share2Settings configName={this.state.configName} shareTo={this.state.shareTo} call={this.showSettingsCall} />, null, function () { that.__settings = this })
    return false
  }
  showSettingsCall = (data) => {
    this.setState({ ...data, shared: true })
  }

  getData() {
    let st = this.state.shareTo
    if (this.state.shared && st === SHARE_SELF) st = SHARE_ALL
    else if (!this.state.shared && st !== SHARE_SELF) st = SHARE_SELF
    return { configName: this.state.configName, shareTo: st }
  }
}

// ~~ 多配置切换
class Share2Switch extends _ChangeHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return <div className={`modal rbalert shareTo--box ${this.props.modalClazz || ''}`} ref={(c) => this._dlg = c} tabIndex="-1">
      <div className="modal-dialog">
        <div className="modal-content">
          <div className="modal-header pb-0">
            <button className="close" type="button" onClick={this.hide}><span className="zmdi zmdi-close" /></button>
          </div>
          <div className="modal-body">
            {this.renderContent()}
          </div>
        </div>
      </div>
    </div>
  }

  renderContent() {
    return <div ref={s => this._scrollbar = s}>
      <ul className="list-unstyled nav-list">
        {(this.props.list || []).map((item) => {
          let st = item[2] === SHARE_ALL ? '全部用户' : (item[2] === SHARE_SELF ? '私有' : `指定用户 (${item[2].split(',').length})`)
          return <li key={'item-' + item[0]}><a href={'?id=' + item[0]}>{item[1] || '默认'}<span className="float-right">{st}</span></a></li>
        })}
      </ul>
    </div>
  }

  componentDidMount() {
    $(this._dlg).modal({ show: true, keyboard: true })
  }
  hide = () => $(this._dlg).modal('hide')
  show = () => $(this._dlg).modal('show')
}

// ~~ 配置共享
class Share2Settings extends Share2Switch {
  constructor(props) {
    super(props)
  }

  renderContent() {
    return <div className="form">
      <div className="form-group">
        <label className="text-bold">共享给</label>
        {this.state.selected && <UserSelector ref={(c) => this._selector = c} selected={this.state.selected} />}
      </div>
      <div className="form-group">
        <input type="text" className="form-control form-control-sm" placeholder="输入共享名称" value={this.state.name || ''} name="name" onChange={this.handleChange} />
      </div>
      <div className="form-group mb-1">
        <button className="btn btn-primary btn-space" type="button" onClick={this.checkData}>确定</button>
      </div>
    </div>
  }

  componentDidMount() {
    super.componentDidMount()

    const p = this.props
    if (p.shareTo && p.shareTo.length > 10) {
      $.post(`${rb.baseUrl}/commons/search/user-selector`, JSON.stringify(p.shareTo.split(',')), (res) => {
        if (res.error_code === 0 && res.data.length > 0) this.setState({ selected: res.data })
        else this.setState({ selected: [] })
      })
    } else {
      this.setState({ selected: [] })
    }
  }

  getData() {
    let s = this._selector.getSelected()
    return {
      configName: this.state.name,
      shareTo: s.length > 0 ? s.join(',') : SHARE_ALL
    }
  }
  checkData = () => {
    this.hide()
    typeof this.props.call === 'function' && this.props.call(this.getData())
  }
}