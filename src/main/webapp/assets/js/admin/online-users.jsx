/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// eslint-disable-next-line no-unused-vars
class OnlineUserViewer extends RbModalHandler {

  constructor(props) {
    super(props)
  }

  render() {
    return <RbModal ref={(c) => this._dlg = c} title="在线用户" disposeOnHide={true}>
      <table className="table table-hover table-sm mb-0">
        <thead>
          <tr>
            <th width="30%">用户</th>
            <th>最近活跃</th>
            <th width="80"></th>
          </tr>
        </thead>
        <tbody>
          {(this.state.users || []).map((item) => {
            return (
              <tr key={`user-${item.user}`}>
                <td className="user-avatar">
                  <img src={`${rb.baseUrl}/account/user-avatar/${item.user}`} />
                  <span>{item.fullName}</span>
                </td>
                <td>
                  <a href="###" className="text-break">{item.activeUrl || '无'}</a>
                  <p className="text-muted fs-12 m-0">{item.activeTime}</p>
                </td>
                <td className="text-right">
                  <button className="btn btn-danger bordered btn-sm" type="button" onClick={() => this._killSession(item.user)}>强退</button>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </RbModal >
  }

  componentDidMount() {
    this._load()
  }

  _load() {
    $.get('/admin/bizuser/online-users', (res) => {
      if (res.error_code === 0) this.setState({ users: res.data })
      else RbHighbar.error(res.error_msg)
    })
  }

  _killSession(user) {
    const that = this
    RbAlert.create('确认强制退出该用户？', {
      confirm: function () {
        $.post(`/admin/bizuser/kill-session?user=${user}`, () => {
          this.hide()
          that._load()
        })
      }
    })
  }
}