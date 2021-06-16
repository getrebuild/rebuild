/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const userId = window.__PageConfig.recordId

$(document).ready(function () {
  $('.J_delete')
    .off('click')
    .click(function () {
      $.get(`/admin/bizuser/delete-checks?id=${userId}`, (res) => {
        if (res.data.hasMember === 0) {
          RbAlert.create($L('此用户可以被安全的删除'), $L('删除用户'), {
            icon: 'alert-circle-o',
            type: 'danger',
            confirmText: $L('删除'),
            confirm: function () {
              deleteUser(userId, this)
            },
          })
        } else {
          RbAlert.create($L('此用户已被使用过，因此不能删除。如不再使用可将其禁用'), $L('无法删除'), {
            icon: 'alert-circle-o',
            type: 'danger',
            confirmText: $L('无法删除选中记录'),
            confirm: function () {
              toggleDisabled(true, this)
            },
          })
        }
      })
    })

  $('.J_disable').click(() => {
    RbAlert.create($L('确定要禁用此用户吗？'), {
      confirmText: $L('禁用'),
      confirm: function () {
        toggleDisabled(true, this)
      },
    })
  })
  $('.J_enable').click(() => toggleDisabled(false))

  $('.J_resetpwd').click(() => {
    const newpwd = $random(null, true, 8) + '!8'
    RbAlert.create($L('密码将重置为 **%s** 是否确认？', newpwd), {
      html: true,
      confirm: function () {
        this.disabled(true)
        $.post(`/admin/bizuser/user-resetpwd?id=${userId}&newp=${$decode(newpwd)}`, (res) => {
          this.disabled()
          if (res.error_code === 0) {
            RbHighbar.success($L('重置密码成功'))
            this.hide()
          } else RbHighbar.error(res.error_code)
        })
      },
    })
  })

  if (rb.isAdminVerified) {
    $.get(`/admin/bizuser/check-user-status?id=${userId}`, (res) => {
      if (res.data.system === true && rb.isAdminVerified === true) {
        $('.view-action').remove()
        $('.J_tips').removeClass('hide').find('.message p').text($L('系统内置超级管理员，不允许修改。此用户拥有最高级系统权限，请谨慎使用'))
        return
      }

      $('.J_changeRole').click(() => renderRbcomp(<DlgEnableUser user={userId} roleSet={true} role={res.data.role} roleAppends={res.data.roleAppends} />))
      $('.J_changeDept').click(() => renderRbcomp(<DlgEnableUser user={userId} deptSet={true} dept={res.data.dept} />))

      if (res.data.disabled === true) {
        $('.J_disable').remove()

        if (!res.data.role || !res.data.dept) {
          $('.J_enable')
            .off('click')
            .click(() =>
              renderRbcomp(
                <DlgEnableUser
                  user={userId}
                  enable={true}
                  roleSet={!res.data.role}
                  role={res.data.role}
                  roleAppends={res.data.roleAppends}
                  deptSet={!res.data.dept}
                  dept={res.data.dept}
                />
              )
            )
        }
      } else {
        $('.J_enable').remove()
      }

      if (!res.data.active) {
        const reasons = []
        if (!res.data.role) reasons.push($L('未指定角色'))
        else if (res.data.roleDisabled) reasons.push($L('所属角色已禁用'))
        if (!res.data.dept) reasons.push($L('未指定部门'))
        else if (res.data.deptDisabled) reasons.push($L('所属部门已禁用'))
        if (res.data.disabled === true) reasons.push($L('已禁用'))
        $('.J_tips')
          .removeClass('hide')
          .find('.message p')
          .text($L('当前用户处于未激活状态，因为其 %s', reasons.join(' / ')))
      }
    })
  }
})

// 启用/禁用
const toggleDisabled = function (disabled, alert) {
  alert && alert.disabled(true)

  const data = {
    user: userId,
    enable: !disabled,
  }
  $.post('/admin/bizuser/enable-user', JSON.stringify(data), (res) => {
    if (res.error_code === 0) {
      RbHighbar.success(disabled ? $L('用户已禁用') : $L('用户已启用'))
      _reload(200)
    } else {
      RbHighbar.error(res.error_msg)
    }
  })
}

// 删除用户
const deleteUser = function (id, alert) {
  alert && alert.disabled(true)

  $.post(`/admin/bizuser/user-delete?id=${id}`, (res) => {
    if (res.error_code === 0) {
      parent.location.hash = '!/View/'
      parent.location.reload()
    } else {
      RbHighbar.error(res.error_msg)
    }
  })
}

// 激活用户/变更部门/角色
class DlgEnableUser extends RbModalHandler {
  constructor(props) {
    super(props)

    if (props.enable) this._title = $L('激活用户')
    else this._title = props.dept ? $L('修改部门') : $L('修改角色')
  }

  render() {
    return (
      <RbModal title={this._title} ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form">
          {this.props.deptSet && (
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('选择所属部门')}</label>
              <div className="col-sm-7">
                <UserSelector
                  hideUser={true}
                  hideRole={true}
                  hideTeam={true}
                  multiple={false}
                  defaultValue={this.props.dept}
                  ref={(c) => (this._deptNew = c)}
                />
              </div>
            </div>
          )}
          {this.props.roleSet && (
            <React.Fragment>
              <div className="form-group row">
                <label className="col-sm-3 col-form-label text-sm-right">{$L('选择角色')}</label>
                <div className="col-sm-7">
                  <UserSelector
                    hideUser={true}
                    hideDepartment={true}
                    hideTeam={true}
                    multiple={false}
                    defaultValue={this.props.role}
                    ref={(c) => (this._roleNew = c)}
                  />
                </div>
              </div>
              <div className="form-group row">
                <label className="col-sm-3 col-form-label text-sm-right">
                  {$L('附加角色')} ({$L('可选')}) <sup className="rbv"></sup>
                </label>
                <div className="col-sm-7">
                  <UserSelector
                    hideUser={true}
                    hideDepartment={true}
                    hideTeam={true}
                    defaultValue={this.props.roleAppends}
                    ref={(c) => (this._roleAppends = c)}
                  />
                  <p className="form-text">{$L('选择的多个角色权限将被合并，高权限优先')}</p>
                </div>
              </div>
            </React.Fragment>
          )}
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary btn-space" type="button" onClick={() => this.post()}>
                {$L('确定')}
              </button>
              <a className="btn btn-link btn-space" onClick={() => this.hide()}>
                {$L('取消')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  post() {
    const data = { user: this.props.user }

    if (this.props.enable === true) {
      data.enable = true
    }
    if (this._deptNew) {
      const v = this._deptNew.val()
      if (v.length === 0) return RbHighbar.create($L('请选择部门'))
      data.dept = v[0]
    }
    if (this._roleNew) {
      const v = this._roleNew.val()
      if (v.length === 0) return RbHighbar.create($L('请选择角色'))
      data.role = v[0]
    }
    if (this._roleAppends) {
      data.roleAppends = this._roleAppends.val().join(',')
      if (data.roleAppends && rb.commercial < 1) {
        return RbHighbar.create($L('免费版不支持附加角色功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)'), {
          type: 'danger',
          html: true,
          timeout: 6000,
        })
      }
    }

    const $btns = $(this._btns).find('.btn').button('loading')
    $.post('/admin/bizuser/enable-user', JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        if (data.enable === true) RbHighbar.success($L('用户已激活'))
        _reload(data.enable ? 200 : 0)
      } else {
        $btns.button('reset')
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

const _reload = function (timeout) {
  setTimeout(() => location.reload(), timeout || 1)
  parent && parent.RbListPage && parent.RbListPage.reload()
}
