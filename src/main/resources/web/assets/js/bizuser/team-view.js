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
      <RbModal ref={(c) => (this._dlg = c)} title={$L('AddMember')} disposeOnHide={true}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('选择,User')}</label>
            <div className="col-sm-7">
              <UserSelector ref={(c) => (this._UserSelector = c)} hideTeam={true} />
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3">
              <button className="btn btn-primary" type="button" onClick={() => this.post()}>
                {$L('确定')}
              </button>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  post = () => {
    const users = this._UserSelector.val()
    if (users.length < 1) return RbHighbar.create($L('请选择,User'))

    this.disabled(true)
    $.post(`/admin/bizuser/team-members-add?team=${this.props.id}`, JSON.stringify(users), (res) => {
      if (res.error_code === 0) {
        this.hide()
        typeof this.props.call === 'function' && this.props.call()
        RbHighbar.success($L('SomeAdded,TeamMember'))
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

// ~ 成员列表
class MemberList extends React.Component {
  state = { ...this.props }

  render() {
    if (this.state.members && this.state.members.length === 0) {
      return (
        <div className="list-nodata">
          <span className="zmdi zmdi-info-outline"></span>
          <p>{$L('PlsAddSome,TeamMember')}</p>
        </div>
      )
    }

    const depts = {}
    this.state.members &&
      this.state.members.forEach((item) => {
        if (!item[2]) return
        depts[item[2]] = (depts[item[2]] || 0) + 1
      })

    return (
      <div className="row">
        <div className="col-9">
          <table className="table table-striped table-hover">
            <tbody>
              {(this.state.members || []).map((item) => {
                if (this.state.activeDept && this.state.activeDept !== item[2]) return null
                return (
                  <tr key={item[0]}>
                    <td className="user-avatar cell-detail user-info">
                      <img src={`${rb.baseUrl}/account/user-avatar/${item[0]}`} alt="Avatar" />
                      <span>{item[1]}</span>
                      <span className="cell-detail-description">{item[2] || '-'}</span>
                    </td>
                    <td className="actions">
                      <a className="icon danger-hover" title={$L('删除')} onClick={() => this._removeMember(item[0])}>
                        <i className="zmdi zmdi-delete"></i>
                      </a>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
        <div className="col-3">
          <div className="nav depts">
            {Object.keys(depts).map((item) => {
              return (
                <a className="nav-link" data-toggle="pill" href="#" key={item} onClick={() => this.setState({ activeDept: item })}>
                  {item} ({depts[item]})
                </a>
              )
            })}
          </div>
        </div>
      </div>
    )
  }

  componentDidMount = () => this.loadMembers()

  loadMembers() {
    $.get(`/admin/bizuser/team-members?team=${this.props.id}`, (res) => this.setState({ members: res.data || [] }))
  }

  _removeMember(user) {
    const that = this
    RbAlert.create($L('DeleteTeamMemberConfirm'), {
      confirm: function () {
        this.disabled(true)
        $.post(`/admin/bizuser/team-members-del?team=${that.props.id}&user=${user}`, (res) => {
          if (res.error_code === 0) {
            this.hide()
            that.loadMembers()
            RbHighbar.success($L('SomeRemoved,TeamMember'))
          } else {
            RbHighbar.error(res.error_msg)
          }
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
      RbAlert.create($L('DeleteTeamConfirm'), $L('DeleteSome,Team'), {
        type: 'danger',
        confirmText: $L('删除'),
        confirm: function () {
          this.disabled(true)
          $.post(`/app/entity/common-delete?id=${teamId}`, (res) => {
            if (res.error_code === 0) {
              parent.location.hash = '!/View/'
              parent.location.reload()
            } else RbHighbar.error(res.error_msg)
          })
        },
      })
    })
})
