$(document).ready(function () {
  $('.J_add').click(() => { renderRbcomp(<DlgEdit />) })
  renderRbcomp(<AppList />, 'appList')
})

class AppList extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, secretShows: [] }
  }

  render() {
    return <React.Fragment>
      {(this.state.list || []).map((item) => {
        let secret = item[2].substr(0, 8) + '...' + item[2].substr(32)
        secret = <a href="#" title="点击显示" onClick={() => this.showSecret(item[2])}>{secret}</a>
        if (this.state.secretShows.contains(item[2])) secret = item[2]

        return <tr key={'api-' + item[0]}>
          <td>{item[1]}</td>
          <td>{secret}</td>
          <td>{item[4] || '无 (拥有全部权限)'}</td>
          <td>{item[5]}</td>
          <td>{item[6] || 0}</td>
          <td className="actions"><a className="icon" onClick={() => this.delete(item[0])}><i className="zmdi zmdi-delete" /></a></td>
        </tr>
      })}
    </React.Fragment>
  }

  componentDidMount() {
    $.get(`${rb.baseUrl}/admin/apis-manager/app-list`, (res) => {
      this.setState({ list: res.data || [] }, () => {
        $('.rb-loading-active').removeClass('rb-loading-active')
        $('.dataTables_info').text(`共 ${this.state.list.length} 个 API 秘钥`)
        if (this.state.list.length === 0) $('.list-nodata').removeClass('hide')
      })
    })
  }

  showSecret(s) {
    event.preventDefault()
    let shows = this.state.secretShows
    shows.push(s)
    this.setState({ secretShows: shows })
  }

  delete(id) {
    RbAlert.create('删除后，使用此 API 秘钥的第三方应用功能将会失败', {
      type: 'danger',
      confirmText: '删除',
      confirm: function () {
        this.disabled(true)
        $.post(`${rb.baseUrl}/admin/apis-manager/app-delete?id=${id}`, (res) => {
          if (res.error_code === 0) location.reload()
          else RbHighbar.error(res.error_msg)
        })
      }
    })
  }
}

class DlgEdit extends RbFormHandler {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }
  render() {
    return (<RbModal title="添加 API 秘钥管理" ref={(c) => this._dlg = c}>
      <div className="form">
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">绑定用户</label>
          <div className="col-sm-7">
            <select className="form-control form-control-sm" ref={(c) => this._user = c}></select>
            <p className="form-text mb-0">为 API 秘钥绑定一个用户，此秘钥将拥有和其一样的权限。如不绑定则拥有全部权限</p>
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3">
            <button className="btn btn-primary" type="button" onClick={this.save}>确定</button>
          </div>
        </div>
      </div>
    </RbModal>)
  }
  componentDidMount() {
    this._select2 = $initUserSelect2(this._user)
  }
  save = () => {
    let bindUser = this._select2.val()
    this.disabled(true)
    $.post(`${rb.baseUrl}/admin/apis-manager/app-create?bind=${bindUser || ''}`, (res) => {
      if (res.error_code === 0) {
        location.reload()
      } else RbHighbar.error(res.error_msg)
      this.disabled()
    })
  }
}