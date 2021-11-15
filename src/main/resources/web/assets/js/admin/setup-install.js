/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global InitModels */

const _INSTALL_STATES = {
  10: ['zmdi-settings zmdi-hc-spin', $L('正在安装')],
  11: ['zmdi-check text-success', $L('安装成功')],
  12: ['zmdi-close-circle-o text-danger', $L('安装失败')],
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
                <i className={`zmdi icon ${state[0]}`} />
              </div>
              <h2 className="mb-0">{state[1]}</h2>
              {this.state.installState === 11 && (
                <a className="btn btn-secondary mt-3" href="../user/login">
                  {$L('立即登录')}
                </a>
              )}
              {this.state.installState === 12 && (
                <a className="btn btn-secondary mt-3" href="install">
                  {$L('重试')}
                </a>
              )}
              {this.state.installState === 12 && this.state.installError && (
                <div className="alert alert-danger alert-icon alert-icon-border alert-sm mt-5 mb-0 text-left">
                  <div className="icon">
                    <i className="zmdi zmdi-close-circle-o" />
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
        <h3>{$L('选择安装模式')}</h3>
        <ul className="list-unstyled">
          <li>
            <a onClick={() => this._next(1)}>
              <h5 className="m-0 text-bold">{$L('标准安装')}</h5>
              <p className="m-0 mt-1 text-muted">{$L('以产品形式安装，用于真实生产环境')}</p>
            </a>
          </li>
          <li>
            <a onClick={() => this._next(99)}>
              <h5 className="m-0 text-bold">{$L('快速安装')}</h5>
              <p className="m-0 mt-1 text-muted">{$L('将使用内置数据库执行安装，仅用于评估演示 (部分功能可能无法使用)')}</p>
            </a>
          </li>
        </ul>
      </div>
    )
  }

  // 开始安装
  _next(type) {
    const commercialTip = (
      <div className="text-left link">
        <div dangerouslySetInnerHTML={{ __html: $('.license').html() }} />
        <div dangerouslySetInnerHTML={{ __html: $L('如果用于商业用途，请注意使用目的。访问 [REBUILD 官网](https://getrebuild.com/#pricing-plans) 了解更多信息。') }} className="text-bold" />
      </div>
    )

    const that = this
    RbAlert.create(commercialTip, {
      type: 'warning',
      cancelText: $L('不同意'),
      confirmText: $L('同意'),
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
        <h3>{$L('设置数据库')}</h3>
        <form>
          <div className="form-group row pt-0">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('数据库类型')}</div>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" name="dbType">
                <option value="mysql">MySQL</option>
              </select>
              <div className="form-text">{$L('支持 MySQL 5.5 或以上版本')}</div>
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('主机')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="dbHost" value={this.state.dbHost || ''} onChange={this.handleValue} placeholder="127.0.0.1" />
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('端口')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="dbPort" value={this.state.dbPort || ''} onChange={this.handleValue} placeholder="3306" />
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('数据库名称')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="dbName" value={this.state.dbName || ''} onChange={this.handleValue} placeholder="rebuild20" />
              <div className="form-text">{$L('如数据库不存在系统将自动创建')}</div>
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('用户')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="dbUser" value={this.state.dbUser || ''} onChange={this.handleValue} placeholder="rebuild" />
              <div className="form-text">{$L('请赋予用户除管理员权限以外的所有权限')}</div>
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('密码')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="dbPassword" value={this.state.dbPassword || ''} onChange={this.handleValue} placeholder="rebuild" />
            </div>
          </div>
        </form>
        <div className="progress">
          <div className="progress-bar" style={{ width: '20%' }} />
        </div>
        <div className="splash-footer">
          {this.state.testMessage && (
            <div className={`alert ${this.state.testState ? 'alert-success' : 'alert-danger'} alert-icon alert-icon-border text-left alert-sm`}>
              <div className="icon">
                <i className={`zmdi ${this.state.testState ? 'zmdi-check' : 'zmdi-close-circle-o'}`} />
              </div>
              <div className="message" dangerouslySetInnerHTML={{ __html: this.state.testMessage }} />
            </div>
          )}
          <button className="btn btn-link float-left text-left pl-0" onClick={this._prev}>
            <i className="zmdi zmdi-chevron-left icon" />
            {$L('选择安装模式')}
          </button>
          <div className="float-right">
            <button className="btn btn-link text-right mr-2" disabled={this.state.inTest} onClick={this._testConnection}>
              {this.state.inTest && <i className="zmdi icon zmdi-refresh zmdi-hc-spin mr-1" />}
              {$L('测试连接')}
            </button>
            <button className="btn btn-secondary" onClick={this._next}>
              {$L('下一步')}
            </button>
          </div>
          <div className="clearfix" />
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
      RbHighbar.create($L('无效端口'))
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
        <h3>{$L('设置缓存服务')}</h3>
        <form>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('缓存类型')}</div>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" name="cacheType" onChange={this.handleValue} defaultValue={this.props.cacheType}>
                <option value="ehcache">EHCACHE ({$L('内置')})</option>
                <option value="redis">REDIS</option>
              </select>
              {this.state.cacheType === 'redis' && <div className="form-text">{$L('支持 Redis 3.2 或以上版本')}</div>}
            </div>
          </div>
          {this.state.cacheType === 'redis' && (
            <React.Fragment>
              <div className="form-group row">
                <div className="col-sm-3 col-form-label text-sm-right">{$L('主机')}</div>
                <div className="col-sm-7">
                  <input type="text" className="form-control form-control-sm" name="CacheHost" value={this.state.CacheHost || ''} onChange={this.handleValue} placeholder="127.0.0.1" />
                </div>
              </div>
              <div className="form-group row">
                <div className="col-sm-3 col-form-label text-sm-right">{$L('端口')}</div>
                <div className="col-sm-7">
                  <input type="text" className="form-control form-control-sm" name="CachePort" value={this.state.CachePort || ''} onChange={this.handleValue} placeholder="6379" />
                </div>
              </div>
              <div className="form-group row">
                <div className="col-sm-3 col-form-label text-sm-right">{$L('密码')}</div>
                <div className="col-sm-7">
                  <input
                    type="text"
                    className="form-control form-control-sm"
                    name="CachePassword"
                    value={this.state.CachePassword || ''}
                    onChange={this.handleValue}
                    placeholder={$L('无密码请留空')}
                  />
                </div>
              </div>
            </React.Fragment>
          )}
        </form>
        <div className="progress">
          <div className="progress-bar" style={{ width: '40%' }} />
        </div>
        <div className="splash-footer">
          {this.state.testMessage && (
            <div className={`alert ${this.state.testState ? 'alert-success' : 'alert-danger'} alert-icon alert-icon-border text-left alert-sm`}>
              <div className="icon">
                <i className={`zmdi ${this.state.testState ? 'zmdi-check' : 'zmdi-close-circle-o'}`} />
              </div>
              <div className="message" dangerouslySetInnerHTML={{ __html: this.state.testMessage }} />
            </div>
          )}
          <button className="btn btn-link float-left text-left pl-0" onClick={this._prev}>
            <i className="zmdi zmdi-chevron-left icon" />
            {$L('设置数据库')}
          </button>
          <div className="float-right">
            {this.state.cacheType === 'redis' && (
              <button className="btn btn-link text-right mr-2" disabled={this.state.inTest} onClick={this._testConnection}>
                {this.state.inTest && <i className="zmdi icon zmdi-refresh zmdi-hc-spin mr-1" />}
                {$L('测试连接')}
              </button>
            )}
            <button className="btn btn-secondary" onClick={this._next}>
              {$L('下一步')}
            </button>
          </div>
          <div className="clearfix" />
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
      RbHighbar.create($L('无效端口'))
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
        <h3>{$L('设置超级管理员')}</h3>
        <form>
          <div className="form-group row pt-0">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('管理员密码')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="adminPasswd" value={this.state.adminPasswd || ''} onChange={this.handleValue} placeholder="admin" />
              <div className="form-text">
                {$L('默认用户名/密码均为')} <code className="text-danger">admin</code>
              </div>
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$L('管理员邮箱')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="adminMail" value={this.state.adminMail || ''} onChange={this.handleValue} placeholder={$L('(选填)')} />
              <div className="form-text">{$L('用于找回密码等重要操作，也可在安装完成后填写')}</div>
            </div>
          </div>
        </form>
        <div className="progress">
          <div className="progress-bar" style={{ width: '60%' }} />
        </div>
        <div className="splash-footer">
          {this.props.$$$parent.state.installType === 1 && (
            <button className="btn btn-link float-left text-left pl-0" onClick={() => this._prev(3)}>
              <i className="zmdi zmdi-chevron-left icon" />
              {$L('设置缓存服务')}
            </button>
          )}
          {this.props.$$$parent.state.installType === 99 && (
            <button className="btn btn-link float-left text-left pl-0" onClick={() => this._prev(0)}>
              <i className="zmdi zmdi-chevron-left icon" />
              {$L('选择安装模式')}
            </button>
          )}
          <div className="float-right">
            <button className="btn btn-secondary" onClick={this._next}>
              {$L('下一步')}
            </button>
          </div>
          <div className="clearfix" />
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
      RbHighbar.create($L('无效管理员邮箱'))
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
class ModelConf extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="rb-model">
        <h3>
          {$L('选择初始业务实体')}
          <div className="link" dangerouslySetInnerHTML={{ __html: $L('你可以选择来自 [RB 仓库](https://github.com/getrebuild/rebuild-datas/) 的业务实体使用，或在安装完成后自行添加') }} />
        </h3>

        {DatabaseConf_mount ? (
          <div className="mb-6">
            <RbAlertBox message={$L('由于使用了已存在的 REBUILD 数据库，因此此步骤不可用，你仍可以继续安装')} />
          </div>
        ) : (
          <InitModels ref={(c) => (this._InitModels = c)} />
        )}

        <div className="progress">
          <div className="progress-bar" style={{ width: '80%' }} />
        </div>
        <div className="splash-footer">
          <button className="btn btn-link float-left text-left pl-0" onClick={() => this._prev(4)}>
            <i className="zmdi zmdi-chevron-left icon" />
            {$L('设置超级管理员')}
          </button>
          <div className="float-right">
            <button className="btn btn-primary" onClick={this._next}>
              {$L('完成安装')}
            </button>
          </div>
          <div className="clearfix" />
        </div>
      </div>
    )
  }

  _buildProps() {
    return this._InitModels ? this._InitModels.getSelected() : []
  }

  _prev = () => this.props.$$$parent.setState({ stepNo: 4, modelProps: this._buildProps() })
  _next = () => {
    const ps = this._buildProps(true)
    this.props.$$$parent.setState({ stepNo: 10, modelProps: ps }, () => this.props.$$$parent.install())
  }
}

$(document).ready(() => {
  renderRbcomp(<Setup />, $('.card-body'))
})
