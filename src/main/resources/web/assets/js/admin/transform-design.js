/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global FieldValueSet*/
/* 转换模式
 * 1. 普通 to 普通
 */

const wpc = window.__PageConfig
const _sourceEntities41 = wpc.sourceDetailEntities || null
if (wpc.sourceDetailEntities) {
  let hs = _sourceEntities41.find((x) => x.entity === wpc.sourceEntity.entity)
  if (!hs) _sourceEntities41.push(wpc.sourceEntity)
}

let _AdvFilter
let _AdvFilter_data
function _saveFilter(res) {
  _AdvFilter_data = res
  if (_AdvFilter_data && _AdvFilter_data.items && _AdvFilter_data.items.length > 0) {
    $('#useFilter').text(`${$L('已设置条件')} (${_AdvFilter_data.items.length})`)
  } else {
    $('#useFilter').text($L('点击设置'))
  }
}

$(document).ready(() => {
  $('#useFilter').on('click', () => {
    if (_AdvFilter) {
      _AdvFilter.show()
    } else {
      renderRbcomp(<AdvFilter title={$L('转换条件')} inModal canNoFilters entity={wpc.sourceEntity.entity} filter={_AdvFilter_data} confirm={_saveFilter} />, function () {
        _AdvFilter = this
      })
    }
  })

  const config = wpc.config || {}
  if (rb.dev === 'env') console.log('[dev]', config)

  let _FieldsMapping
  let _FieldsMapping2_key
  let _FieldsMapping37 = {} // ND[key=T_S]

  function _addDts(s, data) {
    const key = s.target + '_' + s.source
    if (_FieldsMapping37[key]) {
      RbHighbar.createl('添加明细转换重复')
      return false
    }

    const targetEntity = wpc.targetDetailEntities.find((x) => x.entity === s.target)
    const sourceEntity = _sourceEntities41.find((x) => x.entity === s.source)
    if (!targetEntity || !sourceEntity) return // Bad?

    const $tab = $(
      `<li class="nav-item"><a class="nav-link text-ellipsis" href="#${key}" data-toggle="tab">${targetEntity.label}<span>${sourceEntity.label}</span><em title="${$L(
        '移除'
      )}" class="icon mdi mdi-close"></em></a></li>`
    )
    $tab.insertBefore($('.J_add-dts').parent())
    const $pane = $(`<div class="tab-pane" id="${key}"></div>`).appendTo('.fields-mapping')

    $tab.find('em').on('click', (e) => {
      $stopEvent(e, true)
      $($tab, $pane).remove()
      delete _FieldsMapping37[key]
      $('.entities-mapping a:eq(0)')[0].click()
    })

    renderRbcomp(<FieldsMapping source={sourceEntity} target={targetEntity} data={data} isDetail />, key, function () {
      _FieldsMapping37[key] = this
      if (!data) $tab.find('a')[0].click()
    })
  }

  // 主
  renderRbcomp(<FieldsMapping source={wpc.sourceEntity} target={wpc.targetEntity} data={config.fieldsMapping} />, 'EMAIN', function () {
    _FieldsMapping = this
  })
  // 明细
  if (wpc.sourceDetailEntity) {
    renderRbcomp(<FieldsMapping source={wpc.sourceDetailEntity} target={wpc.targetDetailEntity} data={config.fieldsMappingDetail} isDetail />, 'EDETAIL', function () {
      // v3.7
      _FieldsMapping2_key = wpc.targetDetailEntity.entity + '_' + wpc.sourceDetailEntity.entity
      _FieldsMapping37[_FieldsMapping2_key] = this

      // init
      if (config.fieldsMappingDetails) {
        config.fieldsMappingDetails.forEach((fmd) => {
          const key = fmd._.target + '_' + fmd._.source
          if (key === _FieldsMapping2_key) return // default
          _addDts({ target: fmd._.target, source: fmd._.source }, fmd)
        })
      }
    })
  }

  $('.J_add-dts').on('click', function () {
    renderRbcomp(<DlgAddDts source={_sourceEntities41} target={wpc.targetDetailEntities} onConfirm={(s) => _addDts(s)} />)
  })
  // 回填
  const fillbackFields = []
  wpc.sourceEntity.fields.forEach((item) => {
    if (item.name.includes('.')) return
    if ((item.type === 'REFERENCE' && item.ref[0] === wpc.targetEntity.entity) || (item.type === 'N2NREFERENCE' && item.ref[0] === wpc.targetEntity.entity) || item.type === 'ANYREFERENCE') {
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

  if (config.fillbackMode === 2) {
    $('#fillbackMode')[0].checked = true
    $('#fillbackMode').parents('.bosskey-show').removeClass('bosskey-show')
  }

  let _ImportsFilterMapping
  $('#importsMode').on('click', function () {
    if ($val(this)) {
      $('.J_importsMode-set').removeClass('hide')
      if (!_ImportsFilterMapping) {
        renderRbcomp(<ImportsFilterMapping defaultValue={config.importsFilter} />, $('.J_importsMode-fields>span'), function () {
          _ImportsFilterMapping = this
        })
      }
    } else {
      $('.J_importsMode-set').addClass('hide')
    }
  })

  // 4.3
  $('#one2nMode').on('click', function () {
    if ($val(this)) $('.J_one2nMode-set').removeClass('hide')
    else $('.J_one2nMode-set').addClass('hide')
  })
  wpc.sourceEntity.fields.forEach((field) => {
    if (field.name.includes('approvalStepUsers') || field.name.includes('.seq')) return
    // if (field.name.includes('.')) return
    if (['NUMBER', 'N2NREFERENCE', 'MULTISELECT', 'TAG'].includes(field.type)) {
      $(`<option value="${field.name}">${field.label}</option>`).appendTo('.J_one2nMode-fields select')
    }
  })
  if ($('.J_one2nMode-fields select option').length === 0) {
    $(`<option value="">${$L('无')}</option>`).appendTo('.J_one2nMode-fields select')
  }

  const $btn = $('.J_save').on('click', function () {
    const one2nMode = $('#one2nMode').prop('checked')
    if (one2nMode && rb.commercial < 10) {
      RbHighbar.error(WrapHtml($L('免费版不支持启用多记录转换 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }
    const importsMode = $('#importsMode').prop('checked')
    if (importsMode && rb.commercial < 10) {
      RbHighbar.error(WrapHtml($L('免费版不支持启用明细记录导入 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    const fm = _FieldsMapping.buildMapping()
    if (fm === false) return

    if (!fm) {
      RbHighbar.create($L('请至少设置 1 个字段映射'))
      return
    }

    let detailsUnmapping = false
    let fmd36 = null
    const fmdList37 = []
    for (let key in _FieldsMapping37) {
      const fmd = _FieldsMapping37[key].buildMapping()
      if (fmd === false) return

      if (fmd) {
        fmdList37.push(fmd)
        if (key === _FieldsMapping2_key) fmd36 = fmd
      } else {
        detailsUnmapping = true
      }
    }

    const tips = []
    if (detailsUnmapping) tips.push($L('明细实体未配置字段映射，因此明细记录不会转换'))

    let importsFilter

    // 检查必填
    let unset = 0
    for (let k in fm) {
      if (fm[k] === null) unset++
    }
    fmdList37.forEach((fmd) => {
      for (let k in fmd || {}) {
        if (fmd[k] === null) unset++
      }
    })

    if (unset > 0) tips.push($L('部分必填字段未映射，可能导致直接转换失败'))

    if (importsMode) {
      importsFilter = _ImportsFilterMapping.buildFilter()
      if (importsFilter.length === 0) {
        tips.push($L('明细记录导入规则未配置，将导入源实体的所有记录'))
      }
    }

    // save
    const config = {
      fieldsMapping: fm,
      fieldsMappingDetail: fmd36,
      fieldsMappingDetails: fmdList37,
      fillbackField: $('#fillbackField').val(),
      fillbackMode: $val('#fillbackMode') ? 2 : 0,
      useFilter: _AdvFilter_data,
      importsMode: $val('#importsMode'),
      importsFilter: importsFilter || null,
      importsMode2Auto: ($val('#importsMode2Auto1') ? 1 : 0) + ($val('#importsMode2Auto2') ? 2 : 0),
      one2nMode: one2nMode,
      one2nModeField: $val('.J_one2nMode-fields select'),
    }

    if (one2nMode && !config.one2nModeField) {
      RbHighbar.createl('请选择转换依据字段')
      return
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
            {tips.length > 0 && <p className="text-warning m-0 mt-1">{tips.join(' / ')}</p>}
          </RF>
        )
        RbAlert.create(msg, {
          icon: 'info-outline',
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
  })

  // init
  setTimeout(() => {
    _saveFilter(config.useFilter)

    if (config.fillbackField) $('#fillbackField').val(config.fillbackField).trigger('change')
    if (config.importsMode) $('#importsMode').trigger('click')
    if (config.importsMode2Auto === 1 || config.importsMode2Auto === 3) $('#importsMode2Auto1').prop('checked', true)
    if (config.importsMode2Auto === 2 || config.importsMode2Auto === 3) $('#importsMode2Auto2').prop('checked', true)
    if (config.one2nMode) $('#one2nMode').trigger('click')
    if (config.one2nModeField) $('.J_one2nMode-fields select').val(config.one2nModeField)
  }, 100)
})

const _VFIXED = 'VFIXED'
const _AdvFilters = {}

class FieldsMapping extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, useVfixed: {} }
    this._FieldValueSet = {}
  }

  componentDidMount() {
    const data = this.props.data || {}
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

        if (Array.isArray(data[fieldName])) {
          useVfixed[fieldName] = true
        } else {
          $s2.val(data[fieldName] || null).trigger('change')
        }
      })

    this.setState({ useVfixed })

    for (let fieldName in data) {
      if (Array.isArray(data[fieldName])) {
        if (!this._FieldValueSet[fieldName]) continue
        this._FieldValueSet[fieldName].setValue(data[fieldName][0])

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

    if (data && data._ && data._.filter) {
      this.setState({ filterData: data._.filter })
    }
  }

  render() {
    const se = this.props.source
    const te = this.props.target
    if (!te.fields || te.fields.length === 0) {
      return <RbAlertBox message={$L('无可用字段')} />
    }

    return (
      <div ref={(c) => (this._$fieldsMapping = c)}>
        <div className="row title2">
          <div className="col-4 text-bold">{te.label}</div>
          <div className="col-2"></div>
          <div className="col-5 text-bold">
            {se.label}
            {this.props.isDetail && (
              <a className={`filter ${this.state.filterData && 'active'}`} title={$L('明细转换条件')} onClick={() => this._saveFilter()}>
                <i className="icon mdi mdi-filter-check-outline" />
              </a>
            )}
          </div>
        </div>

        {te.fields.map((item, idx) => {
          const isCommon = item.name === 'owningUser' || item.readonly
          return (
            <div className="row" key={idx}>
              <div className="col-4">
                <span className={`badge ${!item.nullable && 'req'} ${!item.repeatable && 'rep'}  ${isCommon && 'readonly'}`}>{item.label}</span>
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
                  <FieldValueSet entity={te.entity} field={item} placeholder={$L('固定值')} defaultValue={null} ref={(c) => (this._FieldValueSet[item.name] = c)} />
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

    if (mapping === false) return false
    if (!hasMapping) return null

    // v3.7
    mapping['_'] = { target: this.props.target.entity, source: this.props.source.entity, filter: this.state.filterData }
    return mapping
  }

  _saveFilter() {
    const key = this.props.target.entity + '_' + this.props.source.entity
    if (_AdvFilters[key]) {
      _AdvFilters[key].show()
    } else {
      renderRbcomp(
        <AdvFilter
          title={$L('明细转换条件')}
          inModal
          canNoFilters
          entity={this.props.source.entity}
          filter={this.state.filterData}
          confirm={(res) => {
            this.setState({ filterData: res && res.items.length > 0 ? res : null })
          }}
        />,
        function () {
          _AdvFilters[key] = this
        }
      )
    }
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
        {state.targetFields && state.targetFields.length === 0 && <span className="text-muted text-italic">{$L('无')}</span>}
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

// 明细转换
class DlgAddDts extends RbAlert {
  renderContent() {
    return (
      <form className="rbalert-form-sm" ref={(c) => (this._$form = c)}>
        <div className="form-group">
          <label>{$L('选择源实体')}</label>
          <select className="form-control form-control-sm">
            {this.props.source.map((item) => {
              return (
                <option value={item.entity} key={item.entity}>
                  {item.label}
                </option>
              )
            })}
          </select>
        </div>
        <div className="form-group">
          <label>{$L('选择目标实体')}</label>
          <select className="form-control form-control-sm">
            {' '}
            {this.props.target.map((item) => {
              return (
                <option value={item.entity} key={item.entity}>
                  {item.label}
                </option>
              )
            })}
          </select>
        </div>
        <div className="form-group mb-2">
          <button type="button" className="btn btn-primary" onClick={this._onConfirm}>
            {$L('确定')}
          </button>
        </div>
      </form>
    )
  }

  componentDidMount() {
    super.componentDidMount()
    $(this._$form).find('select').select2({ allowClear: false })
  }

  _onConfirm = () => {
    const s = { source: $(this._$form).find('select:eq(0)').val(), target: $(this._$form).find('select:eq(1)').val() }
    const res = this.props.onConfirm(s)
    if (res !== false) this.hide()
  }
}
