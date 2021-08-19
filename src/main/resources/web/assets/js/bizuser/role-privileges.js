/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// eslint-disable-next-line no-undef
RbForm.postAfter = function (data) {
  location.href = rb.baseUrl + '/admin/bizuser/role/' + data.id
}

// 当前编辑
const roleId = window.__PageConfig.recordId

$(document).ready(function () {
  $('.J_new-role').click(() => RbFormModal.create({ title: $L('新建角色'), entity: 'Role', icon: 'lock' }))

  if (roleId) {
    $('.J_save').attr('disabled', false).click(updatePrivileges)
    loadPrivileges()
  }

  loadRoles()

  // ENTITY

  // 单个操作
  $('#priv-entity tbody .priv').click(function () {
    const $this = $(this)
    clickPriv($this, $this.data('action'))
  })
  // 批量操作
  $('#priv-entity thead th>a').click(function () {
    const action = $(this).data('action')
    const $items = $('#priv-entity tbody .priv[data-action="' + action + '"]')
    clickPriv($items, action)
  })
  // 批量操作
  $('#priv-entity tbody .name>a').click(function () {
    const $items = $(this).parent().parent().find('.priv')
    const clz = $items.eq(0).hasClass('R0') ? 'R4' : 'R0'
    $items.removeClass('R0 R1 R2 R3 R4').addClass(clz)
  })

  // ZERO

  $('#priv-zero tbody .priv').click(function () {
    clickPriv($(this), 'Z')
  })
  $('#priv-zero thead th>a').click(function () {
    const $privZero = $('#priv-zero tbody .priv[data-action="Z"]')
    clickPriv($privZero, 'Z')
  })
  $('#priv-zero tbody .name>a').click(function () {
    const $el = $(this).parent().next().find('i.priv')
    clickPriv($el, 'Z')
  })
})

const clickPriv = function (elements, action) {
  if (action === 'C' || action === 'Z') {
    if (elements.first().hasClass('R0')) elements.removeClass('R0').addClass('R4')
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
        onItemClick={(item) => (location.href = `${rb.baseUrl}/admin/bizuser/role/${item.id}`)}
        extrasAction={(item) => {
          return (
            <React.Fragment>
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
            </React.Fragment>
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
        let $tr = $('.table-priv tbody td.name>a[data-name="' + this.name + '"]')
        $tr = $tr.parent().parent()
        let defi = {}
        try {
          defi = JSON.parse(this.definition)
        } catch (ignored) {
          // NOOP
        }
        for (let k in defi) {
          $tr
            .find('.priv[data-action="' + k + '"]')
            .removeClass('R0 R1 R2 R3 R4')
            .addClass('R' + defi[k])
        }
      })
    } else {
      $('.J_save').attr('disabled', true)
      $('.J_tips').removeClass('hide').find('.message p').text(res.error_msg)
    }
  })
}

const updatePrivileges = function () {
  const privEntity = {}
  $('#priv-entity tbody>tr').each(function () {
    const $tr = $(this)
    const name = $tr.find('td.name a').data('name')

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
    })
    privEntity[name] = definition
  })

  const privZero = {}
  $('#priv-zero tbody>tr').each(function () {
    const etr = $(this)
    const name = etr.find('td.name a').data('name')
    const definition = etr.find('i.priv').hasClass('R0') ? { Z: 0 } : { Z: 4 }
    privZero[name] = definition
  })

  const _data = { entity: privEntity, zero: privZero }
  $.post(`/admin/bizuser/privileges-update?role=${roleId}`, JSON.stringify(_data), (res) => {
    if (res.error_code === 0) location.reload()
    else RbHighbar.error(res.error_msg)
  })
}
