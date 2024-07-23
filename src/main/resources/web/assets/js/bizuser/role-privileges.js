/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// eslint-disable-next-line no-undef
RbForm.postAfter = function (data) {
  location.href = `${rb.baseUrl}/admin/bizuser/role/${data.id}`
}

const roleId = window.__PageConfig.recordId

// 自定义
let advFilters = {}
let advFilterSettings = {}
let fieldpModals = {}
let fieldpSettings = {}

$(document).ready(() => {
  loadRoles()

  $('.J_new-role').on('click', () => RbFormModal.create({ title: $L('新建角色'), entity: 'Role', icon: 'lock' }))

  if (roleId) {
    $('.J_save').attr('disabled', false)
    $('.J_save').next().attr('disabled', false)

    loadPrivileges()

    $('.J_save').on('click', updatePrivileges)
    $('.J_copy-role').on('click', () => renderRbcomp(<CopyRoleTo roleId={roleId} />))

    $('.nav-tabs li:eq(2)').removeClass('hide')
    renderRbcomp(<MemberList id={roleId} />, 'tab-members', function () {})
  }

  // ENTITY

  // 单个操作
  $('#priv-entity tbody .priv').on('click', function () {
    const $this = $(this)
    _clickPriv($this, $this.data('action'))
  })
  // 批量操作
  $('#priv-entity thead th>a').on('click', function () {
    const action = $(this).data('action')
    const $items = $(`#priv-entity tbody .priv[data-action="${action}"]`)
    _clickPriv($items, action)
  })
  // 批量操作
  $('#priv-entity tbody .name>a').on('click', function () {
    const $items = $(this).parent().parent().find('.priv')
    const clz = $items.eq(0).hasClass('R0') ? 'R4' : 'R0'
    $items.removeClass('R0 R1 R2 R3 R4').addClass(clz)
  })
  // v3.8 字段权限
  $('#priv-entity tbody .name>span>a').on('click', function () {
    const $this = $(this)
    const entity = $this.data('entity')
    const fieldpKey = entity + ':FP'
    if (fieldpModals[fieldpKey]) {
      fieldpModals[fieldpKey].show()
    } else {
      renderRbcomp(
        <FieldsPrivileges
          entity={entity}
          selected={fieldpSettings[fieldpKey]}
          onConfirm={(res) => {
            fieldpSettings[fieldpKey] = res
            // active
            if (res) $this.addClass('active')
            else $this.removeClass('active')
          }}
        />
      )
    }
  })

  // ZERO

  $('#priv-zero tbody .priv').on('click', function () {
    _clickPriv($(this), 'Z')
  })
  $('#priv-zero thead th>a').on('click', function () {
    const $privZero = $('#priv-zero tbody .priv[data-action="Z"]')
    _clickPriv($privZero, 'Z')
  })
  $('#priv-zero tbody .name>a').on('click', function () {
    const $el = $(this).parent().next().find('i.priv')
    _clickPriv($el, 'Z')
  })

  // CUSTOM

  const ACTION_NAMES = {
    'R': $L('读取'),
    'U': $L('更新'),
    'D': $L('删除'),
    'A': $L('分配'),
    'S': $L('共享'),
  }

  $('#priv-entity tbody td>a.cp').on('click', function () {
    if (rb.commercial < 1) {
      RbHighbar.error(WrapHtml($L('免费版不支持自定义权限功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    const entity = $(this).parent().parent().find('.name>a').data('entity')
    const action = $(this).prev().data('action')
    const filterKey = `${entity}:${action}`

    if (advFilters[filterKey]) {
      advFilters[filterKey].show()
    } else {
      renderRbcomp(
        <AdvFilter
          entity={entity}
          filter={advFilterSettings[filterKey]}
          title={
            <RF>
              {$L('自定义%s权限', ACTION_NAMES[action] || '')}
              <sup className="rbv" />
            </RF>
          }
          inModal
          canNoFilters
          confirm={(set) => {
            advFilterSettings[filterKey] = set
            // active
            const $active = $(`.table-priv tbody td.name>a[data-entity="${entity}"]`).parent().parent().find(`a[data-action="${action}9"]`)
            if (set && set.items && set.items.length > 0) $active.addClass('active')
            else $active.removeClass('active')
          }}
        />,
        null,
        function () {
          advFilters[filterKey] = this
        }
      )
    }
  })
})

const _clickPriv = function (elements, action) {
  if (action === 'Z' && elements.hasClass('R0')) {
    const isRbv = elements.parent().prev().find('.rbv')[0]
    if (isRbv && rb.commercial < 1) {
      return RbHighbar.error(WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
    }
  }

  if (action === 'C' || action === 'Z') {
    if (elements.hasClass('R0')) elements.removeClass('R0').addClass('R4')
    else elements.removeClass('R4').addClass('R0')
  } else {
    let clz = 'R0'
    if (elements.hasClass('R0')) clz = 'R1'
    else if (elements.hasClass('R1')) clz = 'R2'
    else if (elements.hasClass('R2')) clz = 'R3'
    else if (elements.hasClass('R3')) clz = 'R4'
    elements.removeClass('R0 R1 R2 R3 R4').addClass(clz)
  }
}

let _AsideTree
const loadRoles = function () {
  $.get('/admin/bizuser/role-list', (res) => {
    if (_AsideTree) {
      ReactDOM.unmountComponentAtNode(document.getElementById('role-tree'))
      _AsideTree = null
    }

    renderRbcomp(
      <AsideTree
        data={res.data}
        activeItem={roleId}
        onItemClick={(item) => {
          location.href = `${rb.baseUrl}/admin/bizuser/role/${item.id}`

          // history.pushState(item.id, null, `${rb.baseUrl}/admin/bizuser/role/${item.id}`)
          // roleId = item.id
          // // reset
          // $('.table-priv i.priv').attr('class', 'priv R0')
          // loadPrivileges()
        }}
        extrasAction={(item) => {
          return (
            <RF>
              <span className="action" onClick={() => RbFormModal.create({ title: $L('编辑角色'), entity: 'Role', icon: 'lock', id: item.id }, true)}>
                <i className="zmdi zmdi-edit" />
              </span>
              <span
                className="action"
                onClick={() => {
                  // eslint-disable-next-line no-undef
                  deleteRole(item.id)
                }}>
                <i className="zmdi zmdi-delete" />
              </span>
            </RF>
          )
        }}
        hideCollapse
      />,
      'role-tree',
      function () {
        _AsideTree = this
      }
    )
  })
}

const loadPrivileges = function () {
  $.get(`/admin/bizuser/privileges-list?role=${roleId}`, function (res) {
    if (res.error_code === 0) {
      $(res.data).each(function () {
        let defs = {}
        try {
          defs = JSON.parse(this.definition)
        } catch (ignored) {
          // NOOP
        }

        const $name = $(`.table-priv tbody td.name>a[data-name="${this.name}"]`)
        const $tr = $name.parent().parent()
        const entity = $name.data('entity')

        for (let k in defs) {
          // filter
          if (k.substr(1, 1) === '9') {
            // set
            if (defs[k] && defs[k].items && defs[k].items.length > 0) {
              const filterKey = `${entity}:${k.substr(0, 1)}`
              advFilterSettings[filterKey] = defs[k]
              $tr.find(`a[data-action="${k}"]`).addClass('active')
            }
          } else if (k === 'FP') {
            fieldpSettings[entity + ':FP'] = defs[k]
            $name.parent().find('span>a').addClass('active')
          } else {
            $tr.find(`i.priv[data-action="${k}"]`).removeClass('R0 R1 R2 R3 R4').addClass(`R${defs[k]}`)
          }
        }
      })
    } else {
      $('.J_save').attr('disabled', true)
      $('.J_save').next().attr('disabled', true)
      $('.J_tips').removeClass('hide').find('.message p').text(res.error_msg)
    }
  })
}

const updatePrivileges = function () {
  const privEntity = {}
  $('#priv-entity tbody>tr').each(function () {
    const $tr = $(this)
    const name = $tr.find('td.name>a').data('name')
    const entity = $tr.find('td.name>a').data('entity')

    const definition = {}
    $tr.find('i.priv').each(function () {
      const $this = $(this)
      const action = $this.data('action')
      let deep = 0
      if ($this.hasClass('R1')) deep = 1
      else if ($this.hasClass('R2')) deep = 2
      else if ($this.hasClass('R3')) deep = 3
      else if ($this.hasClass('R4')) deep = 4
      definition[action] = deep

      const filterKey = `${entity}:${action}`
      const filter = advFilterSettings[filterKey]
      if (filter) definition[`${action}9`] = filter
    })
    // v3.8
    const fieldp = fieldpSettings[entity + ':FP']
    if (fieldp) definition['FP'] = fieldp

    privEntity[name] = definition
  })

  const privZero = {}
  $('#priv-zero tbody>tr').each(function () {
    const $tr = $(this)
    const name = $tr.find('td.name>a').data('name')
    privZero[name] = $tr.find('i.priv').hasClass('R0') ? { Z: 0 } : { Z: 4 }
  })

  const _data = {
    entity: privEntity,
    zero: privZero,
  }

  $.post(`/admin/bizuser/privileges-update?role=${roleId}`, JSON.stringify(_data), (res) => {
    if (res.error_code === 0) location.reload()
    else RbHighbar.error(res.error_msg)
  })
}

// 复制角色
class CopyRoleTo extends RbModalHandler {
  render() {
    return (
      <RbModal title={$L('复制角色')} ref={(c) => (this._dlg = c)} disposeOnHide>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('复制到哪些角色')}</label>
            <div className="col-sm-7">
              <UserSelector hideDepartment hideUser hideTeam ref={(c) => (this._UserSelector = c)} />
              <p className="form-text">{$L('将当前角色权限复制到选择的角色中，选择角色的原有权限会被完全覆盖')}</p>
            </div>
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3">
            <button className="btn btn-primary" type="button" onClick={() => this.submit()} ref={(c) => (this._$btn = c)}>
              {$L('复制')}
            </button>
            <a className="btn btn-link" onClick={this.hide}>
              {$L('取消')}
            </a>
          </div>
        </div>
      </RbModal>
    )
  }

  submit() {
    const post = {
      from: this.props.roleId,
      copyTo: this._UserSelector.val(),
    }
    if ((post.copyTo || []).length === 0) return RbHighbar.create($L('请选择复制给哪些角色'))

    const that = this
    RbAlert.create($L('确定将当前角色权限复制给选择的角色吗？'), {
      onConfirm: function () {
        this.hide()

        const $btn = $(that._$btn).button('loading')
        $.post('/admin/bizuser/role-copyto', JSON.stringify(post), (res) => {
          if (res.error_code === 0) {
            RbHighbar.success($L('复制完成'))
            setTimeout(() => that.hide(), 1500)
          } else {
            RbHighbar.error(res.error_msg)
            $btn.button('reset')
          }
        })
      },
    })
  }
}

// ~ 成员列表
class MemberList extends React.Component {
  state = { ...this.props }

  render() {
    if (this.state.members && this.state.members.length === 0) {
      return (
        <div className="list-nodata pt-8 mb-8">
          <span className="zmdi zmdi-info-outline"></span>
          <p>{$L('暂无使用用户')}</p>
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
                    <a href={`${rb.baseUrl}/app/redirect?id=${item[0]}`} target="_blank">
                      <img src={`${rb.baseUrl}/account/user-avatar/${item[0]}`} alt="Avatar" />
                      <span>{item[1]}</span>
                      <span className="cell-detail-description">{item[2] || '-'}</span>
                    </a>
                  </td>
                  <td className="cell-detail text-right">
                    <div>
                      {!item[3] && <em className="badge badge-warning badge-pill">{$L('未激活')}</em>}
                      {item[4] ? <em className="badge badge-light badge-pill">{$L('附加角色')}</em> : <em className="badge badge-light badge-pill">{$L('主角色')}</em>}
                    </div>
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
        $(`<span class="badge badge-pill badge-primary">${data.length}</span>`).appendTo($('.nav-tabs a:eq(2)'))
      }
    })
  }
}

class FieldsPrivileges extends RbModalHandler {
  render() {
    return (
      <RbModal
        title={
          <RF>
            {$L('字段权限')} (LAB)
            <sup className="rbv" />
          </RF>
        }
        ref={(c) => (this._dlg = c)}>
        <div className="p-2">
          <div className="form-group">
            <label>{$L('不可新建字段')}</label>
            <select className="form-control form-control-sm" multiple ref={(c) => (this._$create = c)}>
              {this.state.fields &&
                this.state.fields.map((item) => {
                  return (
                    <option key={item.name} value={item.name} disabled={item.creatable === false}>
                      {item.label}
                    </option>
                  )
                })}
            </select>
          </div>
          <div className="form-group">
            <label>{$L('不可读取字段')}</label>
            <select className="form-control form-control-sm" multiple ref={(c) => (this._$read = c)}>
              {this.state.fields &&
                this.state.fields.map((item) => {
                  return (
                    <option key={item.name} value={item.name}>
                      {item.label}
                    </option>
                  )
                })}
            </select>
          </div>
          <div className="form-group mb-1">
            <label>{$L('不可编辑字段')}</label>
            <select className="form-control form-control-sm" multiple ref={(c) => (this._$update = c)}>
              {this.state.fields &&
                this.state.fields.map((item) => {
                  return (
                    <option key={item.name} value={item.name} disabled={item.updatable === false}>
                      {item.label}
                    </option>
                  )
                })}
            </select>
          </div>
        </div>

        <div className="dialog-footer">
          <button className="btn btn-secondary btn-space mr-2" type="button" onClick={this.hide}>
            {$L('取消')}
          </button>
          <button className="btn btn-primary btn-space" type="button" onClick={() => this.handleConfirm()}>
            {$L('确定')}
          </button>
        </div>
      </RbModal>
    )
  }

  handleConfirm() {
    if (rb.commercial < 1) {
      RbHighbar.error(WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    const selected = {
      create: $(this._$create).val(),
      read: $(this._$read).val(),
      update: $(this._$update).val(),
    }
    typeof this.props.onConfirm === 'function' && this.props.onConfirm(selected)
    this.hide()
  }

  componentDidMount() {
    $.get(`/commons/metadata/fields?entity=${this.props.entity}`, (res) => {
      this.setState({ fields: res.data }, () => {
        $([this._$create, this._$read, this._$update]).select2({
          placeholder: $L('无'),
          allowClear: true,
        })

        // init
        if (this.props.selected) {
          if (this.props.selected.create) $(this._$create).val(this.props.selected.create).trigger('change')
          if (this.props.selected.read) $(this._$read).val(this.props.selected.read).trigger('change')
          if (this.props.selected.update) $(this._$update).val(this.props.selected.update).trigger('change')
        }
      })
    })
  }
}
