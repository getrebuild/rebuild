/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const _INSTALL_STATES = {
  10: ['zmdi-settings zmdi-hc-spin', $L('正在安装')],
  11: ['zmdi-check text-success', $L('安装成功')],
  12: ['zmdi-close-circle-o text-danger', $L('安装失败')],
}

class Setup extends React.Component {
  state = { installState: 1 }

  render() {
    if (this.state.installState === 1) {
      return (
        <div>
          <div className="rb-welcome pb-1">
            <h3>{$L('选择系统模版')}</h3>
            <ul className="list-unstyled">
              {this.props.data.map((item) => {
                return (
                  <li key={item.file}>
                    <div className="item" onClick={() => this.install(item)}>
                      <h5 className="m-0 text-bold">{item.name}</h5>
                      <p className="m-0 mt-1 text-muted">
                        <span>{item.desc || item.name}</span>
                        {item.source && (
                          <a
                            className="ml-1 link"
                            href={item.source}
                            target="_blank"
                            onClick={(e) => {
                              $stopEvent(e, true)
                              window.open(item.source)
                            }}>
                            ...{$L('详情')}
                          </a>
                        )}
                      </p>
                    </div>
                  </li>
                )
              })}
            </ul>
          </div>
        </div>
      )
    }

    const state = _INSTALL_STATES[this.state.installState]
    return (
      <div>
        <div className="rb-finish text-center">
          <div>
            <i className={`zmdi icon ${state[0]}`} />
          </div>
          <h2>{state[1]}</h2>
          {this.state.installState === 11 && (
            <a className="btn btn-secondary mt-3" href="../user/login">
              {$L('立即登录')}
            </a>
          )}
          {this.state.installState === 12 && (
            <a className="btn btn-secondary mt-3" href="rbsystems">
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
    )
  }

  install(item) {
    const warningTip = (
      <div>
        <div className="text-bold">{WrapHtml($L('安装系统模版将清空您现有系统的所有数据，包括系统配置、业务实体、数据以及附件等。安装前强烈建议您做好系统备份。'))}</div>
      </div>
    )

    const that = this
    RbAlert.create(warningTip, {
      icon: ' mdi mdi-database-refresh-outline',
      type: 'danger',
      confirmText: $L('清空并安装'),
      countdown: 10,
      onConfirm: function () {
        this.hide()
        that.setState({ installState: 10 })
        $.post('/setup/install-rbsystem?file=' + $decode(item.file), (res) => {
          that.setState({ installState: res.error_code === 0 ? 11 : 12, installError: res.error_msg })
        })
      },
    })
  }
}

$(document).ready(() => {
  $.get('/admin/rbstore/load-index?type=rbsystems', (res) => {
    if ((res.data || []).length > 0) {
      renderRbcomp(<Setup data={res.data} />, $('.card-body'))
    } else {
      $('.card-body h2').text($L('暂无可用'))
    }
  })
})
