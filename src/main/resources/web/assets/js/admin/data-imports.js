/*!
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
  only_update: false,
}

let fields_cached
let import_inprogress = false
let import_taskid

const entity = $urlp('entity')
_Config.entity = entity || null

_Config.file = $urlp('file') || null
if (_Config.file) {
  _Config.file = $decode(_Config.file)
  $('.J_upload-input').text($fileCutName(_Config.file))
}

$(document).ready(() => {
  $.get('/commons/metadata/entities?detail=true', (res) => {
    $(res.data).each(function () {
      $(`<option value="${this.name}">${this.label}</option>`).appendTo('#toEntity')
    })

    const $toe = $('#toEntity')
      .select2({
        placeholder: $L('选择实体'),
        allowClear: false,
        disabled: !location.href.includes('/admin/'),
      })
      .on('change', function () {
        _Config.entity = $(this).val()
        _renderRepeatFields()
        _checkUserPrivileges()
      })

    if (entity) $toe.val(entity)
    $toe.trigger('change')
    _Config.entity = $toe.val()
  })

  const $fss = $('.file-select span')
  $createUploader(
    '#upload-input',
    (res) => {
      $fss.text(`${$L('上传中')} ... ${res.percent.toFixed(0)}%`)
    },
    (res) => {
      _Config.file = res.key
      $('.J_upload-input').text($fileCutName(_Config.file))
      $fss.text($L('上传文件'))
    }
  )

  $('input[name=repeatOpt]').on('click', function () {
    _Config.repeat_opt = ~~$(this).val()
    if (_Config.repeat_opt === 3) $('.J_repeatFields').hide()
    else $('.J_repeatFields').show()

    if (_Config.repeat_opt === 1) $('.J_onlyUpdate').show()
    else $('.J_onlyUpdate').hide()
  })

  $('#onlyUpdate').on('click', function (e) {
    _Config.only_update = $val(e.target)
  })

  if ($('#toUser')[0]) {
    const _onSelectUser = function (s, isRemove) {
      if (isRemove || !s) _Config.owning_user = null
      else _Config.owning_user = s.id
      _checkUserPrivileges()
    }
    renderRbcomp(
      <UserSelector hideDepartment={true} hideRole={true} hideTeam={true} multiple={false} onSelectItem={(s, isRemove) => _onSelectUser(s, isRemove)} onClearSelection={() => _onSelectUser()} />,
      'toUser'
    )
  } else {
    $.get(`/app/entity/data-imports/check-user?user=${rb.currentUser}&entity=${_Config.entity}`, (res) => {
      let hasError = []
      if (res.data.canCreate !== true) hasError.push($L('新建'))
      if (res.data.canUpdate !== true) hasError.push($L('编辑'))
      if (hasError.length > 0) {
        $('#user-warn').removeClass('hide')
        renderRbcomp(<RbAlertBox message={$L('你没有 %s 权限，部分数据可能会导入失败', hasError.join('/'))} />, 'user-warn')
      }
    })
  }

  $('.J_step1-btn').on('click', step2_mapping)
  $('.J_step2-btn').on('click', step3_import)
  $('.J_step2-return').on('click', step1_upload)
  $('.J_step3-cancel').on('click', step3_import_cancel)

  import_taskid = $urlp('task', location.hash)
  if (import_taskid) {
    step3_import_show()
    import_inprogress = true
    step3_import_state(import_taskid, true)
  }

  window.onbeforeunload = function () {
    if (import_inprogress === true) return 'SHOW-CLOSE-CONFIRM'
  }

  $('.J_step3-trace').on('click', () => {
    renderRbcomp(<ImportsTraceViewer width="681" taskid={import_taskid} />)
  })
})

// 1. 初始导入
const step1_upload = () => {
  $('.steps li, .step-content .step-pane').removeClass('active complete')
  $('.steps li[data-step=1], .step-content .step-pane[data-step=1]').addClass('active')
}

// 2. 字段映射
const step2_mapping = () => {
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
  $.get(`/app/entity/data-imports/check-file?file=${$encode(_Config.file)}`, (res) => {
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

    _fieldsMapping(_data.preview[0], fields_cached)
    $('.steps li, .step-content .step-pane').removeClass('active complete')
    $('.steps li[data-step=1]').addClass('complete')
    $('.steps li[data-step=2], .step-content .step-pane[data-step=2]').addClass('active')
  })
}

// 3. 开始导入
const step3_import = () => {
  let fsMapping = {}
  $('#fieldsMapping tbody>tr').each(function () {
    const _this = $(this)
    const col = _this.data('col')
    const field = _this.find('select').val()
    if (field) fsMapping[field] = col
  })
  if (Object.keys(fsMapping).length === 0) {
    RbHighbar.create($L('请至少设置 1 个字段映射'))
    return
  }

  const notNullFields = []
  $(fields_cached).each((idx, item) => {
    if (item.nullable === true || !!item.defaultValue) {
      // Not be must
    } else if (fsMapping[item.name] === undefined) {
      notNullFields.push(item.label)
    }
  })

  function _import() {
    _Config.fields_mapping = fsMapping
    RbAlert.create($L('请再次确认导入选项和字段映射。开始导入吗？'), {
      confirm: function () {
        this.disabled(true)
        $.post('/app/entity/data-imports/import-submit', JSON.stringify(_Config), (res) => {
          if (res.error_code === 0) {
            this.hide()
            step3_import_show()
            import_inprogress = true
            import_taskid = res.data.taskid
            location.hash = '#task=' + import_taskid
            step3_import_state(import_taskid)
          } else {
            RbHighbar.error(res.error_msg)
            this.disabled()
          }
        })
      },
    })
  }

  if (notNullFields.length > 0) {
    RbAlert.create($L('部分必填字段未映射，可能导致导入失败。是否继续？'), {
      type: 'warning',
      confirmText: $L('继续'),
      onConfirm: function () {
        this.hide()
        setTimeout(() => _import(), 200)
      },
    })
  } else {
    _import()
  }
}

// 3.1. 开始导入
const step3_import_show = () => {
  $('.steps li, .step-content .step-pane').removeClass('active complete')
  $('.steps li[data-step=1], .steps li[data-step=2]').addClass('complete')
  $('.steps li[data-step=3], .step-content .step-pane[data-step=3]').addClass('active')

  // Next
  if (_Config.entity) {
    $('.J_step3-next').attr('href', `${$('.J_step3-next').attr('href')}?entity=${_Config.entity}`)
  }
}

// 3.2. 导入状态
const step3_import_state = (taskid, inLoad) => {
  $.get(`/commons/task/state?taskid=${taskid}`, (res) => {
    if (res.error_code !== 0) {
      if (inLoad === true) step1_upload()
      else RbHighbar.error(res.error_msg)
      import_inprogress = false
      return
    }
    if (!res.data) {
      setTimeout(() => step3_import_state(taskid), 1000)
      return
    }

    const _state = res.data
    const elapsedTime = ~~_state.elapsedTime / 1000
    $('.J_import_time').text($sec2Time(elapsedTime))
    const sspeed = _state.completed / elapsedTime
    $('.J_import_speed').text($L('%s条/秒', ~~sspeed))
    $('.J_remain_time').text($sec2Time((_state.total - _state.completed) / sspeed))

    if (_state.isCompleted === true) {
      $('.J_import-bar').css('width', '100%')
      $('.J_import_state').text($L('导入完成。共成功导入 %d 条记录', _state.succeeded))
    } else if (_state.isInterrupted === true) {
      $('.J_import_state').text($L('导入被终止。已成功导入 %d 条记录', _state.succeeded))
    }

    if (_state.isCompleted === true || _state.isInterrupted === true) {
      $('.J_step3-cancel').attr('disabled', true).text($L('导入完成'))
      $('.J_step3-next').removeClass('hide')
      import_inprogress = false
      return
    }

    if (_state.progress > 0) {
      $('.J_import_state').text(`${$L('正在导入 ...')} ${_state.completed}/${_state.total}`)
      $('.J_import-bar').css('width', _state.progress * 100 + '%')
    }

    setTimeout(() => {
      step3_import_state(taskid)
    }, 500)
  })
}

// 3.3. 中断导入
const step3_import_cancel = () => {
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

const _LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')
// 渲染字段映射
const _fieldsMapping = (columns, fields) => {
  const requiredText = ` (${$L('必填')})`

  console.log(fields)

  const fieldMap = {}
  const $fieldSelect = $(`<select><option value="">${$L('无')}</option></select>`)
  fields.forEach(function (item) {
    let req = item.nullable === false ? requiredText : ''
    if (item.defaultValue) req = ''
    $(`<option value="${item.name}">${item.label}${req}</option>`).appendTo($fieldSelect)
    fieldMap[item.name] = item
  })

  const $tbody = $('#fieldsMapping tbody').empty()
  $(columns).each(function (idx, item) {
    let L = _LETTERS[idx]
    if (idx > 25) L = `A${_LETTERS[idx - 26] || 'X'}` // AA
    if (idx > 51) L = `B${_LETTERS[idx - 52] || 'X'}` // BA

    const $tr = $(`<tr data-col="${idx}"></tr>`).appendTo($tbody)
    $(`<td><em>${L}</em> ${item || $L('空')}<i class="zmdi zmdi-arrow-right"></i></td>`).appendTo($tr)
    const $td = $('<td></td>').appendTo($tr)
    const $clone = $fieldSelect.clone().appendTo($td)
    $('<td class="pl-3"></td>').appendTo($tr)

    // 根据名称自动映射
    let matchField
    $clone.find('option').each(function (i, o) {
      const name = o.value
      const label = (o.text || '').replace(requiredText, '')
      if ((name && name === item) || (label && label === (item || '').trim())) {
        matchField = name
        return false
      }
    })
    if (matchField) $clone.val(matchField)
  })

  $('#fieldsMapping tbody select')
    .select2({
      placeholder: $L('无'),
      templateResult: function (res) {
        const text = res.text.split(requiredText)
        const $span = $('<span></span>').text(text[0])
        if (text.length > 1) $(`<span class="badge badge-danger badge-pill">${$L('必填')}</span>`).appendTo($span)
        return $span
      },
    })
    .on('change', function () {
      const val = $(this).val()
      const $toe = $(this).parents('td').next()
      if (val) {
        $toe.parent().addClass('table-active')
        const field = fieldMap[val]
        if (field.defaultValue) $toe.text(`${$L('默认值')} : ${field.defaultValue}`)
        else $toe.text('')
      } else {
        $toe.parent().removeClass('table-active')
        $toe.text('')
      }
    })
    .trigger('change')
}

// 检查所属用户权限
function _checkUserPrivileges() {
  if (!_Config.entity || !_Config.owning_user) {
    $('#user-warn').addClass('hide')
    return
  }

  $.get(`/app/entity/data-imports/check-user?user=${_Config.owning_user}&entity=${_Config.entity}`, (res) => {
    const hasError = []
    if (!res.data.canCreate) hasError.push($L('新建'))
    if (!res.data.canUpdate) hasError.push($L('编辑'))
    if (hasError.length > 0) {
      $('#user-warn').removeClass('hide')
      renderRbcomp(<RbAlertBox message={$L('选择的用户无 %s 权限。但作为管理员，你可以强制导入', hasError.join('/'))} />, 'user-warn')
    } else {
      $('#user-warn').addClass('hide')
    }
  })
}

// 渲染重复判断字段
function _renderRepeatFields() {
  const $el = $('#repeatFields').empty()

  if (!_Config.entity) {
    $el.select2({
      placeholder: $L('选择字段'),
    })
    return
  }

  const excludeNames = ['createdBy', 'createdOn', 'modifiedOn', 'modifiedBy']
  const excludeTypes = ['AVATAR', 'FILE', 'IMAGE', 'MULTISELECT', 'N2NREFERENCE', 'LOCATION']

  $.get(`/app/entity/data-imports/import-fields?entity=${_Config.entity}`, (res) => {
    $(res.data).each(function () {
      if (excludeNames.includes(this.name) || excludeTypes.includes(this.type)) return
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
  })
}

const _MAX_SHOWS = 10000
// ~ 导入详情
class ImportsTraceViewer extends RbAlert {
  renderContent() {
    const data = this.state.data || []
    return (
      <RF>
        <table className="table table-fixed">
          <thead>
            <tr>
              <th width="50" className="pr-0">
                {$L('行号')}
              </th>
              <th width="120">{$L('状态')}</th>
              <th>{$L('详情')}</th>
            </tr>
          </thead>
          <tbody>
            {data.map((item) => {
              return (
                <tr key={item[0]}>
                  <th className="pr-0">{item[0] + 1}</th>
                  <td>
                    {item[1] === 'CREATED' && (
                      <a target="_blank" title={$L('查看')} href={`${rb.baseUrl}/app/redirect?id=${item[2]}`}>
                        {$L('新建成功')}
                        <i className="icon zmdi zmdi-open-in-new ml-1" />
                      </a>
                    )}
                    {item[1] === 'UPDATED' && (
                      <a target="_blank" title={$L('查看')} href={`${rb.baseUrl}/app/redirect?id=${item[2]}`}>
                        {$L('更新成功')}
                        <i className="icon zmdi zmdi-open-in-new ml-1" />
                      </a>
                    )}
                    {item[1] === 'SKIP' && <span className="text-muted">{$L('跳过')}</span>}
                    {item[1] === 'ERROR' && <span className="text-danger">{$L('失败')}</span>}
                  </td>
                  <td>{this._formatDetail(item)}</td>
                </tr>
              )
            })}
          </tbody>
        </table>

        <div className={`text-center mt-2 pb-2 ${!this.state.hasMore && 'hide'}`}>
          <button type="button" className="btn btn-link" onClick={() => this.showData(1)}>
            {$L('显示更多')}
          </button>
        </div>
      </RF>
    )
  }

  _formatDetail(item) {
    if (item[1] === 'CREATED' || item[1] === 'UPDATED') {
      return item[3] ? `${$L('单元格值错误')} ${item[3]}` : <span className="text-muted">-</span>
    } else if (item[1] === 'ERROR') {
      return item[2]
    } else {
      return <span className="text-muted">-</span>
    }
  }

  componentDidMount() {
    super.componentDidMount()
    this.load()
  }

  componentWillUnmount() {
    if (this._timer) {
      clearTimeout(this._timer)
      this._timer = null
    }
  }

  load() {
    $.get(`/app/entity/data-imports/import-trace?taskid=${this.props.taskid}`, (res) => {
      if (res.error_code === 0) {
        this._datas = res.data || []
        this.showData()

        // refresh
        if (import_inprogress === true && res.data.length < _MAX_SHOWS) {
          this._timer = setTimeout(() => this.load(), 2000)
        }
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }

  showData() {
    this._showPage = (this._showPage || 0) + 1
    const p = this._datas.slice(0, this._showPage * 500)
    this.setState({ data: p, hasMore: this._datas.length > p.length })
  }
}
