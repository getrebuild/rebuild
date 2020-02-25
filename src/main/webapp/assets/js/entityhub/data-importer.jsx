/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const ientry = {
  file: null,
  entity: null,
  repeat_opt: 1,
  repeat_fields: null,
  owning_user: null,
  fields_mapping: null
}
let fields_cached
let import_inprogress = false
let import_taskid

$(document).ready(() => {
  init_upload()

  const fileds_render = (entity) => {
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

  $.get(`${rb.baseUrl}/commons/metadata/entities?slave=true`, (res) => {
    $(res.data).each(function () {
      $('<option value="' + this.name + '">' + this.label + '</option>').appendTo('#toEntity')
    })

    const entityS2 = $('#toEntity').select2({
      allowClear: false
    }).on('change', function () {
      fileds_render($(this).val())
      check_user()
    })
    if ($urlp('entity')) entityS2.val($urlp('entity'))
    entityS2.trigger('change')
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
        const query = {
          entity: 'User',
          qfields: 'loginName,fullName,email,quickCode',
          q: params.term
        }
        return query
      },
      processResults: function (data) {
        const rs = data.data.map((item) => { return item })
        return { results: rs }
      }
    }
  }).on('change', function () {
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
      if (error === 'ErrorType') RbHighbar.create('请上传 Excel/CSV 文件')
      else if (error === 'ErrorMaxSize') RbHighbar.create('文件不能大于 50M')
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
      } else RbHighbar.error('上传失败，请稍后重试')
    }
  })
}

// 检查所属用户权限
const check_user = () => $setTimeout(check_user0, 200, 'check_user')
const check_user0 = () => {
  if (!ientry.entity || !ientry.owning_user) return
  $.get(`${rb.baseUrl}/admin/datas/data-importer/check-user?user=${ientry.owning_user}&entity=${ientry.entity}`, (res) => {
    let hasError = []
    if (res.data.canCreate !== true) hasError.push('新建')
    if (res.data.canUpdate !== true) hasError.push('更新')
    if (hasError.length > 0) {
      renderRbcomp(<RbAlertBox message={`选择的用户无 ${hasError.join('/')} 权限。但作为管理员，你可以强制导入`} />, 'user-warn')
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
  if (!ientry.entity) { RbHighbar.create('请选择导入实体'); return }
  if (!ientry.file) { RbHighbar.create('请上传数据文件'); return }
  if (ientry.repeat_opt !== 3 && (!ientry.repeat_fields || ientry.repeat_fields.length === 0)) { RbHighbar.create('请选择重复判断字段'); return }

  const $btn = $('.J_step1-btn').button('loading')
  $.get(`${rb.baseUrl}/admin/datas/data-importer/check-file?file=${$encode(ientry.file)}`, (res) => {
    $btn.button('reset')
    if (res.error_code > 0) { RbHighbar.create(res.error_msg); return }

    const _data = res.data
    if (_data.preview.length < 2 || _data.preview[0].length === 0) { RbHighbar.create('上传的文件无有效数据'); return }

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
      RbHighbar.create(item.label + ' 为必填字段，请选择')
      fm = null
      return false
    }
  })
  if (!fm) return
  ientry.fields_mapping = fm

  RbAlert.create('请再次确认导入选项和字段映射。开始导入吗？', {
    confirm: function () {
      this.disabled(true)
      $.post(`${rb.baseUrl}/admin/datas/data-importer/import-submit`, JSON.stringify(ientry), (res) => {
        if (res.error_code === 0) {
          this.hide()
          step_import_show()
          import_inprogress = true
          import_taskid = res.data.taskid
          location.hash = '#task=' + import_taskid
          import_state(import_taskid)
        } else RbHighbar.error(res.error_msg)
      })
    }
  })
}
const step_import_show = () => {
  $('.steps li, .step-content .step-pane').removeClass('active complete')
  $('.steps li[data-step=1], .steps li[data-step=2]').addClass('complete')
  $('.steps li[data-step=3], .step-content .step-pane[data-step=3]').addClass('active')
}
const import_state = (taskid, inLoad) => {
  $.get(`${rb.baseUrl}/commons/task/state?taskid=${taskid}`, (res) => {
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
      $('.J_import_state').text('导入完成。共成功导入 ' + _data.succeeded + ' 条数据')
    } else if (_data.isInterrupted === true) {
      $('.J_import_state').text('导入被终止。已成功导入 ' + _data.succeeded + ' 条数据')
    }
    if (_data.isCompleted === true || _data.isInterrupted === true) {
      $('.J_step3-cancel').attr('disabled', true).text('导入完成')
      $('.J_step3-logs').removeClass('hide')
      import_inprogress = false
      return
    }

    if (_data.total > -1) {
      $('.J_import_state').text('正在导入 ... ' + _data.completed + ' / ' + _data.total)
      $('.J_import-bar').css('width', (_data.progress * 100) + '%')
    }
    setTimeout(() => { import_state(taskid) }, 500)
  })
}
const import_cancel = () => {
  RbAlert.create('确认终止导入？请注意已导入数据无法自动删除', {
    type: 'danger',
    confirmText: '确认终止',
    confirm: function () {
      $.post(`${rb.baseUrl}/commons/task/cancel?taskid=${import_taskid}`, (res) => {
        if (res.error_code > 0) RbHighbar.error(res.error_msg)
      })
      this.hide()
    }
  })
}

// 渲染字段映射
const render_fieldsMapping = (columns, fields) => {
  const fields_map = {}
  const fields_select = $('<select><option value="">无</option></select>')
  $(fields).each((idx, item) => {
    let canNull = item.nullable === false ? ' [必填]' : ''
    if (item.defaultValue) canNull = ''
    $('<option value="' + item.name + '">' + item.label + canNull + '</option>').appendTo(fields_select)
    fields_map[item.name] = item
  })

  const $tbody = $('#fieldsMapping tbody').empty()
  $(columns).each(function (idx, item) {
    const $tr = $('<tr data-col="' + idx + '"></tr>').appendTo($tbody)
    $('<td><em>#' + (idx + 1) + '</em> ' + item + '<i class="zmdi zmdi-arrow-right"></i></td>').appendTo(tr)
    const $td = $('<td></td>').appendTo($tr)
    fields_select.clone().appendTo($td)
    $('<td class="pl-3"></td>').appendTo($tr)
  })
  $('#fieldsMapping tbody select').select2({
    placeholder: '无'
  }).on('change', function () {
    const val = $(this).val()
    const toel = $(this).parents('td').next()
    if (val) {
      toel.parent().addClass('table-active')
      const meta = fields_map[val]
      if (meta.defaultValue) toel.text('默认 : ' + meta.defaultValue)
      else toel.text('')
    } else {
      toel.parent().removeClass('table-active')
      toel.text('')
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