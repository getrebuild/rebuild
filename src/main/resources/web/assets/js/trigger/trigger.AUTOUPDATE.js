/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const UPDATE_MODES = {
  FIELD: $L('UpdateByField'),
  VALUE: $L('UpdateByValue'),
  NULLV: $L('BatchUpdateOpNULL'),
  FORMULA: $L('CalcFORMULA'),
}

// ~~ 自动更新（字段）
// eslint-disable-next-line no-undef
class ContentAutoUpdate extends ActionContentSpec {
  constructor(props) {
    super(props)
    this.state.updateMode = 'FIELD'
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
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('UpdateRule')}</label>
            <div className="col-md-12 col-lg-9">
              <div className="items">
                {(this.state.items || []).length > 0 &&
                  this.state.items.map((item) => {
                    return (
                      <div key={item.targetField}>
                        <div className="row">
                          <div className="col-5">
                            <span className="badge badge-warning">{this._getFieldLabel(this.state.targetFields, item.targetField)}</span>
                          </div>
                          <div className="col-2">
                            <span className="zmdi zmdi-forward zmdi-hc-rotate-180"></span>
                            <span className="badge badge-warning">{UPDATE_MODES[item.updateMode]}</span>
                          </div>
                          <div className="col-5 del-wrap">
                            {item.updateMode === 'FIELD' && <span className="badge badge-warning">{this._getFieldLabel(this.state.sourceFields, item.source)}</span>}
                            {item.updateMode === 'VALUE' && <span className="badge badge-light">{item.source}</span>}
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
                        <option key={item.name} value={item.name}>
                          {item.label}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$L('TargetField')}</p>
                </div>
                <div className="col-2 pr-0">
                  <span className="zmdi zmdi-forward zmdi-hc-rotate-180"></span>
                  <select className="form-control form-control-sm" ref={(c) => (this._updateMode = c)}>
                    {Object.keys(UPDATE_MODES).map((item) => {
                      return (
                        <option key={item} value={item}>
                          {UPDATE_MODES[item]}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$L('UpdateMode')}</p>
                </div>
                <div className="col-5">
                  <div className={this.state.updateMode === 'FIELD' ? '' : 'hide'}>
                    <select className="form-control form-control-sm" ref={(c) => (this._sourceField = c)}>
                      {(this.state.sourceFields || []).map((item) => {
                        return (
                          <option key={item.name} value={item.name}>
                            {item.label}
                          </option>
                        )
                      })}
                    </select>
                    <p>{$L('SourceField')}</p>
                  </div>
                  <div className={this.state.updateMode === 'VALUE' ? '' : 'hide'}>
                    <input type="text" className="form-control form-control-sm" ref={(c) => (this._sourceValue = c)} />
                  </div>
                  <div className={this.state.updateMode === 'FORMULA' ? '' : 'hide'}>
                    <div className="form-control-plaintext formula" _title={$L('CalcFORMULA')} ref={(c) => (this._sourceFormula = c)} onClick={this.showFormula}></div>
                    <p>{$L('CalcFORMULA')}</p>
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
    }
  }

  changeTargetEntity() {
    const te = ($(this._targetEntity).val() || '').split('.')[1]
    if (!te) return
    // 清空现有规则
    this.setState({ items: [] })

    $.get(`/admin/robot/trigger/field-writeback-fields?source=${this.props.sourceEntity}&target=${te}`, (res) => {
      this.setState({ hadApproval: res.data.hadApproval })

      if (this.state.targetFields) {
        this.setState({ targetFields: res.data.target })
      } else {
        this.setState({ sourceFields: res.data.source, targetFields: res.data.target }, () => {
          const s2sf = $(this._sourceField)
            .select2({ placeholder: $L('SelectSome,SourceField') })
            .on('change', (e) => {
              console.log(e.target.value)
            })
          const s2um = $(this._updateMode)
            .select2({ placeholder: $L('SelectSome,UpdateMode') })
            .on('change', (e) => this.setState({ updateMode: e.target.value }))
          const s2tf = $(this._targetField).select2({ placeholder: $L('SelectSome,TargetField') })

          this.__select2.push(s2sf)
          this.__select2.push(s2um)
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

  addItem() {
    const tf = $(this._targetField).val()
    const mode = $(this._updateMode).val()
    if (!tf) {
      RbHighbar.create($L('PlsSelectSome,TargetField'))
      return false
    }

    let sourceAny = null
    if (mode === 'FIELD') {
      sourceAny = $(this._sourceField).val()
      // 目标字段=源字段
      if (sourceAny === $(this._targetEntity).val().split('.')[0] + '.' + tf) {
        RbHighbar.create($L('TargetAndSourceNotSame'))
        return false
      }
    } else if (mode === 'VALUE') {
      sourceAny = $(this._sourceValue).val()
      if (!sourceAny) {
        RbHighbar.create('填写值')
        return false
      }
    }

    const items = this.state.items || []
    const exists = items.find((x) => x.targetField === tf)
    if (exists) {
      RbHighbar.create($L('SomeDuplicate,TargetField'))
      return false
    }

    items.push({ targetField: tf, updateMode: mode, source: sourceAny })
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

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  renderRbcomp(<ContentAutoUpdate {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
  })
}
