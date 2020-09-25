/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~ 添加成员
class MemberAddDlg extends RbFormHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$lang('AddMember')} disposeOnHide={true}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$lang('SelectSome,User')}</label>
            <div className="col-sm-7">
              <UserSelector ref={(c) => (this._UserSelector = c)} hideTeam={true} />
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3">
              <button className="btn btn-primary" type="button" onClick={this._post}>
                {$lang('Confirm')}
              </button>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  _post = () => {
    const users = this._UserSelector.val()
    if (users.length < 1) return RbHighbar.create($lang('PlsSelectSome,User'))

    this.disabled(true)
    $.post(`/admin/bizuser/team-members-add?team=${this.props.id}`, JSON.stringify(users), (res) => {
      if (res.error_code === 0) {
        this.hide()
        typeof this.props.call === 'function' && this.props.call()
      } else RbHighbar.error(res.error_msg)
    })
  }
}

// ~ 成员列表
class MemberList extends React.Component {
  state = { ...this.props }

  render() {
    if (this.state.members && this.state.members.length === 0)
      return (
        <div className="list-nodata">
          <span className="zmdi zmdi-info-outline"></span>
          <p>{$lang('PlsAddSome,TeamMember')}</p>
        </div>
      )
    return (
      <table className="table table-striped table-hover">
        <tbody>
          {(this.state.members || []).map((item) => {
            return (
              <tr key={`member-${item[0]}`}>
                <td className="user-avatar cell-detail user-info">
                  <img src={`${rb.baseUrl}/account/user-avatar/${item[0]}`} alt="Avatar" />
                  <span>{item[1]}</span>
                  <span className="cell-detail-description">{item[2] || '-'}</span>
                </td>
                <td className="actions">
                  <a className="icon" title={$lang('Delete')} onClick={() => this._removeMember(item[0])}>
                    <i className="zmdi zmdi-delete"></i>
                  </a>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    )
  }

  componentDidMount = () => this.loadMembers()

  loadMembers() {
    $.get(`/admin/bizuser/team-members?team=${this.props.id}`, (res) => this.setState({ members: res.data || [] }))
  }

  _removeMember(user) {
    let that = this
    RbAlert.create($lang('DeleteTeamMemberConfirm'), {
      confirm: function () {
        this.disabled(true)
        $.post(`/admin/bizuser/team-members-del?team=${that.props.id}&user=${user}`, (res) => {
          if (res.error_code === 0) {
            this.hide()
            that.loadMembers()
          } else RbHighbar.error(res.error_msg)
        })
      },
    })
  }
}

let memberList
$(document).ready(() => {
  const teamId = window.__PageConfig.recordId
  $('.nav-tabs a:eq(1)').click(() => {
    if (!memberList)
      renderRbcomp(<MemberList id={teamId} />, 'tab-members', function () {
        memberList = this
      })
  })
  $('.J_add-detail')
    .off('click')
    .click(() => renderRbcomp(<MemberAddDlg id={teamId} call={() => memberList && memberList.loadMembers()} />))
  $('.J_delete')
    .off('click')
    .click(() => {
      RbAlert.create($lang('DeleteTeamConfirm'), $lang('DeleteSome,Team'), {
        type: 'danger',
        confirmText: $lang('Delete'),
        confirm: function () {
          this.disabled(true)
          $.post(`/app/entity/record-delete?id=${teamId}`, (res) => {
            if (res.error_code === 0) {
              parent.location.hash = '!/View/'
              parent.location.reload()
            } else RbHighbar.error(res.error_msg)
          })
        },
      })
    })
})
