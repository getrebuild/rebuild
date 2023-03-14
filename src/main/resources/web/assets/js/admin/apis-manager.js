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
  }

  render() {
    return (
      <React.Fragment>
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
              <td>{item[6] || 0}</td>
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
      </React.Fragment>
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
      <React.Fragment>
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
      </React.Fragment>
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
