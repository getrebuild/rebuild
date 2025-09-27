/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global dlgActionAfter */

$(document).ready(function () {
  $('.J_add').on('click', () => renderRbcomp(<AppEdit />))
  renderRbcomp(<AppList />, 'appList')
})

class AppList extends ConfigList {
  constructor(props) {
    super(props)
    this.requestUrl = '/admin/apis-manager/app-list'
    this.state.secretShows = []
    this.state.callTimes = {}
  }

  render() {
    return (
      <RF>
        {(this.state.data || []).map((item) => {
          let secret = `${item[2].substr(0, 8)}...${item[2].substr(32)}`
          secret = (
            <a href="###" title={$L('点击显示')} onClick={(e) => this.showSecret(e, item[2])}>
              {secret}
            </a>
          )
          if (this.state.secretShows.includes(item[2])) secret = item[2]

          // 调用量异步加载
          let times = this.state.callTimes[item[1]]
          if (typeof times === 'undefined') {
            times = '...'
          } else {
            times =
              times > 0 ? (
                <a title={$L('OpenAPI 调用日志')} className="light-link" onClick={() => renderRbcomp(<AppLogsViewer title={$L('OpenAPI 调用日志')} appid={item[1]} maximize disposeOnHide useWhite />)}>
                  {times}
                </a>
              ) : (
                <span className="text-muted">0</span>
              )
          }

          return (
            <tr key={item[0]}>
              <td>{item[1]}</td>
              <td>{secret}</td>
              <td>{item[4] || $L('无 (全部权限)')}</td>
              <td>{this._formatIps(item[6]) || $L('无 (不限)')}</td>
              <td>{times}</td>
              <td>
                <DateShow date={item[5]} />
              </td>
              <td className="actions">
                <a className="icon" title={$L('重置 APP SECRET')} onClick={() => this.resetSecret(item[0])}>
                  <i className="mdi mdi-lock-reset" />
                </a>
                <a className="icon" title={$L('修改')} onClick={() => this.handleEdit(item)}>
                  <i className="zmdi zmdi-edit" />
                </a>
                <a className="icon danger-hover" title={$L('删除')} onClick={() => this.handleDelete(item)}>
                  <i className="zmdi zmdi-delete" />
                </a>
              </td>
            </tr>
          )
        })}
      </RF>
    )
  }

  _formatIps(ips) {
    if (!ips) return null
    return ips.split(/[\s,]+/).map((ip) => {
      return (
        <span className="badge badge-light" key={ip}>
          {ip}
        </span>
      )
    })
  }

  loadDataAfter() {
    this.state.data &&
      this.state.data.forEach((a) => {
        $.get(`/admin/apis-manager/request-times?appid=${a[1]}`, (res) => {
          if (res.error_code === 0) {
            const callTimes = this.state.callTimes
            callTimes[a[1]] = res.data[a[1]] || 0
            this.setState({ callTimes: callTimes })
          }
        })
      })
  }

  handleEdit(app) {
    renderRbcomp(<AppEdit id={app[0]} bindIps={app[6]} bindUser={app[3]} />)
  }

  handleDelete(app) {
    const handle = super.handleDelete
    RbAlert.create($L('删除后，使用此 OpenAPI 密钥的第三方应用功能将会失败'), {
      type: 'danger',
      confirmText: $L('删除'),
      onConfirm: function () {
        this.disabled(true)
        handle(app[0], () => dlgActionAfter(this))
      },
      countdown: 5,
    })
  }

  resetSecret(id) {
    RbAlert.create($L('重置后第三方应用需更换新的 APP SECRET 使用'), {
      type: 'danger',
      confirmText: $L('重置'),
      onConfirm: function () {
        this.disabled(true)
        $.post(`/admin/apis-manager/reset-secret?id=${id}`, () => {
          RbHighbar.success($L('APP SECRET 已重置'))
          dlgActionAfter(this)
        })
      },
      countdown: 5,
    })
  }

  showSecret(e, s) {
    $stopEvent(e, true)
    const shows = this.state.secretShows
    shows.push(s)
    this.setState({ secretShows: shows })
  }
}

class AppEdit extends ConfigFormDlg {
  constructor(props) {
    super(props)
    this.title = props.id ? $L('修改 OpenAPI 密钥') : $L('添加 OpenAPI 密钥')
  }

  renderFrom() {
    return (
      <RF>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('绑定用户 (权限)')}</label>
          <div className="col-sm-7">
            <UserSelector hideDepartment hideRole hideTeam multiple={false} ref={(c) => (this._UserSelector = c)} defaultValue={this.props.bindUser} />
            <p className="form-text">{$L('强烈建议为 OpenAPI 密钥绑定一个用户，此密钥将拥有和其一样的权限。如不绑定则拥有全部权限')}</p>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('IP 白名单')}</label>
          <div className="col-sm-7">
            <textarea className="form-control form-control-sm row2x" ref={(c) => (this._$bindIps = c)} defaultValue={this.props.bindIps} placeholder={$L('(可选)')} />
            <p className="form-text">{$L('白名单内的 IP 才可以通过此 OpenAPI 密钥调用接口，如有多个 IP 请使用逗号或空格分开，留空则不限制')}</p>
          </div>
        </div>
      </RF>
    )
  }

  confirm = () => {
    const post = {
      bindUser: (this._UserSelector.val() || [])[0] || null,
      bindIps: $val(this._$bindIps) || null,
    }

    post.metadata = {
      entity: 'RebuildApi',
      id: this.props.id || null,
    }

    this.disabled(true)
    $.post('/app/entity/common-save', JSON.stringify(post), (res) => {
      if (res.error_code === 0) dlgActionAfter(this)
      else RbHighbar.error(res.error_msg)
      this.disabled()
    })
  }
}

// ~~ LOG 查看
class AppLogsViewer extends RbModal {
  renderContent() {
    if (!this.state.dataLogs) {
      return (
        <div className="rb-loading rb-loading-active">
          <RbSpinner />
          <div style={{ minHeight: 230 }} />
        </div>
      )
    }

    const dataShow = this.state.dataShow
    return (
      <div className="modal-body m-0 p-0">
        <div className="logs">
          <div className="row">
            <div className="col-3 logs-list">
              <div className="search-logs position-relative">
                <input
                  type="text"
                  title={$L('输入关键词回车搜索')}
                  placeholder={$L('搜索')}
                  onKeyDown={(e) => {
                    if (e.keyCode === 13) {
                      this.__q = null
                      this._loadNext(true, e.target.value)
                    }
                  }}
                  maxLength="40"
                />
                <em className={`icon zmdi zmdi-search ${this.state._search && 'animated flash infinite'}`} />
              </div>
              <div className="list-group list-group-flush" ref={(c) => (this._$list = c)}>
                {this.state.dataLogs.map((item) => {
                  const respOk = this._isRespOk(item[5])
                  return (
                    <a
                      key={item[6]}
                      className={`list-group-item list-group-item-action d-flex justify-content-between align-items-center ${dataShow && dataShow[6] === item[6] && 'active'}`}
                      onClick={() => {
                        this.setState({ dataShow: item })
                      }}>
                      <div>
                        {item[3].split('?')[0]}
                        <br />
                        <span className="text-muted fs-11">{item[1].split('UTC')[0]}</span>
                      </div>
                      <span className={`badge badge-${respOk ? 'success' : 'danger'} badge-pill`}>{respOk ? $L('成功') : $L('失败')}</span>
                    </a>
                  )
                })}
              </div>
              {this.state.showMore && (
                <div className="text-center mt-3">
                  <a className="text-primary" onClick={() => this._loadNext()}>
                    {$L('加载更多')}
                  </a>
                </div>
              )}
            </div>
            <div className="col-9">
              {dataShow ? (
                <div className="logs-detail">
                  <dl className="row">
                    <dt className="col-sm-3">{$L('编号')} (X-RB-RequestId)</dt>
                    <dd className="col-sm-9">{dataShow[6]}</dd>
                    <dt className="col-sm-3">{$L('来源 IP')}</dt>
                    <dd className="col-sm-9">{dataShow[0]}</dd>
                    <dt className="col-sm-3">{$L('请求时间')}</dt>
                    <dd className="col-sm-9">{dataShow[1].substr(0, 19)}</dd>
                    <dt className="col-sm-3">{$L('响应时间')}</dt>
                    <dd className="col-sm-9">
                      {dataShow[2].substr(0, 19)}
                      <span className="badge badge-light ml-1 up-1">{$moment(dataShow[2]).diff($moment(dataShow[1]), 'seconds')}s</span>
                    </dd>
                    <dt className="col-sm-3">{$L('请求地址')}</dt>
                    <dd className="col-sm-9 text-break">{dataShow[3]}</dd>
                    <dt className="col-sm-12">{$L('请求数据')}</dt>
                    <dd className="col-sm-12">{dataShow[4] && <CodeViewport code={dataShow[4]} type="json" />}</dd>
                    <dt className="col-sm-12">{$L('响应数据')}</dt>
                    <dd className="col-sm-12 mb-0">{dataShow[5] && <CodeViewport code={dataShow[5]} type="json" />}</dd>
                  </dl>
                </div>
              ) : (
                <div className="text-muted pt-8 pb-8 text-center">
                  <p style={{ fontSize: 40 }}>
                    <i className="mdi mdi-script-text-outline text-muted" />
                  </p>
                  {$L('暂无数据')}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()
    this._loadNext()
  }

  _loadNext(reset, q) {
    if (this.state._search) return
    this.setState({ _search: true })

    this.__pageNo = (this.__pageNo || 0) + 1
    this.__q = q || this.__q
    if (reset) this.__pageNo = 1

    $.get(`/admin/apis-manager/request-logs?appid=${this.props.appid}&pn=${this.__pageNo}&q=${$encode(this.__q)}`, (res) => {
      const _data = res.data || []
      const dataLogs = reset ? _data : (this.state.dataLogs || []).concat(_data)
      this.setState(
        {
          dataLogs: dataLogs,
          dataShow: reset ? _data[0] : this.state.dataShow || _data[0],
          showMore: _data.length >= 40,
        },
        () => {}
      )
      this.setState({ _search: false })
    })
  }

  _isRespOk(resp) {
    try {
      return resp.error_code === 0
    } catch (err) {
      try {
        return resp.includes('调用成功') || resp.length >= 32767
      } catch (ignored) {
        // ignored
      }
    }
    return false
  }
}
