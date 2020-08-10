/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const userId = window.__PageConfig.recordId

$(document).ready(function () {
  $('.J_delete').off('click').click(function () {
    $.get(`/admin/bizuser/delete-checks?id=${userId}`, (res) => {
      if (res.data.hasMember === 0) {
        RbAlert.create('此用户可以被安全的删除', '删除用户', {
          icon: 'alert-circle-o',
          type: 'danger',
          confirmText: '删除',
          confirm: function () { deleteUser(userId, this) }
        })
      } else {
        RbAlert.create('此用户已被使用过，因此不能删除。如不再使用可将其停用', '无法删除', {
          icon: 'alert-circle-o',
          type: 'danger',
          confirmText: '停用',
          confirm: function () { toggleDisabled(true, this) }
        })
      }
    })
  })

  $('.J_disable').click(() => {
    RbAlert.create('确定要停用此用户吗？', {
      confirmText: '停用',
      confirm: function () { toggleDisabled(true, this) }
    })
  })
  $('.J_enable').click(() => toggleDisabled(false))

  $('.J_changeRole').click(() => renderRbcomp(<DlgEnableUser user={userId} role={true} />))
  $('.J_changeDept').click(() => renderRbcomp(<DlgEnableUser user={userId} dept={true} />))

  $('.J_resetpwd').click(() => {
    const newpwd = $random(null, true, 8) + '!8'
    RbAlert.create(`密码将重置为 <code class="fs-13 text-bold">${newpwd}</code> 是否确认？`, {
      html: true,
      confirm: function () {
        this.disabled(true)
        $.post(`/admin/bizuser/user-resetpwd?id=${userId}&newp=${$decode(newpwd)}`, (res) => {
          this.disabled()
          if (res.error_code === 0) {
            RbHighbar.success('密码重置成功')
            this.hide()
          } else RbHighbar.error(res.error_code)
        })
      }
    })
  })

  if (rb.isAdminVerified) {
    $.get(`/admin/bizuser/check-user-status?id=${userId}`, (res) => {
      if (res.error_code > 0) return
      if (res.data.system === true && rb.isAdminVerified === true) {
        $('.view-action').remove()
        $('.J_tips').removeClass('hide').find('.message p').text('系统内建超级管理员，不允许修改。此用户拥有最高级系统权限，请谨慎使用')
        return
      }

      if (res.data.disabled === true) {
        $('.J_disable').remove()

        if (!res.data.role || !res.data.dept) {
          $('.J_enable').off('click').click(() => renderRbcomp(<DlgEnableUser enable={true} user={userId} dept={!res.data.dept} role={!res.data.role} />))
        }
      } else {
        $('.J_enable').remove()
      }

      if (res.data.active === true) return

      const reasons = []
      if (!res.data.role) reasons.push('未指定角色')
      else if (res.data.roleDisabled) reasons.push('所属角色已停用')
      if (!res.data.dept) reasons.push('未指定部门')
      else if (res.data.deptDisabled) reasons.push('所在部门已停用')
      if (res.data.disabled === true) reasons.push('已停用')
      $('.J_tips').removeClass('hide').find('.message p').text('当前用户处于未激活状态，因为其 ' + reasons.join(' / '))
    })
  }
})

// 启用/禁用
const toggleDisabled = function (disabled, alert) {
  alert && alert.disabled(true)

  const data = {
    user: userId,
    enable: !disabled
  }
  $.post('/admin/bizuser/enable-user', JSON.stringify(data), (res) => {
    if (res.error_code === 0) {
      RbHighbar.success('用户已' + (disabled ? '停用' : '启用'))
      _reload(200)
    } else RbHighbar.error(res.error_msg)
  })
}

// 删除用户
const deleteUser = function (id, alert) {
  alert && alert.disabled(true)

  $.post(`/admin/bizuser/user-delete?id=${id}`, (res) => {
    if (res.error_code === 0) {
      parent.location.hash = '!/View/'
      parent.location.reload()
    } else RbHighbar.error(res.error_msg)
  })
}

// 激活用户/变更部门/角色
class DlgEnableUser extends RbModalHandler {

  constructor(props) {
    super(props)

    this.__title = '用户激活'
    if (!props.enable) this.__title = '变更' + (props.dept === true ? '部门' : '角色')
  }

  render() {
    return <RbModal title={this.__title} ref={(c) => this._dlg = c} disposeOnHide={true}>
      <div className="form">
        {this.props.dept === true &&
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">用户部门</label>
            <div className="col-sm-7">
              <UserSelector hideUser={true} hideRole={true} hideTeam={true} multiple={false} ref={(c) => this._deptNew = c} />
            </div>
          </div>}
        {this.props.role === true &&
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">用户角色</label>
            <div className="col-sm-7">
              <UserSelector hideUser={true} hideDepartment={true} hideTeam={true} multiple={false} ref={(c) => this._roleNew = c} />
            </div>
          </div>}
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3" ref={(c) => this._btns = c}>
            <button className="btn btn-primary btn-space" type="button" onClick={() => this.post()}>确定</button>
            <a className="btn btn-link btn-space" onClick={() => this.hide()}>取消</a>
          </div>
        </div>
      </div>
    </RbModal>
  }

  post() {
    const data = { user: this.props.user }

    if (this.props.enable === true) {
      data.enable = true
    }
    if (this._deptNew) {
      const v = this._deptNew.val()
      if (v.length === 0) return RbHighbar.create('请选择部门')
      data.dept = v[0]
    }
    if (this._roleNew) {
      const v = this._roleNew.val()
      if (v.length === 0) return RbHighbar.create('请选择角色')
      data.role = v[0]
    }

    const $btns = $(this._btns).find('.btn').button('loading')
    $.post('/admin/bizuser/enable-user', JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        if (data.enable === true) RbHighbar.success('用户已激活')
        _reload(data.enable ? 200 : 0)
      } else {
        RbHighbar.error(res.error_msg)
      }
      $btns.button('reset')
    })
  }
}

const _reload = function (timeout) {
  setTimeout(() => location.reload(), timeout || 1)
  parent && parent.RbListPage && parent.RbListPage.reload()
}