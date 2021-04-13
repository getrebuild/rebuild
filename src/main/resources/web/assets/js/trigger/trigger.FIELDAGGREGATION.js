/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const CALC_MODES = {
  SUM: $L('CalcSUM'),
  COUNT: $L('CalcCOUNT'),
  COUNT2: $L('CalcCOUNT2'),
  AVG: $L('CalcAVG'),
  MAX: $L('CalcMAX'),
  MIN: $L('CalcMIN'),
  FORMULA: $L('CalcFORMULA'),
}

// ~~ 数据聚合
// eslint-disable-next-line no-undef
class ContentFieldAggregation extends ActionContentSpec {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <div className="field-aggregation">
        <form className="simple">
          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('TargetEntity')}</label>
            <div className="col-md-12 col-lg-9">
              <div className="row">
                <div className="col-5">
                  <select className="form-control form-control-sm" ref={(c) => (this._targetEntity = c)}>
                    {(this.state.targetEntities || []).map((item) => {
                      const val = `${item[2]}.${item[0]}`
                      return (
                        <option key={val} value={val}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                </div>
              </div>
              {this.state.hadApproval && (
                <div className="form-text text-danger">
                  <i className="zmdi zmdi-alert-triangle fs-16 down-1 mr-1"></i>
                  {$L('TriggerTargetEntityTips')}
                </div>
              )}
            </div>
          </div>
          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('AggregationRule')}</label>
            <div className="col-md-12 col-lg-9">
              <div className="items">
                {(this.state.items || []).length > 0 &&
                  this.state.items.map((item) => {
                    return (
                      <div key={item.targetField}>
                        <div className="row">
                          <div className="col-5">
                            <span className="badge badge-warning">{_getFieldLabel(item.targetField, this.state.targetFields)}</span>
                          </div>
                          <div className="col-2">
                            <span className="zmdi zmdi-forward zmdi-hc-rotate-180"></span>
                            <span className="badge badge-warning">{CALC_MODES[item.calcMode]}</span>
                          </div>
                          <div className="col-5 del-wrap">
                            <span className="badge badge-warning">
                              {item.calcMode === 'FORMULA' ? this.textFormula(item.sourceFormula) : _getFieldLabel(item.sourceField, this.__sourceFieldsCache)}
                            </span>
                            <a className="del" title={$L('Remove')} onClick={() => this.delItem(item.targetField)}>
                              <span className="zmdi zmdi-close"></span>
                            </a>
                          </div>
                        </div>
                      </div>
                    )
                  })}
              </div>
              <div className="row">
                <div className="col-5">
                  <select className="form-control form-control-sm" ref={(c) => (this._targetField = c)}>
                    {(this.state.targetFields || []).map((item) => {
                      return (
                        <option key={item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$L('TargetField')}</p>
                </div>
                <div className="col-2 pr-0">
                  <span className="zmdi zmdi-forward zmdi-hc-rotate-180"></span>
                  <select className="form-control form-control-sm" ref={(c) => (this._calcMode = c)}>
                    {Object.keys(CALC_MODES).map((item) => {
                      return (
                        <option key={item} value={item}>
                          {CALC_MODES[item]}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$L('AggregationMethod')}</p>
                </div>
                <div className="col-5">
                  <div className={this.state.calcMode === 'FORMULA' ? '' : 'hide'}>
                    <div className="form-control-plaintext formula" _title={$L('CalcFORMULA')} ref={(c) => (this._$formula = c)} onClick={() => this.showFormula()}></div>
                    <p>{$L('CalcFORMULA')}</p>
                  </div>
                  <div className={this.state.calcMode === 'FORMULA' ? 'hide' : ''}>
                    <select className="form-control form-control-sm" ref={(c) => (this._sourceField = c)}>
                      {(this.state.sourceFields || []).map((item) => {
                        return (
                          <option key={item[0]} value={item[0]}>
                            {item[1]}
                          </option>
                        )
                      })}
                    </select>
                    <p>{$L('SourceField')}</p>
                  </div>
                </div>
              </div>
              <div className="mt-1">
                <button type="button" className="btn btn-primary btn-sm btn-outline" onClick={() => this.addItem()}>
                  + {$L('Add')}
                </button>
              </div>
            </div>
          </div>
          <div className="form-group row pb-0">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right"></label>
            <div className="col-md-12 col-lg-9">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" ref={(c) => (this._readonlyFields = c)} />
                <span className="custom-control-label">{$L('SetTargetFieldReadonly')}</span>
              </label>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('AggregationFilter')}</label>
            <div className="col-md-12 col-lg-9">
              <a className="btn btn-sm btn-link pl-0 text-left down-2" onClick={this._dataAdvFilter}>
                {this.state.dataFilterItems ? `${$L('AdvFiletrSeted')} (${this.state.dataFilterItems})` : $L('ClickSet')}
              </a>
              <div className="form-text mt-0">{$L('AggregationFilterTips')}</div>
            </div>
          </div>
        </form>
      </div>
    )
  }

  componentDidMount() {
    const content = this.props.content
    this.__select2 = []
    $.get(`/admin/robot/trigger/field-aggregation-entities?source=${this.props.sourceEntity}`, (res) => {
      this.setState({ targetEntities: res.data }, () => {
        const $s2te = $(this._targetEntity)
          .select2({ placeholder: $L('SelectSome,TargetEntity') })
          .on('change', () => this._changeTargetEntity())

        if (content && content.targetEntity) {
          $s2te.val(content.targetEntity)
          if (rb.env !== 'dev') $s2te.attr('disabled', true)
        }

        $s2te.trigger('change')
        this.__select2.push($s2te)
      })
    })

    if (content) {
      $(this._readonlyFields).attr('checked', content.readonlyFields === true)
      this._saveAdvFilter(content.dataFilter)
    }
  }

  _changeTargetEntity() {
    const te = ($(this._targetEntity).val() || '').split('.')[1]
    if (!te) return
    // 清空现有规则
    this.setState({ items: [] })

    $.get(`/admin/robot/trigger/field-aggregation-fields?source=${this.props.sourceEntity}&target=${te}`, (res) => {
      this.setState({ hadApproval: res.data.hadApproval })
      this.__sourceFieldsCache = res.data.source

      if (this.state.targetFields) {
        this.setState({ targetFields: res.data.target }, () => {
          $(this._calcMode).trigger('change')
        })
      } else {
        this.setState({ sourceFields: res.data.source, targetFields: res.data.target }, () => {
          const $s2sf = $(this._sourceField).select2({ placeholder: $L('SelectSome,SourceField') })
          const $s2cm = $(this._calcMode)
            .select2({ placeholder: $L('SelectSome,AggregationMethod') })
            .on('change', (e) => {
              this.setState({ calcMode: e.target.value })

              if (e.target.value === 'COUNT' || e.target.value === 'COUNT2') {
                this.setState({ sourceFields: this.__sourceFieldsCache })
              } else {
                // 仅数字字段
                const fs = this.__sourceFieldsCache.filter((x) => x[2] === 'NUMBER' || x[2] === 'DECIMAL')
                this.setState({ sourceFields: fs })
              }
            })
          const $s2tf = $(this._targetField).select2({ placeholder: $L('SelectSome,TargetField') })

          $s2cm.trigger('change')

          this.__select2.push($s2sf)
          this.__select2.push($s2cm)
          this.__select2.push($s2tf)
        })

        if (this.props.content) {
          this.setState({ items: this.props.content.items || [] })
        }
      }
    })
  }

  showFormula() {
    const fs = this.__sourceFieldsCache.filter((x) => x[2] === 'NUMBER' || x[2] === 'DECIMAL')
    renderRbcomp(<FormulaCalc2 fields={fs} onConfirm={(v) => $(this._$formula).attr('data-v', v).text(this.textFormula(v))} />)
  }

  textFormula(formula) {
    return FormulaCalc2.textFormula(formula, this.__sourceFieldsCache)
  }

  _dataAdvFilter = () => {
    const that = this
    if (that._advFilter) that._advFilter.show()
    else
      renderRbcomp(
        <AdvFilter title={$L('DataFilter')} inModal={true} canNoFilters={true} entity={this.props.sourceEntity} filter={that._advFilter__data} confirm={that._saveAdvFilter} />,
        null,
        function () {
          that._advFilter = this
        }
      )
  }

  _saveAdvFilter = (filter) => {
    this._advFilter__data = filter
    this.setState({ dataFilterItems: filter && filter.items ? filter.items.length : 0 })
  }

  addItem() {
    const tf = $(this._targetField).val()
    const calc = $(this._calcMode).val()
    const sf = calc === 'FORMULA' ? null : $(this._sourceField).val()
    const formula = calc === 'FORMULA' ? $(this._$formula).attr('data-v') : null

    if (!tf) return RbHighbar.create($L('PlsSelectSome,TargetField'))
    if (calc === 'FORMULA') {
      if (!formula) return RbHighbar.create($L('PlsInputSome,CalcFORMULA'))
    } else if (!sf) {
      return RbHighbar.create($L('PlsSelectSome,SourceField'))
    }

    // 目标字段=源字段
    const tfFull = `${$(this._targetEntity).val().split('.')[0]}.${tf}`.replace('$PRIMARY$.', '')
    if (sf === tfFull) return RbHighbar.create($L('TargetAndSourceNotSame'))

    const items = this.state.items || []
    const exists = items.find((x) => x.targetField === tf)
    if (exists) return RbHighbar.create($L('SomeDuplicate,TargetField'))

    items.push({ targetField: tf, calcMode: calc, sourceField: sf, sourceFormula: formula })
    this.setState({ items: items })
  }

  delItem(targetField) {
    const itemsNew = (this.state.items || []).filter((item) => {
      return item.targetField !== targetField
    })
    this.setState({ items: itemsNew })
  }

  buildContent() {
    const content = {
      targetEntity: $(this._targetEntity).val(),
      items: this.state.items,
      readonlyFields: $(this._readonlyFields).prop('checked'),
      dataFilter: this._advFilter__data,
    }

    if (!content.targetEntity) {
      RbHighbar.create($L('PlsSelectSome,TargetEntity'))
      return false
    }
    if (content.items.length === 0) {
      RbHighbar.create($L('PlsAdd1AggregationRuleLeast'))
      return false
    }
    return content
  }
}

const _getFieldLabel = function (field, fields) {
  let found = fields.find((x) => x[0] === field)
  if (found) found = found[1]
  return found || '[' + field.toUpperCase() + ']'
}

// ~ 公式编辑器
// eslint-disable-next-line no-undef
class FormulaCalc2 extends FormulaCalc {
  constructor(props) {
    super(props)
  }

  handleInput(v) {
    if (typeof v === 'object') {
      const $field = $(`<span class="v field hover"><i data-toggle="dropdown" data-v="{${v[0]}}" data-name="${v[1]}">{${v[1]}}<i></span>`)
      const $menu = $('<div class="dropdown-menu"></div>').appendTo($field)
      $(['', 'SUM', 'COUNT', 'COUNT2', 'AVG', 'MAX', 'MIN']).each(function () {
        const $a = $(`<a class="dropdown-item" data-mode="${this}">${CALC_MODES[this] || $L('Null')}</a>`).appendTo($menu)
        $a.click(function () {
          _changeCalcMode(this)
        })
      })
      $field.appendTo(this._$formula)
    } else {
      super.handleInput(v)
    }
  }

  confirm() {
    let expr = []
    $(this._$formula)
      .find('i')
      .each(function () {
        const $this = $(this)
        const v = $this.data('v')
        if ($this.attr('data-mode')) expr.push(`${v.substr(0, v.length - 1)}$$$$${$this.attr('data-mode')}}`)
        else expr.push(v)
      })

    expr = expr.join('')
    if ($(this._$formulaInput).val()) expr = $(this._$formulaInput).val()

    typeof this.props.onConfirm === 'function' && this.props.onConfirm(expr)
    this.hide()
  }

  // 公式文本化
  static textFormula(formula, fields) {
    for (let i = 0; i < fields.length; i++) {
      const field = fields[i]
      formula = formula.replace(new RegExp(`{${field[0]}}`, 'ig'), `{${field[1]}}`)
      formula = formula.replace(new RegExp(`{${field[0]}\\$`, 'ig'), `{${field[1]}$`)
    }

    const keys = Object.keys(CALC_MODES)
    keys.reverse()
    keys.forEach((k) => {
      formula = formula.replace(new RegExp(`\\$\\$\\$\\$${k}`, 'g'), ` (${CALC_MODES[k]})`)
    })
    return formula.toUpperCase()
  }
}

const _changeCalcMode = function (el) {
  el = $(el)
  const $field = el.parent().prev()
  const mode = el.data('mode')
  const modeText = mode ? ` (${CALC_MODES[mode]})` : ''
  $field.attr('data-mode', mode || '').text(`{${$field.data('name')}${modeText}}`)
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  renderRbcomp(<ContentFieldAggregation {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
  })
}
