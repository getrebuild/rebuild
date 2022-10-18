/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(function () {
  $('.J_add').on('click', () => renderRbcomp(<DlgEdit />))
  renderRbcomp(<AppList />, 'appList')
})

class AppList extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, secretShows: [] }
  }

  render() {
    return (
      <React.Fragment>
        {(this.state.list || []).map((item) => {
          let secret = item[2].substr(0, 8) + '...' + item[2].substr(32)
          secret = (
            <a href="#" title={$L('点击显示')} onClick={() => this.showSecret(item[2])}>
              {secret}
            </a>
          )
          if (this.state.secretShows.includes(item[2])) secret = item[2]

          return (
            <tr key={'api-' + item[0]}>
              <td>{item[1]}</td>
              <td>{secret}</td>
              <td>{item[4] || $L('无 (拥有全部权限)')}</td>
              <td>{item[6] || 0}</td>
              <td>
                <DateShow date={item[5]} />
              </td>
              <td className="actions">
                <a className="icon danger-hover" onClick={() => this.delete(item)}>
                  <i className="zmdi zmdi-delete" />
                </a>
              </td>
            </tr>
          )
        })}
      </React.Fragment>
    )
  }

  componentDidMount = () => this._componentDidMount()

  _componentDidMount() {
    $.get('/admin/apis-manager/app-list', (res) => {
      const _data = res.data || []
      this.setState({ list: _data }, () => {
        $('.rb-loading-active').removeClass('rb-loading-active')
        $('.dataTables_info').text($L('共 %d 项', _data.length))
        if (_data.length === 0) $('.list-nodata').removeClass('hide')
      })
    })
  }

  showSecret(s) {
    event.preventDefault()
    const shows = this.state.secretShows
    shows.push(s)
    this.setState({ secretShows: shows })
  }

  delete(app) {
    const that = this
    RbAlert.create($L('删除后，使用此 API 秘钥的第三方应用功能将会失败'), {
      type: 'danger',
      confirmText: $L('删除'),
      confirm: function () {
        this.disabled(true)
        $.post(`/admin/apis-manager/app-delete?id=${app[0]}`, (res) => {
          if (res.error_code === 0) {
            RbHighbar.success($L('删除成功'))
            that._componentDidMount()
            this.hide()
          } else {
            RbHighbar.error(res.error_msg)
          }
        })
      },
    })
  }
}

class DlgEdit extends RbFormHandler {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    return (
      <RbModal title={$L('添加 API 秘钥')} ref={(c) => (this._dlg = c)}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('绑定用户 (权限)')}</label>
            <div className="col-sm-7">
              <UserSelector hideDepartment={true} hideRole={true} hideTeam={true} multiple={false} ref={(c) => (this._UserSelector = c)} />
              <p className="form-text mb-0">{$L('强烈建议为 API 秘钥绑定一个用户，此秘钥将拥有和其一样的权限。如不绑定则拥有全部权限')}</p>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={this.save}>
                {$L('确定')}
              </button>
              <a className="btn btn-link" onClick={this.hide}>
                {$L('取消')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  save = () => {
    const bindUser = this._UserSelector.val()
    this.disabled(true)
    $.post(`/admin/apis-manager/app-create?bind=${bindUser || ''}`, (res) => {
      if (res.error_code === 0) location.reload()
      else RbHighbar.error(res.error_msg)
      this.disabled()
    })
  }
}
