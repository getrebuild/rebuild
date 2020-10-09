/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig
const __gExtConfig = {}

const SHOW_REPEATABLE = ['TEXT', 'DATE', 'DATETIME', 'EMAIL', 'URL', 'PHONE', 'REFERENCE', 'CLASSIFICATION']

$(document).ready(function () {
  const dt = wpc.fieldType
  const extConfig = wpc.extConfig

  const $btn = $('.J_save').click(function () {
    if (!wpc.metaId) return
    let data = {
      fieldLabel: $val('#fieldLabel'),
      comments: $val('#comments'),
      nullable: $val('#fieldNullable'),
      creatable: $val('#fieldCreatable'),
      updatable: $val('#fieldUpdatable'),
      repeatable: $val('#fieldRepeatable'),
      queryable: $val('#fieldQueryable'),
    }
    if (data.fieldLabel === '') return RbHighbar.create($lang('PlsInputSome,FieldName'))

    const dv = $val('#defaultValue')
    if (dv) {
      if (checkDefaultValue(dv, dt) === false) return
      else data.defaultValue = dv
    } else if (dv === '') {
      data.defaultValue = dv
    }

    const extConfigNew = { ...__gExtConfig }
    // 不同类型的配置
    $(`.J_for-${dt} .form-control, .J_for-${dt} .custom-control-input`).each(function () {
      const k = $(this).attr('id')
      if (k && 'defaultValue' !== k) extConfigNew[k] = $val(this)
    })
    // 单选
    $(`.J_for-${dt} .custom-radio .custom-control-input:checked`).each(function () {
      const k = $(this).attr('name')
      extConfigNew[k] = $val(this)
    })

    if (!$same(extConfigNew, extConfig)) {
      data['extConfig'] = JSON.stringify(extConfigNew)
      if (Object.keys(extConfigNew).length === 0) data['extConfig'] = ''
    }

    data = $cleanMap(data)
    if (Object.keys(data).length === 0) {
      location.href = '../fields'
      return
    }

    const save = function () {
      data.metadata = { entity: 'MetaField', id: wpc.metaId }
      $btn.button('loading')
      $.post('/admin/entity/field-update', JSON.stringify(data), function (res) {
        if (res.error_code === 0) {
          location.href = '../fields'
        } else {
          $btn.button('reset')
          RbHighbar.error(res.error_msg)
        }
      })
    }

    if (!$('#fieldNullable').prop('disabled') && !$('#fieldNullable').prop('checked') && !$('#fieldCreatable').prop('checked')) {
      RbAlert.create($lang('SetNullAndCreateTips'), {
        confirm: function () {
          this.disabled(true)
          save()
        },
      })
    } else {
      save()
    }
  })

  $('#fieldNullable').attr('checked', $('#fieldNullable').data('o') === true)
  $('#fieldCreatable').attr('checked', $('#fieldCreatable').data('o') === true)
  $('#fieldUpdatable').attr('checked', $('#fieldUpdatable').data('o') === true)
  $('#fieldRepeatable').attr('checked', $('#fieldRepeatable').data('o') === true)
  $('#fieldQueryable').attr('checked', $('#fieldQueryable').data('o') === true)

  $(`.J_for-${dt}`).removeClass('hide')

  // 设置扩展值
  for (let k in extConfig) {
    const $control = $(`#${k}`)
    if ($control.length === 1) {
      if ($control.attr('type') === 'checkbox') $control.attr('checked', extConfig[k] === 'true' || extConfig[k] === true)
      else if ($control.prop('tagName') === 'DIV') $control.text(extConfig[k])
      else $control.val(extConfig[k])
    } else {
      $(`.custom-control-input[name="${k}"][value="${extConfig[k]}"]`).attr('checked', true)
    }
  }

  if (wpc.fieldName === 'approvalState' || wpc.fieldName === 'approvalId') {
    $('.J_for-STATE, .J_for-REFERENCE').remove()
  } else if (dt === 'PICKLIST' || dt === 'MULTISELECT') {
    _handlePicklist(dt)
  } else if (dt === 'SERIES') {
    _handleSeries()
  } else if (dt === 'DATE' || dt === 'DATETIME') {
    _handleDate(dt)
  } else if (dt === 'FILE' || dt === 'IMAGE') {
    _handleFile(extConfig)
  } else if (dt === 'CLASSIFICATION') {
    _handleClassification(extConfig)
  } else if (dt === 'REFERENCE') {
    _handleReference()
  } else if (dt === 'BARCODE') {
    $('.J_options input').attr('disabled', true)
  }

  // 显示重复值选项
  if (SHOW_REPEATABLE.includes(dt) && wpc.fieldName !== 'approvalId') {
    $('#fieldRepeatable').parents('.custom-control').removeClass('hide')
  }

  // 内建字段
  if (wpc.fieldBuildin) {
    $('.J_options input, .J_del').attr('disabled', true)
    if (wpc.isDetailToMainField) {
      $('.J_action').removeClass('hide')
    } else {
      $('.footer .alert').removeClass('hide')
    }
  } else {
    $('.J_action').removeClass('hide')
  }

  // 只读属性
  delete extConfig['classification']
  delete extConfig['stateClass']

  $('.J_del').click(function () {
    if (!wpc.isSuperAdmin) {
      RbHighbar.error($lang('OnlyAdminCanSome,DeleteField'))
      return
    }

    RbAlert.create($lang('DeleteFieldConfirm'), $lang('DeleteField'), {
      type: 'danger',
      confirmText: $lang('Delete'),
      confirm: function () {
        this.disabled(true)
        $.post(`/admin/entity/field-drop?id=${wpc.metaId}`, (res) => {
          if (res.error_code === 0) {
            this.hide()
            RbHighbar.success($lang('SomeDeleted,Field'))
            setTimeout(function () {
              location.replace('../fields')
            }, 1500)
          } else RbHighbar.error(res.error_msg)
        })
      },
      call: function () {
        $countdownButton($(this._dlg).find('.btn-danger'))
      },
    })
  })
})

// Render item to PickList box
const picklistItemRender = function (data) {
  const $item = $(`<li class="dd-item" data-key="${data.id}"><div class="dd-handle">${data.text}</div></li>`).appendTo('#picklist-items')
  if (data['default'] === true) $item.addClass('default')
}

// Check incorrect?
// Also see RbFormElement#checkHasError in rb-forms.js
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
  if (valid === false) RbHighbar.create($lang('SomeInvalid,DefaultValue'))
  return valid
}

// ~~ 日期高级表达式
class AdvDateDefaultValue extends RbAlert {
  constructor(props) {
    super(props)
    this._refs = []
    this.state.uncalc = true
  }

  renderContent() {
    return (
      <form className="ml-6 mr-6">
        <div className="form-group">
          <label className="text-bold">{$lang('SetSome,DateFormula')}</label>
          <div className="input-group">
            <select className="form-control form-control-sm" ref={(c) => (this._refs[0] = c)}>
              <option value="NOW">{$lang('CurrentDate')}</option>
            </select>
            <select className="form-control form-control-sm ml-1" ref={(c) => (this._refs[1] = c)} onChange={(e) => this.setState({ uncalc: !e.target.value })}>
              <option value="">{$lang('CalcNone')}</option>
              <option value="+">{$lang('CalcPlus')}</option>
              <option value="-">{$lang('CalcMinus')}</option>
            </select>
            <input type="number" min="1" max="999999" className="form-control form-control-sm ml-1" defaultValue="1" disabled={this.state.uncalc} ref={(c) => (this._refs[2] = c)} />
            <select className="form-control form-control-sm ml-1" disabled={this.state.uncalc} ref={(c) => (this._refs[3] = c)}>
              <option value="D">{$lang('Year')}</option>
              <option value="M">{$lang('Month')}</option>
              <option value="Y">{$lang('Day')}</option>
              {this.props.type === 'DATETIME' && (
                <React.Fragment>
                  <option value="H">{$lang('Hour')}</option>
                  <option value="I">{$lang('Minte')}</option>
                </React.Fragment>
              )}
            </select>
          </div>
        </div>
        <div className="form-group mb-1">
          <button type="button" className="btn btn-space btn-primary" onClick={this.confirm}>
            {$lang('Confirm')}
          </button>
        </div>
      </form>
    )
  }

  confirm = () => {
    let expr = 'NOW'
    const op = $(this._refs[1]).val()
    const num = $(this._refs[2]).val() || 1
    if (op) {
      if (isNaN(num)) {
        RbHighbar.create($lang('PlsInputSome,Number'))
        return
      }
      expr += ` ${op} ${num}${$(this._refs[3]).val()}`
    }
    $('#defaultValue').val('{' + expr + '}')
    this.hide()
  }
}

const _handleReference = function () {
  const referenceEntity = $('.J_referenceEntity').data('refentity')

  let dataFilter = (wpc.extConfig || {}).referenceDataFilter
  const saveFilter = function (res) {
    if (res && res.items && res.items.length > 0) {
      $('#referenceDataFilter').text(`${$lang('AdvFiletrSeted')} (${res.items.length})`)
      dataFilter = res
    } else {
      $('#referenceDataFilter').text($lang('ClickSet'))
      dataFilter = null
    }
    __gExtConfig.referenceDataFilter = dataFilter
  }
  dataFilter && saveFilter(dataFilter)

  let advFilter
  $('#referenceDataFilter').click(() => {
    if (advFilter) {
      advFilter.show()
    } else {
      renderRbcomp(<AdvFilter title={$lang('SetAdvFiletr')} inModal={true} canNoFilters={true} entity={referenceEntity} filter={dataFilter} confirm={saveFilter} />, null, function () {
        advFilter = this
      })
    }
  })
}

const _handlePicklist = function (dt) {
  $.get(`/admin/field/picklist-gets?entity=${wpc.entityName}&field=${wpc.fieldName}&isAll=false`, function (res) {
    if (res.data.length === 0) {
      $('#picklist-items li').text($lang('PlsAddOption'))
      return
    }
    $('#picklist-items').empty()
    $(res.data).each(function () {
      picklistItemRender(this)
    })
    if (res.data.length > 5) $('#picklist-items').parent().removeClass('autoh')
  })
  $('.J_picklist-edit').click(() => RbModal.create(`/p/admin/metadata/picklist-editor?entity=${wpc.entityName}&field=${wpc.fieldName}&multi=${dt === 'MULTISELECT'}`, $lang('SetOptionList')))
}

const _handleSeries = function () {
  $('#defaultValue').parents('.form-group').remove()
  $('.J_options input').attr('disabled', true)
  $('.J_series-reindex').click(() => {
    RbAlert.create($lang('AppendSeriesConfirm'), {
      confirm: function () {
        this.disabled(true)
        $.post(`/admin/field/series-reindex?entity=${wpc.entityName}&field=${wpc.fieldName}`, () => {
          this.hide()
          RbHighbar.success($lang('SomeSuccess,AppendSeries'))
        })
      },
    })
  })
}

const _handleDate = function (dt) {
  $('#defaultValue').datetimepicker({
    format: dt === 'DATE' ? 'yyyy-mm-dd' : 'yyyy-mm-dd hh:ii:ss',
    minView: dt === 'DATE' ? 2 : 0,
  })
  $('#defaultValue')
    .next()
    .removeClass('hide')
    .find('button')
    .click(() => renderRbcomp(<AdvDateDefaultValue type={dt} />))
}

const _handleFile = function (extConfig) {
  let uploadNumber = [0, 9]
  if (extConfig['uploadNumber']) {
    uploadNumber = extConfig['uploadNumber'].split(',')
    uploadNumber[0] = ~~uploadNumber[0]
    uploadNumber[1] = ~~uploadNumber[1]
    $('.J_minmax b').eq(0).text(uploadNumber[0])
    $('.J_minmax b').eq(1).text(uploadNumber[1])
  }

  $('input.bslider')
    .slider({ value: uploadNumber })
    .on('change', function (e) {
      const v = e.value.newValue
      $setTimeout(
        () => {
          $('.J_minmax b').eq(0).text(v[0])
          $('.J_minmax b').eq(1).text(v[1])
          $('#fieldNullable').attr('checked', v[0] <= 0)
        },
        200,
        'bslider-change'
      )
    })
  $('#fieldNullable').attr('disabled', true)
}

const _handleClassification = function (extConfig) {
  $.get(`/admin/metadata/classification/info?id=${extConfig.classification}`, function (res) {
    $('#useClassification a')
      .attr({ href: `${rb.baseUrl}/admin/metadata/classification/${extConfig.classification}` })
      .text(res.data.name)
  })
}
