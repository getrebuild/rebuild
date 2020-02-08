const CALC_MODES = { 'SUM': '求和', 'COUNT': '计数', 'AVG': '平均值', 'MAX': '最大', 'MIN': '最小', 'FORMULA': '计算公式' }

// ~~ 数据聚合
// eslint-disable-next-line no-undef
class ContentFieldAggregation extends ActionContentSpec {

  constructor(props) {
    super(props)
  }

  render() {
    return <div className="field-aggregation">
      <form className="simple">
        <div className="form-group row">
          <label className="col-md-12 col-lg-3 col-form-label text-lg-right">聚合目标实体</label>
          <div className="col-md-12 col-lg-9">
            <div className="row">
              <div className="col-5">
                <select className="form-control form-control-sm" ref={(c) => this._targetEntity = c}>
                  {(this.state.targetEntities || []).map((item) => {
                    return <option key={'te-' + item[2] + item[0]} value={item[2] + '.' + item[0]}>{item[1]}</option>
                  })}
                </select>
              </div>
            </div>
            {this.state.hadApproval
              && <div className="form-text text-danger"><i className="zmdi zmdi-alert-triangle fs-16 down-1"></i> 目标实体已启用审批流程，可能影响源实体操作（触发动作）</div>}
          </div>
        </div>
        <div className="form-group row">
          <label className="col-md-12 col-lg-3 col-form-label text-lg-right">聚合规则</label>
          <div className="col-md-12 col-lg-9">
            <div className="items">
              {(!this.state.items || this.state.items.length === 0) ? null : this.state.items.map((item) => {
                return <div key={'item-' + item.targetField}>
                  <div className="row">
                    <div className="col-5"><span className="badge badge-warning">{this.__fieldLabel(this.state.targetFields, item.targetField)}</span></div>
                    <div className="col-2">
                      <span className="zmdi zmdi-forward zmdi-hc-rotate-180"></span>
                      <span className="badge badge-warning">{CALC_MODES[item.calcMode]}</span>
                    </div>
                    <div className="col-5 del-wrap">
                      <span className="badge badge-warning">
                        {item.calcMode === 'FORMULA' ? this.textFormula(item.sourceFormula) : this.__fieldLabel(this.state.sourceFields, item.sourceField)}
                      </span>
                      <a className="del" title="移除" onClick={() => this.delItem(item.targetField)}><span className="zmdi zmdi-close"></span></a>
                    </div>
                  </div>
                </div>
              })}
            </div>
            <div className="row">
              <div className="col-5">
                <select className="form-control form-control-sm" ref={(c) => this._targetField = c}>
                  {(this.state.targetFields || []).map((item) => {
                    return <option key={'tf-' + item[0]} value={item[0]}>{item[1]}</option>
                  })}
                </select>
                <p>目标字段</p>
              </div>
              <div className="col-2 pr-0">
                <span className="zmdi zmdi-forward zmdi-hc-rotate-180"></span>
                <select className="form-control form-control-sm" ref={(c) => this._calcMode = c}>
                  {Object.keys(CALC_MODES).map((item) => {
                    return <option key={'opt-' + item} value={item}>{CALC_MODES[item]}</option>
                  })}
                </select>
                <p>聚合方式</p>
              </div>
              <div className="col-5">
                <div className={this.state.calcMode === 'FORMULA' ? '' : 'hide'}>
                  <div className="form-control-plaintext formula" ref={(c) => this._$formula = c} onClick={this.showFormula}></div>
                  <p>计算公式 (源字段)</p>
                </div>
                <div className={this.state.calcMode === 'FORMULA' ? 'hide' : ''}>
                  <select className="form-control form-control-sm" ref={(c) => this._sourceField = c}>
                    {(this.state.sourceFields || []).map((item) => {
                      return <option key={'sf-' + item[0]} value={item[0]}>{item[1]}</option>
                    })}
                  </select>
                  <p>源字段</p>
                </div>
              </div>
            </div>
            <div className="mt-1">
              <button type="button" className="btn btn-primary btn-sm bordered" onClick={() => this.addItem()}>添加</button>
            </div>
          </div>
        </div>
        <div className="form-group row pb-0">
          <label className="col-md-12 col-lg-3 col-form-label text-lg-right"></label>
          <div className="col-md-12 col-lg-9">
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" ref={(c) => this._readonlyFields = c} />
              <span className="custom-control-label">自动设置目标字段为只读</span>
            </label>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-md-12 col-lg-3 col-form-label text-lg-right">聚合数据条件</label>
          <div className="col-md-12 col-lg-9">
            <a className="btn btn-sm btn-link pl-0 text-left down-2" onClick={this._dataAdvFilter}>
              {this.state.dataFilterItems ? `已设置条件 (${this.state.dataFilterItems})` : '点击设置'}
            </a>
            <div className="form-text mt-0">仅会聚合符合过滤条件的数据</div>
          </div>
        </div>
      </form>
    </div>
  }

  componentDidMount() {
    const content = this.props.content
    this.__select2 = []
    $.get(`${rb.baseUrl}/admin/robot/trigger/field-aggregation-entities?source=${this.props.sourceEntity}`, (res) => {
      this.setState({ targetEntities: res.data }, () => {
        const s2te = $(this._targetEntity).select2({ placeholder: '选择聚合目标实体' })
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

    $.get(`${rb.baseUrl}/admin/robot/trigger/field-aggregation-fields?source=${this.props.sourceEntity}&target=${te}`, (res) => {
      this.setState({ hadApproval: res.data.hadApproval })

      if (this.state.targetFields) {
        this.setState({ targetFields: res.data.target })
      } else {
        this.setState({ sourceFields: res.data.source, targetFields: res.data.target }, () => {
          const s2sf = $(this._sourceField).select2({ placeholder: '选择源字段' })
          const s2cm = $(this._calcMode).select2({ placeholder: '选择聚合方式' })
            .on('change', (e) => this.setState({ calcMode: e.target.value }))
          const s2tf = $(this._targetField).select2({ placeholder: '选择目标字段' })

          this.__select2.push(s2sf)
          this.__select2.push(s2cm)
          this.__select2.push(s2tf)
        })

        if (this.props.content) this.setState({ items: this.props.content.items || [] })
      }
    })
  }

  __fieldLabel(fields, field) {
    let found = fields.find((x) => { return x[0] === field })
    if (found) found = found[1]
    return found || ('[' + field.toUpperCase() + ']')
  }

  textFormula(formula) {
    const fs = this.state.sourceFields
    for (let i = 0; i < fs.length; i++) {
      const field = fs[i]
      formula = formula.replace(new RegExp(`{${field[0]}}`, 'ig'), `{${field[1]}}`)
      formula = formula.replace(new RegExp(`{${field[0]}\\$`, 'ig'), `{${field[1]}$`)
    }
    for (let k in CALC_MODES) {
      formula = formula.replace(new RegExp(`\\$\\$\\$\\$${k}`, 'g'), ` (${CALC_MODES[k]})`)
    }
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
      RbHighbar.create('请选择目标字段')
      return false
    }
    if (calc === 'FORMULA') {
      if (!formula) {
        RbHighbar.create('请填写计算公式')
        return false
      }
    } else if (!sf) {
      RbHighbar.create('请选择源字段')
      return false
    }

    const items = this.state.items || []
    const found = items.find((x) => { return x.targetField === tf })
    if (found) { RbHighbar.create('目标字段重复'); return false }

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
      dataFilter: this._advFilter__data
    }
    if (!content.targetEntity) { RbHighbar.create('请选择聚合目标实体'); return false }
    if (content.items.length === 0) { RbHighbar.create('请至少添加 1 个聚合规则'); return false }
    return content
  }

  _dataAdvFilter = () => {
    const that = this
    if (that._advFilter) that._advFilter.show()
    else renderRbcomp(<AdvFilter title="数据过滤条件" inModal={true} canNoFilters={true}
      entity={this.props.sourceEntity}
      filter={that._advFilter__data}
      confirm={that._saveAdvFilter} />, null, function () { that._advFilter = this })
  }

  _saveAdvFilter = (filter) => {
    this._advFilter__data = filter
    this.setState({ dataFilterItems: filter && filter.items ? filter.items.length : 0 })
  }
}

// ~公式计算器
const INPUT_KEYS = ['+', 1, 2, 3, '-', 4, 5, 6, '×', 7, 8, 9, '÷', '(', ')', 0, '.', '回退', '清空']
class FormulaCalc extends RbAlert {

  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  renderContent() {
    return <div className="formula-calc">
      <div className="form-control-plaintext formula mb-2" ref={(c) => this._$formula = c}></div>
      <div className="row">
        <div className="col-6">
          <div className="fields rb-scroller" ref={(c) => this._$fields = c}>
            <ul className="list-unstyled mb-0">
              {this.props.fields.map((item) => {
                return <li key={`flag-${item}`}><a onClick={() => this.handleInput(item)}>{item[1]}</a></li>
              })}
            </ul>
          </div>
        </div>
        <div className="col-6 pl-0">
          <ul className="list-unstyled numbers mb-0">
            {INPUT_KEYS.map((item) => {
              return <li className="list-inline-item" key={`flag-${item}`}><a onClick={() => this.handleInput(item)}>{item}</a></li>
            })}
            <li className="list-inline-item"><a onClick={() => this.confirm()} className="confirm">确定</a></li>
          </ul>
        </div>
      </div>
    </div>
  }

  componentDidMount() {
    super.componentDidMount()
    $(this._$fields).perfectScrollbar()
  }

  handleInput(v) {
    if (v === '回退') {
      $(this._$formula).find('.v:last').remove()
    } else if (v === '清空') {
      $(this._$formula).empty()
    } else if (typeof v === 'object') {
      const $field = $(`<span class="v field"><i data-toggle="dropdown" data-v="{${v[0]}}" data-name="${v[1]}">{${v[1]}}<i></span>`)
      const $menu = $('<div class="dropdown-menu"></div>').appendTo($field)
      $(['', 'SUM', 'COUNT', 'AVG', 'MAX', 'MIN']).each(function () {
        const $a = $(`<a class="dropdown-item" data-mode="${this}">${CALC_MODES[this] || '无'}</a>`).appendTo($menu)
        $a.click(function () { FormulaCalc._changeCalcMode(this) })
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
    $(this._$formula).find('i').each(function () {
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
  // eslint-disable-next-line no-undef
  renderRbcomp(<ContentFieldAggregation {...props} />, 'react-content', function () { contentComp = this })
}