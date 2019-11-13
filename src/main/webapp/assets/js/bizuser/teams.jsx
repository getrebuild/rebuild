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
  }
}

// ~ 成员列表
class MemberList extends React.Component {
  state = { ...this.props }

  render() {
    return <table className="table table-striped table-hover">
      <tbody>
        {(this.state.members || []).map((item) => {
          return <tr key={`member-${item[0]}`}>
            <td className="user-avatar cell-detail user-info">
              <img src={`${rb.baseUrl}/account/user-avatar/${item[1]}`} alt="Avatar" />
              <span>{item[2]}</span>
              <span className="cell-detail-description">{item[3]}</span>
            </td>
          </tr>
        })}
      </tbody>
    </table>
  }

  componentDidMount = () => this.loadMembers()
  loadMembers() {
  }
}

let memberList
$(document).ready(() => {
  $('.nav-tabs a:eq(1)').click(() => {
    if (!memberList) renderRbcomp(<MemberList />, 'tab-members', function () { memberList = this })
  })
  $('.J_add-slave').off('click').click(() => renderRbcomp(<MemberAddDlg />))
})