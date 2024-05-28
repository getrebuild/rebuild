/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// eslint-disable-next-line no-undef, no-unused-vars
window.clickIcon = function (icon) {
  $('#entityIcon').attr('value', icon).find('i').attr('class', `icon zmdi zmdi-${icon}`)
  RbModal.hide()
}

const wpc = window.__PageConfig

$(document).ready(() => {
  if (!wpc.metaId) $('.footer .alert').removeClass('hide')
  else $('.footer .J_action').removeClass('hide')

  $(`.nav-tabs>li[data-name=${wpc.entity}]>a`).addClass('active')
  if ($('.J_details')[0]) {
    const $toggle = $('.J_for-details')
    $('<i class="icon zmdi zmdi-caret-down ml-1 mr-0 text-muted fs-16"></i>').appendTo($toggle)
    $toggle.attr('data-toggle', 'dropdown')
    $toggle.next().find(`a[data-name=${wpc.entity}]`).addClass('text-primary')
  }

  const $btn = $('.J_save').on('click', () => {
    if (!wpc.metaId) return

    let data = {
      entityLabel: $val('#entityLabel'),
      nameField: $val('#nameField'),
      comments: $val('#comments'),
    }
    if (data.entityLabel === '') return RbHighbar.create($L('请输入实体名称'))

    const icon = $val('#entityIcon')
    if (icon) data.icon = icon

    let extConfig = {
      quickFields: $('#quickFields').val().join(','),
      tags: $('#tags').val().join(','),
    }
    if ($('#detailsNotEmpty')[0]) {
      extConfig.detailsNotEmpty = $val('#detailsNotEmpty')
      extConfig.detailsGlobalRepeat = $val('#detailsGlobalRepeat')
      extConfig.detailsShowAt2 = $val('#detailsShowAt2')
      // v3.6
      extConfig.detailsCopiable = $val('#detailsCopiable')
    }
    extConfig.repeatFieldsCheckMode = $val('#repeatFieldsCheckMode') ? 'and' : 'or'
    extConfig.disabledViewEditable = $val('#disabledViewEditable')
    extConfig.enableRecordMerger = $val('#enableRecordMerger')

    // v3.6
    if (rb.commercial < 10) {
      const checkAdv = ['detailsNotEmpty', 'detailsGlobalRepeat', 'detailsShowAt2', 'detailsCopiable', 'repeatFieldsCheckMode', 'disabledViewEditable', 'enableRecordMerger']
      let needRbv = false
      for (let i = 0; i < checkAdv.length; i++) {
        if ($val(`#${checkAdv[i]}`)) {
          needRbv = true
          break
        }
      }

      if (needRbv) {
        RbHighbar.error(WrapHtml($L('免费版不支持高级选项 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
        return
      }
    }

    extConfig = wpc.extConfig ? { ...wpc.extConfig, ...extConfig } : extConfig
    if (!$same(extConfig, wpc.extConfig)) data.extConfig = extConfig

    data = $cleanMap(data)
    if (Object.keys(data).length === 0) {
      location.reload()
      return
    }

    data.metadata = {
      entity: 'MetaEntity',
      id: wpc.metaId,
    }

    $btn.button('loading')
    $.post('../entity-update', JSON.stringify(data), (res) => {
      if (res.error_code === 0) location.reload()
      else RbHighbar.error(res.error_msg)
    })
  })

  $('#entityIcon').on('click', () => RbModal.create('/p/common/search-icon', $L('选择图标')))

  const SYS_FIELDS = ['approvalId', 'approvalLastUser']
  const CAN_NAME = ['TEXT', 'EMAIL', 'URL', 'PHONE', 'SERIES', 'LOCATION', 'PICKLIST', 'CLASSIFICATION', 'DATE', 'DATETIME', 'TIME', 'REFERENCE']
  const CAN_QUICK = ['TEXT', 'EMAIL', 'URL', 'PHONE', 'SERIES', 'LOCATION', 'PICKLIST', 'CLASSIFICATION', 'REFERENCE']

  $.get(`/commons/metadata/fields?deep=2&entity=${wpc.entity}`, (res) => {
    // 名称字段
    const canNameFields = []
    res.data.forEach((item) => {
      let canName = CAN_NAME.includes(item.type) && !SYS_FIELDS.includes(item.name)
      if (canName && item.type === 'REFERENCE') canName = item.ref[0] !== wpc.entity
      if (canName && item.name.includes('.')) canName = false

      if (canName) {
        canNameFields.push({
          id: item.name,
          text: item.label,
        })
      }
    })
    $('#nameField')
      .select2({
        placeholder: $L('选择字段'),
        allowClear: false,
        data: canNameFields,
      })
      .val(wpc.nameField || null)
      .trigger('change')

    // 快速查询
    const canQuickFields = []
    res.data.forEach((item) => {
      let canQuick = CAN_QUICK.includes(item.type)
      if (canQuick && item.type === 'REFERENCE') canQuick = item.ref[0] !== wpc.entity

      if (canQuick) {
        canQuickFields.push({
          id: item.name,
          text: item.label,
        })
      }
    })
    $('#quickFields').select2({
      placeholder: $L('默认'),
      allowClear: true,
      data: canQuickFields,
      multiple: true,
      maximumSelectionLength: 9,
    })

    if (wpc.extConfig.quickFields) {
      $('#quickFields').val(wpc.extConfig.quickFields.split(',')).trigger('change')
    }
  })

  $.get('/admin/entity/entity-tags', (res) => {
    let s2data = res.data || []
    s2data = s2data.map((item) => {
      return { id: item, text: item }
    })
    $('#tags').select2({
      placeholder: $L('无'),
      data: s2data,
      multiple: true,
      maximumSelectionLength: 9,
      language: {
        noResults: function () {
          return $L('请输入')
        },
      },
      tags: true,
      theme: 'default select2-tag',
    })

    if (wpc.extConfig.tags) {
      $('#tags').val(wpc.extConfig.tags.split(',')).trigger('change')
    }

    $('.adv-options-btn').on('click', () => {
      $('.adv-options-btn').addClass('hide')
      $('.adv-options').toggleClass('hide')
    })
  })

  if (wpc.extConfig.detailsNotEmpty) $('#detailsNotEmpty').attr('checked', true)
  if (wpc.extConfig.detailsGlobalRepeat) $('#detailsGlobalRepeat').attr('checked', true)
  if (wpc.extConfig.repeatFieldsCheckMode === 'and') $('#repeatFieldsCheckMode').attr('checked', true)
  if (wpc.extConfig.disabledViewEditable) $('#disabledViewEditable').attr('checked', true)
  if (wpc.extConfig.detailsShowAt2) $('#detailsShowAt2').attr('checked', true)
  // v3.6
  if (wpc.extConfig.detailsCopiable) $('#detailsCopiable').attr('checked', true)
  if (wpc.extConfig.enableRecordMerger) $('#enableRecordMerger').attr('checked', true)
})
