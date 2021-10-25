/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const UNICON_NAME = 'texture'
let _Share2
let _entity_data = {}

$(document).ready(function () {
  $('.J_add-menu').click(() => render_item({}, true))

  $.get('/commons/metadata/entities?detail=true', function (res) {
    $(res.data).each(function () {
      $(`<option value="${this.name}">${this.label}</option>`).appendTo('.J_menuEntity optgroup:eq(0)')
      _entity_data[this.name] = this
    })

    const $ref = $('.J_menuEntity')
      .select2({
        placeholder: $L('选择关联项'),
        allowClear: false,
      })
      .on('change', () => {
        if (item_current_isNew === true) {
          const d = _entity_data[$ref.val()]
          if (d) {
            $('.J_menuIcon .zmdi').attr('class', `zmdi zmdi-${d.icon}`)
            $('.J_menuName').val(d.label)
          }
        }
      })
  })

  $('.J_menuIcon').click(function () {
    parent.clickIcon = function (s) {
      $('.J_menuIcon .zmdi').attr('class', `zmdi zmdi-${s}`)
      parent.RbModal.hide()
    }
    parent.RbModal.create('/p/common/search-icon', $L('选择图标'))
  })

  $('.J_menuConfirm').click(function () {
    const name = $val('.J_menuName')
    if (!name) return RbHighbar.create($L('请输入菜单名称'))

    const type = $('.J_menuType.active').attr('href').substr(1)
    let value
    if (type === 'ENTITY') {
      value = $('.J_menuEntity').val()
      if (!value) return RbHighbar.create($L('请选择关联项'))
    } else {
      value = $val('.J_menuUrl')
      if (!value) {
        return RbHighbar.create($L('请输入外部地址'))
      } else if (!($regex.isUrl(value) || $regex.isUrl(`https://getrebuild.com${value}`))) {
        return RbHighbar.create($L('请输入有效的外部地址'))
      }
    }

    const icon = $('.J_menuIcon i').attr('class').replace('zmdi zmdi-', '')
    render_item({
      id: item_currentid,
      text: name,
      type: type,
      value: value,
      icon: icon,
    })

    item_currentid = null
    $('.J_config li').removeClass('active')
    $('.J_edit-tips').removeClass('hide')
    $('.J_edit-menu').addClass('hide')
  })

  let overwriteMode = false
  let cfgid = $urlp('id')
  const _save = function (navs) {
    const $btn = $('.J_save').button('loading')
    const std = _Share2 ? _Share2.getData() : { shareTo: 'SELF' }
    $.post(`/app/settings/nav-settings?id=${cfgid || ''}&configName=${$encode(std.configName || '')}&shareTo=${std.shareTo || ''}`, JSON.stringify(navs), function (res) {
      $btn.button('reset')
      if (res.error_code === 0) parent.location.reload()
    })
  }

  $('.J_save').click(function () {
    const navs = []
    $('.J_config>.dd-item').each(function () {
      const $item = build_item($(this), navs)
      if ($item) navs.push($item)
    })
    if (navs.length === 0) return RbHighbar.create($L('请至少设置 1 个菜单项'))

    if (overwriteMode) {
      RbAlert.create($L('保存将覆盖你现有的导航菜单。继续吗？'), {
        confirm: function () {
          this.hide()
          _save(navs)
        },
      })
    } else {
      _save(navs)
    }
  })

  // 加载

  use_sortable('.J_config')
  $.get(`/app/settings/nav-settings?id=${cfgid || ''}`, function (res) {
    if (res.data) {
      cfgid = res.data.id
      $(res.data.config).each(function () {
        const $item = render_item(this)
        if (this.sub) {
          const $subUl = $('<ul></ul>').appendTo($item)
          $(this.sub).each(function () {
            render_item(this, false, $subUl)
          })
          use_sortable($subUl)
        }
      })

      // 覆盖自有配置
      overwriteMode = !rb.isAdminUser && res.data.shareTo !== 'SELF'
    }

    const _current = res.data || {}
    $.get('/app/settings/nav-settings/alist', (res) => {
      const cc = res.data.find((x) => x[0] === _current.id)
      if (rb.isAdminUser) {
        renderRbcomp(<Share2 title={$L('导航菜单')} list={res.data} configName={cc ? cc[1] : ''} shareTo={_current.shareTo} id={_current.id} />, 'shareTo', function () {
          _Share2 = this
        })
      } else {
        // overSelf = cc && cc[3] !== rb.currentUser
        // eslint-disable-next-line no-undef
        renderSwitchButton(res.data, $L('导航菜单'), cc ? cc[0] : null)
      }

      // 有自有才提示覆盖
      if (overwriteMode) {
        const haveSelf = res.data.find((x) => x[2] === 'SELF')
        overwriteMode = !!haveSelf
      }
    })
    // ~
  })
})

const use_sortable = function (el) {
  $(el)
    .sortable({
      placeholder: 'dd-placeholder',
      handle: '>.dd3-handle',
      axis: 'y',
    })
    .disableSelection()
}

const build_item = function (item) {
  const data = {
    text: $.trim(item.find('.dd3-content').eq(0).text()),
    type: item.attr('attr-type'),
    value: item.attr('attr-value'),
    icon: item.attr('attr-icon'),
  }
  if (!data.value) return null

  const $subNavs = item.find('ul>li')
  if ($subNavs.length > 0) {
    data.sub = []
    $subNavs.each(function () {
      const sub = build_item($(this))
      if (sub) data.sub.push(sub)
    })
  }
  return data
}

let item_currentid
let item_current_isNew
let item_randomid = new Date().getTime()

const render_item = function (data, isNew, append2) {
  data.id = data.id || item_randomid++
  data.text = data.text || $L('未命名')
  data.icon = data.icon || UNICON_NAME
  append2 = append2 || '.J_config'

  let $item = $('.J_config').find(`li[attr-id='${data.id}']`)
  if ($item.length === 0) {
    $item = $('<li class="dd-item dd3-item"><div class="dd-handle dd3-handle"></div><div class="dd3-content"><i class="zmdi"></i><span></span></div></li>').appendTo(append2)
    const $action = $(
      `<div class="dd3-action"><a class="J_addsub" title="${$L('添加子菜单')}"><i class="zmdi zmdi-plus"></i></a><a class="J_del" title="${$L('移除')}"><i class="zmdi zmdi-close"></i></a></div>`
    ).appendTo($item)

    $action
      .find('a.J_del')
      .off('click')
      .click(function () {
        $item.remove()
        fix_parents()
      })
    $action
      .find('a.J_addsub')
      .off('click')
      .click(function () {
        let $subUl = $item.find('ul')
        if ($subUl.length === 0) {
          $subUl = $('<ul></ul>').appendTo($item)
          use_sortable($subUl)
        }

        render_item({}, true, $subUl)
        fix_parents()
      })

    if (!$(append2).hasClass('J_config')) $action.find('a.J_addsub').remove()
  }

  const $content3 = $item.find('.dd3-content').eq(0)
  $content3.find('.zmdi').attr('class', 'zmdi zmdi-' + data.icon)
  $content3.find('span').text(data.text)
  $item.attr({
    'attr-id': data.id,
    'attr-type': data.type || 'ENTITY',
    'attr-value': data.value || '',
    'attr-icon': data.icon,
  })

  // Event
  $content3.off('click').click(function () {
    $('.J_config li').removeClass('active')
    $item.addClass('active')

    $('.J_edit-tips').addClass('hide')
    $('.J_edit-menu').removeClass('hide')

    $('.J_menuName').val(data.text)
    $('.J_menuIcon i').attr('class', `zmdi zmdi-${data.icon}`)
    $('.J_menuUrl, .J_menuEntity').val('')
    if (data.type === 'URL') {
      $('.J_menuType').eq(1).click()
      $('.J_menuUrl').val(data.value)
    } else {
      $('.J_menuType').eq(0).click()
      data.value = $item.attr('attr-value') // force renew
      const $me = $('.J_menuEntity').val(data.value).trigger('change')
      $me.attr('disabled', data.value === '$PARENT$')

      // 实体已经不存在
      // if (_entity_data[data.value]) $me.removeClass('is-invalid')
      // else $me.addClass('is-invalid')
    }

    item_currentid = data.id
  })

  if (isNew === true) {
    $content3.trigger('click')
    $('.J_menuName').focus()
  }

  item_current_isNew = isNew
  return $item
}

const fix_parents = function () {
  $('.J_config>li').each(function () {
    const $me = $(this)
    if ($me.find('ul>li').length > 0) $me.attr({ 'attr-value': '$PARENT$' })
    else if ($me.attr('attr-value') === '$PARENT$') $me.attr({ 'attr-value': '' })
  })
}
