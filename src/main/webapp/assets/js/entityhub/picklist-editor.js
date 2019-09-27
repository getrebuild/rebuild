/* eslint-disable no-undef */
const multi = $urlp('multi') === 'true'
const maxOptions = multi ? 20 : 40
$(document).ready(function () {
  const query = 'entity=' + $urlp('entity') + '&field=' + $urlp('field')

  $.get(`${rb.baseUrl}/admin/field/picklist-gets?isAll=true&${query}`, function (res) {
    $(res.data).each(function () {
      if (this.hide === true) render_unset([this.id, this.text])
      else {
        let item = render_item([this.id, this.text])
        if (this['default']) item.find('.defset').trigger('click')
      }
    })
  })

  $('.J_confirm').click(function () {
    if ($('.J_config>li').length > maxOptions) { RbHighbar.create('最多支持' + maxOptions + '个选项'); return false }
    let text = $val('.J_text')
    if (!text) { RbHighbar.create('请输入选项值'); return false }
    let repeated = false
    $('.dd-list .dd3-content, .dd-list .dd-handle>span').each(function () {
      if ($(this).text() === text) {
        repeated = true
        return false
      }
    })
    if (repeated) { RbHighbar.create('包含重复选项'); return false }

    let id = $('.J_text').attr('attr-id')
    $('.J_text').val('').attr('attr-id', '')
    $('.J_confirm').text('添加')
    if (!id) render_item([$random(), text])
    else {
      let item = $('.J_config li[data-key="' + id + '"]')
      item.attr('data-key', id)
      item.find('.dd3-content').text(text)
    }
    return false
  })

  $('.J_save').click(function () {
    let show_items = []
    $('.J_config>li').each(function () {
      let $this = $(this)
      let id = $this.attr('data-key')
      show_items.push({
        id: id,
        text: $this.find('.dd3-content').text(),
        'default': $this.hasClass('active')
      })
    })
    let hide_items = []
    let force_del = 0
    $('.unset-list>li').each(function () {
      let $this = $(this)
      if ($this.data('del') === 'force') {
        force_del++
      } else {
        hide_items.push({
          id: $this.attr('data-key'),
          text: $this.find('.dd-handle').text().replace('[移除]', '')
        })
      }
    })
    let _data = { show: show_items, hide: hide_items }

    let $btn = $(this)
    let del_confirm = function () {
      $btn.button('loading')
      $.post(`${rb.baseUrl}/admin/field/picklist-sets?${query}`, JSON.stringify(_data), (res) => {
        if (res.error_code > 0) RbHighbar.error(res.error_msg)
        else parent.location.reload()
      })
    }

    if (force_del > 0) {
      RbAlert.create('将删除部分选项，使用了这些选项的数据（字段）将无法显示。<br>确定要删除吗？', {
        html: true,
        type: 'danger',
        confirm: del_confirm
      })
    } else {
      del_confirm()
    }
  })
})

render_unset_after = function (item) {
  let del = $('<a href="javascript:;" class="action">[移除]</a>').appendTo(item.find('.dd-handle'))
  del.click(() => {
    del.text('保存后删除')
    del.parent().parent().attr('data-del', 'force')
    return false
  })
}

render_item_after = function (item, data) {
  item.find('a').eq(0).text('[禁用]')
  let edit = $('<a href="javascript:;">[修改]</a>').appendTo(item.find('.dd3-action'))
  edit.click(function () {
    $('.J_confirm').text('修改')
    $('.J_text').val(data[1]).attr('attr-id', data[0]).focus()
  })

  let $def = $('<a href="javascript:;" class="defset">[默认]</a>').appendTo(item.find('.dd3-action'))
  $def.click(function () {
    let $dd3 = $def.parent().parent()
    if ($dd3.hasClass('active')) {
      $dd3.removeClass('active')
      $def.text('[默认]')
    } else {
      if (!multi) $('.J_config li').removeClass('active').find('.defset').text('[默认]')
      $dd3.addClass('active')
      $def.text('[取消]')
    }
  })

  // 新增加的还未保存
  if (data[0].substr(0, 4) !== '012-') {
    item.find('.dd3-action>a:eq(0)').off('click').click(() => {
      item.remove()
    })
  }
}