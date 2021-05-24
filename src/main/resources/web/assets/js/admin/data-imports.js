/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 导入规则
const _Config = {
  file: null,
  entity: null,
  repeat_opt: 1,
  repeat_fields: null,
  owning_user: null,
  fields_mapping: null,
}

let fields_cached
let import_inprogress = false
let import_taskid

$(document).ready(() => {
  $.get('/commons/metadata/entities?detail=true', (res) => {
    $(res.data).each(function () {
      $('<option value="' + this.name + '">' + this.label + '</option>').appendTo('#toEntity')
    })

    const $toe = $('#toEntity')
      .select2({
        allowClear: false,
      })
      .on('change', function () {
        _renderRepeatFields($(this).val())
        _checkUserPrivileges()
      })
    if ($urlp('entity')) $toe.val($urlp('entity'))
    $toe.trigger('change')
  })

  $createUploader('#upload-input', null, (res) => {
    _Config.file = res.key
    $('.J_upload-input').text($fileCutName(_Config.file))
  })

  $('input[name=repeatOpt]').click(function () {
    _Config.repeat_opt = ~~$(this).val()
    if (_Config.repeat_opt === 3) $('.J_repeatFields').hide()
    else $('.J_repeatFields').show()
  })

  const _onSelectUser = function (s, isRemove) {
    if (isRemove || !s) _Config.owning_user = null
    else _Config.owning_user = s.id
  }
  renderRbcomp(
    <UserSelector hideDepartment={true} hideRole={true} hideTeam={true} multiple={false} onSelectItem={(s, isRemove) => _onSelectUser(s, isRemove)} onClearSelection={() => _onSelectUser()} />,
    'toUser'
  )

  $('.J_step1-btn').click(step_mapping)
  $('.J_step2-btn').click(step_import)
  $('.J_step2-return').click(step_upload)
  $('.J_step3-cancel').click(import_cancel)

  import_taskid = $urlp('task', location.hash)
  if (import_taskid) {
    step_import_show()
    import_inprogress = true
    import_state(import_taskid, true)
  }

  window.onbeforeunload = function () {
    if (import_inprogress === true) return false
  }
})

// 1. 初始导入
const step_upload = () => {
  $('.steps li, .step-content .step-pane').removeClass('active complete')
  $('.steps li[data-step=1], .step-content .step-pane[data-step=1]').addClass('active')
}

// 2. 字段映射
const step_mapping = () => {
  if (!_Config.entity) {
    RbHighbar.create($L('请选择导入实体'))
    return
  }
  if (!_Config.file) {
    RbHighbar.create($L('请上传数据文件'))
    return
  }
  if (_Config.repeat_opt !== 3 && (!_Config.repeat_fields || _Config.repeat_fields.length === 0)) {
    RbHighbar.create($L('请选择充重复判断字段'))
    return
  }

  const $btn = $('.J_step1-btn').button('loading')
  $.get(`/admin/data/data-imports/check-file?file=${$encode(_Config.file)}`, (res) => {
    $btn.button('reset')
    if (res.error_code > 0) {
      RbHighbar.create(res.error_msg)
      return
    }

    const _data = res.data
    if (_data.preview.length < 2 || _data.preview[0].length === 0) {
      RbHighbar.create($L('上传的文件无有效数据'))
      return
    }

    render_fieldsMapping(_data.preview[0], fields_cached)
    $('.steps li, .step-content .step-pane').removeClass('active complete')
    $('.steps li[data-step=1]').addClass('complete')
    $('.steps li[data-step=2], .step-content .step-pane[data-step=2]').addClass('active')
  })
}

// 3. 开始导入
const step_import = () => {
  let fm = {}
  $('#fieldsMapping tbody>tr').each(function () {
    const _this = $(this)
    const col = _this.data('col')
    const field = _this.find('select').val()
    if (field) fm[field] = col
  })
  $(fields_cached).each((idx, item) => {
    if (item.nullable === true || !!item.defaultValue) {
      // Not be must
    } else if (fm[item.name] === undefined) {
      RbHighbar.create($L('%s 为必填字段，请选择', item.label))
      fm = null
      return false
    }
  })
  if (!fm) return
  _Config.fields_mapping = fm

  RbAlert.create($L('请再次确认导入选项和字段映射。开始导入吗？'), {
    confirm: function () {
      this.disabled(true)
      $.post('/admin/data/data-imports/import-submit', JSON.stringify(_Config), (res) => {
        if (res.error_code === 0) {
          this.hide()
          step_import_show()
          import_inprogress = true
          import_taskid = res.data.taskid
          location.hash = '#task=' + import_taskid
          import_state(import_taskid)
        } else RbHighbar.error(res.error_msg)
      })
    },
  })
}

// 3.1. 开始导入
const step_import_show = () => {
  $('.steps li, .step-content .step-pane').removeClass('active complete')
  $('.steps li[data-step=1], .steps li[data-step=2]').addClass('complete')
  $('.steps li[data-step=3], .step-content .step-pane[data-step=3]').addClass('active')
}

// 3.2. 导入状态
const import_state = (taskid, inLoad) => {
  $.get(`/commons/task/state?taskid=${taskid}`, (res) => {
    if (res.error_code !== 0) {
      if (inLoad === true) step_upload()
      else RbHighbar.error(res.error_msg)
      import_inprogress = false
      return
    }
    if (!res.data) {
      setTimeout(() => import_state(taskid), 1000)
      return
    }

    const _data = res.data
    $('.J_import_time').text(_secToTime(~~_data.elapsedTime / 1000))

    if (_data.isCompleted === true) {
      $('.J_import-bar').css('width', '100%')
      $('.J_import_state').text($L('导入完成。共成功导入 %d 条数据', _data.succeeded))
    } else if (_data.isInterrupted === true) {
      $('.J_import_state').text($L('导入被终止。已成功导入 %d 条数据', _data.succeeded))
    }

    if (_data.isCompleted === true || _data.isInterrupted === true) {
      $('.J_step3-cancel').attr('disabled', true).text($L('导入完成'))
      $('.J_step3-logs').removeClass('hide')
      import_inprogress = false
      return
    }

    if (_data.progress > 0) {
      $('.J_import_state').text($L('正在导入 ...') + ' ' + _data.completed + '/' + _data.total)
      $('.J_import-bar').css('width', _data.progress * 100 + '%')
    }

    setTimeout(() => {
      import_state(taskid)
    }, 500)
  })
}

// 3.3. 中断导入
const import_cancel = () => {
  RbAlert.create($L('确认终止导入？请注意已导入数据无法自动删除'), {
    type: 'danger',
    confirm: function () {
      $.post(`/commons/task/cancel?taskid=${import_taskid}`, (res) => {
        if (res.error_code > 0) RbHighbar.error(res.error_msg)
      })
      this.hide()
    },
  })
}

// 渲染字段映射
const render_fieldsMapping = (columns, fields) => {
  const fieldMap = {}
  const fieldSelect = $(`<select><option value="">${$L('无')}</option></select>`)

  $(fields).each((idx, item) => {
    let canNull = item.nullable === false ? ` [${$L('必填')}]` : ''
    if (item.defaultValue) canNull = ''
    $('<option value="' + item.name + '">' + item.label + canNull + '</option>').appendTo(fieldSelect)
    fieldMap[item.name] = item
  })

  const $tbody = $('#fieldsMapping tbody').empty()
  $(columns).each(function (idx, item) {
    const $tr = $('<tr data-col="' + idx + '"></tr>').appendTo($tbody)
    $('<td><em>#' + (idx + 1) + '</em> ' + item + '<i class="zmdi zmdi-arrow-right"></i></td>').appendTo($tr)
    const $td = $('<td></td>').appendTo($tr)
    fieldSelect.clone().appendTo($td)
    $('<td class="pl-3"></td>').appendTo($tr)
  })

  $('#fieldsMapping tbody select')
    .select2({
      placeholder: $L('无'),
    })
    .on('change', function () {
      const val = $(this).val()
      const $toe = $(this).parents('td').next()
      if (val) {
        $toe.parent().addClass('table-active')
        const field = fieldMap[val]
        if (field.defaultValue) $toe.text($L('默认') + ' : ' + field.defaultValue)
        else $toe.text('')
      } else {
        $toe.parent().removeClass('table-active')
        $toe.text('')
      }
    })
}

// 格式化秒显示
function _secToTime(s) {
  if (!s || s <= 0) return '00:00:00'
  let hh = Math.floor(s / 3600)
  let mm = Math.floor(s / 60) % 60
  let ss = ~~(s % 60)
  if (hh < 10) hh = '0' + hh
  if (mm < 10) mm = '0' + mm
  if (ss < 10) ss = '0' + ss
  return hh + ':' + mm + ':' + ss
}

// 检查所属用户权限
function _checkUserPrivileges() {
  if (!_Config.entity || !_Config.owning_user) return
  $.get(`/admin/data/data-imports/check-user?user=${_Config.owning_user}&entity=${_Config.entity}`, (res) => {
    let hasError = []
    if (res.data.canCreate !== true) hasError.push($L('新建'))
    if (res.data.canUpdate !== true) hasError.push($L('编辑'))
    if (hasError.length > 0) {
      renderRbcomp(<RbAlertBox message={$L('选择的用户无 %s 权限。但作为管理员，你可以强制导入', hasError.join('/'))} />, 'user-warn')
    } else {
      $('#user-warn').empty()
    }
  })
}

// 渲染重复判断字段
function _renderRepeatFields(entity) {
  if (!entity) return
  const $el = $('#repeatFields').empty()
  $.get(`/admin/data/data-imports/import-fields?entity=${entity}`, (res) => {
    $(res.data).each(function () {
      if (this.name === 'createdBy' || this.name === 'createdOn' || this.name === 'modifiedOn' || this.name === 'modifiedBy') return
      $('<option value="' + this.name + '">' + this.label + '</option>').appendTo($el)
    })

    $el
      .select2({
        maximumSelectionLength: 3,
        placeholder: $L('选择字段'),
      })
      .on('change', function () {
        _Config.repeat_fields = $(this).val()
      })

    fields_cached = res.data
    _Config.entity = entity
  })
}
