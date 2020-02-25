/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

class _ChangeHandler extends React.Component {

  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  handleChange = (e) => {
    const target = e.target
    const val = target.type === 'checkbox' ? target.checked : target.value
    let s = {}
    s[target.name] = val
    this.setState(s)
  }
}

const SHARE_ALL = 'ALL'
const SHARE_SELF = 'SELF'

// ~~ 共享组件
// eslint-disable-next-line no-unused-vars
class Share2 extends _ChangeHandler {

  constructor(props) {
    super(props)
    if (props.shareTo && props.shareTo !== SHARE_SELF) this.state.shared = true
  }

  render() {
    if (!rb.isAdminUser) return null
    return <React.Fragment>
      {this.props.noSwitch !== true &&
        <div className="float-left">
          <div className="btn-group">
            <button type="button" className="btn btn-link" data-toggle="dropdown"><i className="zmdi zmdi-settings icon"></i></button>
            <div className="dropdown-menu">
              <a className="dropdown-item" href={`?id=NEW&entity=${this.props.entity || ''}`}>添加{this.props.title || '配置'}</a>
              <a className="dropdown-item" onClick={this.showSwitch}>切换{this.props.title || '配置'}</a>
            </div>
          </div>
        </div>}
      <label className="custom-control custom-checkbox custom-control-inline custom-control-sm">
        <input className="custom-control-input" type="checkbox" checked={this.state.shared === true} name="shared" onChange={this.handleChange} />
        {(this.state.shareTo && this.state.shareTo.length >= 20) ?
          <span className="custom-control-label">共享给 <a href="#" onClick={this.showSettings}>指定用户({this.state.shareTo.split(',').length})</a></span>
          :
          <span className="custom-control-label">共享给全部用户或 <a href="#" onClick={this.showSettings}>指定用户</a></span>
        }
      </label>
    </React.Fragment>
  }

  showSwitch = () => {
    const that = this
    if (that.__switch) that.__switch.show()
    else renderRbcomp(<Share2Switch modalClazz="select-list" list={this.props.list} entity={this.props.entity} id={this.props.id} />, null, function () { that.__switch = this })
  }

  showSettings = () => {
    event.preventDefault()
    const that = this
    if (that.__settings) that.__settings.show()
    else renderRbcomp(<Share2Settings configName={this.props.configName} shareTo={this.props.shareTo} call={this.showSettingsCall} id={this.props.id} noName={this.props.noSwitch} />, null, function () { that.__settings = this })
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
    const list = this.props.list || []
    return <div className="rb-scroller" ref={s => this._scrollbar = s}>
      <ul className="list-unstyled nav-list">
        {list.map((item) => {
          let st = item[2] === SHARE_ALL ? '全部用户' : (item[2] === SHARE_SELF ? '私有' : `指定用户(${item[2].split(',').length})`)
          if (this.props.id === item[0]) st += ' [当前]'
          return <li key={'item-' + item[0]}><a href={`?id=${item[0]}&entity=${this.props.entity || ''}`}>{item[1] || '未命名'}<span className="muted">{st}</span></a></li>
        })}
      </ul>
      {list.length === 0 && <p className="text-muted">暂无配置</p>}
    </div>
  }

  componentDidMount() {
    $(this._dlg).modal({ show: true, keyboard: true })
    if (this._scrollbar) $(this._scrollbar).perfectScrollbar()
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
        <UserSelector ref={(c) => this._selector = c} selected={this.state.selected} />
        <p className="form-text">可以共享给不同的角色或职能部门，便于统一管理</p>
      </div>
      {this.props.noName !== true &&
        <div className="form-group">
          <input type="text" className="form-control form-control-sm" placeholder="输入共享名称" value={this.state.configName || ''} name="configName" onChange={this.handleChange} />
        </div>}
      <div className="form-group mb-1">
        <button className="btn btn-primary btn-space" type="button" onClick={this.checkData}>确定</button>
        {this.props.id && <button className="btn btn-danger bordered btn-space" type="button" onClick={this.delete}><i className="zmdi zmdi-delete icon" /> 删除</button>}
      </div>
    </div>
  }

  componentDidMount() {
    super.componentDidMount()

    const p = this.props
    if (p.shareTo && p.shareTo.length > 10) {
      $.post(`${rb.baseUrl}/commons/search/user-selector`, JSON.stringify(p.shareTo.split(',')), (res) => {
        if (res.error_code === 0 && res.data.length > 0) this._selector.setState({ selected: res.data })
      })
    }
  }

  getData() {
    const s = this._selector.getSelected()
    return {
      configName: this.state.configName,
      shareTo: s.length > 0 ? s.join(',') : SHARE_ALL
    }
  }

  checkData = () => {
    this.hide()
    typeof this.props.call === 'function' && this.props.call(this.getData())
  }

  delete = () => {
    const id = this.props.id
    RbAlert.create('确认删除此配置？', {
      confirm: function () {
        this.disabled(true)
        $.post(`${rb.baseUrl}/app/entity/record-delete?id=${id}`, () => parent.location.reload())
      }
    })
  }
}

// 普通用户切换设置
// eslint-disable-next-line no-unused-vars
const renderSwitchButton = (data, title, current) => {
  if (!data || data.length === 0) return null
  const comp = (
    <div className="float-left">
      <div className="btn-group">
        <button type="button" className="btn btn-link" data-toggle="dropdown"><i className="zmdi zmdi-swap-vertical icon"></i> {`切换${title}`}</button>
        <div className="dropdown-menu">
          {data.map((x) => {
            let name = x[1] || '未命名'
            if (x[3] === rb.currentUser) name = `我的${title}`
            else name += ' (共享的)'
            if (current && current === x[0]) name += ' [当前]'
            return <a key={`sw-${x[0]}`} className="dropdown-item" href={`?id=${x[0]}${x[4] ? (`&entity=${x[4]}`) : ''}`}>{name}</a>
          })
          }
        </div>
      </div>
    </div>
  )
  renderRbcomp(comp, 'shareTo')
}