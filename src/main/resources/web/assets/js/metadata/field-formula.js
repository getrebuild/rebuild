/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 验证公式有效性
function verifyFormula(formula, entity, onConfirm) {
  formula = formula.replace(/\n/gi, '\\n')
  $.post(`/admin/robot/trigger/verify-formula?entity=${entity}`, formula, (res) => {
    if (res.error_code === 0) {
      onConfirm()
    } else {
      RbAlert.create(
        <RF>
          <p>{$L('计算公式可能存在错误，这会导致触发器执行失败。是否继续？')}</p>
          {res.error_msg && <pre className="text-danger p-2">{res.error_msg}</pre>}
        </RF>,
        {
          type: 'warning',
          onConfirm: function () {
            this.hide()
            onConfirm()
          },
          onCancel: function () {
            this.hide()
          },
        }
      )
    }
  })
}

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
                    <li key={item.name} className={`flag-${item.type || 'N'}`}>
                      <a onClick={() => this.handleInput(item)} title={item.label}>
                        {item.label}
                      </a>
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
      $(`<i class="v field" data-v="{${v.name}}">{${v.label}}</i>`).appendTo(this._$formula)
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

    const formula = expr.join('')
    const that = this
    function _onConfirm() {
      typeof that.props.onConfirm === 'function' && that.props.onConfirm(formula)
      that.hide()
    }

    if (formula && this.props.verifyFormula) {
      verifyFormula(formula, this.props.entity, _onConfirm)
    } else {
      _onConfirm()
    }
  }

  // 公式文本化
  static textFormula(formula, fields) {
    if (!formula) return ''

    for (let i = 0; i < fields.length; i++) {
      const field = fields[i]
      formula = formula.replace(new RegExp(`\\{${field.name}\\}`, 'ig'), `{____${field.label}}`)
    }
    formula = formula.replace(new RegExp('\\{____', 'g'), '{') // fix: Label 与 Name 名称冲突

    return formula //.toUpperCase()
  }
}

// ~ 聚合公式编辑器
// eslint-disable-next-line no-unused-vars
class FormulaAggregation extends FormulaCalc {
  handleInput(v) {
    if (typeof v === 'object') {
      const that = this
      const $field = $(`<span class="v field hover"><i data-toggle="dropdown" data-v="{${v.name}}" data-name="${v.label}">{${v.label}}<i></span>`)
      const $aggrMenu = $('<div class="dropdown-menu dropdown-menu-sm"></div>').appendTo($field)
      $(['', 'SUM', 'COUNT', 'COUNT2', 'AVG', 'MAX', 'MIN']).each(function () {
        const $a = $(`<a class="dropdown-item" data-mode="${this}">${FormulaAggregation.CALC_MODES[this] || $L('无')}</a>`).appendTo($aggrMenu)
        $a.on('click', function () {
          that._changeCalcMode(this)
        })
      })
      $field.appendTo(this._$formula)
      $aggrMenu.find('a:eq(1)').trigger('click') // default:SUM
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
    const expr = []
    $(this._$formula)
      .find('i')
      .each(function () {
        const $this = $(this)
        const v = $this.data('v')
        if ($this.attr('data-mode')) expr.push(`${v.substr(0, v.length - 1)}$$$$${$this.attr('data-mode')}}`)
        else expr.push(v)
      })

    let formula
    if ($(this._$formulaInput).val()) formula = $(this._$formulaInput).val()
    else formula = expr.join('')

    const that = this
    function _onConfirm() {
      typeof that.props.onConfirm === 'function' && that.props.onConfirm(formula)
      that.hide()
    }

    if (formula && this.props.verifyFormula) {
      verifyFormula(formula, this.props.entity, _onConfirm)
    } else {
      _onConfirm()
    }
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
    if (!formula) return ''

    for (let i = 0; i < fields.length; i++) {
      const field = fields[i]
      formula = formula.replace(new RegExp(`\\{${field.name}\\}`, 'ig'), `{${field.label}}`)
      formula = formula.replace(new RegExp(`\\{${field.name}\\$`, 'ig'), `{${field.label}$`)
    }

    const keys = Object.keys(FormulaAggregation.CALC_MODES)
    keys.reverse()
    keys.forEach((k) => {
      formula = formula.replace(new RegExp(`\\$\\$\\$\\$${k}`, 'g'), ` (${FormulaAggregation.CALC_MODES[k]})`)
    })
    return formula //.toUpperCase()
  }

  /**
   * @param {*} name
   * @param {*} fields
   * @returns
   */
  static getLabel(name, fields) {
    const x = fields.find((x) => x.name === name)
    return x ? x.label : `[${name.toUpperCase()}]`
  }
}

// ~~ 日期公式编辑器
// eslint-disable-next-line no-unused-vars
class FormulaDate extends RbAlert {
  constructor(props) {
    super(props)
    this.state = { calcNum: 1, calcUnit: props.type === 'TIME' ? 'H' : 'D' }
  }

  renderContent() {
    const base = this.props.base ? this.props.base : [['NOW', $L('当前时间')]]
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
              {this.props.type !== 'TIME' && (
                <RF>
                  <option value="D">{$L('日')}</option>
                  <option value="M">{$L('月')}</option>
                  <option value="Y">{$L('年')}</option>
                </RF>
              )}
              {(this.props.type === 'DATETIME' || this.props.type === 'TIME') && (
                <RF>
                  <option value="H">{$L('小时')}</option>
                  <option value="I">{$L('分钟')}</option>
                </RF>
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

// ~~ 匹配字段
// eslint-disable-next-line no-unused-vars
class MatchFields extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
    this.state.targetFields = this.reset(props, true)
  }

  // fix:3.8.2
  _groupFields38() {
    let _groupFields = this.state.groupFields
    if (_groupFields && typeof _groupFields === 'string') {
      eval(`_groupFields = ${_groupFields}`)
    }
    return _groupFields
  }

  render() {
    const groupFields = this._groupFields38()
    return (
      <div className="group-fields">
        {groupFields && groupFields.length > 0 && (
          <div className="mb-1">
            {groupFields.map((item) => {
              return (
                <span className="d-inline-block mb-1" key={item.targetField}>
                  <span className="badge badge-primary badge-close m-0 mr-1">
                    <span>{FormulaAggregation.getLabel(item.targetField, this.__targetFields)}</span>
                    <i className="mdi mdi-arrow-left-right ml-1 mr-1" />
                    <span>{FormulaAggregation.getLabel(item.sourceField, this.__sourceFields)}</span>
                    <a className="close" title={$L('移除')} onClick={() => this._delGroupField(item.targetField)}>
                      <i className="mdi mdi-close" />
                    </a>
                  </span>
                </span>
              )
            })}
          </div>
        )}

        <div className="row">
          <div className="col-5">
            <select className="form-control form-control-sm" ref={(c) => (this._$targetField = c)}>
              {(this.state.targetFields || []).map((item) => {
                if (['createdBy', 'createdOn', 'modifiedBy', 'modifiedOn', 'owningUser', 'owningDept'].includes(item.name) || item.type === 'DATETIME') return null
                return (
                  <option key={item.name} value={item.name}>
                    {item.label}
                  </option>
                )
              })}
            </select>
            <p>{$L('目标匹配字段')}</p>
          </div>
          <div className="col-5">
            <i className="zmdi mdi mdi-arrow-left-right" />
            <select className="form-control form-control-sm" ref={(c) => (this._$sourceField = c)}>
              {(this.state.sourceFields || []).map((item) => {
                return (
                  <option key={item.name} value={item.name}>
                    {item.label}
                  </option>
                )
              })}
            </select>
            <p>{$L('源匹配字段')}</p>
          </div>
        </div>
        <div className="mt-1">
          <button type="button" className="btn btn-primary btn-sm btn-outline" onClick={() => this._addGroupField()}>
            + {$L('添加')}
          </button>
        </div>
      </div>
    )
  }

  componentDidMount() {
    $(this._$sourceField)
      .select2({ placeholder: $L('选择源匹配字段') })
      .on('change', () => {})

    $(this._$targetField)
      .select2({ placeholder: $L('选择目标匹配字段') })
      .on('change', (e) => {
        let TF = e.target.value
        if (!TF) return
        TF = this.__targetFields.find((x) => x.name === TF)

        // 仅同类型的字段（DATE/DATETIME 兼容）
        const SF = this.__sourceFields.filter((x) => {
          if (TF.type === 'DATE' && x.type === 'DATETIME') return true
          if (TF.type === 'DATETIME' && x.type === 'DATE') return true
          if (TF.type === x.type) {
            if (x.type === 'REFERENCE') return TF.ref[0] === x.ref[0]
            if (x.type === 'CLASSIFICATION') return TF.classification === x.classification
            return true
          }
          // 文本兼容
          if (['TEXT', 'PHONE'].includes(TF.type) && ['TEXT', 'PHONE'].includes(x.type)) return true
          if (['TEXT', 'EMAIL'].includes(TF.type) && ['TEXT', 'EMAIL'].includes(x.type)) return true
          if (['TEXT', 'SERIES'].includes(TF.type) && ['TEXT', 'SERIES'].includes(x.type)) return true
          return false
        })
        this.setState({ sourceFields: SF })
      })
      .trigger('change')
  }

  _addGroupField() {
    const item = { targetField: $(this._$targetField).val(), sourceField: $(this._$sourceField).val() }
    if (!item.targetField) return RbHighbar.create($L('请选择目标匹配字段'))
    if (!item.sourceField) return RbHighbar.create($L('请选择源匹配字段'))

    const groupFields = this._groupFields38()
    let exists = groupFields.find((x) => item.targetField === x.targetField)
    if (exists) return RbHighbar.create($L('目标匹配字段已添加'))
    exists = groupFields.find((x) => item.sourceField === x.sourceField)
    if (exists) return RbHighbar.create($L('源匹配字段已添加'))

    groupFields.push(item)
    this.setState({ groupFields })
  }

  _delGroupField(TF) {
    const groupFields = this._groupFields38().filter((x) => x.targetField !== TF)
    this.setState({ groupFields })
  }

  reset(props, init) {
    this.__targetFields = props.targetFields || []
    this.__sourceFields = props.sourceFields || []

    // TODO 开放更多匹配字段
    const targetFields = []
    this.__targetFields.forEach((item) => {
      if (['SERIES', 'TEXT', 'PHONE', 'EMAIL', 'DATE', 'DATETIME', 'CLASSIFICATION', 'REFERENCE'].includes(item.type)) targetFields.push(item)
    })

    if (init) {
      return targetFields
    } else {
      this.setState({ targetFields, groupFields: props.groupFields || [] }, () => $(this._$targetField).trigger('change'))
    }
  }

  val() {
    return this._groupFields38() || []
  }
}

// ~ 带字段的文本域
// eslint-disable-next-line no-unused-vars
class EditorWithFieldVars extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    let attrs = {
      className: 'form-control',
      maxLength: 2000,
      placeholder: this.props.placeholder || null,
    }

    if (this.props.isCode) {
      attrs = { ...attrs, className: 'formula-code', maxLength: 6000, autoFocus: true }
    }

    return (
      <div className="textarea-wrap">
        <textarea {...attrs} spellCheck="false" ref={(c) => (this._$content = c)} />
        <div className="dropdown fields-vars">
          <a title={$L('插入字段变量')} data-toggle="dropdown">
            <i className="mdi mdi-code-braces" />
          </a>
          <div className="dropdown-menu auto-scroller dropdown-menu-right" style={{ maxHeight: 388 }} ref={(c) => (this._$fieldVars = c)}>
            {(this.state.fieldVars || []).map((item) => {
              let typeMark = 'T'
              if (['DATE', 'DATETIME', 'TIME'].includes(item.type)) typeMark = 'D'
              else if (['NUMBER', 'DECIMAL'].includes(item.type)) typeMark = 'N'
              return (
                <a
                  key={item.name}
                  className="dropdown-item"
                  data-name={item.name}
                  data-pinyin={item.quickCode}
                  onClick={() => {
                    $(this._$content).insertAtCursor(`{${item.name}}`)
                  }}>
                  <em>{typeMark}</em>
                  {item.label}
                </a>
              )
            })}
          </div>
        </div>
        {this.state.funcs && (
          <div className="dropdown fields-vars funcs">
            <a title={$L('插入函数')} data-toggle="dropdown">
              <i className="mdi mdi-function-variant" />
            </a>
            <div className="dropdown-menu auto-scroller dropdown-menu-right" ref={(c) => (this._$funcs = c)}>
              {this.state.funcs.map((item) => {
                return (
                  <a
                    key={item.name}
                    className="dropdown-item"
                    data-name={item.name}
                    onClick={() => {
                      $(this._$content).insertAtCursor(`${item.name}()`)
                    }}>
                    {item.label}
                  </a>
                )
              })}
            </div>
          </div>
        )}
      </div>
    )
  }

  componentDidMount() {
    $.get(`/commons/metadata/fields?entity=${this.props.entity}&deep=3`, (res) => {
      this.setState({ fieldVars: res.data || [] }, () => {
        $(this._$fieldVars).perfectScrollbar()
      })
    })

    // v4.2
    if (this.props.showFuncs) {
      const IGNORED_NAMES = ['CACHE', 'LOG', 'RAWSQLQUERY', 'RAWSQLUPDATE', 'USERUPDATE', 'DEPTUPDATE', 'PDFMERGE', 'HANLPSIM', 'HANLPSEG', 'HANLPPINY', '$L']
      $.get('/admin/robot/trigger/field-writeback-custom-funcs', (res) => {
        let funcs = []
        res.data.forEach((name) => {
          if (!IGNORED_NAMES.includes(name)) funcs.push({ name: name, label: name })
        })
        this.setState({ funcs }, () => {
          $(this._$funcs).perfectScrollbar()
        })
      })
    }

    // eslint-disable-next-line no-undef
    autosize(this._$content)
    setTimeout(() => {
      const evt = new Event('input')
      this._$content.dispatchEvent(evt)
      // search
      $dropdownMenuSearch(this._$fieldVars)
    }, 200)
  }

  val() {
    if (arguments.length > 0) {
      $(this._$content).val(arguments[0])
      // eslint-disable-next-line no-undef
      autosize.update(this._$content)
    } else {
      return $(this._$content).val()
    }
  }

  focus() {
    setTimeout(() => this._$content.focus(), 20)
  }
}
