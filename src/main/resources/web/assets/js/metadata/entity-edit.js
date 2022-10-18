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

$(document).ready(function () {
  if (!wpc.metaId) $('.footer .alert').removeClass('hide')
  else $('.footer .J_action').removeClass('hide')

  $(`.J_tab-${wpc.entity} a`).addClass('active')

  const $btn = $('.J_save').on('click', function () {
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
    $.post('../entity-update', JSON.stringify(data), function (res) {
      if (res.error_code === 0) location.reload()
      else RbHighbar.error(res.error_msg)
    })
  })

  $('#entityIcon').on('click', function () {
    RbModal.create('/p/common/search-icon', $L('选择图标'))
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
  const _SYS_REF_FIELDS = ['createdBy', 'modifiedBy', 'owningUser', 'owningDept', 'approvalId', 'approvalLastUser']

  $.get(`/commons/metadata/fields?deep=1&entity=${wpc.entity}`, function (d) {
    // 名称字段
    const cNameFields = d.data.map((item) => {
      const canName =
        item.type === 'TEXT' ||
        item.type === 'EMAIL' ||
        item.type === 'URL' ||
        item.type === 'PHONE' ||
        item.type === 'SERIES' ||
        item.type === 'LOCATION' ||
        item.type === 'PICKLIST' ||
        item.type === 'CLASSIFICATION' ||
        item.type === 'DATE' ||
        item.type === 'DATETIME' ||
        item.type === 'TIME' ||
        /* 开放引用字段是否有问题 ??? */
        (item.type === 'REFERENCE' && !_SYS_REF_FIELDS.includes(item.name))
      return {
        id: item.name,
        text: item.label,
        disabled: canName === false,
        title: canName === false ? $L('字段 (类型) 不适用') : item.label,
      }
    })

    $('#nameField')
      .select2({
        placeholder: $L('选择字段'),
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
        item.type === 'LOCATION' ||
        item.type === 'PICKLIST' ||
        item.type === 'CLASSIFICATION' ||
        // item.type === 'DATE' ||
        // item.type === 'DATETIME'
        (item.type === 'REFERENCE' && _SYS_REF_FIELDS.indexOf(item.name) === -1)

      return {
        id: item.name,
        text: item.label,
        disabled: canQuick === false,
        title: canQuick === false ? $L('字段 (类型) 不适用') : item.label,
      }
    })

    $('#quickFields').select2({
      placeholder: $L('默认'),
      allowClear: true,
      data: sortFields(cQuickFields),
      multiple: true,
      maximumSelectionLength: 5,
    })
    if (wpc.extConfig.quickFields) {
      $('#quickFields').val(wpc.extConfig.quickFields.split(',')).trigger('change')
    }
  })

  $.get('/admin/entity/entity-tags', (res) => {
    let data = res.data || []
    data = data.sort().map((item) => {
      return { id: item, text: item }
    })

    $('#tags').select2({
      placeholder: $L('无'),
      data: data,
      multiple: true,
      maximumSelectionLength: 5,
      language: {
        noResults: function () {
          return $L('输入标签')
        },
      },
      tags: true,
    })

    if (wpc.extConfig.tags) {
      $('#tags').val(wpc.extConfig.tags.split(',')).trigger('change')
    }
  })
})
