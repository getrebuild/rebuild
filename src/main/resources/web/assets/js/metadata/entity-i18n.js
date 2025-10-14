/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

$(document).ready(() => {
  // 选择语言
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

  // LIST
  $.get(`./i18n-list?entity=${wpc.entityName}`, (res) => renderI18nList(res.data || []))

  // SAVE
  const $btn = $('.J_save').on('click', () => {
    $btn.button('loading')
    const post = buildI18nList()
    $.post('/admin/i18n/translation-post', JSON.stringify(post), (res) => {
      if (res.error_code === 0) location.reload()
      else RbHighbar.error(res.error_msg)
    })
  })
})

function renderI18nList(data) {
  const $tbody = $('#i18nList tbody')
  const $tmpl = $('#i18nList tr.hide')

  data.forEach((item) => {
    const $tr = $tmpl.clone().removeClass('hide').appendTo($tbody)
    for (let L in item) {
      const $td = $tr.find(`td[data-locale=${L}]`)
      if (L === '_def') $td.text(item[L]).attr('data-key', item._key)
      else $td.find('textarea').val(item[L])
    }
  })

  $('#i18nList').parent().removeClass('rb-loading-active')

  // 记住隐藏
  const hide = $storage.get('i18n-hide')
  hide && hide.split(';').forEach((L) => $(`.J_show-locales input[value=${L}]`)[0].click())
}

function buildI18nList() {
  let data = []
  $('#i18nList tbody tr').each(function () {
    const $tr = $(this)
    if ($tr.hasClass('hide')) return

    let key = $tr.find('td[data-key]').data('key')
    $tr.find('textarea').each(function () {
      const L = $(this).parent().data('locale')
      const text = $(this).val()
      data.push([L, key, text || null])
    })
  })
  return data
}
