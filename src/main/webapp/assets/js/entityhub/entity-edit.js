// eslint-disable-next-line no-unused-vars
// eslint-disable-next-line no-undef
window.clickIcon = function (icon) {
  $('#entityIcon').attr('value', icon).find('i').attr('class', 'icon zmdi zmdi-' + icon)
  RbModal.hide()
}

const wpc = window.__PageConfig
$(document).ready(function () {
  if (!wpc.metaId) $('.footer .alert').removeClass('hide')
  else $('.footer .J_action').removeClass('hide')

  $('.J_tab-' + wpc.entity + ' a').addClass('active')

  let _btn = $('.J_save').click(function () {
    if (!wpc.metaId) return
    let _data = {
      entityLabel: $val('#entityLabel'),
      comments: $val('#comments'),
      nameField: $val('#nameField')
    }
    if (_data.label === '') { RbHighbar.create('请输入实体名称'); return }
    let icon = $val('#entityIcon')
    if (icon) _data.icon = icon
    _data = $cleanMap(_data)
    if (Object.keys(_data) === 0) { location.reload(); return }

    _data.metadata = { entity: 'MetaEntity', id: wpc.metaId }
    _btn.button('loading')
    $.post('../entity-update', JSON.stringify(_data), function (res) {
      if (res.error_code === 0) location.reload()
      else RbHighbar.error(res.error_msg)
    })
  })

  $('#entityIcon').click(function () {
    RbModal.create(rb.baseUrl + '/p/commons/search-icon', '选择图标')
  })

  $.get(rb.baseUrl + '/commons/metadata/fields?entity=' + wpc.entity, function (d) {
    let rs = d.data.map((item) => {
      let canName = item.type === 'NUMBER' || item.type === 'DECIMAL' ||
        item.type === 'TEXT' || item.type === 'EMAIL' || item.type === 'URL' || item.type === 'PHONE' ||
        item.type === 'SERIES' || item.type === 'PICKLIST' || item.type === 'CLASSIFICATION' ||
        item.type === 'DATE' || item.type === 'DATETIME'
      return {
        id: item.name,
        text: item.label,
        disabled: canName === false,
        title: canName === false ? '此字段（类型）不支持作为名称字段使用' : ''
      }
    })
    let rsSort = []
    rs.forEach((item) => {
      if (item.disabled === false) rsSort.push(item)
    })
    rs.forEach((item) => {
      if (item.disabled === true) rsSort.push(item)
    })
    rs = rsSort

    $('#nameField').select2({
      placeholder: '选择字段',
      allowClear: false,
      data: rs
    }).val(wpc.nameField).trigger('change')
  })
})