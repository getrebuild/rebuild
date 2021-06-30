/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~ 公式编辑器
const INPUT_KEYS = ['+', 1, 2, 3, '-', 4, 5, 6, '×', 7, 8, 9, '÷', '(', ')', 0, '.', $L('回退'), $L('清空')]
// eslint-disable-next-line no-unused-vars
class FormulaCalc extends RbAlert {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  renderContent() {
    return (
      <div className="formula-calc">
        <div className="form-control-plaintext formula mb-2" _title={$L('计算公式')} ref={(c) => (this._$formula = c)} />
        <div className="row unselect">
          <div className="col-6">
            <div className="fields rb-scroller" ref={(c) => (this._$fields = c)}>
              <ul className="list-unstyled mb-0" _title={$L('无可用字段')}>
                {this.props.fields.map((item) => {
                  return (
                    <li key={item[0]} className={item[2] ? `flag-${item[2]}` : ''}>
                      <a onClick={() => this.handleInput(item)}>{item[1]}</a>
                    </li>
                  )
                })}
              </ul>
            </div>
          </div>
          <div className="col-6 pl-0">
            <ul className="list-unstyled numbers mb-0">
              {this.renderExtraKeys()}
              {INPUT_KEYS.map((item) => {
                return (
                  <li className="list-inline-item" key={`N-${item}`}>
                    <a onClick={() => this.handleInput(item)}>{item}</a>
                  </li>
                )
              })}
              <li className="list-inline-item">
                <a onClick={() => this.confirm()} className="confirm">
                  {$L('确定')}
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

  renderExtraKeys() {
    return null
  }

  handleInput(v) {
    if (v === $L('回退')) {
      $(this._$formula).find('.v:last').remove()
    } else if (v === $L('清空')) {
      $(this._$formula).empty()
    } else if (typeof v === 'object') {
      $(`<i class="v field" data-v="{${v[0]}}">{${v[1]}}</i>`).appendTo(this._$formula)
    } else if (['+', '-', '×', '÷', '(', ')'].includes(v)) {
      $(`<i class="v oper" data-v="${v}">${v}</em>`).appendTo(this._$formula)
    } else {
      $(`<i class="v num" data-v="${v}">${v}</i>`).appendTo(this._$formula)
    }
  }

  confirm() {
    const expr = []
    $(this._$formula)
      .find('i')
      .each(function () {
        expr.push($(this).data('v'))
      })

    typeof this.props.onConfirm === 'function' && this.props.onConfirm(expr.join(''))
    this.hide()
  }

  // 公式文本化
  static textFormula(formula, fields) {
    if (!formula) return ''
    for (let i = 0; i < fields.length; i++) {
      const item = fields[i]
      formula = formula.replace(new RegExp(`{${item[0]}}`, 'ig'), `{${item[1]}}`)
    }
    return formula.toUpperCase()
  }
}

// ~~ 日期公式
// eslint-disable-next-line no-unused-vars
class FormulaDate extends RbAlert {
  constructor(props) {
    super(props)
    this.state = { calcNum: 1, calcUnit: 'D' }
  }

  renderContent() {
    const base = this.props.base ? this.props.base : [['NOW', $L('当前日期')]]
    return (
      <form className="ml-6 mr-6">
        <div className="form-group">
          <label className="text-bold">{$L('设置日期公式')}</label>
          <div className="input-group">
            <select className="form-control form-control-sm" ref={(c) => (this._$base = c)}>
              {base.map((item) => {
                return (
                  <option key={item[0]} value={item[0]}>
                    {item[1]}
                  </option>
                )
              })}
            </select>
            <select className="form-control form-control-sm ml-1" onChange={(e) => this.setState({ calcOp: e.target.value })}>
              <option value="">{$L('不计算')}</option>
              <option value="+">{$L('加上')}</option>
              <option value="-">{$L('减去')}</option>
            </select>
            <input
              type="number"
              min="1"
              max="999999"
              className="form-control form-control-sm ml-1"
              defaultValue="1"
              disabled={!this.state.calcOp}
              onChange={(e) => this.setState({ calcNum: e.target.value })}
            />
            <select className="form-control form-control-sm ml-1" disabled={!this.state.calcOp} onChange={(e) => this.setState({ calcUnit: e.target.value })}>
              <option value="D">{$L('日')}</option>
              <option value="M">{$L('月')}</option>
              <option value="Y">{$L('年')}</option>
              {this.props.type === 'DATETIME' && (
                <React.Fragment>
                  <option value="H">{$L('小时')}</option>
                  <option value="I">{$L('分钟')}</option>
                </React.Fragment>
              )}
            </select>
          </div>
        </div>
        <div className="form-group mb-1">
          <button type="button" className="btn btn-primary" onClick={() => this.confirm()}>
            {$L('确定')}
          </button>
        </div>
      </form>
    )
  }

  confirm() {
    let expr = $(this._$base).val()
    if (!expr) return

    if (this.state.calcOp) {
      if (isNaN(this.state.calcNum) || this.state.calcNum < 1) {
        return RbHighbar.create($L('请输入数字'))
      }
      expr += ` ${this.state.calcOp} ${this.state.calcNum}${this.state.calcUnit}`
    }

    typeof this.props.onConfirm === 'function' && this.props.onConfirm(`{${expr}}`)
    this.hide()
  }
}

// ~ 聚合公式
// eslint-disable-next-line no-unused-vars
class FormulaAggregation extends FormulaCalc {
  handleInput(v) {
    if (typeof v === 'object') {
      const that = this
      const $field = $(`<span class="v field hover"><i data-toggle="dropdown" data-v="{${v[0]}}" data-name="${v[1]}">{${v[1]}}<i></span>`)
      const $menu = $('<div class="dropdown-menu"></div>').appendTo($field)
      $(['', 'SUM', 'COUNT', 'COUNT2', 'AVG', 'MAX', 'MIN']).each(function () {
        const $a = $(`<a class="dropdown-item" data-mode="${this}">${FormulaAggregation.CALC_MODES[this] || $L('无')}</a>`).appendTo($menu)
        $a.click(function () {
          that._changeCalcMode(this)
        })
      })
      $field.appendTo(this._$formula)
    } else {
      super.handleInput(v)
    }
  }

  _changeCalcMode(el) {
    el = $(el)
    const $field = el.parent().prev()
    const mode = el.data('mode')
    const modeText = mode ? ` (${FormulaAggregation.CALC_MODES[mode]})` : ''
    $field.attr('data-mode', mode || '').text(`{${$field.data('name')}${modeText}}`)
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

    if ($(this._$formulaInput).val()) expr = $(this._$formulaInput).val()
    else expr = expr.join('')

    typeof this.props.onConfirm === 'function' && this.props.onConfirm(expr)
    this.hide()
  }

  static CALC_MODES = {
    SUM: $L('求和'),
    COUNT: $L('计数'),
    COUNT2: $L('去重计数'),
    AVG: $L('平均值'),
    MAX: $L('最大值'),
    MIN: $L('最小值'),
    FORMULA: $L('计算公式'),
  }

  /**
   * 公式文本化
   *
   * @param {*} formula
   * @param {*} fields
   * @returns
   */
  static textFormula(formula, fields) {
    for (let i = 0; i < fields.length; i++) {
      const field = fields[i]
      formula = formula.replace(new RegExp(`{${field[0]}}`, 'ig'), `{${field[1]}}`)
      formula = formula.replace(new RegExp(`{${field[0]}\\$`, 'ig'), `{${field[1]}$`)
    }

    const keys = Object.keys(FormulaAggregation.CALC_MODES)
    keys.reverse()
    keys.forEach((k) => {
      formula = formula.replace(new RegExp(`\\$\\$\\$\\$${k}`, 'g'), ` (${FormulaAggregation.CALC_MODES[k]})`)
    })
    return formula.toUpperCase()
  }
}
