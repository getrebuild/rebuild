/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global FieldValueSet, EditorWithFieldVars, MatchFields */

const wpc = window.__PageConfig

const UPDATE_MODES = {
  FIELD: $L('字段值'),
  VFIXED: $L('固定值'),
  VNULL: $L('置空'),
  FORMULA: $L('计算公式'),
}

let __LAB_MATCHFIELDS = false

// ~~ 字段更新
// eslint-disable-next-line no-undef
class ContentFieldWriteback extends ActionContentSpec {
  constructor(props) {
    super(props)
    this.state.updateMode = 'FIELD'
  }

  render() {
    const targetEntities2 = []
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
                      if (item[2] === '$') {
                        targetEntities2.push(item)
                        return null
                      }
                      const val = `${item[2]}.${item[0]}`
                      return (
                        <option key={val} value={val}>
                          {item[1]}
                        </option>
                      )
                    })}

                    {targetEntities2.length > 0 && (
                      <optgroup label={$L('通过字段匹配')}>
                        {targetEntities2.map((item) => {
                          const val = `${item[2]}.${item[0]}`
                          return (
                            <option key={val} value={val}>
                              {item[1]}
                            </option>
                          )
                        })}
                      </optgroup>
                    )}
                  </select>
                </div>
              </div>
              {this.state.hasWarning && (
                <div className="form-text text-danger">
                  <i className="zmdi zmdi-alert-triangle fs-16 down-1 mr-1" />
                  {this.state.hasWarning}
                </div>
              )}
            </div>
          </div>

          {this.state.showMatchFields && (
            <div className="form-group row">
              <label className="col-md-12 col-lg-3 col-form-label text-lg-right"></label>
              <div className="col-md-12 col-lg-9">
                <h5 className="mt-0 text-bold">{$L('字段匹配规则')}</h5>
                <MatchFields targetFields={this.state.targetFields4Group} sourceFields={this.__sourceFieldsCache} ref={(c) => (this._MatchFields = c)} />
              </div>
            </div>
          )}

          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('更新规则')}</label>
            <div className="col-md-12 col-lg-9">
              <div className="items">
                {(this.state.items || []).length > 0 &&
                  this.state.items.map((item, idx) => {
                    // fix: v2.2
                    if (!item.updateMode) item.updateMode = item.sourceField.includes('#') ? 'FORMULA' : 'FIELD'

                    const field = item.updateMode === 'VFIXED' ? this.state.targetFields.find((x) => x.name === item.targetField) : null
                    const isFORMULACode = item.updateMode === 'FORMULA' && FieldFormula.isCode(item.sourceField)
                    return (
                      <div key={item.targetField}>
                        <div className="row">
                          <div className="col-5">
                            <span className="badge badge-warning">{_getFieldLabel(this.state.targetFields, item.targetField)}</span>
                          </div>
                          <div className="col-2">
                            <i className="zmdi zmdi-forward zmdi-hc-rotate-180" />
                            <span className="badge badge-warning">{UPDATE_MODES[item.updateMode]}</span>
                          </div>
                          <div className="col-5 del-wrap">
                            {item.updateMode === 'FIELD' && <span className="badge badge-warning">{_getFieldLabel(this.__sourceFieldsCache, item.sourceField)}</span>}
                            {item.updateMode === 'VFIXED' && <span className="badge badge-light text-break">{FieldValueSet.formatFieldText(item.sourceField, field)}</span>}
                            {item.updateMode === 'FORMULA' && (
                              <span className={`badge badge-warning ${isFORMULACode && 'w-100'}`}>{FieldFormula.formatText(item.sourceField, this.__sourceFieldsCache)}</span>
                            )}
                            <RF>
                              {isFORMULACode && (
                                <a className="edit-code" title={$L('编辑计算公式')} onClick={() => this._editCode(item, idx)}>
                                  <i className="zmdi zmdi-edit" />
                                </a>
                              )}
                              <a className="del" title={$L('移除')} onClick={() => this.delItem(item.targetField)}>
                                <i className="zmdi zmdi-close" />
                              </a>
                            </RF>
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
                      if (item.type === 'SERIES') return null
                      return (
                        <option key={item.name} value={item.name}>
                          {item.label}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$L('目标字段')}</p>
                </div>
                <div className="col-2 pr-0">
                  <i className="zmdi zmdi-forward zmdi-hc-rotate-180" />
                  <select className="form-control form-control-sm" ref={(c) => (this._$updateMode = c)}>
                    {Object.keys(UPDATE_MODES).map((item) => {
                      return (
                        <option key={item} value={item}>
                          {UPDATE_MODES[item]}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$L('更新方式')}</p>
                </div>
                <div className={`col-5 ${this.state.targetField ? '' : 'hide'}`}>
                  <div className={this.state.updateMode === 'FIELD' ? '' : 'hide'}>
                    <select className="form-control form-control-sm" ref={(c) => (this._$sourceField = c)}>
                      {(this.state.sourceFields || []).map((item) => {
                        return (
                          <option key={item.name} value={item.name}>
                            {item.label}
                          </option>
                        )
                      })}
                    </select>
                    <p>{$L('源字段')}</p>
                  </div>
                  <div className={this.state.updateMode === 'VFIXED' ? '' : 'hide'}>
                    {this.state.updateMode === 'VFIXED' && this.state.targetField && (
                      <FieldValueSet entity={this.state.targetEntity} field={this.state.targetField} placeholder={$L('固定值')} ref={(c) => (this._$sourceValue = c)} />
                    )}
                    <p>{$L('固定值')}</p>
                  </div>
                  <div className={this.state.updateMode === 'FORMULA' ? '' : 'hide'}>
                    {this.state.updateMode === 'FORMULA' && this.state.targetField && (
                      <FieldFormula entity={this.props.sourceEntity} fields={this.__sourceFieldsCache} targetField={this.state.targetField} ref={(c) => (this._$sourceFormula = c)} />
                    )}
                    <p>{$L('计算公式')}</p>
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

          <div className="form-group row pb-0">
            <label className="col-md-12 col-lg-3 col-form-label" />
            <div className="col-md-12 col-lg-9">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" ref={(c) => (this._$readonlyFields = c)} />
                <span className="custom-control-label">
                  {$L('自动设置目标字段为只读')}
                  <i className="zmdi zmdi-help zicon down-1" data-toggle="tooltip" title={$L('本选项仅针对表单有效')} />
                </span>
              </label>
              <div className="mt-2">
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._$forceUpdate = c)} />
                  <span className="custom-control-label">
                    {$L('允许强制更新')}
                    <i className="zmdi zmdi-help zicon down-1" data-toggle="tooltip" title={$L('强制更新只读记录')} />
                  </span>
                </label>
              </div>
              <div className="mt-2">
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._$clearFields = c)} />
                  <span className="custom-control-label">{$L('源字段为空时置空目标字段')}</span>
                </label>
              </div>
              <div className="mt-2">
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._$stopPropagation = c)} />
                  <span className="custom-control-label">{$L('禁用级联执行')}</span>
                </label>
              </div>
              <div className="mt-2">
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._$autoCreate = c)} />
                  <span className="custom-control-label">
                    {$L('目标记录不存在时自动新建')}
                    <i className="zmdi zmdi-help zicon down-1" data-toggle="tooltip" title={$L('仅使用“通过字段匹配”时有效')} />
                  </span>
                </label>
              </div>
              <div className="mt-2 bosskey-show">
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._$lockMode = c)} />
                  <span className="custom-control-label">{$L('启用加锁模式')} (LAB)</span>
                </label>
              </div>
            </div>
          </div>
        </form>
      </div>
    )
  }

  componentDidMount() {
    const content = this.props.content
    this.__select2 = []
    $.get(`/admin/robot/trigger/field-writeback-entities?source=${this.props.sourceEntity}&matchfields=${__LAB_MATCHFIELDS}`, (res) => {
      this.setState({ targetEntities: res.data || [] }, () => {
        const $s2te = $(this._$targetEntity)
          .select2({
            placeholder: $L('选择目标实体'),
            templateResult: function (res) {
              const text = res.text.split(' (N)')
              const $span = $('<span></span>').text(text[0])
              if (text.length > 1) $('<span class="badge badge-default badge-pill">N</span>').appendTo($span)
              else if (res.children && res.children.length > 0) $('<sup class="rbv ml-1"></sup>').appendTo($span)
              return $span
            },
          })
          .on('change', () => this._changeTargetEntity())

        if (content && content.targetEntity) {
          $s2te.val(content.targetEntity)
          if (!$s2te.val()) this.setState({ hasWarning: `${$L('目标实体已经不可用')} [${content.targetEntity.toUpperCase()}]` })
          if (rb.env !== 'dev') $s2te.attr('disabled', true)
        }

        $s2te.trigger('change')
        this.__select2.push($s2te)
      })
    })

    if (content) {
      $(this._$readonlyFields).attr('checked', content.readonlyFields === true)
      $(this._$forceUpdate).attr('checked', content.forceUpdate === true)
      $(this._$clearFields).attr('checked', content.clearFields === true)
      $(this._$stopPropagation).attr('checked', content.stopPropagation === true)
      $(this._$autoCreate).attr('checked', content.autoCreate === true)
      if (content.lockMode === true) {
        $(this._$lockMode).attr('checked', true).parents('.mt-2').removeClass('bosskey-show')
      }
    }
  }

  _changeTargetEntity() {
    const teSplit = ($(this._$targetEntity).val() || '').split('.')
    if (!teSplit || !teSplit[1]) return
    // 清空现有规则
    this.setState({ items: [], targetEntity: teSplit[1] })
    if (__LAB_MATCHFIELDS) {
      this.setState({ showMatchFields: teSplit[0] === '$' })
    }

    $.get(`/admin/robot/trigger/field-writeback-fields?source=${this.props.sourceEntity}&target=${teSplit[1]}`, (res) => {
      const _data = res.data || {}
      this.setState({ hasWarning: _data.hadApproval ? $L('目标实体已启用审批流程，可能影响源实体操作 (触发动作)，建议启用“允许强制更新”') : null })

      this.__sourceFieldsCache = _data.source
      let fieldsProps = {
        targetFields: _data.target,
        targetFields4Group: _data.target4Group,
      }

      if (this.state.targetFields) {
        this.setState({ ...fieldsProps }, () => {
          $(this._$targetField).trigger('change')
        })
      } else {
        // init
        this.setState({ sourceFields: _data.source, ...fieldsProps }, () => {
          const $s2tf = $(this._$targetField)
            .select2({ placeholder: $L('选择目标字段') })
            .on('change', () => this._changeTargetField())
          const $s2um = $(this._$updateMode)
            .select2({ placeholder: $L('选择更新方式') })
            .on('change', (e) => {
              this.setState({ updateMode: e.target.value })
            })
          const $s2sf = $(this._$sourceField).select2({ placeholder: $L('选择源字段') })

          $s2tf.trigger('change')
          this.__select2.push($s2tf)
          this.__select2.push($s2um)
          this.__select2.push($s2sf)
        })

        const content = this.props.content
        if (content) {
          this.setState({ items: content.items || [] })
          if (content.targetEntityMatchFields) {
            // v3.8 兼容
            if (typeof content.targetEntityMatchFields === 'string') {
              try {
                eval(`content.targetEntityMatchFields = ${content.targetEntityMatchFields}`)
              } catch (err) {
                // NOOP
              }
            }
            setTimeout(() => this._MatchFields && this._MatchFields.setState({ groupFields: content.targetEntityMatchFields }), 200)
          }
        }
      }

      this._MatchFields && this._MatchFields.reset({ targetFields: this.state.targetFields4Group, sourceFields: this.__sourceFieldsCache })
    })
  }

  _changeTargetField() {
    const tf = $(this._$targetField).val()
    if (!tf) return
    const targetField = this.state.targetFields.find((x) => x.name === tf)

    // 获取可回填字段（兼容的）
    const sourceFields = []
    $(this.__sourceFieldsCache).each(function () {
      if ($fieldIsCompatible(this, targetField)) {
        sourceFields.push(this)
      }
    })

    // fix: GitHub#491
    let sfLast = $(this._$sourceField).val()
    sfLast = sourceFields.find((x) => x.name === sfLast)

    this.setState({ targetField: null, sourceFields: sourceFields }, () => {
      if (!sfLast && sourceFields.length > 0) sfLast = sourceFields[0]

      // 强制销毁后再渲染
      this.setState({ targetField: targetField }, () => {
        if (sfLast) $(this._$sourceField).val(sfLast.name)
        $(this._$sourceField).trigger('change')
      })
    })
  }

  _editCode(item, idx) {
    const initCode = item.sourceField.substr(4, item.sourceField.length - 8)
    renderRbcomp(
      <FormulaCalcWithCode
        entity={this.props.sourceEntity}
        fields={this.__sourceFieldsCache}
        forceCode
        initCode={initCode}
        verifyFormula
        onConfirm={(expr) => {
          if (!expr) return
          const itemsNew = this.state.items
          item.sourceField = expr
          itemsNew[idx] = item
          this.setState({ items: itemsNew })
        }}
      />
    )
  }

  addItem() {
    const targetField = $(this._$targetField).val()
    const mode = $(this._$updateMode).val()
    if (!targetField) return RbHighbar.create($L('请选择目标字段'))

    let sourceAny = null
    if (mode === 'FIELD') {
      sourceAny = $(this._$sourceField).val()
      if (!sourceAny) return RbHighbar.create('请选择源字段')

      // 目标字段=源字段
      const targetFieldFull = `${$(this._$targetEntity).val().split('.')[0]}.${targetField}`.replace('$PRIMARY$.', '')
      if (targetFieldFull === sourceAny) return RbHighbar.create($L('目标字段与源字段不能为同一字段'))

      // ...
    } else if (mode === 'FORMULA') {
      sourceAny = this._$sourceFormula.val()
      if (!sourceAny) return RbHighbar.create($L('请输入计算公式'))
    } else if (mode === 'VFIXED') {
      sourceAny = this._$sourceValue.val()
      if (!sourceAny) return
    } else if (mode === 'VNULL') {
      // v3.6 不校验
      // const tf = this.state.targetFields.find((x) => x.name === targetField)
      // if (!tf.nullable) return RbHighbar.create($L('目标字段 %s 不能为空', tf.label))
    }

    const items = this.state.items || []
    const exists = items.find((x) => x.targetField === targetField)
    if (exists) return RbHighbar.create($L('目标字段重复'))

    items.push({ targetField: targetField, updateMode: mode, sourceField: sourceAny })
    this.setState({ items: items }, () => this._$sourceFormula && this._$sourceFormula.clear())
  }

  delItem(targetField) {
    const itemsNew = (this.state.items || []).filter((item) => {
      return item.targetField !== targetField
    })
    this.setState({ items: itemsNew })
  }

  buildContent() {
    const content = {
      targetEntity: $(this._$targetEntity).val(),
      targetEntityMatchFields: null,
      items: this.state.items,
      readonlyFields: $(this._$readonlyFields).prop('checked'),
      forceUpdate: $(this._$forceUpdate).prop('checked'),
      clearFields: $(this._$clearFields).prop('checked'),
      stopPropagation: $(this._$stopPropagation).prop('checked'),
      autoCreate: $(this._$autoCreate).prop('checked'),
      lockMode: $(this._$lockMode).prop('checked'),
    }
    if (!content.targetEntity) {
      RbHighbar.create($L('请选择目标实体'))
      return false
    }

    if (this.state.showMatchFields) {
      const v = this._MatchFields.val()
      if (v.length === 0) {
        RbHighbar.create($L('请添加字段匹配规则'))
        return false
      } else {
        if (rb.commercial < 1) {
          RbHighbar.error(WrapHtml($L('免费版不支持%s功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)', $L('通过字段匹配'))))
          return false
        }
      }
      content.targetEntityMatchFields = v
    }

    if (content.items.length === 0) {
      RbHighbar.create($L('请至少添加 1 个更新规则'))
      return false
    }

    const one2one = this.state.targetEntities.find((x) => `${x[2]}.${x[0]}` === content.targetEntity)
    if (one2one && one2one[3] === 'one2one') content.one2one = true

    return content
  }
}

const _getFieldLabel = function (fields, fieldName) {
  let found = fields.find((x) => x.name === fieldName)
  if (found) found = found.label
  return found || '[' + fieldName.toUpperCase() + ']'
}

// 公式
class FieldFormula extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    const toFieldType = this.state.targetField.type
    // @see DisplayType.java
    if (['AVATAR', 'SIGN'].includes(toFieldType)) {
      return <div className="form-control-plaintext text-danger">{$L('暂不支持')}</div>
    } else {
      return (
        <div className="form-control-plaintext formula" _title={$L('计算公式')} title={$L('编辑计算公式')} onClick={() => this.show(toFieldType)}>
          {this.state.valueText}
        </div>
      )
    }
  }

  show(type) {
    const fieldVars = []
    this.props.fields.forEach((item) => {
      if (['NUMBER', 'DECIMAL', 'DATE', 'DATETIME'].includes(item.type)) {
        fieldVars.push(item)
      }
    })

    // 数字、日期支持计算器模式
    const forceCode = !['NUMBER', 'DECIMAL', 'DATE', 'DATETIME'].includes(type)
    const initCode = this._value && this._value.startsWith('{{{{') ? this._value.substr(4, this._value.length - 8) : null

    renderRbcomp(<FormulaCalcWithCode entity={this.state.entity} fields={fieldVars} forceCode={forceCode} initCode={initCode} onConfirm={(expr) => this.onConfirm(expr)} verifyFormula />)
  }

  onConfirm(expr) {
    this._value = expr
    this.setState({ valueText: FieldFormula.formatText(expr, this.props.fields) })
  }

  val() {
    return this._value
  }

  clear() {
    this._value = null
    this.setState({ valueText: null })
  }
}

FieldFormula.isCode = function (formula) {
  return formula && formula.startsWith('{{{{')
}
FieldFormula.formatText = function (formula, fields) {
  if (!formula) return null

  // CODE
  if (FieldFormula.isCode(formula)) {
    return FormulaCode.textCode(formula)
  }
  // compatible: DATE
  if (formula.includes('#')) {
    const fs = formula.split('#')
    const field = fields.find((x) => x.name === fs[0])
    return `{${field ? field.label : `[${fs[0].toUpperCase()}]`}}` + (fs[1] || '')
  }
  // NUM,DATE
  else {
    return FormulaCalcWithCode.textFormula(formula, fields)
  }
}

// ~ 公式编辑器
// eslint-disable-next-line no-undef
class FormulaCalcWithCode extends FormulaCalc {
  renderContent() {
    if (this.props.forceCode || !$empty(this.props.initCode) || this.state.useCode) {
      return (
        <FormulaCode
          initCode={this.props.initCode}
          onConfirm={(code) => {
            this.props.onConfirm(!$trim(code) ? null : `{{{{${code}}}}}`)
            this.hide()
          }}
          verifyFormula
          entity={this.props.entity}
        />
      )
    } else {
      return super.renderContent()
    }
  }

  renderExtraKeys() {
    return (
      <RF>
        <li className="list-inline-item">
          <a data-toggle="dropdown">{$L('函数')}</a>
          <div className="dropdown-menu">
            <a className="dropdown-item" onClick={() => this.handleInput('DATEDIFF')} title="DATEDIFF($DATE1, $DATE2, [H|D|M|Y])">
              DATEDIFF
            </a>
            <a className="dropdown-item" onClick={() => this.handleInput('DATEADD')} title="DATEADD($DATE, $NUMBER[H|D|M|Y])">
              DATEADD
            </a>
            <a className="dropdown-item" onClick={() => this.handleInput('DATESUB')} title="DATESUB($DATE, $NUMBER[H|D|M|Y])">
              DATESUB
            </a>
            <a className="dropdown-item" onClick={() => this.handleInput('DATEPICKAT')} title="DATEPICKAT($DATE, [Y|Q|M|D|H|I])">
              DATEPICKAT
            </a>
            <div className="dropdown-divider" />
            <a className="dropdown-item pointer" target="_blank" href="https://getrebuild.com/docs/admin/trigger/fieldwriteback#%E4%BD%BF%E7%94%A8%E6%97%A5%E6%9C%9F%E5%87%BD%E6%95%B0">
              <i className="zmdi zmdi-help icon" />
              {$L('如何使用函数')}
            </a>
          </div>
        </li>
        <li className="list-inline-item">
          <a data-toggle="dropdown">{$L('单位')}</a>
          <div className="dropdown-menu">
            <a className="dropdown-item" onClick={() => this.handleInput('H')}>
              H ({$L('小时')})
            </a>
            <a className="dropdown-item" onClick={() => this.handleInput('D')}>
              D ({$L('日')})
            </a>
            <a className="dropdown-item" onClick={() => this.handleInput('M')}>
              M ({$L('月')})
            </a>
            <a className="dropdown-item" onClick={() => this.handleInput('Y')}>
              Y ({$L('年')})
            </a>
          </div>
        </li>
        <li className="list-inline-item">
          <a onClick={() => this.handleInput('"')}>&#34;</a>
        </li>
        <li className="list-inline-item">
          <a onClick={() => this.handleInput(',')}>,</a>
        </li>
      </RF>
    )
  }

  componentDidMount() {
    if (this._$fields) {
      $(this._$fields).css('max-height', 221)

      const $btn = $(`<a class="switch-code-btn" title="${$L('使用高级计算公式')}"><i class="icon mdi mdi-code-tags"></i></a>`)
      $(this._$formula).addClass('switch-code').after($btn)
      $btn.on('click', () => this.setState({ useCode: true }))
    }

    super.componentDidMount()
  }

  handleInput(v) {
    if (['DATEDIFF', 'DATEADD', 'DATESUB', ',', '"'].includes(v)) {
      $(`<i class="v oper">${v}</em>`).appendTo(this._$formula).attr('data-v', v)

      if (['DATEDIFF', 'DATEADD', 'DATESUB'].includes(v)) {
        setTimeout(() => this.handleInput('('), 200)
      }
    } else {
      super.handleInput(v)
    }
  }
}

class FormulaCode extends React.Component {
  render() {
    return (
      <div>
        <EditorWithFieldVars entity={wpc.sourceEntity} ref={(c) => (this._formulaCode = c)} placeholder="## Support AviatorScript" isCode />
        <div className="row mt-1">
          <div className="col pt-2">
            <span className="d-inline-block">
              <a href="https://getrebuild.com/docs/admin/trigger/fieldwriteback#%E9%AB%98%E7%BA%A7%E8%AE%A1%E7%AE%97%E5%85%AC%E5%BC%8F" target="_blank" className="link">
                {$L('如何使用高级计算公式')}
              </a>
              <i className="zmdi zmdi-help zicon" />
            </span>
          </div>
          <div className="col text-right">
            <button type="button" className="btn btn-primary" onClick={() => this.handleConfirm()}>
              {$L('确定')}
            </button>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    this._formulaCode.val(this.props.initCode || '')
  }

  handleConfirm() {
    const formula = this._formulaCode.val()
    const that = this
    function _onConfirm() {
      typeof that.props.onConfirm === 'function' && that.props.onConfirm(formula)
    }

    if (formula && this.props.verifyFormula) {
      // in field-formula.js
      // eslint-disable-next-line no-undef
      verifyFormula(formula, this.props.entity, _onConfirm)
    } else {
      _onConfirm()
    }
  }

  // 格式化显示
  static textCode(code) {
    code = code.substr(4, code.length - 8) // Remove {{{{ xxx }}}}
    code = code.replace(/( )/gi, '&nbsp;').replace(/</gi, '&lt;').replace(/\n/gi, '<br/>')
    return <code style={{ lineHeight: 1.2 }} dangerouslySetInnerHTML={{ __html: code }} />
  }
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  __LAB_MATCHFIELDS = window.__BOSSKEY || !!(props.content && props.content.targetEntityMatchFields)
  __LAB_MATCHFIELDS = true // v3.8
  renderRbcomp(<ContentFieldWriteback {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
    $('#react-content [data-toggle="tooltip"]').tooltip()
  })
}
