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
}

let fields_cached
let import_inprogress = false
let import_taskid

const entity = $urlp('entity')

$(document).ready(() => {
  $.get('/commons/metadata/entities?detail=true', (res) => {
    $(res.data).each(function () {
      $(`<option value="${this.name}">${this.label}</option>`).appendTo('#toEntity')
    })

    const $toe = $('#toEntity')
      .select2({
        placeholder: $L('选择实体'),
        allowClear: false,
      })
      .on('change', function () {
        _renderRepeatFields($(this).val())
        _checkUserPrivileges()
      })

    if (entity) $toe.val(entity)
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

  $('.J_step1-btn').click(step2_mapping)
  $('.J_step2-btn').click(step3_import)
  $('.J_step2-return').click(step1_upload)
  $('.J_step3-cancel').click(step3_import_cancel)

  import_taskid = $urlp('task', location.hash)
  if (import_taskid) {
    step3_import_show()
    import_inprogress = true
    step3_import_state(import_taskid, true)
  }

  window.onbeforeunload = function () {
    if (import_inprogress === true) return false
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
  $(fields_cached).each((idx, item) => {
    if (item.nullable === true || !!item.defaultValue) {
      // Not be must
    } else if (fsMapping[item.name] === undefined) {
      RbHighbar.create($L('%s 为必填字段，请选择', item.label))
      fsMapping = null
      return false
    }
  })
  if (!fsMapping) return

  _Config.fields_mapping = fsMapping

  RbAlert.create($L('请再次确认导入选项和字段映射。开始导入吗？'), {
    confirm: function () {
      this.disabled(true)
      $.post('/admin/data/data-imports/import-submit', JSON.stringify(_Config), (res) => {
        if (res.error_code === 0) {
          this.hide()
          step3_import_show()
          import_inprogress = true
          import_taskid = res.data.taskid
          location.hash = '#task=' + import_taskid
          step3_import_state(import_taskid)
        } else RbHighbar.error(res.error_msg)
      })
    },
  })
}

// 3.1. 开始导入
const step3_import_show = () => {
  $('.steps li, .step-content .step-pane').removeClass('active complete')
  $('.steps li[data-step=1], .steps li[data-step=2]').addClass('complete')
  $('.steps li[data-step=3], .step-content .step-pane[data-step=3]').addClass('active')

  // Next
  if (_Config.entity || entity) {
    $('.J_step3-next').attr('href', `${$('.J_step3-next').attr('href')}?entity=${_Config.entity || entity}`)
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
      $('.J_step3-next').removeClass('hide')
      import_inprogress = false
      return
    }

    if (_data.progress > 0) {
      $('.J_import_state').text(`${$L('正在导入 ...')} ${_data.completed}/${_data.total}`)
      $('.J_import-bar').css('width', _data.progress * 100 + '%')
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
  const canNullText = ` [${$L('必填')}]`

  const fieldMap = {}
  const $fieldSelect = $(`<select><option value="">${$L('无')}</option></select>`)
  $(fields).each((idx, item) => {
    let canNull = item.nullable === false ? canNullText : ''
    if (item.defaultValue) canNull = ''
    $(`<option value="${item.name}">${item.label}${canNull}</option>`).appendTo($fieldSelect)
    fieldMap[item.name] = item
  })

  const $tbody = $('#fieldsMapping tbody').empty()
  $(columns).each(function (idx, item) {
    let L = _LETTERS[idx]
    if (idx > 25) L = `A${_LETTERS[idx - 26] || 'X'}`  // AA
    if (idx > 51) L = `B${_LETTERS[idx - 52] || 'X'}`  // BA

    const $tr = $(`<tr data-col="${idx}"></tr>`).appendTo($tbody)
    $(`<td><em>${L}</em> ${item || $L('空')}<i class="zmdi zmdi-arrow-right"></i></td>`).appendTo($tr)
    const $td = $('<td></td>').appendTo($tr)
    const $clone = $fieldSelect.clone().appendTo($td)
    $('<td class="pl-3"></td>').appendTo($tr)

    // 根据名称自动映射
    let matchField
    $clone.find('option').each(function (i, o) {
      const name = o.value
      const label = (o.text || '').replace(canNullText, '')
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
  const $el = $('#repeatFields').empty()

  if (!entity) {
    $el.select2({
      placeholder: $L('选择字段'),
    })
    return
  }

  const excludeNames = ['createdBy', 'createdOn', 'modifiedOn', 'modifiedBy']
  const excludeTypes = ['AVATAR', 'FILE', 'IMAGE', 'MULTISELECT', 'N2NREFERENCE', 'LOCATION']

  $.get(`/admin/data/data-imports/import-fields?entity=${entity}`, (res) => {
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
    _Config.entity = entity
  })
}

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
                      <a target="_blank" title={$L('查看')} href={`${rb.baseUrl}/app/list-and-view?id=${item[2]}`}>
                        {$L('新建成功')}
                        <i className="icon zmdi zmdi-open-in-new ml-1" />
                      </a>
                    )}
                    {item[1] === 'UPDATED' && (
                      <a target="_blank" title={$L('查看')} href={`${rb.baseUrl}/app/list-and-view?id=${item[2]}`}>
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
    $.get(`/admin/data/data-imports/import-trace?taskid=${this.props.taskid}`, (res) => {
      if (res.error_code === 0) {
        this._datas = res.data || []
        this.showData()

        // refresh
        if (import_inprogress === true && res.data.length < 2000) this._timer = setTimeout(() => this.load(), 1500)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }

  showData() {
    this._showPage = (this._showPage || 0) + 1
    const p = this._datas.slice(0, this._showPage * 200)
    this.setState({ data: p, hasMore: this._datas.length > p.length })
  }
}
