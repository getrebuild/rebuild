/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const _INSTALL_STATES = {
  10: ['zmdi-settings zmdi-hc-spin', $L('Installing')],
  11: ['zmdi-check text-success', $L('InstallSucceed')],
  12: ['zmdi-close-circle-o text-danger', $L('InstallFailed')],
}

class Setup extends React.Component {
  state = { ...this.props, stepNo: 0, installState: 10 }

  render() {
    const state = _INSTALL_STATES[this.state.installState]
    return (
      <div>
        {!this.state.stepNo && <RbWelcome $$$parent={this} />}
        {this.state.stepNo === 2 && <DatabaseConf {...this.state.databaseProps} $$$parent={this} />}
        {this.state.stepNo === 3 && <CacheConf {...this.state.cacheProps} $$$parent={this} />}
        {this.state.stepNo === 4 && <AdminConf {...this.state.adminProps} $$$parent={this} />}
        {this.state.stepNo === 5 && <ModelConf {...this.state.modelProps} $$$parent={this} />}
        {this.state.stepNo === 10 && (
          <div>
            <div className="rb-finish text-center">
              <div>
                <i className={`zmdi icon ${state[0]}`}></i>
              </div>
              <h2 className="mb-0">{state[1]}</h2>
              {this.state.installState === 11 && (
                <a className="btn btn-secondary mt-3" href="../user/login">
                  {$L('LoginNow')}
                </a>
              )}
              {this.state.installState === 12 && (
                <a className="btn btn-secondary mt-3" href="install">
                  {$L('Retry')}
                </a>
              )}
              {this.state.installState === 12 && this.state.installError && (
                <div className="alert alert-danger alert-icon alert-icon-border alert-sm mt-5 mb-0 text-left">
                  <div className="icon">
                    <span className="zmdi zmdi-close-circle-o"></span>
                  </div>
                  <div className="message">{this.state.installError}</div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    )
  }

  install = () => {
    const data = {
      installType: this.state.installType || 1,
      databaseProps: this.state.databaseProps || {},
      cacheProps: this.state.cacheProps || {},
      adminProps: this.state.adminProps || {},
      modelProps: this.state.modelProps || {},
    }
    this.setState({ installState: 10 })
    $.post('/setup/install-rebuild', JSON.stringify(data), (res) => {
      this.setState({ installState: res.error_code === 0 ? 11 : 12, installError: res.error_msg })
    })
  }
}

// ~
class RbWelcome extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="rb-welcome pb-1">
        <h3>{$L('SelectSome,InstallMode')}</h3>
        <ul className="list-unstyled">
          <li>
            <a onClick={() => this._next(1)}>
              <h5 className="m-0 text-bold">{$L('InstallMySql')}</h5>
              <p className="m-0 mt-1 text-muted">{$L('InstallMySqlTips')}</p>
            </a>
          </li>
          <li>
            <a onClick={() => this._next(99)}>
              <h5 className="m-0 text-bold">{$L('InstallH2')}</h5>
              <p className="m-0 mt-1 text-muted">{$L('InstallH2Tips')}</p>
            </a>
          </li>
        </ul>
      </div>
    )
  }

  // 开始安装
  _next(type) {
    const that = this
    RbAlert.create(`<div class="text-left link">${$('.license').html()}<p class="text-bold">${$L('CommercialTips')}</p></div>`, {
      html: true,
      type: 'warning',
      cancelText: $L('Disagree'),
      confirmText: $L('Agree'),
      confirm: function () {
        this.hide()
        that.props.$$$parent.setState({ installType: type, stepNo: type === 1 ? 2 : 4 })
      },
    })
  }
}

// ~
let DatabaseConf_mount = false
class DatabaseConf extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="rb-database">
        <h3>{$L('SetSome,Database')}</h3>
        <form>
          <div className="form-group row pt-0">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('DbType')}</div>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" name="dbType">
                <option value="mysql">MySQL</option>
              </select>
              <div className="form-text">{$L('DbMySqlTips')}</div>
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('Host')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="dbHost" value={this.state.dbHost || ''} onChange={this.handleValue} placeholder="127.0.0.1" />
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('Port')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="dbPort" value={this.state.dbPort || ''} onChange={this.handleValue} placeholder="3306" />
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('DbName')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="dbName" value={this.state.dbName || ''} onChange={this.handleValue} placeholder="rebuild20" />
              <div className="form-text">{$L('DbNameTips')}</div>
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('DbUser')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="dbUser" value={this.state.dbUser || ''} onChange={this.handleValue} placeholder="rebuild" />
              <div className="form-text">{$L('DbUserTips')}</div>
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('Passwd')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="dbPassword" value={this.state.dbPassword || ''} onChange={this.handleValue} placeholder="rebuild" />
            </div>
          </div>
        </form>
        <div className="progress">
          <div className="progress-bar" style={{ width: '20%' }}></div>
        </div>
        <div className="splash-footer">
          {this.state.testMessage && (
            <div className={`alert ${this.state.testState ? 'alert-success' : 'alert-danger'} alert-icon alert-icon-border text-left alert-sm`}>
              <div className="icon">
                <span className={`zmdi ${this.state.testState ? 'zmdi-check' : 'zmdi-close-circle-o'}`}></span>
              </div>
              <div className="message" dangerouslySetInnerHTML={{ __html: this.state.testMessage }}></div>
            </div>
          )}
          <button className="btn btn-link float-left text-left pl-0" onClick={this._prev}>
            <i className="zmdi zmdi-chevron-left icon" />
            {$L('SelectSome,InstallMode')}
          </button>
          <div className="float-right">
            <button className="btn btn-link text-right mr-2" disabled={this.state.inTest} onClick={this._testConnection}>
              {this.state.inTest && <i className="zmdi icon zmdi-refresh zmdi-hc-spin mr-1" />}
              {$L('TestConnection')}
            </button>
            <button className="btn btn-secondary" onClick={this._next}>
              {$L('NextStep')}
            </button>
          </div>
          <div className="clearfix"></div>
        </div>
      </div>
    )
  }

  handleValue = (e) => {
    const name = e.target.name
    const value = $(e.target).attr('type') === 'checkbox' ? $(e.target).prop('checked') : e.target.value
    this.setState({ [name]: value })
  }

  _buildProps(check) {
    const ps = {
      dbType: 'mysql',
      dbHost: this.state.dbHost || '127.0.0.1',
      dbPort: this.state.dbPort || 3306,
      dbName: this.state.dbName || 'rebuild20',
      dbUser: this.state.dbUser || 'rebuild',
      dbPassword: this.state.dbPassword || 'rebuild',
    }
    if (check && isNaN(ps.dbPort)) {
      RbHighbar.create($L('SomeInvalid,Port'))
      return
    }
    return ps
  }

  _testConnection = (call) => {
    if (this.state.inTest) return
    const ps = this._buildProps(true)
    if (!ps) return

    this.setState({ inTest: true })
    $.post('/setup/test-connection', JSON.stringify(ps), (res) => {
      let msg = res.data || res.error_msg
      if (msg.substr(0, 2) === '1#') {
        msg = msg.substr(2)
        DatabaseConf_mount = true
      } else {
        DatabaseConf_mount = false
      }
      this.setState({ inTest: false, testState: res.error_code === 0, testMessage: msg }, () => typeof call === 'function' && call(ps, res))
    })
  }

  _prev = () => this.props.$$$parent.setState({ stepNo: 0, databaseProps: this._buildProps() })
  _next = () => {
    this._testConnection((ps, res) => {
      if (res.error_code === 0) this.props.$$$parent.setState({ stepNo: 3, databaseProps: ps })
    })
  }
}

// ~
class CacheConf extends DatabaseConf {
  state = { ...this.props }

  render() {
    return (
      <div className="rb-cache">
        <h3>{$L('SetSome,CacheSrv')}</h3>
        <form>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('CacheType')}</div>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" name="cacheType" onChange={this.handleValue} defaultValue={this.props.cacheType}>
                <option value="ehcache">EHCACHE ({$L('BuiltIn')})</option>
                <option value="redis">REDIS</option>
              </select>
              {this.state.cacheType === 'redis' && <div className="form-text">{$L('CacheRedisTips')}</div>}
            </div>
          </div>
          {this.state.cacheType === 'redis' && (
            <React.Fragment>
              <div className="form-group row">
                <div className="col-sm-3 col-form-label text-sm-right">{$L('Host')}</div>
                <div className="col-sm-7">
                  <input type="text" className="form-control form-control-sm" name="CacheHost" value={this.state.CacheHost || ''} onChange={this.handleValue} placeholder="127.0.0.1" />
                </div>
              </div>
              <div className="form-group row">
                <div className="col-sm-3 col-form-label text-sm-right">{$L('Port')}</div>
                <div className="col-sm-7">
                  <input type="text" className="form-control form-control-sm" name="CachePort" value={this.state.CachePort || ''} onChange={this.handleValue} placeholder="6379" />
                </div>
              </div>
              <div className="form-group row">
                <div className="col-sm-3 col-form-label text-sm-right">{$L('Passwd')}</div>
                <div className="col-sm-7">
                  <input
                    type="text"
                    className="form-control form-control-sm"
                    name="CachePassword"
                    value={this.state.CachePassword || ''}
                    onChange={this.handleValue}
                    placeholder={$L('CacheNoPasswdTips')}
                  />
                </div>
              </div>
            </React.Fragment>
          )}
        </form>
        <div className="progress">
          <div className="progress-bar" style={{ width: '40%' }}></div>
        </div>
        <div className="splash-footer">
          {this.state.testMessage && (
            <div className={`alert ${this.state.testState ? 'alert-success' : 'alert-danger'} alert-icon alert-icon-border text-left alert-sm`}>
              <div className="icon">
                <span className={`zmdi ${this.state.testState ? 'zmdi-check' : 'zmdi-close-circle-o'}`}></span>
              </div>
              <div className="message" dangerouslySetInnerHTML={{ __html: this.state.testMessage }}></div>
            </div>
          )}
          <button className="btn btn-link float-left text-left pl-0" onClick={this._prev}>
            <i className="zmdi zmdi-chevron-left icon" />
            {$L('SetSome,Database')}
          </button>
          <div className="float-right">
            {this.state.cacheType === 'redis' && (
              <button className="btn btn-link text-right mr-2" disabled={this.state.inTest} onClick={this._testConnection}>
                {this.state.inTest && <i className="zmdi icon zmdi-refresh zmdi-hc-spin mr-1" />}
                {$L('TestConnection')}
              </button>
            )}
            <button className="btn btn-secondary" onClick={this._next}>
              {$L('NextStep')}
            </button>
          </div>
          <div className="clearfix"></div>
        </div>
      </div>
    )
  }

  _buildProps(check) {
    if (this.state.cacheType !== 'redis') return {}
    const ps = {
      cacheType: 'redis',
      CacheHost: this.state.CacheHost || '127.0.0.1',
      CachePort: this.state.CachePort || 6379,
      CachePassword: this.state.CachePassword || '',
    }
    if (check && isNaN(ps.CachePort)) {
      RbHighbar.create($L('SomeInvalid,Port'))
      return
    }
    return ps
  }

  _testConnection = (call) => {
    if (this.state.inTest) return
    const ps = this._buildProps(true)
    if (!ps) return

    this.setState({ inTest: true })
    $.post('/setup/test-cache', JSON.stringify(ps), (res) => {
      this.setState({ inTest: false, testState: res.error_code === 0, testMessage: res.data || res.error_msg }, () => typeof call === 'function' && call(ps, res))
    })
  }

  _prev = () => this.props.$$$parent.setState({ stepNo: 2, cacheProps: this._buildProps() })
  _next = () => {
    if (this.state.cacheType === 'redis') {
      this._testConnection((ps, res) => {
        if (res.error_code === 0) this.props.$$$parent.setState({ stepNo: 4, cacheProps: ps })
      })
    } else {
      this.props.$$$parent.setState({ stepNo: 4, cacheProps: {} })
    }
  }
}

// ~
class AdminConf extends DatabaseConf {
  state = { ...this.props }

  render() {
    return (
      <div className="rb-admin">
        <h3>{$L('SetSome,SuperAdmin')}</h3>
        <form>
          <div className="form-group row pt-0">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('AdminPasswd')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="adminPasswd" value={this.state.adminPasswd || ''} onChange={this.handleValue} placeholder="admin" />
              <div className="form-text">
                {$L('DefaultPasswd')} <code className="text-danger">admin</code>
              </div>
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('AdminEmail')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="adminMail" value={this.state.adminMail || ''} onChange={this.handleValue} placeholder="(选填)" />
              <div className="form-text">{$L('AdminEmailTips')}</div>
            </div>
          </div>
        </form>
        <div className="progress">
          <div className="progress-bar" style={{ width: '60%' }}></div>
        </div>
        <div className="splash-footer">
          {this.props.$$$parent.state.installType === 1 && (
            <button className="btn btn-link float-left text-left pl-0" onClick={() => this._prev(3)}>
              <i className="zmdi zmdi-chevron-left icon" />
              {$L('SetSome,CacheSrv')}
            </button>
          )}
          {this.props.$$$parent.state.installType === 99 && (
            <button className="btn btn-link float-left text-left pl-0" onClick={() => this._prev(0)}>
              <i className="zmdi zmdi-chevron-left icon" />
              {$L('SelectSome,InstallMode')}
            </button>
          )}
          <div className="float-right">
            <button className="btn btn-secondary" onClick={this._next}>
              {$L('NextStep')}
            </button>
          </div>
          <div className="clearfix"></div>
        </div>
      </div>
    )
  }

  _buildProps(check) {
    const ps = {
      adminPasswd: this.state.adminPasswd,
      adminMail: this.state.adminMail,
    }
    if (check && ps.adminMail && !$regex.isMail(ps.adminMail)) {
      RbHighbar.create($L('SomeInvalid,AdminEmail'))
      return
    }
    return ps
  }

  _prev = (stepNo) => this.props.$$$parent.setState({ stepNo: stepNo || 0, adminProps: this._buildProps() })
  _next = () => {
    const ps = this._buildProps(true)
    if (!ps) return
    this.props.$$$parent.setState({ stepNo: 5, adminProps: ps })
  }
}

// ~
let ModelConf_data
let ModelConf_error
class ModelConf extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
    this._$refs = {}
  }

  render() {
    const _canUse = !DatabaseConf_mount && !ModelConf_error && ModelConf_data
    return (
      <div className="rb-model">
        <h3>{$L('SelectSome,InitEntity')}</h3>
        <form>
          {_canUse &&
            ModelConf_data.map((item) => {
              return (
                <div key={item.key}>
                  <label className="custom-control custom-checkbox" title={item.desc} ref={(c) => (this._$refs[item.key] = c)}>
                    <input className="custom-control-input" type="checkbox" value={item.key} data-refs={item.refs} onClick={(e) => this._onClick(e)} />
                    <span className="custom-control-label text-bold">{item.name}</span>
                    <p>{item.desc}</p>
                  </label>
                </div>
              )
            })}
        </form>
        {_canUse && <p className="mt-1 mb-1 protips">{$L('SelectInitEntityTips')}</p>}
        {DatabaseConf_mount && (
          <div className="mb-6">
            <RbAlertBox message={$L('CantSelectInitEntityTips')} />
          </div>
        )}
        {!DatabaseConf_mount && ModelConf_error && (
          <div className="mb-6">
            <RbAlertBox message={ModelConf_error} />
          </div>
        )}
        <div className="clearfix"></div>
        <div className="progress">
          <div className="progress-bar" style={{ width: '80%' }}></div>
        </div>
        <div className="splash-footer">
          <button className="btn btn-link float-left text-left pl-0" onClick={() => this._prev(4)}>
            <i className="zmdi zmdi-chevron-left icon" />
            {$L('SetSome,SuperAdmin')}
          </button>
          <div className="float-right">
            <button className="btn btn-primary" onClick={this._next}>
              {$L('FinishInstall')}
            </button>
          </div>
          <div className="clearfix"></div>
        </div>
      </div>
    )
  }

  _onClick(e) {
    const $el = $(e.currentTarget)
    const refs = ($el.data('refs') || '').split(',')
    refs.forEach((s) => {
      $(this._$refs[s]).find('input').prop('checked', true)
    })
  }

  _buildProps() {
    if (ModelConf_error) return []
    const sm = []
    for (let k in this._$refs) {
      const $s = $(this._$refs[k]).find('input')
      if ($s.prop('checked')) sm.push($s.val())
    }
    return sm
  }

  _prev = () => this.props.$$$parent.setState({ stepNo: 4, modelProps: this._buildProps() })
  _next = () => {
    const ps = this._buildProps(true)
    this.props.$$$parent.setState({ stepNo: 10, modelProps: ps }, () => this.props.$$$parent.install())
  }
}

$(document).ready(() => {
  renderRbcomp(<Setup />, $('.card-body'))

  $.get('/setup/init-entity', (res) => {
    if (res.error_code === 0) ModelConf_data = res.data
    else ModelConf_error = res.error_msg
  })
})
