/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-undef */

const isMulti = $urlp('multi') === 'true'
const maxOptions = isMulti ? 20 : 40

$(document).ready(function () {
  const query = `entity=${$urlp('entity')}&field=${$urlp('field')}`

  $.get(`/admin/field/picklist-gets?isAll=true&${query}`, (res) => {
    $(res.data).each(function () {
      if (this.hide === true) {
        render_unset([this.id, this.text])
      } else {
        const item = render_item([this.id, this.text, this['default']])
        if (this['default']) item.find('.defset').trigger('click')
      }
    })
  })

  $('.J_confirm').on('click', function () {
    if ($('.J_config>li').length > maxOptions) {
      RbHighbar.create($L('最多支持 %d 个选项', maxOptions))
      return false
    }

    const id = $('.J_text').attr('attr-id')
    const text = $val('.J_text')

    if (!text) {
      RbHighbar.create($L('请输入选项值'))
      return false
    }

    let exists = null
    $('.J_config .dd3-content, .unset-list .dd-handle>span').each(function () {
      if ($(this).text() === text) {
        exists = $(this).parent().attr('data-key')
      }
    })

    if (exists) {
      if (exists !== id) {
        RbHighbar.create($L('选项值重复'))
        return false
      }
    }

    $('.J_text').val('').attr('attr-id', '')
    $('.J_confirm').text($L('添加'))

    if (!id) {
      render_item([$random(), text])
    } else {
      const item = $('.J_config li[data-key="' + id + '"]')
      item.attr('data-key', id)
      item.find('.dd3-content').text(text)
    }
    return false
  })

  $('.J_save').on('click', function () {
    const show_items = []
    $('.J_config>li').each(function () {
      const $this = $(this)
      const id = $this.attr('data-key')
      show_items.push({
        id: id,
        text: $this.find('.dd3-content').text(),
        default: $this.hasClass('active'),
      })
    })

    const hide_items = []
    let force_del = 0
    $('.unset-list>li').each(function () {
      const $this = $(this)
      if ($this.data('del') === 'force') {
        force_del++
      } else {
        hide_items.push({
          id: $this.attr('data-key'),
          text: $this.find('.dd-handle>span').text(),
        })
      }
    })

    const _data = {
      show: show_items,
      hide: hide_items,
    }

    const $btn = $(this)
    const delConfirm = function () {
      $btn.button('loading')
      $.post(`/admin/field/picklist-sets?${query}`, JSON.stringify(_data), (res) => {
        if (res.error_code > 0) RbHighbar.error(res.error_msg)
        else parent.location.reload()
      })
    }

    if (force_del > 0) {
      RbAlert.create(WrapHtml($L('将删除部分选项，使用了这些选项的数据 (字段) 将无法显示。[] 确定要删除吗？')), {
        type: 'danger',
        confirm: delConfirm,
      })
    } else {
      delConfirm()
    }
  })
})

render_unset_after = function (item) {
  const $del = $(`<a href="javascript:;" class="action">[${$L('删除')}]</a>`).appendTo(item.find('.dd-handle'))
  $del.on('click', () => {
    $del.text(`[${$L('保存后删除')}]`)
    $del.parent().parent().attr('data-del', 'force')
    return false
  })
}

render_item_after = function (item, data) {
  if (data[2]) item.addClass('active')
  item.find('a.J_del').attr('title', $L('禁用'))

  const $edit = $(`<a title="${$L('修改')}"><i class="zmdi zmdi-edit"></i></a>`)
  item.find('.dd3-action').prepend($edit)
  $edit.on('click', function () {
    $('.J_confirm').text($L('修改'))
    const txt = $edit.parent().prev().text()
    $('.J_text').val(txt).attr('attr-id', data[0]).focus()
  })

  const $def = $(`<a title="${$L('设为默认')}" class="J_def"><i class="zmdi zmdi-${isMulti ? 'check-square' : 'check-circle'}"></i></a>`)
  item.find('.dd3-action').prepend($def)
  $def.on('click', function () {
    if (item.hasClass('active')) {
      item.removeClass('active')
      $def.attr('title', $L('设为默认'))
    } else {
      // 单选
      if (!isMulti) $('.J_config li').removeClass('active').find('.J_def').attr('title', $L('设为默认'))
      item.addClass('active')
      $def.attr('title', $L('取消默认'))
    }
  })

  // 新增加的还未保存，禁用=删除
  if (data[0].substr(0, 4) !== '012-') {
    item
      .find('.dd3-action>a.J_del')
      .attr('title', '删除')
      .off('click')
      .on('click', () => item.remove())
  }
}
