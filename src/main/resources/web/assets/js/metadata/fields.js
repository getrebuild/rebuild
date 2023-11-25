/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

$(document).ready(() => {
  loadFields()

  $('.input-search .btn').on('click', () => renderList())
  $('.input-search .form-control').keydown((e) => {
    if (e.which === 13) $('.input-search .btn').trigger('click')
  })
  $('.J_new-field').on('click', () => {
    if (wpc.isSuperAdmin) RbModal.create(`/p/admin/metadata/field-new?entity=${wpc.entityName}`, $L('添加字段'))
    else RbHighbar.error($L('仅超级管理员可添加字段'))
  })

  $('.J_new2-field').on('click', () => {
    if (wpc.isSuperAdmin) RbModal.create(`/p/admin/metadata/field-new2?entity=${wpc.entityName}`, $L('批量添加字段'), { width: 1064 })
    else RbHighbar.error($L('仅超级管理员可添加字段'))
  })

  $('.J_export-fields').on('click', exportFields)
})

let fields_data = []
const loadFields = function () {
  $.get(`../list-field?entity=${wpc.entityName}&refname=true`, function (res) {
    fields_data = res.data || []
    renderList()

    $('.tablesort').tablesort()
  })
}

const renderList = function () {
  const $tbody = $('#dataList tbody').empty()
  const q = ($val('.input-search .form-control') || '').toLowerCase()

  fields_data.forEach((item) => {
    if (q && !(item.fieldName.toLowerCase().contains(q) || item.fieldLabel.toLowerCase().contains(q))) return

    const $tr = $(`<tr data-id="${item.fieldId || ''}"></tr>`).appendTo($tbody)
    const $name = $(`<td><a href="field/${item.fieldName}" class="column-main">${item.fieldLabel}</a></td>`).appendTo($tr)
    if (item.fieldName === wpc.nameField) {
      $tr.addClass('primary')
      $(`<span class="badge badge-pill badge-secondary font-weight-light ml-1 pb-0">${$L('名称')}</span>`).appendTo($name)
    } else if (!item.creatable) {
      $tr.addClass('muted')
    } else if (!item.nullable) {
      $tr.addClass('danger')
    }
    $(`<td><div class="text-muted">${item.fieldName}</div></td>`).appendTo($tr)
    let type = item.displayType
    if (item.displayTypeRef) type += ` (${item.displayTypeRef[1]})`
    $(`<td><div class="text-muted">${type}</div></td>`).appendTo($tr)
    $(`<td><div class="text-none" _title="${$L('无')}">${item.comments || ''}</div></td>`).appendTo($tr)
    $(`<td class="actions"><a class="icon J_edit" href="field/${item.fieldName}"><i class="zmdi zmdi-settings"></i></a></td>`).appendTo($tr)
  })

  $('.dataTables_info').text($L('共 %d 个字段', $tbody.find('tr').length))
  $('#dataList').parent().removeClass('rb-loading-active')
}

function exportFields() {
  const rows = [[$L('内部标识'), $L('字段名称'), $L('类型'), $L('必填'), $L('只读'), $L('备注')].join(', ')]
  rows.push([`${wpc.entityName}Id`, $L('主键'), 'ID', 'N', 'Y', ''].join(', '))
  fields_data.forEach((item) => {
    let type = item.displayType
    if (item.displayTypeRef) type += ` (${item.displayTypeRef[1]})`
    rows.push([item.fieldName, item.fieldLabel, type, item.nullable ? 'N' : 'Y', item.creatable ? 'N' : 'Y', item.comments ? item.comments.replace(/[,;]/, ' ') : ''].join(', '))
  })

  const encodedUri = encodeURI('data:text/csv;charset=utf-8,\ufeff' + rows.join('\n'))
  const link = document.createElement('a')
  link.setAttribute('href', encodedUri)
  link.setAttribute('download', `RBFIELDS_${wpc.entityName.toUpperCase()}.csv`)
  document.body.appendChild(link)
  link.click()
  setTimeout(() => document.body.removeChild(link), 1000)
}
