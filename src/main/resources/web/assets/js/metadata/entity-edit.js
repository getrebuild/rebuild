/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// eslint-disable-next-line no-undef, no-unused-vars
window.clickIcon = function (icon) {
  $('#entityIcon')
    .attr('value', icon)
    .find('i')
    .attr('class', 'icon zmdi zmdi-' + icon)
  RbModal.hide()
}

const wpc = window.__PageConfig

$(document).ready(function () {
  if (!wpc.metaId) $('.footer .alert').removeClass('hide')
  else $('.footer .J_action').removeClass('hide')

  $('.J_tab-' + wpc.entity + ' a').addClass('active')

  const $btn = $('.J_save').click(function () {
    if (!wpc.metaId) return

    let data = {
      entityLabel: $val('#entityLabel'),
      nameField: $val('#nameField'),
      comments: $val('#comments'),
    }
    if (data.entityLabel === '') return RbHighbar.create($L('PlsInputSome,EntityName'))

    const icon = $val('#entityIcon')
    if (icon) data.icon = icon

    const quickFields = $('#quickFields').val().join(',')
    if (quickFields !== wpc.extConfig.quickFields) {
      data.extConfig = {
        quickFields: quickFields,
      }
    }

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
    $.post('../entity-update', JSON.stringify(data), function (res) {
      if (res.error_code === 0) location.reload()
      else RbHighbar.error(res.error_msg)
    })
  })

  $('#entityIcon').click(function () {
    RbModal.create('/p/common/search-icon', $L('SelectSome,Icon'))
  })

  // 排序
  function sortFields(fields) {
    const ss = []
    fields.forEach((item) => {
      if (item.disabled === false) ss.push(item)
    })
    fields.forEach((item) => {
      if (item.disabled === true) ss.push(item)
    })
    return ss
  }

  // 系统级引用字段
  const _SYS_REF_FIELDS = ['createdBy', 'modifiedBy', 'owningUser', 'owningDept', 'approvalId']

  $.get(`/commons/metadata/fields?deep=1&entity=${wpc.entity}`, function (d) {
    // 名称字段
    const cNameFields = d.data.map((item) => {
      const canName =
        item.type === 'TEXT' ||
        item.type === 'EMAIL' ||
        item.type === 'URL' ||
        item.type === 'PHONE' ||
        item.type === 'SERIES' ||
        item.type === 'PICKLIST' ||
        item.type === 'CLASSIFICATION' ||
        item.type === 'DATE' ||
        item.type === 'DATETIME' ||
        /* 开放引用字段是否有问题 ??? */
        (item.type === 'REFERENCE' && _SYS_REF_FIELDS.indexOf(item.name) === -1)
      return {
        id: item.name,
        text: item.label,
        disabled: canName === false,
        title: canName === false ? $L('FieldNotApply') : item.label,
      }
    })

    $('#nameField')
      .select2({
        placeholder: $L('SelectSome,Field'),
        allowClear: false,
        data: sortFields(cNameFields),
      })
      .val(wpc.nameField)
      .trigger('change')

    // 快速查询
    const cQuickFields = d.data.map((item) => {
      const canQuick =
        item.type === 'TEXT' ||
        item.type === 'EMAIL' ||
        item.type === 'URL' ||
        item.type === 'PHONE' ||
        item.type === 'SERIES' ||
        item.type === 'PICKLIST' ||
        item.type === 'CLASSIFICATION' ||
        // item.type === 'DATE' ||
        // item.type === 'DATETIME'
        (item.type === 'REFERENCE' && _SYS_REF_FIELDS.indexOf(item.name) === -1)

      return {
        id: item.name,
        text: item.label,
        disabled: canQuick === false,
        title: canQuick === false ? $L('FieldNotApply') : item.label,
      }
    })

    $('#quickFields').select2({
      placeholder: $L('SelectSome,Field'),
      allowClear: true,
      data: sortFields(cQuickFields),
      multiple: true,
      maximumSelectionLength: 5,
    })
    if (wpc.extConfig.quickFields) {
      $('#quickFields').val(wpc.extConfig.quickFields.split(',')).trigger('change')
    }
  })
})
