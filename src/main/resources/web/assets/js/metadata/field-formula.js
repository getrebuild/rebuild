/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 字段公式

// ~ 数字公式
// @see trigger.FIELDAGGREGATION.js
const INPUT_KEYS = ['+', 1, 2, 3, '-', 4, 5, 6, '×', 7, 8, 9, '÷', '(', ')', 0, '.', $L('Back'), $L('Clear')]
class FormulaCalc extends RbAlert {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  renderContent() {
    return (
      <div className="formula-calc">
        <div className="form-control-plaintext formula mb-2" _title={$L('CalcFORMULA')} ref={(c) => (this._formula = c)}></div>
        <div className="row">
          <div className="col-6">
            <div className="fields rb-scroller" ref={(c) => (this._fields = c)}>
              <ul className="list-unstyled mb-0" _title={$L('NoUsesField')}>
                {this.props.fields.map((item) => {
                  return (
                    <li key={item[0]}>
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
                  <li className="list-inline-item" key={`N-${item}`}>
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
    $(this._fields).perfectScrollbar()
  }

  handleInput(v) {
    if (v === $L('Back')) {
      $(this._formula).find('.v:last').remove()
    } else if (v === $L('Clear')) {
      $(this._formula).empty()
    } else if (typeof v === 'object') {
      $(`<i class="v field" data-v="{${v[0]}}">{${v[1]}}</i>`).appendTo(this._formula)
    } else if (['+', '-', '×', '÷', '(', ')'].includes(v)) {
      $(`<i class="v oper" data-v="${v}">${v}</em>`).appendTo(this._formula)
    } else {
      $(`<i class="v num" data-v="${v}">${v}</i>`).appendTo(this._formula)
    }
  }

  confirm() {
    const expr = []
    $(this._formula)
      .find('i')
      .each(function () {
        expr.push($(this).data('v'))
      })

    typeof this.props.onConfirm === 'function' && this.props.onConfirm(expr.join(''))
    this.hide()
  }
}

// 公式文本化
FormulaCalc.textFormula = function (formula, fields) {
  if (!formula) return ''
  for (let i = 0; i < fields.length; i++) {
    const item = fields[i]
    formula = formula.replace(new RegExp(`{${item[0]}}`, 'ig'), `{${item[1]}}`)
  }
  return formula.toUpperCase()
}

// ~~ 日期公式
// eslint-disable-next-line no-unused-vars
class FormulaDate extends RbAlert {
  constructor(props) {
    super(props)
    this.state = { calcNum: 1, calcUnit: 'D' }
  }

  renderContent() {
    const base = this.props.base ? this.props.base : [['NOW', $L('CurrentDate')]]
    return (
      <form className="ml-6 mr-6">
        <div className="form-group">
          <label className="text-bold">{$L('SetSome,DateFormula')}</label>
          <div className="input-group">
            <select className="form-control form-control-sm" ref={(c) => (this._base = c)}>
              {base.map((item) => {
                return (
                  <option key={item[0]} value={item[0]}>
                    {item[1]}
                  </option>
                )
              })}
            </select>
            <select className="form-control form-control-sm ml-1" onChange={(e) => this.setState({ calcOp: e.target.value })}>
              <option value="">{$L('CalcNone')}</option>
              <option value="+">{$L('CalcPlus')}</option>
              <option value="-">{$L('CalcMinus')}</option>
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
              <option value="D">{$L('Day')}</option>
              <option value="M">{$L('Month')}</option>
              <option value="Q">{$L('Quarter')}</option>
              <option value="Y">{$L('Year')}</option>
              {this.props.type === 'DATETIME' && (
                <React.Fragment>
                  <option value="H">{$L('Hour')}</option>
                  <option value="I">{$L('Minte')}</option>
                </React.Fragment>
              )}
            </select>
          </div>
        </div>
        <div className="form-group mb-1">
          <button type="button" className="btn btn-space btn-primary" onClick={() => this.confirm()}>
            {$L('Confirm')}
          </button>
        </div>
      </form>
    )
  }

  confirm() {
    let expr = $(this._base).val()
    if (!expr) return

    if (this.state.calcOp) {
      if (isNaN(this.state.calcNum) || this.state.calcNum < 1) {
        return RbHighbar.create($L('PlsInputSome,Number'))
      }
      expr += ` ${this.state.calcOp} ${this.state.calcNum}${this.state.calcUnit}`
    }

    typeof this.props.onConfirm === 'function' && this.props.onConfirm(`{${expr}}`)
    this.hide()
  }
}
