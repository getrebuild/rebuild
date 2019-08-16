let fields_cached
let ientry = {
  file: null,
  entity: null,
  repeat_opt: 1,
  repeat_fields: null,
  owning_user: null,
  fields_mapping: null
}
let import_inprogress = false
let import_taskid
$(document).ready(() => {
  init_upload()

  let fileds_render = (entity) => {
    if (!entity) return
    let el = $('#repeatFields').empty()
    $.get(`${rb.baseUrl}/admin/datas/data-importer/import-fields?entity=${entity}`, (res) => {
      $(res.data).each(function () {
        if (this.name === 'createdBy' || this.name === 'createdOn' || this.name === 'modifiedOn' || this.name === 'modifiedBy') return
        $('<option value="' + this.name + '">' + this.label + '</option>').appendTo(el)
      })

      el.select2({
        maximumSelectionLength: 3,
        placeholder: '选择字段'
      }).on('change', function () {
        ientry.repeat_fields = $(this).val()
      })

      fields_cached = res.data
      ientry.entity = entity
    })
  }
  $.get(`${rb.baseUrl}/commons/metadata/entities`, (res) => {
    $(res.data).each(function () {
      $('<option value="' + this.name + '">' + this.label + '</option>').appendTo('#toEntity')
    })
    $('#toEntity').select2({
      allowClear: false
    }).on('change', function () {
      fileds_render($(this).val())
      check_ouser()
    }).trigger('change')
  })

  $('input[name=repeatOpt]').click(function () {
    ientry.repeat_opt = ~~$(this).val()
    if (ientry.repeat_opt === 3) $('.J_repeatFields').hide()
    else $('.J_repeatFields').show()
  })

  $('#toUser').select2({
    placeholder: '默认',
    minimumInputLength: 1,
    ajax: {
      url: rb.baseUrl + '/commons/search/search',
      delay: 300,
      data: function (params) {
        let query = {
          entity: 'User',
          qfields: 'loginName,fullName,email,quickCode',
          q: params.term
        }
        return query
      },
      processResults: function (data) {
        let rs = data.data.map((item) => { return item })
        return { results: rs }
      }
    }
  }).on('change', function () {
    ientry.owning_user = $(this).val() || null
    check_ouser()
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
      if (error === 'ErrorType') RbHighbar.create('请上传 Excel/CSV 文件')
      else if (error === 'ErrorMaxSize') RbHighbar.create('文件不能大于 20M')
    },
    onSuccess: function (d) {
      d = JSON.parse(d.currentTarget.response)
      if (d.error_code === 0) {
        ientry.file = d.data
        $('.J_upload-input').text($fileCutName(ientry.file))
      } else RbHighbar.error('上传失败，请稍后重试')
    }
  })
}

const check_ouser = () => {
  $setTimeout(check_ouser0, 200, 'check_ouser')
}
const check_ouser0 = () => {
  if (!(ientry.entity && ientry.owning_user)) return
  $.get(`${rb.baseUrl}/admin/datas/data-importer/check-user-privileges?ouser=${ientry.owning_user}&entity=${ientry.entity}`, (res) => {
    let hasError = []
    if (res.data.canCreate !== true) hasError.push('新建')
    if (res.data.canUpdate !== true) hasError.push('更新')
    if (hasError.length >= 2) {
      $('.J_step1-btn').attr('disabled', true)
      renderRbcomp(<RbAlertBox type="danger" message={'选择的用户无' + hasError.join('及') + '权限，请选择其他用户'} />, 'ouser-warn')
    } else {
      $('.J_step1-btn').attr('disabled', false)
      if (hasError.length > 0) {
        renderRbcomp(<RbAlertBox message={'选择的用户无' + hasError.join('/') + '权限，可能导致部分数据导入失败'} />, 'ouser-warn')
      } else {
        $('#ouser-warn').empty()
      }
    }
  })
}

const step_upload = () => {
  $('.steps li, .step-content .step-pane').removeClass('active complete')
  $('.steps li[data-step=1], .step-content .step-pane[data-step=1]').addClass('active')
}
const step_mapping = () => {
  if (!ientry.entity) { RbHighbar.create('请选择导入实体'); return }
  if (!ientry.file) { RbHighbar.create('请上传数据文件'); return }
  if (ientry.repeat_opt !== 3 && (!ientry.repeat_fields || ientry.repeat_fields.length === 0)) { RbHighbar.create('请选择重复判断字段'); return }

  let btn = $('.J_step1-btn').button('loading')
  $.get(`${rb.baseUrl}/admin/datas/data-importer/check-file?file=${$encode(ientry.file)}`, (res) => {
    btn.button('reset')
    if (res.error_code > 0) { RbHighbar.create(res.error_msg); return }
    let _data = res.data
    if (_data.count < 2 || _data.preview.length < 2 || _data.preview[0].length === 0) { RbHighbar.create('上传的文件无有效数据'); return }

    render_fieldsMapping(_data.preview[0], fields_cached)
    $('.steps li, .step-content .step-pane').removeClass('active complete')
    $('.steps li[data-step=1]').addClass('complete')
    $('.steps li[data-step=2], .step-content .step-pane[data-step=2]').addClass('active')
  })
}
const step_import = () => {
  let fm = {}
  $('#fieldsMapping tbody>tr').each(function () {
    let _this = $(this)
    let col = _this.data('col')
    let field = _this.find('select').val()
    if (field) fm[field] = col
  })
  $(fields_cached).each((idx, item) => {
    if (item.isNullable === true || !!item.defaultValue) {
      // Not be must
    } else if (fm[item.name] === undefined) {
      RbHighbar.create(item.label + ' 为必填字段，请选择')
      fm = null
      return false
    }
  })
  if (!fm) return
  ientry.fields_mapping = fm

  step_import_show()
  $.post(`${rb.baseUrl}/admin/datas/data-importer/import-submit`, JSON.stringify(ientry), function (res) {
    if (res.error_code === 0) {
      import_inprogress = true
      import_taskid = res.data.taskid
      location.hash = '#task=' + import_taskid
      import_state(import_taskid)
    } else RbHighbar.error(res.error_msg)
  })
}
const step_import_show = () => {
  $('.steps li, .step-content .step-pane').removeClass('active complete')
  $('.steps li[data-step=1], .steps li[data-step=2]').addClass('complete')
  $('.steps li[data-step=3], .step-content .step-pane[data-step=3]').addClass('active')
}
const import_state = (taskid, inLoad) => {
  $.get(`${rb.baseUrl}/admin/datas/data-importer/import-state?taskid=${taskid}`, (res) => {
    if (res.error_code !== 0) {
      if (inLoad === true) step_upload()
      else RbHighbar.error(res.error_msg)
      import_inprogress = false
      return
    }
    if (!res.data) {
      setTimeout(() => { import_state(taskid) }, 1000)
      return
    }

    let _data = res.data
    $('.J_import_time').text(sec_to_time(~~_data.elapsedTime / 1000))

    if (_data.isCompleted === true) {
      $('.J_import-bar').css('width', '100%')
      $('.J_import_state').text('导入完成。共成功导入 ' + _data.success + ' 条数据')
    } else if (_data.isInterrupted === true) {
      $('.J_import_state').text('导入被终止。已成功导入 ' + _data.success + ' 条数据')
    }
    if (_data.isCompleted === true || _data.isInterrupted === true) {
      $('.J_step3-cancel').attr('disabled', true).text('导入完成')
      $('.J_step3-logs').removeClass('hide')
      import_inprogress = false
      return
    }

    if (_data.total > -1) {
      $('.J_import_state').text('正在导入 ... ' + _data.complete + ' / ' + _data.total)
      $('.J_import-bar').css('width', (_data.complete * 100 / _data.total) + '%')
    }
    setTimeout(() => { import_state(taskid) }, 500)
  })
}
const import_cancel = () => {
  RbAlert.create('确认要终止导入？请注意已导入数据无法自动删除', {
    type: 'danger',
    confirmText: '确认终止',
    confirm: function () {
      $.post(`${rb.baseUrl}/admin/datas/data-importer/import-cancel?taskid=${import_taskid}`, (res) => {
        if (res.error_code > 0) RbHighbar.error(res.error_msg)
      })
      this.hide()
    }
  })
}

const render_fieldsMapping = (columns, fields) => {
  let fields_map = {}
  let fields_select = $('<select><option value="">无</option></select>')
  $(fields).each((idx, item) => {
    let canNull = item.isNullable === false ? ' [必填]' : ''
    if (item.defaultValue) canNull = ''
    $('<option value="' + item.name + '">' + item.label + canNull + '</option>').appendTo(fields_select)
    fields_map[item.name] = item
  })

  let tbody = $('#fieldsMapping tbody').empty()
  $(columns).each(function (idx, item) {
    let tr = $('<tr data-col="' + idx + '"></tr>').appendTo(tbody)
    $('<td><em>#' + (idx + 1) + '</em> ' + item + '<i class="zmdi zmdi-arrow-right"></i></td>').appendTo(tr)
    let td = $('<td></td>').appendTo(tr)
    fields_select.clone().appendTo(td)
    $('<td class="pl-3"></td>').appendTo(tr)
  })
  $('#fieldsMapping tbody select').select2({
    placeholder: '无'
  }).on('change', function () {
    let val = $(this).val()
    let toel = $(this).parents('td').next()
    if (val) {
      toel.parent().addClass('table-active')
      let meta = fields_map[val]
      if (meta.defaultValue) toel.text('默认 : ' + meta.defaultValue)
      else toel.text('')
    } else {
      toel.parent().removeClass('table-active')
      toel.text('')
    }
  })
}

var sec_to_time = function (s) {
  if (!s || s <= 0) return '00:00:00'
  let hh = Math.floor(s / 3600)
  let mm = Math.floor(s / 60) % 60
  let ss = ~~(s % 60)
  if (hh < 10) hh = '0' + hh
  if (mm < 10) mm = '0' + mm
  if (ss < 10) ss = '0' + ss
  return hh + ':' + mm + ':' + ss
}
