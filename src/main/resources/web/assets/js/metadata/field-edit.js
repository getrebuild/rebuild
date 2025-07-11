/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global FormulaDate, FormulaCalc, FIELD_TYPES */

const wpc = window.__PageConfig
const __gExtConfig = {}

const SHOW_REPEATABLE = ['TEXT', 'DATE', 'EMAIL', 'URL', 'PHONE', 'REFERENCE', 'CLASSIFICATION', 'ANYREFERENCE', 'PICKLIST']
const SHOW_DEFAULTVALUE = ['TEXT', 'NTEXT', 'EMAIL', 'PHONE', 'URL', 'NUMBER', 'DECIMAL', 'DATETIME', 'DATE', 'TIME', 'BOOL', 'CLASSIFICATION', 'REFERENCE', 'N2NREFERENCE', 'ANYREFERENCE']
const SHOW_ADVDESENSITIZED = ['TEXT', 'PHONE', 'EMAIL', 'NUMBER', 'DECIMAL']
const SHOW_ADVPATTERN = ['TEXT', 'NTEXT']
const SHOW_SCANCODE = ['TEXT', 'REFERENCE']

const CURRENT_BIZZ = '{CURRENT}'

$(document).ready(function () {
  const dt = wpc.fieldType
  const extConfig = wpc.extConfig

  // 内置字段
  if (wpc.fieldBuildin) {
    $('.J_fieldAttrs, .J_advOpt, .J_for-STATE').remove()
    $('#referenceCascadingField, #referenceQuickNew, #referenceDataFilter').parents('.form-group').remove()
  } else {
    Object.keys(__TYPE2TYPE).includes(wpc.fieldType) && $('.J_cast-type').parent().removeClass('hide')
  }
  // 显示重复值选项
  if (SHOW_REPEATABLE.includes(dt) && wpc.fieldName !== 'approvalId') {
    $('#fieldRepeatable').parents('.custom-control').removeClass('hide')
  }
  // 默认值
  if (!SHOW_DEFAULTVALUE.includes(dt)) $('#defaultValue').remove()

  // 脱敏
  if (SHOW_ADVDESENSITIZED.includes(dt)) {
    $('.J_advOpt').removeClass('hide')
  } else {
    $('#advDesensitized').parent().remove()
  }
  // 正则
  if (SHOW_ADVPATTERN.includes(dt)) {
    $('.J_advOpt').removeClass('hide')
    $('.J_advPattern .badge').on('click', function () {
      $('#advPattern').val($(this).data('patt'))
    })
  } else {
    $('#advPattern').parent().remove()
  }
  // 扫码
  if (SHOW_SCANCODE.includes(dt)) {
    $('.J_advOpt').removeClass('hide')
  } else {
    $('#textScanCode').parent().remove()
  }
  // 文件
  if (dt === 'FILE') {
    $('.J_fileSuffix .badge').on('click', function () {
      $('#fileSuffix').val($(this).data('suff'))
    })
  }
  // fix:v3.8.4
  if ($('#referenceQuickNew').attr('disabled')) {
    console.log($('#referenceQuickNew').attr('disabled'))
    $('#referenceQuickNew')[0].checked = false
  }

  const $btn = $('.J_save').on('click', function () {
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
    if (data.fieldLabel === '') return RbHighbar.create($L('请输入字段名称'))

    // 默认值
    let dv = $val('.J_defaultValue')
    if (dt === 'CLASSIFICATION' || dt === 'REFERENCE' || dt === 'N2NREFERENCE' || dt === 'ANYREFERENCE') {
      dv = $('.J_defaultValue').attr('data-value-id') || ''
      const odv = $('.J_defaultValue').attr('data-o') || ''
      if (dv === odv) dv = null
    }

    if (dv) {
      if (checkDefaultValue(dv, dt) === false) return
      else data.defaultValue = dv
    } else if (dv === '') {
      data.defaultValue = dv
    }

    const extConfigNew = { ...extConfig, ...__gExtConfig }

    // 不同类型的扩展配置
    $(`.J_for-${dt} .form-control, .J_for-${dt} .custom-control-input`).each(function () {
      const k = $(this).attr('id')
      if (k) extConfigNew[k] = $val(this)
    })
    // 单选类型
    $(`.J_for-${dt} .custom-radio .custom-control-input:checked`).each(function () {
      const k = $(this).attr('name')
      if (k) extConfigNew[k] = $val(this)
    })

    // 文件
    if (dt === 'FILE' && extConfigNew['fileSuffix']) {
      const fix = []
      extConfigNew['fileSuffix'].split(/[,，;；\s]/).forEach((n) => {
        n = $trim(n)
        if (n) {
          if (n.substring(0, 1) !== '.' && !n.includes('/*')) n = `.${n.trim()}`
          fix.push(n.trim())
        }
      })
      extConfigNew['fileSuffix'] = fix.join(',')
    }
    // 文本
    if (dt === 'TEXT' && extConfigNew['textCommon']) {
      const fix = []
      extConfigNew['textCommon'].split(/[,，]/).forEach((n) => n && fix.push(n.trim()))
      extConfigNew['textCommon'] = fix.join(',')
    }
    // 二维码
    if (dt === 'BARCODE' && !extConfigNew['barcodeFormat']) {
      return RbHighbar.create($L('请输入编码规则'))
    }
    // 自动编号
    if (dt === 'SERIES' && !extConfigNew['seriesFormat']) {
      return RbHighbar.create($L('请输入编号规则'))
    }
    // 标签
    if (dt === 'TAG') {
      const items = []
      $('#tag-items li').each(function () {
        const $this = $(this)
        const name = $this.find('span').text()
        if (!name) return
        items.push({
          name: name,
          color: $this.data('color') || null,
          default: $this.hasClass('default') || false,
        })
      })
      extConfigNew['tagList'] = items
    }
    // 小数-货币
    if (dt === 'DECIMAL' && extConfigNew['decimalType'] === '¥') {
      extConfigNew['decimalType'] = $val('.J_decimalTypeFlag') || '¥'
      if (extConfigNew['decimalType'] === '_') extConfigNew['decimalType'] = $val('.J_decimalTypeFlag2') || '¥'
    }

    // fix
    delete extConfigNew['undefined']
    delete extConfigNew['advDesensitized']
    delete extConfigNew['advPattern']
    delete extConfigNew['textScanCode']

    if (SHOW_ADVDESENSITIZED.includes(dt)) extConfigNew['advDesensitized'] = $val('#advDesensitized')
    if (SHOW_ADVPATTERN.includes(dt)) extConfigNew['advPattern'] = $val('#advPattern')
    if (SHOW_SCANCODE.includes(dt)) extConfigNew['textScanCode'] = $val('#textScanCode')

    if ((extConfigNew['advDesensitized'] || extConfigNew['advPattern'] || extConfigNew['textScanCode']) && rb.commercial < 1) {
      RbHighbar.error(WrapHtml($L('免费版不支持高级选项 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    if (!$same(extConfigNew, extConfig)) {
      data['extConfig'] = JSON.stringify(extConfigNew)
      if (Object.keys(extConfigNew).length === 0) data['extConfig'] = ''
    }

    data = $cleanMap(data)
    if (Object.keys(data).length === 0) {
      if (rb.env === 'dev') location.reload()
      else location.href = '../fields'
      return
    }

    const save = function () {
      data.metadata = {
        entity: 'MetaField',
        id: wpc.metaId,
      }

      $btn.button('loading')
      $.post('/admin/entity/field-update', JSON.stringify(data), function (res) {
        if (res.error_code === 0) {
          if (rb.env === 'dev') location.reload()
          else location.href = '../fields'
        } else {
          $btn.button('reset')
          RbHighbar.error(res.error_msg)
        }
      })
    }

    if (!$('#fieldNullable').prop('disabled') && !$('#fieldNullable').prop('checked') && !$('#fieldCreatable').prop('checked')) {
      RbAlert.create($L('同时设置不允许为空和不允许新建可能导致无法新建记录。是否仍要保存？'), {
        onConfirm: function () {
          this.disabled(true)
          save()
        },
      })
    } else {
      save()
    }
  })

  $('#fieldNullable').attr('checked', $isTrue($('#fieldNullable').data('o')))
  $('#fieldCreatable').attr('checked', $isTrue($('#fieldCreatable').data('o')))
  $('#fieldUpdatable').attr('checked', $isTrue($('#fieldUpdatable').data('o')))
  $('#fieldRepeatable').attr('checked', $isTrue($('#fieldRepeatable').data('o')))
  $('#fieldQueryable').attr('checked', $isTrue($('#fieldQueryable').data('o')))

  if (extConfig.advDesensitized) $('#advDesensitized').attr('checked', true)
  if (extConfig.advPattern) $('#advPattern').val(extConfig.advPattern)
  if (extConfig.textScanCode) $('#textScanCode').val(extConfig.textScanCode)

  // 设置扩展值
  for (let k in extConfig) {
    const $control = $(`#${k}`)
    if ($control.length === 1) {
      if ($control.attr('type') === 'checkbox') $control.attr('checked', $isTrue(extConfig[k]))
      else if ($control.prop('tagName') === 'DIV') $control.text(extConfig[k])
      else $control.val(extConfig[k])
    } else {
      $(`.custom-control-input[name="${k}"][value="${extConfig[k]}"]`).attr('checked', true)
    }
  }

  if (dt === 'PICKLIST' || dt === 'MULTISELECT') {
    _handlePicklist(dt)
  } else if (dt === 'SERIES') {
    _handleSeries()
  } else if (dt === 'DATE' || dt === 'DATETIME' || dt === 'TIME') {
    // 暂不支持 TIME
    if (dt === 'DATE' || dt === 'DATETIME') _handleCalcFormula(extConfig.calcFormula)
    _handleDatetime(dt)
  } else if (dt === 'FILE' || dt === 'IMAGE') {
    _handleFile(extConfig.uploadNumber, extConfig)
  } else if (dt === 'CLASSIFICATION') {
    _handleClassification(extConfig.classification)
  } else if (dt === 'REFERENCE') {
    _handleReference()
  } else if (dt === 'N2NREFERENCE') {
    _handleReference(true)
  } else if (dt === 'BOOL') {
    const $dv = $('.J_defaultValue')
    if ($dv.data('o')) $dv.val($dv.data('o'))
  } else if (dt === 'BARCODE') {
    $('.J_fieldAttrs input').attr('disabled', true)
  } else if (dt === 'NUMBER' || dt === 'DECIMAL') {
    _handleCalcFormula(extConfig.calcFormula)
    if (dt === 'DECIMAL') {
      if (!extConfig.decimalType || extConfig.decimalType === 0 || extConfig.decimalType === '0' || extConfig.decimalType === '%') {
        // 数字、百分比
      } else {
        $('input[name="decimalType"]:eq(2)')[0].click()
        $('.J_decimalTypeFlag').val(extConfig.decimalType)
        if ($(`.J_decimalTypeFlag option[value="${extConfig.decimalType}"]`).length === 0) {
          $('.J_decimalTypeFlag').val('_')
          $('.J_decimalTypeFlag2').val(extConfig.decimalType)
          $('.J_decimalTypeFlag2').removeClass('hide')
        }
      }

      $('.J_decimalTypeFlag')
        .on('click', () => {
          $('input[name="decimalType"]:eq(2)')[0].click()
        })
        .on('change', (e) => {
          if (e.target.value === '_') $('.J_decimalTypeFlag2').removeClass('hide')
          else $('.J_decimalTypeFlag2').addClass('hide')
        })
    }
  } else if (dt === 'TAG') {
    _handleTag(extConfig.tagList || [], extConfig.tagMaxSelect || null)
  } else if (dt === 'ANYREFERENCE') {
    _handleAnyReference(extConfig.anyreferenceEntities)
  } else if (dt === 'TEXT' || dt === 'NTEXT') {
    _handleTextCommon(extConfig.textCommon)
  }

  // 只读属性
  // delete extConfig['classification']
  // delete extConfig['stateClass']

  $('.J_del').on('click', function () {
    RbAlert.create($L('字段删除后将无法恢复，请务必谨慎操作。确认删除吗？'), $L('删除字段'), {
      type: 'danger',
      confirmText: $L('删除'),
      onConfirm: function () {
        this.disabled(true)
        $.post(`/admin/entity/field-drop?id=${wpc.metaId}`, (res) => {
          if (res.error_code === 0) {
            this.hide()
            RbHighbar.success($L('字段已删除'))
            setTimeout(() => location.replace('../fields'), 1500)
          } else {
            RbHighbar.error(res.error_msg)
            this.disabled()
          }
        })
      },
      countdown: 5,
    })
  })

  $('.J_cast-type').on('click', () => {
    if (rb.commercial < 10) {
      RbHighbar.error(WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }
    renderRbcomp(<FieldTypeCast entity={wpc.entityName} field={wpc.fieldName} fromType={wpc.fieldType} />)
  })

  // v3.7
  $('.page-help>a').attr('href', $('.page-help>a').attr('href') + `field-${wpc.fieldType.toLowerCase()}`)
})

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
  if (valid === false) RbHighbar.create($L('默认值无效'))
  return valid
}

const _handlePicklist = function (dt) {
  $.get(`/admin/field/picklist-gets?entity=${wpc.entityName}&field=${wpc.fieldName}&isAll=false`, function (res) {
    if (res.data.length === 0) {
      $('#picklist-items li').text($L('请添加选项'))
      return
    }

    $('#picklist-items').empty()
    $(res.data).each(function () {
      const $item = $(`<li class="dd-item" data-key="${this.mask || this.id}"><div class="dd-handle" style="color:${this.color || 'inherit'} !important"></div></li>`).appendTo('#picklist-items')
      $item.find('div').text(this.text)
      if ($isTrue(this['default'])) $item.addClass('default')
    })
    if (res.data.length > 5) $('#picklist-items').parent().removeClass('autoh')

    let rbapi = []
    res.data &&
      res.data.forEach((item) => {
        rbapi.push([item.mask || item.id, item.text])
      })
    rbapi.length > 0 && $logRBAPI(JSON.stringify(rbapi), 'FieldOptions')
  })

  $('.J_picklist-edit').on('click', () => {
    RbModal.create(`/p/admin/metadata/picklist-editor?entity=${wpc.entityName}&field=${wpc.fieldName}&type=${dt}`, $L('配置选项'))
  })
}

const _handleSeries = function () {
  $('.J_fieldAttrs input').attr({
    checked: false,
    disabled: true,
  })

  $('.J_action .dropdown-toggle').removeClass('hide')
  $(`<a class="dropdown-item">${$L('补充编号')}</a>`)
    .appendTo('.J_action .dropdown-menu')
    .on('click', () => {
      RbAlert.create($L('此操作将为空字段补充编号，空字段过多耗时会较长，请耐心等待。是否继续？'), {
        onConfirm: function () {
          this.disabled(true)
          $.post(`/admin/field/series-reindex?entity=${wpc.entityName}&field=${wpc.fieldName}`, (res) => {
            if (res.error_code === 0) RbHighbar.success($L('补充编号成功'))
            else RbHighbar.error(res.error_msg)
            this.hide()
          })
        },
      })
    })
  $(`<a class="dropdown-item">${$L('重置自增数字')}</a>`)
    .appendTo('.J_action .dropdown-menu')
    .on('click', () => {
      const M = (
        <RF>
          <p>{$L('此操作将重置自增数字，可能导致编号重复因而新建记录失败，请谨慎执行。是否继续？')}</p>
          <div className="series-starts">
            <input type="text" className="form-control form-control-sm text-center" />
          </div>
        </RF>
      )

      RbAlert.create(M, {
        type: 'danger',
        onConfirm: function () {
          this.disabled(true)
          let s = $(this._element).find('input').val() || 0
          $.post(`/admin/field/series-reset?entity=${wpc.entityName}&field=${wpc.fieldName}&s=${s}`, (res) => {
            setTimeout(() => {
              if (res.error_code === 0) RbHighbar.success($L('自增数字重置成功'))
              else RbHighbar.error(res.error_msg)
              this.hide()
            }, 1001)
          })
        },
        onRendered: function () {
          $.get(`/admin/field/series-current?entity=${wpc.entityName}&field=${wpc.fieldName}`, (res) => {
            $(this._element)
              .find('input')
              .attr('placeholder', $L('留空则重置为 0 (当前值 %s)', res.data || 0))
          })
        },
        countdown: 5,
      })
    })
}

const _handleDatetime = function (dt) {
  const dpOption = {
    clearBtn: true,
    format: dt === 'DATE' ? 'yyyy-mm-dd' : 'yyyy-mm-dd hh:ii:ss',
    minView: dt === 'DATE' ? 2 : 0,
    forceParse: false,
  }
  if (dt === 'TIME') {
    dpOption.format = 'hh:ii:ss'
    dpOption.minView = 0
    dpOption.maxView = 1
    dpOption.startView = 1
  }
  $('.J_defaultValue').attr('readonly', true).datetimepicker(dpOption)

  $(`<button class="btn btn-secondary" type="button" title="${$L('日期公式')}"><i class="icon mdi mdi-function-variant"></i></button>`)
    .appendTo('.J_defaultValue-append')
    .on('click', () => renderRbcomp(<FormulaDate type={dt} onConfirm={(expr) => $('.J_defaultValue').val(expr)} />))
}

const _handleFile = function (uploadNumber, config) {
  if (uploadNumber) {
    uploadNumber = uploadNumber.split(',')
    uploadNumber[0] = ~~uploadNumber[0]
    uploadNumber[1] = ~~uploadNumber[1]
    $('.J_minmax b').eq(0).text(uploadNumber[0])
    $('.J_minmax b').eq(1).text(uploadNumber[1])

    // v4.1
    if (uploadNumber[1] > 9) {
      $('input.bslider').attr({
        'data-slider-max': 99,
        'data-slider-value': '[0,99]',
      })
    }
  } else {
    uploadNumber = [0, 9]
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

  // v3.8
  if (config) {
    if (config.imageCaptureDef) $('#imageCaptureDef').attr('checked', true)
    if (config.imageCapture) $('#imageCapture').attr('checked', true)
    if (!config.imageCaptureDef && !config.imageCapture) $('#imageCaptureDef').attr('checked', true)
  }
}

const _handleClassification = function (useClassification) {
  const $dv = $('.J_defaultValue')
  const $dvClear = $('.J_defaultValue-clear').on('click', () => {
    $dv.attr('data-value-id', '').val('')
    $dvClear.addClass('hide')
  })

  // 修改等级需要重新设置默认值
  $('#classificationLevel').on('change', () => $dvClear.trigger('click'))

  function _showSelector(openLevel) {
    renderRbcomp(
      // eslint-disable-next-line react/jsx-no-undef
      <ClassificationSelector
        entity={wpc.entityName}
        field={wpc.fieldName}
        label={$L('默认值')}
        openLevel={openLevel}
        onSelect={(s) => {
          $dv.attr('data-value-id', s.id).val(s.text)
          $dvClear.removeClass('hide')
        }}
      />
    )
  }

  const $append = $(`<button class="btn btn-secondary" type="button" title="${$L('选择默认值')}"><i class="icon zmdi zmdi-search"></i></button>`).appendTo('.J_defaultValue-append')

  $.get(`/admin/metadata/classification/info?id=${useClassification}`, (res) => {
    $('#useClassification a')
      .attr({ href: `${rb.baseUrl}/admin/metadata/classification/${useClassification}` })
      .text(res.data.name)

    $dv.attr('readonly', true)
    $append.on('click', () => {
      const openLevel = ~~$val('#classificationLevel')
      _showSelector(openLevel > -1 ? openLevel : res.data.openLevel)
    })
  })

  _loadRefsLabel($dv, $dvClear)
}

const _handleReference = function (isN2N) {
  const referenceEntity = $('.J_referenceEntity').data('refentity')

  // 父级字段
  $.get(`/admin/entity/field-cascading-fields?entity=${wpc.entityName}&field=${wpc.fieldName}`, (res) => {
    res.data &&
      res.data.forEach((item) => {
        $(`<option value="${item.name}">${item.label}</option>`).appendTo('#referenceCascadingField')
      })
    wpc.extConfig.referenceCascadingField && $('#referenceCascadingField').val(wpc.extConfig.referenceCascadingField)
  })

  // 数据过滤
  let dataFilter = (wpc.extConfig || {}).referenceDataFilter
  const saveFilter = function (res) {
    if (res && res.items && res.items.length > 0) {
      $('#referenceDataFilter').text(`${$L('已设置条件')} (${res.items.length})`)
      dataFilter = res
    } else {
      $('#referenceDataFilter').text($L('点击设置'))
      dataFilter = null
    }
    __gExtConfig.referenceDataFilter = dataFilter
  }
  dataFilter && saveFilter(dataFilter)

  let advFilter
  $('#referenceDataFilter').on('click', () => {
    if (advFilter) {
      advFilter.show()
    } else {
      renderRbcomp(<AdvFilter title={$L('附加过滤条件')} inModal={true} canNoFilters={true} entity={referenceEntity} filter={dataFilter} confirm={saveFilter} />, function () {
        advFilter = this
      })
    }
  })

  _initRefsDefaultValue([referenceEntity], isN2N)

  // Bizz
  if (['User', 'Department', 'Team'].includes(referenceEntity)) {
    const $dv = $('.J_defaultValue')
    const $dvClear = $('.J_defaultValue-clear')

    const $current = $(`<button class="btn btn-secondary" type="button" title="${$L('当前用户')}"><i class="icon mdi mdi-function-variant"></i></button>`).appendTo('.J_defaultValue-append')
    $current.on('click', () => {
      $dv.attr('data-value-id', CURRENT_BIZZ).val(CURRENT_BIZZ)
      $dvClear.removeClass('hide')
    })
    $dvClear.css({ right: 75 })
  }
}

const _handleAnyReference = function (es) {
  $.get('/commons/metadata/entities?bizz=true&detail=true', (res) => {
    res.data &&
      res.data.forEach((item) => {
        $(`<option value="${item.name}">${item.label}</option>`).appendTo('#anyreferenceEntities')
      })

    const $s2 = $('#anyreferenceEntities').select2({
      placeholder: $L('不限'),
      tag: true,
    })

    es && $s2.val(es.split(',')).trigger('change')
    _initRefsDefaultValue(() => $s2.val())
  })
}

const _initRefsDefaultValue = function (allowEntities, isN2N) {
  const $dv = $('.J_defaultValue')
  const $dvClear = $('.J_defaultValue-clear').on('click', () => {
    $dv.attr('data-value-id', '').val('')
    $dvClear.addClass('hide')
  })

  const $append = $(`<button class="btn btn-secondary" type="button" title="${$L('选择默认值')}"><i class="icon zmdi zmdi-search"></i></button>`).appendTo('.J_defaultValue-append')
  $dv.attr('readonly', true)
  $append.on('click', () => {
    const ae = typeof allowEntities === 'function' ? allowEntities() : allowEntities
    RecordSelectorModal.create({
      title: $L('选择默认值'),
      allowEntities: ae,
      onConfirm: (val) => {
        if (isN2N) {
          let valKeep = $dv.attr('data-value-id')
          if (valKeep) val = valKeep + ',' + val
        }
        $dv.attr('data-value-id', val).val(val)
        _loadRefsLabel($dv, $dvClear)
      },
    })
  })

  _loadRefsLabel($dv, $dvClear)
}

const _loadRefsLabel = function ($dv, $dvClear) {
  const def = $dv.val()
  if (def === CURRENT_BIZZ) {
    $dv.attr('data-value-id', CURRENT_BIZZ)
    $dvClear.removeClass('hide')
  } else if (def) {
    $.get(`/commons/search/read-labels?ids=${encodeURIComponent(def)}&ignoreMiss=false`, (res) => {
      if (res.data) {
        const ids = []
        const labels = []
        for (let k in res.data) {
          ids.push(k)
          labels.push(res.data[k])
        }

        $dv.attr('data-value-id', ids.join(','))
        $dv.val(labels.join(', '))
      }
    })

    $dvClear.removeClass('hide')
  }
}

let FIELDS_CACHE
const _handleCalcFormula = function (formula) {
  const $el = $('#calcFormula2')
  function _call(s) {
    $('#calcFormula').val(s || '')
    $el.text(FormulaCalc.textFormula(s, FIELDS_CACHE))
  }

  if (!FIELDS_CACHE) {
    $.get(`/commons/metadata/fields?entity=${wpc.entityName}`, (res) => {
      const fs = []
      res.data.forEach((item) => {
        if (['NUMBER', 'DECIMAL', 'DATE', 'DATETIME'].includes(item.type) && !['approvalLastTime'].includes(item.name) && item.name !== wpc.fieldName) {
          fs.push(item)
        }
      })
      fs.push({ name: '{NOW}', label: $L('当前日期'), type: 'NOW' })

      FIELDS_CACHE = fs
      if (formula) _call(formula)
    })
  }

  $el.on('click', () => renderRbcomp(<FormulaCalc onConfirm={_call} fields={FIELDS_CACHE} />))
}

const _handleTag = function (tagList, tagMaxSelect) {
  const $items = $('#tag-items')
  function _add(item) {
    $items.find('.no-item').addClass('hide')
    const $item = $(`<li class="dd-item" data-color="${item.color || ''}"><div class="dd-handle" style="color:${item.color || 'inherit'} !important"><span>${item.name}</span></div></li>`).appendTo(
      $items
    )
    if ($isTrue(item['default'])) $item.addClass('default')

    const $del = $(`<a title="${$L('移除')}"><i class="zmdi zmdi-close"></i></a>`).appendTo($item.find('.dd-handle'))
    $del.on('click', () => {
      $item.remove()
      tagList = tagList.filter((x) => x.name !== item.name)
      if (tagList.length === 0) $items.find('.no-item').removeClass('hide')
    })
  }

  tagList.forEach((item) => _add(item))
  $items.find('.no-item').text($L('请添加标签'))

  $('.J_tag-add').on('click', () => {
    renderRbcomp(
      <TagEditor
        onConfirm={(d) => {
          let r = false
          $(tagList).each(function () {
            if (this.name === d.name) {
              r = true
              return false
            }
          })

          if (r) {
            RbHighbar.create($L('标签重复'))
            return false
          }

          tagList.push(d)
          _add(d)
          return true
        }}
      />
    )
  })

  if (tagMaxSelect) $('.J_tagMaxSelect b').text(tagMaxSelect)
  $('#tagMaxSelect')
    .slider({ value: tagMaxSelect || 9 })
    .on('change', function (e) {
      $('.J_tagMaxSelect b').text(e.value.newValue)
    })
}

// 常用值
const _handleTextCommon = function (common) {
  let s2data = common ? common.split(',') : []
  s2data = s2data.map((item) => {
    return { id: item, text: item, selected: true }
  })
  $('#textCommon').select2({
    placeholder: $L('(选填)'),
    data: s2data,
    multiple: true,
    maximumSelectionLength: 99,
    language: {
      noResults: function () {
        return $L('请输入')
      },
    },
    tags: true,
    theme: 'default select2-tag',
    allowClear: true,
  })
}

class TagEditor extends RbAlert {
  renderContent() {
    return (
      <div className="rbalert-form-sm">
        <div className="form-group">
          <label className="text-bold">{$L('标签')}</label>
          <input type="text" className="form-control form-control-sm" name="name" placeholder={$L('输入标签')} ref={(c) => (this._$name = c)} />
          <div className="rbcolors mt-2" ref={(c) => (this._$rbcolors = c)}>
            <a className="default" title={$L('默认')}></a>
          </div>
        </div>
        <div className="form-group">
          <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mt-0 mb-0">
            <input className="custom-control-input" type="checkbox" ref={(c) => (this._$default = c)} />
            <span className="custom-control-label">{$L('设为默认')}</span>
          </label>
        </div>

        <div className="mt-2 mb-2">
          <button className="btn btn-primary" type="button" onClick={() => this._onConfirm()}>
            {$L('确定')}
          </button>
        </div>
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()

    const $cs = $(this._$rbcolors)
    RBCOLORS.forEach((c) => {
      $(`<a style="background-color:${c}" data-color="${c}"></a>`).appendTo($cs)
    })
    $cs.find('>a').on('click', function () {
      $cs.find('>a .zmdi').remove()
      $('<i class="zmdi zmdi-check"></i>').appendTo(this)
      this._color = null
    })

    $('<input type="color" />')
      .appendTo($cs)
      .on('change', (e) => {
        $cs.find('>a .zmdi').remove()
        this._color = e.target.value
        if (this._color === '#000000') this._color = null
      })
  }

  _onConfirm() {
    const name = $val(this._$name)
    if (!name) return RbHighbar.create($L('请输入标签'))
    const color = this._color || $(this._$rbcolors).find('>a>i').parent().data('color') || ''
    const ok = this.props.onConfirm({ name, color, default: $val(this._$default) })
    ok && this.hide()
  }
}

// 字段类型转换
const __TYPE2TYPE = {
  'NUMBER': ['DECIMAL'],
  'DECIMAL': ['NUMBER'],
  'DATE': ['DATETIME'],
  'DATETIME': ['DATE'],
  'TEXT': ['NTEXT', 'PHONE', 'EMAIL', 'URL'],
  'PHONE': ['TEXT'],
  'EMAIL': ['TEXT'],
  'URL': ['TEXT'],
  'NTEXT': ['TEXT'],
  'IMAGE': ['FILE'],
  'FILE': ['IMAGE'],
}
class FieldTypeCast extends RbFormHandler {
  render() {
    const toTypes = __TYPE2TYPE[this.props.fromType] || []

    return (
      <RbModal title={$L('转换字段类型')} ref={(c) => (this._dlg = c)} disposeOnHide>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('当前字段类型')}</label>
            <div className="col-sm-7">
              <div className="form-control-plaintext text-bold">{FIELD_TYPES[this.props.fromType][0]}</div>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('新字段类型')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref={(c) => (this._$toType = c)}>
                {toTypes.map((t) => {
                  return (
                    <option value={t} key={t}>
                      {(FIELD_TYPES[t] || [t])[0]}
                    </option>
                  )
                })}
              </select>
              <p className="form-text">{$L('转换可能导致一定的精度损失，请谨慎进行')}</p>
            </div>
          </div>
          <div className="form-group row footer" ref={(c) => (this._$btns = c)}>
            <div className="col-sm-7 offset-sm-3">
              <button className="btn btn-primary" type="button" onClick={() => this.post()}>
                {$L('确定')}
              </button>
              <button className="btn btn-link" type="button" onClick={() => this.hide()}>
                {$L('取消')}
              </button>
              <button className="btn btn-light w-auto dropdown-toggle bosskey-show" type="button" data-toggle="dropdown" title={$L('更多操作')}>
                <i className="icon zmdi zmdi-more fs-18" />
              </button>
              <div className="dropdown-menu">
                <a className="dropdown-item" onClick={() => this.post('DATETIME40')}>
                  修订日期时间类型
                </a>
                <a className="dropdown-item" onClick={() => this.post('UPLOADNUMBER41')}>
                  放大允许上传数量
                </a>
              </div>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    $(this._$toType).select2({
      placeholder: $L('不可转换'),
      templateResult: function (res) {
        const $span = $('<span class="icon-append"></span>').attr('title', res.text).text(res.text)
        $(`<i class="icon mdi ${(FIELD_TYPES[res.id] || [])[1]}"></i>`).appendTo($span)
        return $span
      },
    })
  }

  post(fixType) {
    const toType = fixType || $(this._$toType).val()
    if (!toType) return RbHighbar.create($L('不可转换'))

    const $btn = $(this._$btns).find('.btn').button('loading')
    $.post(`/admin/entity/field-type-cast?entity=${this.props.entity}&field=${this.props.field}&toType=${toType}`, (res) => {
      if (res.error_code === 0) {
        location.reload()
      } else {
        $btn.button('reset')
        RbHighbar.error(res.error_msg)
      }
    })
  }
}
