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
                  <i className="zmdi zmdi-alert-triangle fs-16 down-1"></i>
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
                      <div key={'item-' + item.targetField}>
                        <div className="row">
                          <div className="col-5">
                            <span className="badge badge-warning">{this._getFieldLabel(this.state.targetFields, item.targetField)}</span>
                          </div>
                          <div className="col-2">
                            <span className="zmdi zmdi-forward zmdi-hc-rotate-180"></span>
                            <span className="badge badge-warning">{CALC_MODES[item.calcMode]}</span>
                          </div>
                          <div className="col-5 del-wrap">
                            <span className="badge badge-warning">
                              {item.calcMode === 'FORMULA' ? this.textFormula(item.sourceFormula) : this._getFieldLabel(this.state.sourceFields, item.sourceField)}
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
                        <option key={'tf-' + item[0]} value={item[0]}>
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
                        <option key={'opt-' + item} value={item}>
                          {CALC_MODES[item]}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$L('AggregationMethod')}</p>
                </div>
                <div className="col-5">
                  <div className={this.state.calcMode === 'FORMULA' ? '' : 'hide'}>
                    <div className="form-control-plaintext formula" _title={$L('CalcFORMULA')} ref={(c) => (this._$formula = c)} onClick={this.showFormula}></div>
                    <p>
                      {$L('CalcFORMULA')} ({$L('SourceField')})
                    </p>
                  </div>
                  <div className={this.state.calcMode === 'FORMULA' ? 'hide' : ''}>
                    <select className="form-control form-control-sm" ref={(c) => (this._sourceField = c)}>
                      {(this.state.sourceFields || []).map((item) => {
                        return (
                          <option key={'sf-' + item[0]} value={item[0]}>
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
                <span className="custom-control-label">{$L('AutoSetTargetFieldReadonly')}</span>
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
        const s2te = $(this._targetEntity)
          .select2({ placeholder: $L('SelectSome,TargetEntity') })
          .on('change', () => this.changeTargetEntity())

        if (content && content.targetEntity) {
          s2te.val(content.targetEntity)
          if (rb.env !== 'dev') s2te.attr('disabled', true)
        }
        s2te.trigger('change')
        this.__select2.push(s2te)
      })
    })

    if (content) {
      $(this._readonlyFields).attr('checked', content.readonlyFields === true)
      this._saveAdvFilter(content.dataFilter)
    }
  }

  changeTargetEntity() {
    const te = ($(this._targetEntity).val() || '').split('.')[1]
    if (!te) return
    // 清空现有规则
    this.setState({ items: [] })

    $.get(`/admin/robot/trigger/field-aggregation-fields?source=${this.props.sourceEntity}&target=${te}`, (res) => {
      this.setState({ hadApproval: res.data.hadApproval })

      if (this.state.targetFields) {
        this.setState({ targetFields: res.data.target })
      } else {
        this.setState({ sourceFields: res.data.source, targetFields: res.data.target }, () => {
          const s2sf = $(this._sourceField).select2({ placeholder: $L('SelectSome,SourceField') })
          const s2cm = $(this._calcMode)
            .select2({ placeholder: $L('SelectSome,AggregationMethod') })
            .on('change', (e) => this.setState({ calcMode: e.target.value }))
          const s2tf = $(this._targetField).select2({ placeholder: $L('SelectSome,TargetField') })

          this.__select2.push(s2sf)
          this.__select2.push(s2cm)
          this.__select2.push(s2tf)
        })

        if (this.props.content) this.setState({ items: this.props.content.items || [] })
      }
    })
  }

  _getFieldLabel(fields, field) {
    let found = fields.find((x) => x[0] === field)
    if (found) found = found[1]
    return found || '[' + field.toUpperCase() + ']'
  }

  textFormula(formula) {
    const fs = this.state.sourceFields
    for (let i = 0; i < fs.length; i++) {
      const field = fs[i]
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

  showFormula = () => {
    renderRbcomp(<FormulaCalc fields={this.state.sourceFields} call={(v) => $(this._$formula).attr('data-v', v).text(this.textFormula(v))} />)
  }

  addItem() {
    const tf = $(this._targetField).val()
    const calc = $(this._calcMode).val()
    const sf = calc === 'FORMULA' ? null : $(this._sourceField).val()
    const formula = calc === 'FORMULA' ? $(this._$formula).attr('data-v') : null
    if (!tf) {
      RbHighbar.create($L('PlsSelectSome,TargetField'))
      return false
    }
    if (calc === 'FORMULA') {
      if (!formula) {
        RbHighbar.create($L('PlsInputSome,CalcFORMULA'))
        return false
      }
    } else if (!sf) {
      RbHighbar.create($L('PlsSelectSome,SourceField'))
      return false
    }

    // 目标字段=源字段
    if (sf === $(this._targetEntity).val().split('.')[0] + '.' + tf) {
      RbHighbar.create($L('TargetAndSourceNotSame'))
      return false
    }

    const items = this.state.items || []
    const found = items.find((x) => {
      return x.targetField === tf
    })
    if (found) {
      RbHighbar.create($L('SomeDuplicate,TargetField'))
      return false
    }

    items.push({ targetField: tf, calcMode: calc, sourceField: sf, sourceFormula: formula })
    this.setState({ items: items })
  }

  delItem(targetField) {
    const items = (this.state.items || []).filter((item) => {
      return item.targetField !== targetField
    })
    this.setState({ items: items })
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
}

// ~公式计算器
const INPUT_KEYS = ['+', 1, 2, 3, '-', 4, 5, 6, '×', 7, 8, 9, '÷', '(', ')', 0, '.', $L('Back'), $L('Clear')]

class FormulaCalc extends RbAlert {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  renderContent() {
    return (
      <div className="formula-calc">
        <div className="form-control-plaintext formula mb-2" _title={$L('CalcFORMULA')} ref={(c) => (this._$formula = c)}></div>
        <div className="row">
          <div className="col-6">
            <div className="fields rb-scroller" ref={(c) => (this._$fields = c)}>
              <ul className="list-unstyled mb-0">
                {this.props.fields.map((item) => {
                  return (
                    <li key={`flag-${item}`}>
                      <a onClick={() => this.handleInput(item)}>{item[1]}</a>
                    </li>
                  )
                })}
              </ul>
            </div>
          </div>
          <div className="col-6 pl-0">
            <ul className="list-unstyled numbers mb-0">
              {INPUT_KEYS.map((item) => {
                return (
                  <li className="list-inline-item" key={`flag-${item}`}>
                    <a onClick={() => this.handleInput(item)}>{item}</a>
                  </li>
                )
              })}
              <li className="list-inline-item">
                <a onClick={() => this.confirm()} className="confirm">
                  {$L('Confirm')}
                </a>
              </li>
            </ul>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()
    $(this._$fields).perfectScrollbar()
  }

  handleInput(v) {
    if (v === $L('Back')) {
      $(this._$formula).find('.v:last').remove()
    } else if (v === $L('Clear')) {
      $(this._$formula).empty()
    } else if (typeof v === 'object') {
      const $field = $(`<span class="v field"><i data-toggle="dropdown" data-v="{${v[0]}}" data-name="${v[1]}">{${v[1]}}<i></span>`)
      const $menu = $('<div class="dropdown-menu"></div>').appendTo($field)
      $(['', 'SUM', 'COUNT', 'COUNT2', 'AVG', 'MAX', 'MIN']).each(function () {
        const $a = $(`<a class="dropdown-item" data-mode="${this}">${CALC_MODES[this] || $L('Null')}</a>`).appendTo($menu)
        $a.click(function () {
          FormulaCalc._changeCalcMode(this)
        })
      })
      $field.appendTo(this._$formula)
    } else if (['+', '-', '×', '÷', '(', ')'].includes(v)) {
      $(`<i class="v oper" data-v="${v}">${v}</em>`).appendTo(this._$formula)
    } else {
      $(`<i class="v num" data-v="${v}">${v}</i>`).appendTo(this._$formula)
    }
  }

  confirm() {
    let vvv = []
    $(this._$formula)
      .find('i')
      .each(function () {
        const $this = $(this)
        const v = $this.data('v')
        if ($this.attr('data-mode')) vvv.push(`${v.substr(0, v.length - 1)}$$$$${$this.attr('data-mode')}}`)
        else vvv.push(v)
      })
    typeof this.props.call === 'function' && this.props.call(vvv.join(''))
    this.hide()
  }

  static _changeCalcMode(el) {
    el = $(el)
    const $field = el.parent().prev()
    const mode = el.data('mode')
    const modeText = mode ? ` (${CALC_MODES[mode]})` : ''
    $field.attr('data-mode', mode || '').text(`{${$field.data('name')}${modeText}}`)
  }
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  renderRbcomp(<ContentFieldAggregation {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
  })
}
