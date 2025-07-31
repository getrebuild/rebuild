/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

$(document).ready(() => {
  $unhideDropdown('.J_show-locales')
  $('.J_show-locales input').on('click', () => {
    const $table = $('#i18nList')
    $table.find('[data-locale]').removeClass('hide')

    let hide = []
    $('.J_show-locales input:not(:checked)').each(function () {
      hide.push($(this).val())
    })
    hide.forEach((locale) => {
      $table.find(`[data-locale=${locale}]`).addClass('hide')
    })
    $storage.set('i18n-hide', hide.join(';'))
  })

  $.get(`./i18n-list?entity=${wpc.entityName}`, (res) => i18nList(res.data || []))

  $('.J_save').on('click', () => {
    const post = buildI18nList()
    $.post('/admin/i18n/editor-post', JSON.stringify(post), (res) => {
      if (res.error_code === 0) {
        location.reload()
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  })
})

function i18nList(data) {
  const $tbody = $('#i18nList tbody')
  const $tmpl = $('#i18nList tr.hide')

  data.forEach((item) => {
    const $tr = $tmpl.clone().removeClass('hide').appendTo($tbody)
    for (let k in item) {
      const $td = $tr.find(`td[data-locale=${k}]`)
      if (k === '_def') $td.text(item[k]).attr('data-key', item._key)
      else $td.find('textarea').val(item[k])
    }
  })

  $('#i18nList').parent().removeClass('rb-loading-active')
  $tmpl.remove()

  // 记住隐藏
  const hide = $storage.get('i18n-hide')
  if (hide) {
    hide.split(';').forEach((locale) => {
      $(`.J_show-locales input[value=${locale}]`)[0].click()
    })
  }
}

function buildI18nList() {
  let data = []
  $('#i18nList tbody tr').each(function () {
    const $tr = $(this)
    const key = $tr.find('td[data-key]').data('key')

    $tr.find('textarea').each(function () {
      const text = $(this).val()
      const locale = $(this).parent().data('locale')
      data.push([locale, key, text || null])
    })
  })
  return data
}
