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

$(document).ready(() => {
  loadRoles()

  $('.J_new-role').on('click', () => RbFormModal.create({ title: $L('新建角色'), entity: 'Role', icon: 'lock' }))

  if (roleId) {
    $('.J_save').attr('disabled', false)
    $('.J_save').next().attr('disabled', false)

    loadPrivileges()

    $('.J_save').on('click', updatePrivileges)
    $('.J_copy-role').on('click', () => renderRbcomp(<CopyRoleTo roleId={roleId} />))
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
              <sup className="rbv" title={$L('增值功能')} />
            </RF>
          }
          inModal
          canNoFilters
          confirm={(set) => {
            advFilterSettings[filterKey] = set

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
              <span
                className="action"
                onClick={() => {
                  RbFormModal.create({
                    title: $L('编辑角色'),
                    entity: 'Role',
                    icon: 'lock',
                    id: item.id,
                  })
                }}>
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

        let $tr = $(`.table-priv tbody td.name>a[data-name="${this.name}"]`)
        const entity = $tr.data('entity')
        $tr = $tr.parent().parent()

        for (let k in defs) {
          // filter
          if (k.substr(1, 1) === '9') {
            // set
            if (defs[k] && defs[k].items && defs[k].items.length > 0) {
              const filterKey = `${entity}:${k.substr(0, 1)}`
              advFilterSettings[filterKey] = defs[k]
              $tr.find(`a[data-action="${k}"]`).addClass('active')
            }
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
            <label className="col-sm-3 col-form-label text-sm-right">{$L('复制给哪些角色')}</label>
            <div className="col-sm-7">
              <UserSelector hideDepartment hideUser hideTeam ref={(c) => (this._UserSelector = c)} />
              <p className="form-text">{$L('将当前角色权限复制给选择的角色，选择角色的原有权限会被完全覆盖')}</p>
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
