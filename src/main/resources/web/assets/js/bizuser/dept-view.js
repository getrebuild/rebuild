/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global RbForm */

let RbForm_postAfter = RbForm.postAfter
RbForm.postAfter = function (data, next) {
  RbForm_postAfter(data, next)
  if (parent && parent.loadDeptTree) parent.loadDeptTree()
}

const deptId = window.__PageConfig.recordId

const deleteDept = function (_alert) {
  _alert && _alert.disabled(true)
  $.post(`/admin/bizuser/dept-delete?transfer=&id=${deptId}`, (res) => {
    if (res.error_code === 0) {
      parent.location.hash = '!/View/'
      parent.location.reload()
    } else {
      RbHighbar.error(res.error_msg)
      _alert && _alert.disabled()
    }
  })
}

// ~ 成员列表
class MemberList extends React.Component {
  state = { ...this.props }

  render() {
    if (this.state.members && this.state.members.length === 0) {
      return (
        <div className="list-nodata">
          <span className="zmdi zmdi-info-outline"></span>
          <p>{$L('暂无成员')}</p>
        </div>
      )
    }

    return (
      <div>
        <table className="table table-striped table-hover table-btm-line">
          <tbody>
            {(this.state.members || []).map((item) => {
              return (
                <tr key={item[0]}>
                  <td className="user-avatar cell-detail user-info">
                    <a
                      onClick={() => {
                        window.RbViewPage && window.RbViewPage.clickView(`#!/View/User/${item[0]}`)
                      }}>
                      <img src={`${rb.baseUrl}/account/user-avatar/${item[0]}`} alt="Avatar" />
                      <span>{item[1]}</span>
                      <span className="cell-detail-description">{item[2] || '-'}</span>
                    </a>
                  </td>
                  <td className="cell-detail text-right">
                    <div>{!item[3] && <em className="badge badge-warning badge-pill">{$L('未激活')}</em>}</div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    )
  }

  componentDidMount = () => this.loadMembers()

  loadMembers() {
    $.get(`/admin/bizuser/group-members?id=${this.props.id}`, (res) => {
      const data = res.data || []
      this.setState({ members: data })

      if (data.length > 0) {
        $(`<span class="badge badge-pill badge-primary">${data.length}</span>`).appendTo($('.nav-tabs a:eq(1)'))
      }
    })
  }
}

$(document).ready(function () {
  if (rb.isAdminUser) {
    renderRbcomp(<MemberList id={deptId} />, 'tab-members', function () {})
  }

  $('.J_delete-dept').on('click', () => {
    $.get(`/admin/bizuser/delete-checks?id=${deptId}`, (res) => {
      const limits = []
      if (res.data.hasMember > 0) limits.push($L('**%d** 个用户', res.data.hasMember))
      if (res.data.hasChild > 0) limits.push($L('**%d** 个子部门', res.data.hasChild))

      if (limits.length === 0) {
        RbAlert.create($L('此部门可以被安全的删除'), $L('删除部门'), {
          icon: 'alert-circle-o',
          type: 'danger',
          confirmText: $L('删除'),
          confirm: function () {
            deleteDept(this)
          },
        })
      } else {
        RbAlert.create(WrapHtml($L('此部门下有 %s [] 需要先将他们转移至其他部门，然后才能安全删除', limits.join(' / '))), $L('无法删除'), {
          type: 'danger',
        })
      }
    })
  })
})
