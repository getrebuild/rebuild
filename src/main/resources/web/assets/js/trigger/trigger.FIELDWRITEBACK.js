/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// @see trigger.FIELDAGGREGATION.js auto-fillin.js

const EXPR_SPLIT = '#'

// ~~ 数据转写
// eslint-disable-next-line no-undef
class ContentFieldWriteback extends ActionContentSpec {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <div className="field-aggregation field-writeback">
        <form className="simple">
          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$lang('TargetEntity')}</label>
            <div className="col-md-12 col-lg-9">
              <div className="row">
                <div className="col-5">
                  <select className="form-control form-control-sm" ref={(c) => (this._targetEntity = c)}>
                    {(this.state.targetEntities || []).map((item) => {
                      const val = item[2] + '.' + item[0]
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
                  {$lang('TriggerTargetEntityTips')}
                </div>
              )}
            </div>
          </div>
          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$lang('WritebackRule')}</label>
            <div className="col-md-12 col-lg-9">
              <div className="items">
                {(this.state.items || []).length > 0 &&
                  this.state.items.map((item) => {
                    return (
                      <div key={'item-' + item.targetField}>
                        <div className="row">
                          <div className="col-5">
                            <span className="badge badge-warning">{this.__fieldLabel(this.__targetFieldsCache, item.targetField)}</span>
                          </div>
                          <div className="col-5 del-wrap">
                            <span className="zmdi zmdi-forward zmdi-hc-rotate-180"></span>
                            <span className="badge badge-warning">{this.__fieldLabel(this.state.sourceFields, item.sourceField)}</span>
                            <a className="del" title={$lang('Remove')} onClick={() => this.delItem(item.targetField)}>
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
                  <p>{$lang('TargetField')}</p>
                </div>
                <div className="col-5">
                  <span className="zmdi zmdi-forward zmdi-hc-rotate-180"></span>
                  <select className="form-control form-control-sm" ref={(c) => (this._sourceField = c)}>
                    {(this.state.sourceFields || []).map((item) => {
                      return (
                        <option key={'sf-' + item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$lang('SourceField')}</p>
                </div>
                {this.state.showDateExpr && (
                  <div className="col-2 pl-0" style={{ marginLeft: -13 }}>
                    <button type="button" ref={(c) => (this._btnDateExpr = c)} title={$lang('DateFormula')} className="btn btn-secondary mw-auto" onClick={(e) => this._showDateExpr(e)}>
                      <i className="zmdi zmdi-settings-square icon" />
                    </button>
                  </div>
                )}
              </div>
              <div className="mt-1">
                <button type="button" className="btn btn-primary btn-sm bordered" onClick={() => this.addItem()}>
                  + {$lang('Add')}
                </button>
              </div>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right"></label>
            <div className="col-md-12 col-lg-9">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" ref={(c) => (this._readonlyFields = c)} />
                <span className="custom-control-label">{$lang('AutoSetTargetFieldReadonly')}</span>
              </label>
            </div>
          </div>
        </form>
      </div>
    )
  }

  componentDidMount() {
    $('.J_when')
      .find('.custom-control-input')
      .each(function () {
        const v = ~~$(this).val()
        if (v === 2) $(this).attr('disabled', true)
      })

    const content = this.props.content
    this.__select2 = []
    $.get(`/admin/robot/trigger/field-aggregation-entities?source=${this.props.sourceEntity}&self=false`, (res) => {
      this.setState({ targetEntities: res.data }, () => {
        const s2te = $(this._targetEntity)
          .select2({ placeholder: $lang('SelectSome,TargetEntity') })
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
    }
  }

  changeTargetEntity() {
    const te = ($(this._targetEntity).val() || '').split('.')[1]
    if (!te) return
    // 清空现有规则
    this.setState({ items: [] })

    $.get(`/admin/robot/trigger/field-writeback-fields?source=${this.props.sourceEntity}&target=${te}`, (res) => {
      this.setState({ hadApproval: res.data.hadApproval })
      this.__targetFieldsCache = res.data.target

      if (this.state.targetFields) {
        this.setState({ targetFields: this.selectTargetFields() })
      } else {
        this.setState({ sourceFields: res.data.source, targetFields: [] }, () => {
          const s2sf = $(this._sourceField)
            .select2({ placeholder: $lang('SelectSome,SourceField') })
            .on('change', () => this.setState({ targetFields: this.selectTargetFields() }))
          const s2tf = $(this._targetField).select2({ placeholder: $lang('SelectSome,TargetField') })
          s2sf.trigger('change')

          this.__select2.push(s2sf)
          this.__select2.push(s2tf)
        })

        if (this.props.content) this.setState({ items: this.props.content.items || [] })
      }
    })
  }

  __fieldLabel(fields, field) {
    const fe = field.split(EXPR_SPLIT)
    field = fe[0]
    let found = fields.find((x) => {
      return x[0] === field
    })
    if (found) found = found[1]
    return (found || `[${field.toUpperCase()}]`) + (fe[1] ? ` {${fe[1]}}` : '')
  }

  // 获取可回填字段（兼容的）
  selectTargetFields() {
    const te = $(this._targetEntity).val()
    const sf = $(this._sourceField).val()
    const source = this.state.sourceFields.find((x) => {
      return x[0] === sf
    })

    // 日期高级表达式
    this.setState({ showDateExpr: source[2] === 'DATE' || source[2] === 'DATETIME' })

    const canFillinByType = CAN_FILLIN_MAPPINGS[source[2]] || []
    canFillinByType.push('TEXT')
    canFillinByType.push('NTEXT')

    const tFields = []
    $(this.__targetFieldsCache).each(function () {
      if (te === this[0] + '.' + this[3] || (source[2] === 'FILE' && this[2] !== 'FILE') || (source[2] === 'IMAGE' && this[2] !== 'IMAGE') || (source[2] === 'AVATAR' && this[2] !== 'AVATAR')) return

      if (source[2] === this[2] || canFillinByType.includes(this[2])) {
        if (source[2] === 'REFERENCE' || source[2] === 'STATE') {
          if (source[3] === this[3] || canFillinByType.includes(this[2])) tFields.push(this)
        } else {
          tFields.push(this)
        }
      }
    })
    return tFields
  }

  _showDateExpr() {
    const sf = $(this._sourceField).val()
    const found = this.state.sourceFields.find((x) => {
      return x[0] === sf
    })
    const $btn = $(this._btnDateExpr)
    renderRbcomp(
      <AdvDateValue
        field={[sf, found[1], found[2]]}
        call={(expr) => {
          if (expr === null) $btn.html('<i class="zmdi zmdi-settings-square icon"></i>').removeAttr('data-expr')
          else $btn.html(`{${expr}}`).attr('data-expr', expr)
        }}
      />
    )
  }

  addItem() {
    const tf = $(this._targetField).val()
    const sf = $(this._sourceField).val()
    if (!tf) {
      RbHighbar.create($lang('PlsSelectSome,TargetField'))
      return false
    }
    if (!sf) {
      RbHighbar.create($lang('PlsSelectSome,SourceField'))
      return false
    }

    // 目标字段=源字段
    if (sf === $(this._targetEntity).val().split('.')[0] + '.' + tf) {
      RbHighbar.create($lang('TargetAndSourceNotSame'))
      return false
    }

    const items = this.state.items || []
    const found = items.find((x) => {
      return x.targetField === tf
    })
    if (found) {
      RbHighbar.create($lang('SomeDuplicate,TargetField'))
      return false
    }

    const dateExpr = this.state.showDateExpr ? $(this._btnDateExpr).attr('data-expr') : null

    items.push({ targetField: tf, sourceField: sf + (dateExpr ? `${EXPR_SPLIT}${dateExpr}` : '') })
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
    }
    if (!content.targetEntity) {
      RbHighbar.create($lang('PlsSelectSome,TargetEntity'))
      return false
    }
    if (content.items.length === 0) {
      RbHighbar.create($lang('PlsAdd1WritebackRuleLeast'))
      return false
    }
    return content
  }
}

const CAN_FILLIN_MAPPINGS = {
  NUMBER: ['DECIMAL'],
  DECIMAL: ['NUMBER'],
  DATE: ['DATETIME'],
  DATETIME: ['DATE'],
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  renderRbcomp(<ContentFieldWriteback {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
  })
}

// see: field-edit.js#AdvDateDefaultValue
// ~~ 日期高级表达式
class AdvDateValue extends RbAlert {
  constructor(props) {
    super(props)
    this._refs = []
  }

  renderContent() {
    return (
      <form className="ml-6 mr-6">
        <div className="form-group">
          <label className="text-bold">{$lang('SetSome,DateFormula')}</label>
          <div className="input-group">
            <select className="form-control form-control-sm" ref={(c) => (this._refs[0] = c)}>
              <option value={this.props.field[0]}>{this.props.field[1]}</option>
            </select>
            <select className="form-control form-control-sm ml-1" ref={(c) => (this._refs[1] = c)}>
              <option value="+">{$lang('CalcPlus')}</option>
              <option value="-">{$lang('CalcMinus')}</option>
            </select>
            <input type="number" min="1" max="999999" className="form-control form-control-sm ml-1" defaultValue="1" ref={(c) => (this._refs[2] = c)} />
            <select className="form-control form-control-sm ml-1" ref={(c) => (this._refs[3] = c)}>
              <option value="D">{$lang('Day')}</option>
              <option value="M">{$lang('Month')}</option>
              <option value="Y">{$lang('Year')}</option>
              {this.props.field[2] === 'DATETIME' && (
                <React.Fragment>
                  <option value="H">{$lang('Hour')}</option>
                  <option value="I">{$lang('Minte')}</option>
                </React.Fragment>
              )}
            </select>
          </div>
        </div>
        <div className="form-group mb-1">
          <button type="button" className="btn btn-space btn-primary" onClick={this.confirm}>
            {$lang('Confirm')}
          </button>
          <button type="button" className="btn btn-space btn-secondary" onClick={this.clean}>
            {$lang('Clear')}
          </button>
        </div>
      </form>
    )
  }

  confirm = () => {
    const num = $(this._refs[2]).val() || 1
    if (isNaN(num)) {
      RbHighbar.create($lang('PlsInputSome,Number'))
      return
    }

    const expr = `${$(this._refs[1]).val()}${num}${$(this._refs[3]).val()}`
    typeof this.props.call === 'function' && this.props.call(expr)
    this.hide()
  }

  clean = () => {
    typeof this.props.call === 'function' && this.props.call(null)
    this.hide()
  }
}
