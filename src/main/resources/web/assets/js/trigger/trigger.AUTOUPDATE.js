/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global FieldValueSet */

const UPDATE_MODES = {
  FIELD: $L('UpdateByField'),
  VFIXED: $L('UpdateByValue'),
  VNULL: $L('BatchUpdateOpNULL'),
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
                    const field = item.updateMode === 'VFIXED' ? this.state.targetFields.find((x) => x.name === item.targetField) : null
                    return (
                      <div key={item.targetField}>
                        <div className="row">
                          <div className="col-5">
                            <span className="badge badge-warning">{_getFieldLabel(this.state.targetFields, item.targetField)}</span>
                          </div>
                          <div className="col-2">
                            <span className="zmdi zmdi-forward zmdi-hc-rotate-180"></span>
                            <span className="badge badge-warning">{UPDATE_MODES[item.updateMode]}</span>
                          </div>
                          <div className="col-5 del-wrap">
                            {item.updateMode === 'FIELD' && <span className="badge badge-warning">{_getFieldLabel(this.__sourceFieldsCache, item.sourceAny)}</span>}
                            {item.updateMode === 'VFIXED' && <span className="badge badge-light text-break">{FieldValueSet.formatFieldText(item.sourceAny, field)}</span>}
                            {item.updateMode === 'FORMULA' && <span className="badge badge-warning">{item.sourceAny}</span>}
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
                  <div className={this.state.updateMode === 'VFIXED' ? '' : 'hide'}>
                    {this.state.updateMode === 'VFIXED' && this.state.targetField && (
                      <FieldValueSet entity={this.state.targetEntity} field={this.state.targetField} placeholder="固定值" ref={(c) => (this._sourceValue = c)} />
                    )}
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
    }
  }

  _changeTargetEntity() {
    const te = ($(this._targetEntity).val() || '').split('.')[1]
    if (!te) return
    // 清空现有规则
    this.setState({ targetEntity: te, items: [] })

    $.get(`/admin/robot/trigger/field-writeback-fields?source=${this.props.sourceEntity}&target=${te}`, (res) => {
      this.setState({ hadApproval: res.data.hadApproval })
      this.__sourceFieldsCache = res.data.source

      if (this.state.targetFields) {
        this.setState({ targetFields: res.data.target }, () => {
          $(this._targetField).trigger('change')
        })
      } else {
        this.setState({ sourceFields: res.data.source, targetFields: res.data.target }, () => {
          const $s2tf = $(this._targetField)
            .select2({ placeholder: $L('SelectSome,TargetField') })
            .on('change', () => this._changeTargetField())
          const $s2um = $(this._updateMode)
            .select2({ placeholder: $L('SelectSome,UpdateMode') })
            .on('change', (e) => {
              this.setState({ updateMode: e.target.value })
            })
          const $s2sf = $(this._sourceField).select2({ placeholder: $L('SelectSome,SourceField') })

          $s2tf.trigger('change')
          this.__select2.push($s2tf)
          this.__select2.push($s2um)
          this.__select2.push($s2sf)
        })

        if (this.props.content) {
          this.setState({ items: this.props.content.items || [] })
        }
      }
    })
  }

  _changeTargetField() {
    const tf = $(this._targetField).val()
    if (!tf) return
    const targetField = this.state.targetFields.find((x) => x.name === tf)

    // 获取可回填字段（兼容的）
    const sourceFields = []
    $(this.__sourceFieldsCache).each(function () {
      if ($fieldIsCompatible(this, targetField)) {
        sourceFields.push(this)
      }
    })

    this.setState({ targetField: null, sourceFields: sourceFields }, () => {
      if (sourceFields.length > 0) $(this._sourceField).val(sourceFields[0].name)
    })
    // 强制销毁后再渲染
    setTimeout(() => this.setState({ targetField: targetField }), 200)
  }

  addItem() {
    const tf = $(this._targetField).val()
    const mode = $(this._updateMode).val()
    if (!tf) return RbHighbar.create($L('PlsSelectSome,TargetField'))

    let sourceAny = null
    if (mode === 'FIELD') {
      sourceAny = $(this._sourceField).val()

      // 目标字段=源字段
      const tfFull = `${$(this._targetEntity).val().split('.')[0]}.${tf}`.replace('$PRIMARY$.', '')
      if (tfFull === sourceAny) return RbHighbar.create($L('TargetAndSourceNotSame'))
    } else if (mode === 'FORMULA') {
      sourceAny = $(this._sourceFormula).attr('data-value')
      if (!sourceAny) return RbHighbar.create($L('PlsInputSome,CalcFORMULA'))
    } else if (mode === 'VFIXED') {
      sourceAny = this._sourceValue.val()
      if (!sourceAny) return
    } else if (mode === 'VNULL') {
      const tf2 = this.state.targetFields.find((x) => x.name === tf)
      if (!tf2.nullable) return RbHighbar.create($L('SomeNotEmpty').replace('{0}', tf2.label))
    }

    const items = this.state.items || []
    const exists = items.find((x) => x.targetField === tf)
    if (exists) return RbHighbar.create($L('SomeDuplicate,TargetField'))

    items.push({ targetField: tf, updateMode: mode, sourceAny: sourceAny })
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

const _getFieldLabel = function (fields, fieldName) {
  let found = fields.find((x) => x.name === fieldName)
  if (found) found = found.label
  return found || '[' + fieldName.toUpperCase() + ']'
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  // 禁用`删除`
  $('.J_when .custom-control-input').each(function () {
    if (~~$(this).val() === 2) $(this).attr('disabled', true)
  })

  renderRbcomp(<ContentAutoUpdate {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
  })
}
