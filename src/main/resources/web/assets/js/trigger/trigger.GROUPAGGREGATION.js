/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const CALC_MODES = {
  SUM: $L('求和'),
  COUNT: $L('计数'),
  COUNT2: $L('去重计数'),
  AVG: $L('平均值'),
  MAX: $L('最大值'),
  MIN: $L('最小值'),
  FORMULA: $L('计算公式'),
}

// ~~ 分组聚合
// eslint-disable-next-line no-undef
class ContentGroupAggregation extends ActionContentSpec {
  render() {
    return (
      <div className="field-aggregation">
        <form className="simple">
          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('目标实体')}</label>
            <div className="col-md-12 col-lg-9">
              <div className="row">
                <div className="col-5">
                  <select className="form-control form-control-sm" ref={(c) => (this._$targetEntity = c)}>
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
            </div>
          </div>

          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('分组字段')}</label>
            <div className="col-md-12 col-lg-9">
              {this.state.groupFieldsSet && this.state.groupFieldsSet.length > 0 && (
                <div className="mb-2 mt-1">
                  {(this.state.groupFieldsSet || []).map((item) => {
                    return (
                      <span className="badge badge-square badge-close" key={item} data-field={item}>
                        {_getFieldLabel(item, this.state.groupFields)}
                        <a className="close" title={$L('移除')} onClick={() => this.delGroupField(item)}>
                          <i className="zmdi zmdi-close" />
                        </a>
                      </span>
                    )
                  })}
                </div>
              )}
              <div className="row">
                <div className="col-5">
                  <select className="form-control form-control-sm" ref={(c) => (this._$groupField = c)}>
                    {(this.state.groupFields || []).map((item) => {
                      return (
                        <option key={item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                  <div className="mt-1">
                    <button type="button" className="btn btn-primary btn-sm btn-outline" onClick={() => this.addGroupField()}>
                      + {$L('添加')}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('聚合规则')}</label>
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
                            <i className="zmdi zmdi-forward zmdi-hc-rotate-180" />
                            <span className="badge badge-warning">{CALC_MODES[item.calcMode]}</span>
                          </div>
                          <div className="col-5 del-wrap">
                            <span className="badge badge-warning">
                              {item.calcMode === 'FORMULA'
                                ? FormulaCalcExt.textFormula(item.sourceFormula, this.__sourceFieldsCache)
                                : _getFieldLabel(item.sourceField, this.__sourceFieldsCache)}
                            </span>
                            <a className="del" title={$L('移除')} onClick={() => this.delItem(item.targetField)}>
                              <i className="zmdi zmdi-close" />
                            </a>
                          </div>
                        </div>
                      </div>
                    )
                  })}
              </div>
              <div className="row">
                <div className="col-5">
                  <select className="form-control form-control-sm" ref={(c) => (this._$targetField = c)}>
                    {(this.state.targetFields || []).map((item) => {
                      return (
                        <option key={item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$L('目标字段')}</p>
                </div>
                <div className="col-2 pr-0">
                  <i className="zmdi zmdi-forward zmdi-hc-rotate-180" />
                  <select className="form-control form-control-sm" ref={(c) => (this._$calcMode = c)}>
                    {Object.keys(CALC_MODES).map((item) => {
                      return (
                        <option key={item} value={item}>
                          {CALC_MODES[item]}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$L('聚合方式')}</p>
                </div>
                <div className="col-5">
                  <div className={this.state.calcMode === 'FORMULA' ? '' : 'hide'}>
                    <div
                      className="form-control-plaintext formula"
                      _title={$L('计算公式')}
                      ref={(c) => (this._$sourceFormula = c)}
                      onClick={() => this.showFormula()}
                    />
                    <p>{$L('计算公式')}</p>
                  </div>
                  <div className={this.state.calcMode === 'FORMULA' ? 'hide' : ''}>
                    <select className="form-control form-control-sm" ref={(c) => (this._$sourceField = c)}>
                      {(this.state.sourceFields || []).map((item) => {
                        return (
                          <option key={item[0]} value={item[0]}>
                            {item[1]}
                          </option>
                        )
                      })}
                    </select>
                    <p>{$L('聚合字段')}</p>
                  </div>
                </div>
              </div>
              <div className="mt-1">
                <button type="button" className="btn btn-primary btn-sm btn-outline" onClick={() => this.addItem()}>
                  + {$L('添加')}
                </button>
              </div>
            </div>
          </div>

          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('聚合数据条件')}</label>
            <div className="col-md-12 col-lg-9">
              <a className="btn btn-sm btn-link pl-0 text-left down-2" onClick={() => this.dataAdvFilter()}>
                {this.state.dataFilterItems ? `${$L('已设置条件')} (${this.state.dataFilterItems})` : $L('点击设置')}
              </a>
              <div className="form-text mt-0">{$L('仅会聚合符合过滤条件的数据')}</div>
            </div>
          </div>

          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right" />
            <div className="col-md-12 col-lg-9">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" ref={(c) => (this._$autoCreate = c)} />
                <span className="custom-control-label">{$L('目标记录不存在时自动新建')}</span>
              </label>
            </div>
          </div>
        </form>
      </div>
    )
  }

  componentDidMount() {
    const content = this.props.content
    this.__select2 = []

    $.get(`/admin/robot/trigger/group-aggregation-entities?source=${this.props.sourceEntity}`, (res) => {
      this.__sourceFieldsCache = res.data.sourceFields

      this.setState({ ...res.data }, () => {
        const $s2te = $(this._$targetEntity)
          .select2({ placeholder: $L('选择目标实体') })
          .on('change', () => this._changeTargetEntity())

        if (content && content.targetEntity) {
          $s2te.val(content.targetEntity)
          if (rb.env !== 'dev') $s2te.attr('disabled', true)
        }

        $s2te.trigger('change')
        this.__select2.push($s2te)

        if (content) {
          this.setState({ groupFieldsSet: content.groupFields || [] })
        }
      })
    })

    if (content) {
      $(this._$autoCreate).attr('checked', content.autoCreate === true)
      this.saveAdvFilter(content.dataFilter)
    } else {
      $(this._$autoCreate).attr('checked', true)
    }
  }

  _changeTargetEntity() {
    const te = ($(this._$targetEntity).val() || '').split('.')[1]
    if (!te) return
    // 清空现有规则
    this.setState({ items: [] })

    $.get(`/admin/robot/trigger/group-aggregation-fields?source=${this.props.sourceEntity}&target=${te}`, (res) => {
      if (this.state.targetFields) {
        this.setState({ targetFields: res.data.targetFields }, () => $(this._$calcMode).trigger('change'))
      } else {
        // init
        this.setState({ ...res.data }, () => {
          const $s2sf = $(this._$sourceField).select2({ placeholder: $L('选择聚合字段') })
          const $s2cm = $(this._$calcMode)
            .select2({ placeholder: $L('选择聚合方式') })
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
          const $s2tf = $(this._$targetField).select2({ placeholder: $L('选择目标字段') })

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
    renderRbcomp(
      <FormulaCalcExt
        fields={fs}
        onConfirm={(v) => {
          $(this._$sourceFormula).attr('data-v', v).text(FormulaCalcExt.textFormula(v, this.__sourceFieldsCache))
        }}
      />
    )
  }

  dataAdvFilter() {
    if (this._advFilter) {
      this._advFilter.show()
    } else {
      const that = this
      renderRbcomp(
        <AdvFilter
          title={$L('数据过滤条件')}
          inModal={true}
          canNoFilters={true}
          entity={this.props.sourceEntity}
          filter={that._advFilter__data}
          confirm={(f) => that.saveAdvFilter(f)}
        />,
        null,
        function () {
          that._advFilter = this
        }
      )
    }
  }

  saveAdvFilter(filter) {
    this._advFilter__data = filter
    this.setState({ dataFilterItems: filter && filter.items ? filter.items.length : 0 })
  }

  addItem() {
    const tf = $(this._$targetField).val()
    const calc = $(this._$calcMode).val()
    const sf = calc === 'FORMULA' ? null : $(this._$sourceField).val()
    const formula = calc === 'FORMULA' ? $(this._$sourceFormula).attr('data-v') : null

    if (!tf) return RbHighbar.create($L('请选择目标字段'))
    if (calc === 'FORMULA') {
      if (!formula) return RbHighbar.create($L('请输入计算公式'))
    } else if (!sf) {
      return RbHighbar.create($L('请选择聚合字段'))
    }

    // 目标字段=源字段
    const tfFull = `${$(this._$targetEntity).val().split('.')[0]}.${tf}`.replace('$PRIMARY$.', '')
    if (sf === tfFull) return RbHighbar.create($L('目标字段与聚合字段不能为同一字段'))

    const items = this.state.items || []
    const exists = items.find((x) => x.targetField === tf)
    if (exists) return RbHighbar.create($L('目标字段重复'))

    items.push({ targetField: tf, calcMode: calc, sourceField: sf, sourceFormula: formula })
    this.setState({ items: items }, () => $(this._$sourceFormula).empty())
  }

  delItem(targetField) {
    const itemsNew = (this.state.items || []).filter((x) => x.targetField !== targetField)
    this.setState({ items: itemsNew })
  }

  addGroupField() {
    const groupFieldsSet = [...(this.state.groupFieldsSet || [])]
    const field = $(this._$groupField).val()
    if (groupFieldsSet.contains(field)) return RbHighbar.create($L('分组字段已添加'))
    else groupFieldsSet.push(field)
    this.setState({ groupFieldsSet })
  }

  delGroupField(field) {
    const groupFieldsSet = this.state.groupFieldsSet.filter((x) => x !== field)
    this.setState({ groupFieldsSet })
  }

  buildContent() {
    const content = {
      targetEntity: $(this._$targetEntity).val(),
      items: this.state.items || [],
      dataFilter: this._advFilter__data,
      autoCreate: $(this._$autoCreate).prop('checked'),
      groupFields: this.state.groupFieldsSet || [],
    }

    if (!content.targetEntity) {
      RbHighbar.create($L('请选择目标实体'))
      return false
    }
    if (content.groupFields.length === 0) {
      RbHighbar.create($L('请至少添加 1 个分组字段'))
      return false
    }
    if (content.items.length === 0) {
      RbHighbar.create($L('请至少添加 1 个聚合规则'))
      return false
    }

    return content
  }
}

const _getFieldLabel = function (field, fields) {
  let x = fields.find((x) => x[0] === field)
  if (x) x = x[1]
  return x || `[${field.toUpperCase()}]`
}

const _changeCalcMode = function (el) {
  el = $(el)
  const $field = el.parent().prev()
  const mode = el.data('mode')
  const modeText = mode ? ` (${CALC_MODES[mode]})` : ''
  $field.attr('data-mode', mode || '').text(`{${$field.data('name')}${modeText}}`)
}

// ~ 公式编辑器
// eslint-disable-next-line no-undef
class FormulaCalcExt extends FormulaCalc {
  handleInput(v) {
    if (typeof v === 'object') {
      const $field = $(`<span class="v field hover"><i data-toggle="dropdown" data-v="{${v[0]}}" data-name="${v[1]}">{${v[1]}}<i></span>`)
      const $menu = $('<div class="dropdown-menu"></div>').appendTo($field)
      $(['', 'SUM', 'COUNT', 'COUNT2', 'AVG', 'MAX', 'MIN']).each(function () {
        const $a = $(`<a class="dropdown-item" data-mode="${this}">${CALC_MODES[this] || $L('无')}</a>`).appendTo($menu)
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

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  renderRbcomp(<ContentGroupAggregation {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
    $('#react-content [data-toggle="tooltip"]').tooltip()
  })
}
