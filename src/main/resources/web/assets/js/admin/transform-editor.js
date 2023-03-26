/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global FieldValueSet*/

const wpc = window.__PageConfig

$(document).ready(() => {
  let advFilter
  $('#useFilter').on('click', () => {
    if (advFilter) {
      advFilter.show()
    } else {
      renderRbcomp(<AdvFilter title={$L('附加过滤条件')} inModal={true} canNoFilters={true} entity={wpc.sourceEntity.entity} filter={advFilter_data} confirm={_saveFilter} />, null, function () {
        advFilter = this
      })
    }
  })

  const config = wpc.config || {}

  let _FieldsMapping
  let _FieldsMapping2 // 明细
  renderRbcomp(<FieldsMapping source={wpc.sourceEntity} target={wpc.targetEntity} data={config.fieldsMapping} />, 'EMAIN', function () {
    _FieldsMapping = this
  })
  if (wpc.sourceDetailEntity) {
    renderRbcomp(<FieldsMapping source={wpc.sourceDetailEntity} target={wpc.targetDetailEntity} data={config.fieldsMappingDetail} />, 'EDETAIL', function () {
      _FieldsMapping2 = this
    })
  }

  const fillbackFields = []
  wpc.sourceEntity.fields.forEach((item) => {
    if (!item.name.includes('.') && item.type === 'REFERENCE' && item.ref[0] === wpc.targetEntity.entity) {
      fillbackFields.push({ id: item.name, text: item.label })
    }
  })

  $('#fillbackField')
    .select2({
      placeholder: `${$L('(可选)')}`,
      data: fillbackFields,
      allowClear: true,
      language: {
        noResults: () => $L('无可用字段'),
      },
    })
    .val(null)
    .trigger('change')

  let _ImportsFilterMapping
  $('#importsMode').on('click', function () {
    if ($val(this)) {
      $('#filterFields').parents('.form-group').removeClass('hide')

      if (!_ImportsFilterMapping) {
        renderRbcomp(<ImportsFilterMapping defaultValue={config.importsFilter} />, 'filterFields', function () {
          _ImportsFilterMapping = this
        })
      }
    } else {
      $('#filterFields').parents('.form-group').addClass('hide')
    }
  })

  const $btn = $('.J_save').on('click', function () {
    const importsMode = $('#importsMode').prop('checked')
    if (importsMode && rb.commercial < 10) {
      RbHighbar.error(WrapHtml($L('免费版不支持启用明细记录导入 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    const fm = _FieldsMapping.buildMapping()
    if (fm === false) return
    if (!fm) {
      RbHighbar.create($L('请至少添加 1 个字段映射'))
      return
    }

    const tips = []

    const fmd = _FieldsMapping2 ? _FieldsMapping2.buildMapping() : null
    if (fmd === false) return
    if (_FieldsMapping2 && !fmd) {
      tips.push($L('明细实体未配置字段映射，因此明细记录不会转换'))
    }

    let importsFilter

    function _save(_tips) {
      const config = {
        fieldsMapping: fm,
        fieldsMappingDetail: fmd,
        fillbackField: $('#fillbackField').val(),
        transformMode: $('#transformMode').prop('checked') ? 2 : 1,
        useFilter: advFilter_data,
        importsMode: $val('#importsMode'),
        importsFilter: importsFilter || null,
      }

      const _data = {
        config: config,
        metadata: {
          entity: 'TransformConfig',
          id: wpc.configId,
        },
      }

      $btn.button('loading')
      $.post('/app/entity/common-save', JSON.stringify(_data), (res) => {
        if (res.error_code === 0) {
          const msg = (
            <RF>
              <strong>{$L('保存成功')}</strong>
              {_tips.length > 0 && <p className="text-warning m-0 mt-1">{_tips.join(' / ')}</p>}
            </RF>
          )
          RbAlert.create(msg, {
            icon: 'alert-circle-o',
            cancelText: $L('返回列表'),
            cancel: () => location.replace('../transforms'),
            confirmText: $L('继续编辑'),
            confirm: () => location.reload(),
          })
        } else {
          RbHighbar.error(res.error_msg)
        }
        $btn.button('reset')
      })
    }

    let unset = 0
    for (let k in fm) {
      if (fm[k] === null) unset++
    }
    for (let k in fmd || {}) {
      if (fmd[k] === null) unset++
    }
    if (unset > 0) tips.push($L('部分必填字段未映射，可能导致转换失败'))

    if (importsMode) {
      importsFilter = _ImportsFilterMapping.buildFilter()
      if (importsFilter.length === 0) {
        tips.push($L('明细记录导入条件未配置，将导入源实体的所有记录'))
      }
    }

    _save(tips)
  })

  // Load
  setTimeout(() => {
    _saveFilter(config.useFilter)

    if (config.fillbackField) {
      $('#fillbackField').val(config.fillbackField).trigger('change')
    }
    if (config.transformMode === 2) {
      $('#transformMode').attr('checked', true)
    }
    if (config.importsMode) {
      $('#importsMode').trigger('click')
    }
  }, 100)
})

const _VFIXED = 'VFIXED'

class FieldsMapping extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, useVfixed: {} }
    this._FieldValueSet = []
  }

  componentDidMount() {
    const mapping = this.props.data || {}
    const that = this

    let useVfixed = {}
    $(this._$fieldsMapping)
      .find('select.J_mapping')
      .each(function () {
        const $this = $(this)

        const fieldName = $this.data('field')
        const targetField = that.props.target.fields.find((x) => fieldName === x.name)

        const sourceFields = []
        that.props.source.fields.forEach((item) => {
          if ($fieldIsCompatible(item, targetField)) {
            sourceFields.push({ id: item.name, text: item.label })
          }
        })

        const $s2 = $this
          .select2({
            placeholder: $L('选择源字段'),
            allowClear: true,
            data: sourceFields,
            language: {
              noResults: () => $L('无可用字段'),
            },
          })
          .on('change', function () {
            if ($this.val()) $this.parents('.row').addClass('active')
            else $this.parents('.row').removeClass('active')
          })

        if ($.isArray(mapping[fieldName])) {
          useVfixed[fieldName] = true
        } else {
          $s2.val(mapping[fieldName] || null).trigger('change')
        }
      })

    this.setState({ useVfixed })

    for (let fieldName in mapping) {
      if ($.isArray(mapping[fieldName])) {
        this._FieldValueSet[fieldName].setValue(mapping[fieldName][0])

        const $this = $(this._$fieldsMapping).find(`.J_vfixed-${fieldName}`)
        $this.val(_VFIXED)
        $this.parents('.row').addClass('active')
      }
    }

    $(this._$fieldsMapping)
      .find('.J_vfixed')
      .select2({
        allowClear: false,
      })
      .on('change', function () {
        const $this = $(this)
        let useVfixed = that.state.useVfixed
        let fieldName = $this.data('field')
        useVfixed[fieldName] = $this.val() === _VFIXED
        that.setState({ useVfixed })

        if (useVfixed[fieldName]) $this.parents('.row').addClass('active')
        else $this.parents('.row').removeClass('active')
      })
  }

  render() {
    const _source = this.props.source
    const _target = this.props.target

    if (!_target.fields || _target.fields.length === 0) {
      return <RbAlertBox message={$L('无可用字段')} />
    }

    return (
      <div ref={(c) => (this._$fieldsMapping = c)}>
        <div className="row title2">
          <div className="col-4 text-bold">{_target.label}</div>
          <div className="col-2"></div>
          <div className="col-5 text-bold">{_source.label}</div>
        </div>

        {_target.fields.map((item, idx) => {
          const isCommon = item.name === 'owningUser'
          return (
            <div className="row" key={idx}>
              <div className="col-4">
                <span className={`badge ${!item.nullable && 'req'} ${isCommon && 'readonly'}`}>{item.label}</span>
              </div>
              <div className="col-2 pr-0">
                <select className={`form-control form-control-sm J_vfixed J_vfixed-${item.name}`} data-field={item.name} defaultValue="FIELD">
                  <option value="FIELD">{$L('字段值')}</option>
                  <option value={_VFIXED}>{$L('固定值')}</option>
                </select>
              </div>
              <div className="col-5">
                <div className={this.state.useVfixed[item.name] ? 'hide' : ''}>
                  <select className="form-control form-control-sm J_mapping" data-field={item.name} data-req={!item.nullable && !isCommon} />
                </div>
                <div className={this.state.useVfixed[item.name] ? '' : 'hide'}>
                  <FieldValueSet entity={_target.entity} field={item} placeholder={$L('固定值')} defaultValue={null} ref={(c) => (this._FieldValueSet[item.name] = c)} />
                </div>
              </div>
            </div>
          )
        })}
      </div>
    )
  }

  buildMapping() {
    let mapping = {}
    let hasMapping = false

    const that = this
    $(this._$fieldsMapping)
      .find('select.J_mapping')
      .each(function () {
        if (!mapping) return

        const $this = $(this)
        const target = $this.data('field') // Target field
        let val = $this.val()
        if (that.state.useVfixed[target]) {
          val = that._FieldValueSet[target].val()

          if (val === false) {
            mapping = false
            return false
          }
          if (!val) {
            RbHighbar.create($L('请填写固定值'))
            mapping = false
            return false
          }

          val = [val, _VFIXED] // array
        }

        // req tips
        if ($this.data('req') && !val) {
          mapping[target] = null
        } else if (val) {
          mapping[target] = val
          hasMapping = true
        }
      })

    return mapping === false ? false : hasMapping ? mapping : null
  }
}

let advFilter_data
function _saveFilter(res) {
  advFilter_data = res
  if (advFilter_data && advFilter_data.items && advFilter_data.items.length > 0) {
    $('#useFilter').text(`${$L('已设置条件')} (${advFilter_data.items.length})`)
  } else {
    $('#useFilter').text($L('点击设置'))
  }
}

class ImportsFilterMapping extends React.Component {
  constructor(props) {
    super(props)

    this._defaultValue = {}
    props.defaultValue &&
      props.defaultValue.forEach((item) => {
        this._defaultValue[item[0]] = item[1]
      })
  }

  render() {
    const state = this.state || {}
    const sourceFields = state.sourceFields || []

    return (
      <div ref={(c) => (this._$filters = c)}>
        {(state.targetFields || []).map((item) => {
          return (
            <div className="row mt-2" key={item[0]}>
              <div className="col-4 pt-1">
                <span className="badge badge-primary" data-field={item[0]}>
                  <span>{item[1]}</span>
                </span>
                <i className="mdi mdi-arrow-left-right" />
              </div>
              <div className="col-5">
                <select className="form-control form-control-sm" defaultValue={this._defaultValue[item[0]] || null}>
                  <option value="">{$L('无')}</option>
                  {sourceFields.map((item2) => {
                    if (item[2] !== item2[2]) return null
                    return (
                      <option key={item2[0]} value={item2[0]}>
                        {item2[1]}
                      </option>
                    )
                  })}
                </select>
              </div>
            </div>
          )
        })}
      </div>
    )
  }

  componentDidMount() {
    $.get(`imports-filter-fields?id=${wpc.configId}`, (res) => {
      this.setState({ ...res.data })

      $(this._$filters)
        .find('select')
        .each(function () {
          const $this = $(this)
          $this.on('change', function () {
            const $mdi = $this.parent().parent().find('.mdi')
            if ($this.val()) {
              $mdi.addClass('text-primary')
            } else {
              $mdi.removeClass('text-primary')
            }
          })
        })
        .trigger('change')
    })
  }

  buildFilter() {
    const filters = []
    $(this._$filters)
      .find('.row')
      .each(function () {
        const $row = $(this)
        const t = $row.find('.badge').data('field')
        const s = $row.find('select').val()
        if (s) filters.push([t, s])
      })
    return filters
  }
}
