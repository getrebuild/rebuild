/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const ientry = {
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
  init_upload()

  const fileds_render = (entity) => {
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
          placeholder: $lang('SelectSome,Field'),
        })
        .on('change', function () {
          ientry.repeat_fields = $(this).val()
        })

      fields_cached = res.data
      ientry.entity = entity
    })
  }

  $.get('/commons/metadata/entities?detail=true', (res) => {
    $(res.data).each(function () {
      $('<option value="' + this.name + '">' + this.label + '</option>').appendTo('#toEntity')
    })

    const $toe = $('#toEntity')
      .select2({
        allowClear: false,
      })
      .on('change', function () {
        fileds_render($(this).val())
        check_user()
      })
    if ($urlp('entity')) $toe.val($urlp('entity'))
    $toe.trigger('change')
  })

  $('input[name=repeatOpt]').click(function () {
    ientry.repeat_opt = ~~$(this).val()
    if (ientry.repeat_opt === 3) $('.J_repeatFields').hide()
    else $('.J_repeatFields').show()
  })

  $('#toUser')
    .select2({
      placeholder: $lang('Default'),
      minimumInputLength: 1,
      ajax: {
        url: '/commons/search/search',
        delay: 300,
        data: function (params) {
          const query = {
            entity: 'User',
            quickFields: 'loginName,fullName,email,quickCode',
            q: params.term,
          }
          return query
        },
        processResults: function (data) {
          const rs = data.data.map((item) => {
            return item
          })
          return { results: rs }
        },
      },
    })
    .on('change', function () {
      ientry.owning_user = $(this).val() || null
      check_user()
    })

  $('.J_step1-btn').click(step_mapping)
  $('.J_step2-btn').click(step_import)
  $('.J_step2-return').click(step_upload)
  $('.J_step3-cancel').click(import_cancel)

  window.onbeforeunload = function () {
    if (import_inprogress === true) return false
  }
  import_taskid = $urlp('task', location.hash)
  if (import_taskid) {
    step_import_show()
    import_inprogress = true
    import_state(import_taskid, true)
  }
})

const init_upload = () => {
  $('#upload-input').html5Uploader({
    postUrl: rb.baseUrl + '/filex/upload?temp=yes',
    onSelectError: function (field, error) {
      if (error === 'ErrorType') RbHighbar.create($lang('PlsUploadSomeFile').replace('%s', 'EXCEL/CSV '))
      else if (error === 'ErrorMaxSize') RbHighbar.create($lang('ExceedMaxLimit') + ' (50MB)')
    },
    onClientLoad: function () {
      $mp.start()
    },
    onSuccess: function (d) {
      $mp.end()
      d = JSON.parse(d.currentTarget.response)
      if (d.error_code === 0) {
        ientry.file = d.data
        $('.J_upload-input').text($fileCutName(ientry.file))
      } else {
        RbHighbar.error($lang('ErrorUpload'))
      }
    },
  })
}

// 检查所属用户权限
const check_user = () => $setTimeout(check_user0, 200, 'check_user')
const check_user0 = () => {
  if (!ientry.entity || !ientry.owning_user) return
  $.get(`/admin/data/data-imports/check-user?user=${ientry.owning_user}&entity=${ientry.entity}`, (res) => {
    let hasError = []
    if (res.data.canCreate !== true) hasError.push($lang('Create'))
    if (res.data.canUpdate !== true) hasError.push($lang('Update'))
    if (hasError.length > 0) {
      renderRbcomp(<RbAlertBox message={$lang('SelectUserNoPermissionConfirm').replace('%s', hasError.join('/'))} />, 'user-warn')
    } else {
      $('#user-warn').empty()
    }
  })
}

const step_upload = () => {
  $('.steps li, .step-content .step-pane').removeClass('active complete')
  $('.steps li[data-step=1], .step-content .step-pane[data-step=1]').addClass('active')
}
const step_mapping = () => {
  if (!ientry.entity) {
    RbHighbar.create($lang('PlsSelectSome,ImportEntity'))
    return
  }
  if (!ientry.file) {
    RbHighbar.create($lang('PlsUploadSome,DataFile'))
    return
  }
  if (ientry.repeat_opt !== 3 && (!ientry.repeat_fields || ientry.repeat_fields.length === 0)) {
    RbHighbar.create($lang('PlsSelectSome,DuplicateFields'))
    return
  }

  const $btn = $('.J_step1-btn').button('loading')
  $.get(`/admin/data/data-imports/check-file?file=${$encode(ientry.file)}`, (res) => {
    $btn.button('reset')
    if (res.error_code > 0) {
      RbHighbar.create(res.error_msg)
      return
    }

    const _data = res.data
    if (_data.preview.length < 2 || _data.preview[0].length === 0) {
      RbHighbar.create($lang('UploadedFileNoData'))
      return
    }

    render_fieldsMapping(_data.preview[0], fields_cached)
    $('.steps li, .step-content .step-pane').removeClass('active complete')
    $('.steps li[data-step=1]').addClass('complete')
    $('.steps li[data-step=2], .step-content .step-pane[data-step=2]').addClass('active')
  })
}
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
      RbHighbar.create(item.label + ' ' + $lang('SomeRequiredField'))
      fm = null
      return false
    }
  })
  if (!fm) return
  ientry.fields_mapping = fm

  RbAlert.create($lang('DataImportConfirm'), {
    confirm: function () {
      this.disabled(true)
      $.post('/admin/data/data-imports/import-submit', JSON.stringify(ientry), (res) => {
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
const step_import_show = () => {
  $('.steps li, .step-content .step-pane').removeClass('active complete')
  $('.steps li[data-step=1], .steps li[data-step=2]').addClass('complete')
  $('.steps li[data-step=3], .step-content .step-pane[data-step=3]').addClass('active')
}
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
    $('.J_import_time').text(sec_to_time(~~_data.elapsedTime / 1000))

    if (_data.isCompleted === true) {
      $('.J_import-bar').css('width', '100%')
      $('.J_import_state').text($lang('ImportFinshedTips').replace('%d', _data.succeeded))
    } else if (_data.isInterrupted === true) {
      $('.J_import_state').text($lang('ImportInterrupttedTips').replace('%d', _data.succeeded))
    }
    if (_data.isCompleted === true || _data.isInterrupted === true) {
      $('.J_step3-cancel').attr('disabled', true).text($lang('ImportFinshed'))
      $('.J_step3-logs').removeClass('hide')
      import_inprogress = false
      return
    }

    if (_data.total > -1) {
      $('.J_import_state').text($lang('DataImporting') + ' ' + _data.completed + '/' + _data.total)
      $('.J_import-bar').css('width', _data.progress * 100 + '%')
    }
    setTimeout(() => {
      import_state(taskid)
    }, 500)
  })
}
const import_cancel = () => {
  RbAlert.create($lang('ImportInterrupttedConfirm'), {
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
  const fieldSelect = $(`<select><option value="">${$lang('Null')}</option></select>`)

  $(fields).each((idx, item) => {
    let canNull = item.nullable === false ? ` [${$lang('Required')}]` : ''
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
      placeholder: $lang('Null'),
    })
    .on('change', function () {
      const val = $(this).val()
      const $toe = $(this).parents('td').next()
      if (val) {
        $toe.parent().addClass('table-active')
        const field = fieldMap[val]
        if (field.defaultValue) $toe.text($lang('Default') + ' : ' + field.defaultValue)
        else $toe.text('')
      } else {
        $toe.parent().removeClass('table-active')
        $toe.text('')
      }
    })
}

const sec_to_time = function (s) {
  if (!s || s <= 0) return '00:00:00'
  let hh = Math.floor(s / 3600)
  let mm = Math.floor(s / 60) % 60
  let ss = ~~(s % 60)
  if (hh < 10) hh = '0' + hh
  if (mm < 10) mm = '0' + mm
  if (ss < 10) ss = '0' + ss
  return hh + ':' + mm + ':' + ss
}
