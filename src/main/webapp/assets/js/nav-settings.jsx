let UNICON_NAME = 'texture'
let shareTo
$(document).ready(function () {
  $('.J_add-menu').click(function () {
    render_item({}, true)
  })

  $.get(rb.baseUrl + '/commons/metadata/entities', function (res) {
    $(res.data).each(function () {
      $('<option value="' + this.name + '" data-icon="' + this.icon + '">' + this.label + '</option>').appendTo('.J_menuEntity optgroup:eq(0)')
    })
  })

  $('.J_menuEntity').change(function () {
    if (item_current_isNew === true) {
      let icon = $('.J_menuEntity option:selected').data('icon')
      $('.J_menuIcon .zmdi').attr('class', 'zmdi zmdi-' + icon)
      let name = $('.J_menuEntity option:selected').text()
      $('.J_menuName').val(name)
    }
  })
  $('.J_menuIcon').click(function () {
    let url = rb.baseUrl + '/p/commons/search-icon'
    parent.clickIcon = function (s) {
      $('.J_menuIcon .zmdi').attr('class', 'zmdi zmdi-' + s)
      parent.RbModal.hide(url)
    }
    parent.RbModal.create(url, '选择图标')
  })
  $('.J_menuConfirm').click(function () {
    let name = $val('.J_menuName')
    if (!name) { RbHighbar.create('请输入菜单名称'); return }
    let type = $('.J_menuType.active').attr('href').substr(1)
    let value
    if (type === 'ENTITY') {
      value = $val('.J_menuEntity')
      if (!value) { RbHighbar.create('请选择关联项'); return }
    } else {
      value = $val('.J_menuUrl')
      if (!value) {
        RbHighbar.create('请输入 URL'); return
      } else if (!!value && !$regex.isUrl(value)) { RbHighbar.create('请输入有效的 URL'); return }
    }
    let icon = $('.J_menuIcon i').attr('class').replace('zmdi zmdi-', '')

    render_item({ id: item_currentid, text: name, type: type, value: value, icon: icon })

    item_currentid = null
    $('.J_config li').removeClass('active')
    $('.J_edit-tips').removeClass('hide')
    $('.J_edit-menu').addClass('hide')
  })

  let cfgid = $urlp('id')
  $('.J_save').click(function () {
    let navs = []
    $('.J_config>.dd-item').each(function () {
      let item = build_item($(this), navs)
      if (item) navs.push(item)
    })
    if (navs.length === 0) {
      RbHighbar.create('请至少设置一个菜单项')
      return
    }

    let btn = $(this).button('loading')
    let shareToData = shareTo ? shareTo.getData() : {}
    $.post(`${rb.baseUrl}/app/settings/nav-settings?id=${cfgid}&configName=${$encode(shareToData.configName || '')}&shareTo=${shareToData.shareTo}`, JSON.stringify(navs), function (res) {
      btn.button('reset')
      if (res.error_code === 0) parent.location.reload()
    })
  })

  add_sortable('.J_config')
  $.get(`${rb.baseUrl}/app/settings/nav-settings?id=${cfgid}`, function (res) {
    if (res.data) {
      cfgid = res.data.id
      $(res.data.config).each(function () {
        let item = render_item(this)
        if (this.sub) {
          let subUl = $('<ul></ul>').appendTo(item)
          $(this.sub).each(function () {
            render_item(this, false, subUl)
          })
          add_sortable(subUl)
        }
      })
    }

    let _data = res.data || {}
    if (rb.isAdminUser) {
      $.get(`${rb.baseUrl}/app/settings/nav-settings/alist`, (res) => {
        let configName = null
        $(res.data).each(function () {
          if (this[0] === _data.id) {
            configName = this[1]
            return false
          }
        })
        // eslint-disable-next-line react/jsx-no-undef
        renderRbcomp(<Share2 title="导航菜单" list={res.data} configName={configName} shareTo={_data.shareTo} id={_data.id} />, 'shareTo', function () { shareTo = this })
      })
    }
  })

})

const add_sortable = function (el) {
  $(el).sortable({
    placeholder: 'dd-placeholder',
    handle: '>.dd3-handle',
    axis: 'y'
  }).disableSelection()
}

const build_item = function (item) {
  let data = {
    text: $.trim(item.find('.dd3-content').eq(0).text()),
    type: item.attr('attr-type'),
    value: item.attr('attr-value'),
    icon: item.attr('attr-icon')
  }
  if (!data.value) return null

  let subNavs = item.find('ul>li')
  if (subNavs.length > 0) {
    data.sub = []
    subNavs.each(function () {
      let sub = build_item($(this))
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
  data.text = data.text || '未命名菜单'
  data.icon = data.icon || UNICON_NAME
  append2 = append2 || '.J_config'

  let item = $('.J_config').find('li[attr-id=\'' + data.id + '\']')
  if (item.length === 0) {
    item = $('<li class="dd-item dd3-item"><div class="dd-handle dd3-handle"></div><div class="dd3-content"><i class="zmdi"></i><span></span></div></li>').appendTo(append2)
    let action = $('<div class="dd3-action"><a class="J_addsub" title="添加子菜单"><i class="zmdi zmdi-plus"></i></a><a class="J_del" title="移除"><i class="zmdi zmdi-close"></i></a></div>').appendTo(item)
    action.find('a.J_del').off('click').click(function () {
      item.remove()
      fixParents()
    })
    action.find('a.J_addsub').off('click').click(function () {
      let subUl = item.find('ul')
      if (subUl.length === 0) {
        subUl = $('<ul></ul>').appendTo(item)
        add_sortable(subUl)
      }
      render_item({}, true, subUl)
      fixParents()
    })
    if (!$(append2).hasClass('J_config')) {
      action.find('a.J_addsub').remove()
    }
  }

  let content3 = item.find('.dd3-content').eq(0)
  content3.find('.zmdi').attr('class', 'zmdi zmdi-' + data.icon)
  content3.find('span').text(data.text)
  item.attr({
    'attr-id': data.id,
    'attr-type': data.type || 'ENTITY',
    'attr-value': data.value || '',
    'attr-icon': data.icon,
  })

  // Event
  content3.off('click').click(function () {
    $('.J_config li').removeClass('active')
    item.addClass('active')

    $('.J_edit-tips').addClass('hide')
    $('.J_edit-menu').removeClass('hide')

    $('.J_menuName').val(data.text)
    $('.J_menuIcon i').attr('class', 'zmdi zmdi-' + data.icon)
    $('.J_menuUrl, .J_menuEntity').val('')
    if (data.type === 'URL') {
      $('.J_menuType').eq(1).click()
      $('.J_menuUrl').val(data.value)
    } else {
      $('.J_menuType').eq(0).click()
      data.value = item.attr('attr-value')  // force renew
      let $me = $('.J_menuEntity').val(data.value)
      $me.attr('disabled', data.value === '$PARENT$')
      if (!$me.find('option:selected').text()) $me.val('').addClass('is-invalid')
      else $me.removeClass('is-invalid')
    }
    item_currentid = data.id
  })

  if (isNew === true) {
    content3.trigger('click')
    $('.J_menuName').focus()
  }
  item_current_isNew = isNew
  return item
}

const fixParents = function () {
  $('.J_config>li').each(function () {
    let $me = $(this)
    if ($me.find('ul>li').length > 0) $me.attr({ 'attr-value': '$PARENT$' })
    else if ($me.attr('attr-value') === '$PARENT$') $me.attr({ 'attr-value': '' })
  })
}