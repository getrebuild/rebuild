const wpc = window.__PageConfig
$(document).ready(function () {
  let dt = wpc.fieldType
  if (dt.indexOf('(') > -1) dt = dt.match('\\((.+?)\\)')[1]
  const extConfigOld = wpc.extConfig

  $('.J_save').click(function () {
    if (!wpc.metaId) return
    let label = $val('#fieldLabel'),
      comments = $val('#comments'),
      nullable = $val('#fieldNullable'),
      updatable = $val('#fieldUpdatable')
    let _data = {
      fieldLabel: label,
      comments: comments,
      nullable: nullable,
      updatable: updatable
    }
    _data = $cleanMap(_data)

    let extConfig = {}
    $('.J_for-' + dt + ' .form-control').each(function () {
      let k = $(this).attr('id')
      let v = $val(this)
      extConfig[k] = v
      if ('defaultValue' === k && checkDefaultValue(v, dt) === false) {
        extConfig = null
        return false
      }
    })
    if (!extConfig) return

    for (let k in extConfig) {
      if (extConfig[k] !== extConfigOld[k]) {
        _data['extConfig'] = JSON.stringify(extConfig)
        break
      }
    }

    _data = $cleanMap(_data)
    if (Object.keys(_data).length === 0) {
      location.href = '../fields'
      return
    }

    _data.metadata = {
      entity: 'MetaField',
      id: wpc.metaId
    }
    _data = JSON.stringify(_data)

    let _btn = $(this).button('loading')
    $.post(rb.baseUrl + '/admin/entity/field-update', _data, function (res) {
      if (res.error_code === 0) {
        location.href = '../fields'
      } else {
        _btn.button('reset')
        rb.hberror(res.error_msg)
      }
    })
  })

  $('#fieldNullable').attr('checked', $('#fieldNullable').data('o') === true)
  $('#fieldUpdatable').attr('checked', $('#fieldUpdatable').data('o') === true)

  $('.J_for-' + dt).removeClass('hide')

  let uploadNumber = [1, 5]
  for (let k in extConfigOld) {
    if (k === 'uploadNumber') {
      uploadNumber = extConfigOld[k].split(',')
      uploadNumber[0] = ~~uploadNumber[0]
      uploadNumber[1] = ~~uploadNumber[1]
      $('.J_minmax b').eq(0).text(uploadNumber[0])
      $('.J_minmax b').eq(1).text(uploadNumber[1])
    } else $('#' + k).val(extConfigOld[k])
  }
  $('input.bslider').slider({
    value: uploadNumber
  }).on('change', function (e) {
    let v = e.value.newValue
    $('.J_minmax b').eq(0).text(v[0])
    $('.J_minmax b').eq(1).text(v[1])
  })

  if (dt === 'PICKLIST') {
    $.get(`${rb.baseUrl}/admin/field/picklist-gets?entity=${wpc.entityName}&field=${wpc.fieldName}&isAll=false`, function (res) {
      if (res.data.length === 0) {
        $('#picklist-items li').text('请添加选项');
        return
      }
      $('#picklist-items').empty()
      $(res.data).each(function () {
        picklistItemRender(this)
      })
      if (res.data.length > 5) $('#picklist-items').parent().removeClass('autoh')
    })

    $('.J_picklist-edit').click(function () {
      rb.modal(`${rb.baseUrl}/admin/p/entity/picklist-config?entity=${wpc.entityName}&field=${wpc.fieldName}`, '配置列表选项')
    })
  } else if (dt === 'SERIES' || wpc.fieldBuildin === true) {
    $('#fieldNullable, #fieldUpdatable').attr('disabled', true)
    $('#defaultValue').parents('.form-group').remove()
  } else if (dt === 'DATE' || dt === 'DATETIME') {
    $('#defaultValue').datetimepicker({
      componentIcon: 'zmdi zmdi-calendar',
      navIcons: {
        rightIcon: 'zmdi zmdi-chevron-right',
        leftIcon: 'zmdi zmdi-chevron-left'
      },
      format: dt === 'DATE' ? 'yyyy-mm-dd' : 'yyyy-mm-dd hh:ii:ss',
      minView: dt === 'DATE' ? 2 : 0,
      weekStart: 1,
      autoclose: true,
      language: 'zh',
      todayHighlight: false,
      showMeridian: false,
      keyboardNavigation: false,
      minuteStep: 5,
    }).on('changeDate', function () {
      let val = $(this).val()

    })
  }

  if (wpc.fieldBuildin === true) $('.footer .alert').removeClass('hide')
  else $('.footer .J_action').removeClass('hide')

  $('.J_del').click(function () {
    if (!wpc.isSuperAdmin) {
      rb.hberror('仅超级管理员可删除字段');
      return
    }
    let alertExt = {
      type: 'danger',
      confirmText: '删除'
    }
    alertExt.confirm = function () {
      $(this.refs['btns']).find('.btn').button('loading')
      let thatModal = this
      $.post(`${rb.baseUrl}/admin/entity/field-drop?id=${wpc.metaId}`, function (res) {
        if (res.error_code === 0) {
          thatModal.hide()
          rb.hbsuccess('字段已删除')
          setTimeout(function () {
            location.replace('../fields')
          }, 1500)
        } else rb.hberror(res.error_msg)
      })
    }
    rb.alert('字段删除后将无法恢复，请务必谨慎操作！确认删除吗？', '删除字段', alertExt)
  })
})

// Render item to PickList box
const picklistItemRender = function (data) {
  let item = $('<li class="dd-item" data-key="' + data.id + '"><div class="dd-handle">' + data.text + '</div></li>').appendTo('#picklist-items')
  if (data['default'] === true) item.addClass('default')
}

// Check incorrect?
// Also see RbFormElement#checkHasError in rb-forms.jsx
const checkDefaultValue = function (v, t) {
  let valid = true
  if (t === 'NUMBER' || t === 'DECIMAL') {
    valid = !isNaN(v)
  } else if (t === 'URL') {
    valid = $regex.isUrl(v)
  } else if (t === 'EMAIL') {
    valid = $regex.isMail(v)
  } else if (t === 'PHONE') {
    valid = $regex.isTel(v)
  }
  if (valid === false) rb.highbar('默认值设置有误')
  return valid
}