/* eslint-disable react/prop-types */
// ~ 添加成员
class MemberAddDlg extends RbFormHandler {

  constructor(props) {
    super(props)
  }

  render() {
    return <RbModal ref={(c) => this._dlg = c} title="添加成员">
      <div className="form">
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">选择成员用户</label>
          <div className="col-sm-7">
            <UserSelector ref={(c) => this._userSelector = c} />
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3">
            <button className="btn btn-primary" type="button" onClick={this._post}>确定</button>
          </div>
        </div>
      </div>
    </RbModal>
  }

  _post = () => {
    let users = this._userSelector.val()
    if (users.length < 1) { RbHighbar.create('请选择成员用户'); return }

    this.disabled(true)
    $.post(`${rb.baseUrl}/admin/bizuser/team-members-add?team=${this.props.id}`, JSON.stringify(users), () => {
      this.hide()
      this.props.call && this.props.call()
    })
  }
}

// ~ 成员列表
class MemberList extends React.Component {
  state = { ...this.props }

  render() {
    if (this.state.members && this.state.members.length === 0) return <div className="list-nodata"><span className="zmdi zmdi-info-outline"></span><p>请添加成员</p></div>
    return <table className="table table-striped table-hover">
      <tbody>
        {(this.state.members || []).map((item) => {
          return <tr key={`member-${item[0]}`}>
            <td className="user-avatar cell-detail user-info">
              <img src={`${rb.baseUrl}/account/user-avatar/${item[0]}`} alt="Avatar" />
              <span>{item[1]}</span>
              <span className="cell-detail-description">{item[2] || '-'}</span>
            </td>
            <td className="actions">
              <a className="icon" title="移除" onClick={() => this._removeMember(item[0])}><i className="zmdi zmdi-delete"></i></a>
            </td>
          </tr>
        })}
      </tbody>
    </table>
  }

  componentDidMount = () => this.loadMembers()
  loadMembers() {
    $.get(`${rb.baseUrl}/admin/bizuser/team-members?team=${this.props.id}`, (res) => this.setState({ members: res.data || [] }))
  }

  _removeMember(user) {
    let that = this
    RbAlert.create('确认将用户移出当前团队？', {
      confirm: function () {
        this.disabled(true)
        $.post(`${rb.baseUrl}/admin/bizuser/team-members-del?team=${that.props.id}&user=${user}`, () => {
          this.hide()
          that.loadMembers()
        })
      }
    })
  }
}

let memberList

$(document).ready(() => {
  const teamId = window.__PageConfig.recordId
  $('.nav-tabs a:eq(1)').click(() => {
    if (!memberList) renderRbcomp(<MemberList id={teamId} />, 'tab-members', function () { memberList = this })
  })
  $('.J_add-slave').off('click').click(() => renderRbcomp(<MemberAddDlg id={teamId} call={() => memberList && memberList.loadMembers()} />))
})