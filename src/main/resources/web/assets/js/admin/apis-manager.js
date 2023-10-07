/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global dlgActionAfter hljs */

$(document).ready(function () {
  $('.J_add').on('click', () => renderRbcomp(<AppEdit />))
  renderRbcomp(<AppList />, 'appList')
})

class AppList extends ConfigList {
  constructor(props) {
    super(props)
    this.requestUrl = '/admin/apis-manager/app-list'
    this.state.secretShows = []
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

          return (
            <tr key={item[0]}>
              <td>{item[1]}</td>
              <td>{secret}</td>
              <td>{item[4] || $L('无 (拥有全部权限)')}</td>
              <td>{item[7] || $L('无 (不限制)')}</td>
              <td>
                <a onClick={() => renderRbcomp(<ApiLogsViewer width="1001" appid={item[1]} />)}>{item[6] || 0}</a>
              </td>
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

  handleEdit(app) {
    renderRbcomp(<AppEdit id={app[0]} bindIps={app[7]} bindUser={app[3]} />)
  }

  handleDelete(app) {
    const handle = super.handleDelete
    RbAlert.create($L('删除后，使用此 API 密钥的第三方应用功能将会失败'), {
      type: 'danger',
      confirmText: $L('删除'),
      onConfirm: function () {
        this.disabled(true)
        handle(app[0], () => dlgActionAfter(this))
      },
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
    this.title = props.id ? $L('修改 API 密钥') : $L('添加 API 密钥')
  }

  renderFrom() {
    return (
      <RF>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('绑定用户 (权限)')}</label>
          <div className="col-sm-7">
            <UserSelector hideDepartment hideRole hideTeam multiple={false} ref={(c) => (this._UserSelector = c)} defaultValue={this.props.bindUser} />
            <p className="form-text">{$L('强烈建议为 API 密钥绑定一个用户，此密钥将拥有和其一样的权限。如不绑定则拥有全部权限')}</p>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('IP 白名单')}</label>
          <div className="col-sm-7">
            <textarea className="form-control form-control-sm row2x" ref={(c) => (this._$bindIps = c)} defaultValue={this.props.bindIps} placeholder={$L('(可选)')} />
            <p className="form-text">{$L('白名单内的 IP 才可以通过此 API 密钥调用接口，如有多个 IP 请使用逗号或空格分开，留空则不限制')}</p>
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
class ApiLogsViewer extends RbModal {
  render() {
    return (
      <div
        className="modal rbmodal"
        ref={(c) => {
          this._rbmodal = c
          this._element = c
        }}>
        <div className="modal-dialog modal-xl">
          <div className="modal-content">
            <div className="modal-header">
              <h3 className="modal-title">{$L('API 调用日志')}</h3>
              <button className="close" type="button" onClick={() => this.hide()} title={$L('关闭')}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body p-0">
              <div className="logs">{this.renderLogs()}</div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  renderLogs() {
    if (!this.state.dataLogs) {
      return <RbSpinner />
    }
    if (this.state.dataLogs.length <= 0) {
      return <div className="list-nodata">{$L('暂无数据')}</div>
    }

    const dataShow = this.state.dataShow || []
    return (
      <div className="row">
        <div className="col-3">
          <div className="list-group list-group-flush" ref={(c) => (this._$list = c)}>
            {this.state.dataLogs.map((item) => {
              const respOk = this._isRespOk(item[5])
              return (
                <a
                  key={item[6]}
                  className={`list-group-item list-group-item-action d-flex justify-content-between align-items-center ${dataShow[6] === item[6] && 'active'}`}
                  onClick={() => {
                    this.setState({ dataShow: item })
                  }}>
                  <div>
                    {item[3].split('?')[0]}
                    <br />
                    <span className="text-muted fs-12">{$fromNow(dataShow[1])}</span>
                  </div>
                  <span className={`badge badge-${respOk ? 'success' : 'danger'} badge-pill`}>{respOk ? $L('成功') : $L('失败')}</span>
                </a>
              )
            })}
          </div>
        </div>
        <div className="col-9">
          <div className="logs-detail">
            <dl className="row">
              <dt className="col-sm-3">{$L('编号')}</dt>
              <dd className="col-sm-9">{dataShow[6]}</dd>
              <dt className="col-sm-3">{$L('来源 IP')}</dt>
              <dd className="col-sm-9">{dataShow[0]}</dd>
              <dt className="col-sm-3">{$L('请求时间')}</dt>
              <dd className="col-sm-9">{dataShow[1]}</dd>
              <dt className="col-sm-3">{$L('响应时间')}</dt>
              <dd className="col-sm-9">{dataShow[2]}</dd>
              <dt className="col-sm-3">{$L('请求地址')}</dt>
              <dd className="col-sm-9">{dataShow[3]}</dd>
            </dl>
            <dl className="row">
              <dt className="col-sm-12">{$L('请求数据')}</dt>
              <dd className="col-sm-12">
                <pre>
                  <code ref={(c) => (this._$code1 = c)}>{JSON.stringify(dataShow[4])}</code>
                </pre>
              </dd>
            </dl>
            <dl className="row">
              <dt className="col-sm-12">{$L('响应数据')}</dt>
              <dd className="col-sm-12">
                <pre>
                  <code ref={(c) => (this._$code2 = c)}>{JSON.stringify(dataShow[5])}</code>
                </pre>
              </dd>
            </dl>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()
    this._loadNext()
  }

  componentDidUpdate(prevProps, prevState) {
    if ((this.state.dataShow || [])[6] !== (prevState || [])[6]) {
      let c1 = hljs.highlight(JSON.stringify(this.state.dataShow[4]), { language: 'json' })
      $(this._$code1).html(c1.value)
      let c2 = hljs.highlight(JSON.stringify(this.state.dataShow[5]), { language: 'json' })
      $(this._$code2).html(c2.value)
    }
  }

  _loadNext() {
    this.__pageNo = (this.__pageNo || 0) + 1
    $.get(`/admin/apis-manager/request-logs?appid=${this.props.appid}&pn=${this.__pageNo}`, (res) => {
      const _data = res.data || []
      const dataLogs = (this.state.dataLogs || []).concat(_data)
      this.setState(
        {
          dataLogs: dataLogs,
          dataShow: this.state.dataShow || _data[0],
          showMore: _data.length >= 40,
        },
        () => {}
      )
    })
  }

  _isRespOk(resp) {
    try {
      return resp.error_code === 0
    } catch (err) {
      try {
        return resp.includes('调用成功')
      } catch (ignored) {
        // ignored
      }
    }
    return false
  }
}
