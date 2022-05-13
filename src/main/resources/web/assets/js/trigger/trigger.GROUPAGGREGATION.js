/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global FormulaAggregation, ActionContentSpec */

// ~~ 分组聚合
class ContentGroupAggregation extends ActionContentSpec {
  render() {
    return (
      <div className="field-aggregation group-aggregation">
        <form className="simple">
          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('目标实体')}</label>
            <div className="col-md-12 col-lg-9">
              <div className="row">
                <div className="col-5">
                  <select className="form-control form-control-sm" ref={(c) => (this._$targetEntity = c)}>
                    {(this.state.targetEntities || []).map((item) => {
                      return (
                        <option key={item[0]} value={item[0]}>
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
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('分组字段关联')}</label>
            <div className="col-md-12 col-lg-9">
              {this.state.groupFields && this.state.groupFields.length > 0 && (
                <div style={{ marginBottom: 12 }}>
                  {this.state.groupFields.map((item) => {
                    return (
                      <span className="mt-1 d-inline-block" key={item.targetField}>
                        <span className="badge badge-primary badge-close m-0 mr-1">
                          <span>{_getFieldLabel(item.targetField, this.state.targetGroupFields)}</span>
                          <i className="zmdi zmdi-swap ml-2 mr-2" />
                          <span>{_getFieldLabel(item.sourceField, this.__sourceGroupFieldsCache)}</span>
                          <a className="close" title={$L('移除')} onClick={(e) => this.delGroupField(item.targetField, e)}>
                            <i className="zmdi zmdi-close" />
                          </a>
                        </span>
                      </span>
                    )
                  })}
                </div>
              )}
              <div className="row">
                <div className="col-5">
                  <select className="form-control form-control-sm" ref={(c) => (this._$targetGroupField = c)}>
                    {(this.state.targetGroupFields || []).map((item) => {
                      if (['createdBy', 'createdOn', 'modifiedBy', 'modifiedOn', 'owningUser', 'owningDept'].includes(item[0]) || item[2] === 'DATETIME') return null

                      return (
                        <option key={item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$L('目标字段')}</p>
                </div>
                <div className="col-5">
                  <i className="zmdi zmdi-swap" />
                  <select className="form-control form-control-sm" ref={(c) => (this._$sourceGroupField = c)}>
                    {(this.state.sourceGroupFields || []).map((item) => {
                      return (
                        <option key={item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$L('源字段')}</p>
                </div>
              </div>
              <div className="mt-1">
                <button type="button" className="btn btn-primary btn-sm btn-outline" onClick={() => this.addGroupField()}>
                  + {$L('添加')}
                </button>
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
                            <span className="badge badge-warning">{FormulaAggregation.CALC_MODES[item.calcMode]}</span>
                          </div>
                          <div className="col-5 del-wrap">
                            <span className="badge badge-warning">
                              {item.calcMode === 'FORMULA' ? FormulaAggregation.textFormula(item.sourceFormula, this.__sourceFieldsCache) : _getFieldLabel(item.sourceField, this.__sourceFieldsCache)}
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
                    {Object.keys(FormulaAggregation.CALC_MODES).map((item) => {
                      return (
                        <option key={item} value={item}>
                          {FormulaAggregation.CALC_MODES[item]}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$L('聚合方式')}</p>
                </div>
                <div className="col-5">
                  <div className={this.state.calcMode === 'FORMULA' ? '' : 'hide'}>
                    <div className="form-control-plaintext formula" _title={$L('计算公式')} ref={(c) => (this._$sourceFormula = c)} onClick={() => this.showFormula()} />
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
                    <p>{$L('源字段')}</p>
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
      this.__sourceGroupFieldsCache = res.data.sourceGroupFields

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
    const te = $(this._$targetEntity).val()
    if (!te) return
    // 清空现有规则
    this.setState({ items: [], groupFields: [] })

    $.get(`/admin/robot/trigger/group-aggregation-fields?source=${this.props.sourceEntity}&target=${te}`, (res) => {
      if (this.state.targetFields) {
        this.setState({ ...res.data }, () => {
          $(this._$targetGroupField).trigger('change')
          $(this._$calcMode).trigger('change')
        })
      } else {
        // init
        this.setState({ ...res.data }, () => {
          // 字段关联

          const $s2tgf = $(this._$targetGroupField)
            .select2({ placeholder: $L('选择目标字段') })
            .on('change', () => {
              let stf = $s2tgf.val()
              stf = this.state.targetGroupFields.find((x) => x[0] === stf)

              // 仅同类型的字段
              // DATE DATETIME 兼容
              const fs = this.__sourceGroupFieldsCache.filter((x) => {
                if (stf[2] === 'DATE' && x[2] === 'DATETIME') return true
                if (stf[2] === 'DATETIME' && x[2] === 'DATE') return true
                return x[2] === stf[2]
              })
              this.setState({ sourceGroupFields: fs })
            })
          const $s2sgf = $(this._$sourceGroupField).select2({ placeholder: $L('选择源字段') })

          $s2tgf.trigger('change')

          this.__select2.push($s2tgf)
          this.__select2.push($s2sgf)

          // 聚合规则

          const $s2sf = $(this._$sourceField).select2({ placeholder: $L('选择源字段') })
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

          if (this.props.content) {
            this.setState({
              groupFields: this.props.content.groupFields || [],
              items: this.props.content.items || [],
            })
          }
        })
      } // End `if`
    })
  }

  showFormula() {
    const fs = this.__sourceFieldsCache.filter((x) => x[2] === 'NUMBER' || x[2] === 'DECIMAL')
    renderRbcomp(
      <FormulaAggregation
        fields={fs}
        onConfirm={(v) => {
          $(this._$sourceFormula).attr('data-v', v).text(FormulaAggregation.textFormula(v, this.__sourceFieldsCache))
        }}
        verifyFormula
        entity={this.props.sourceEntity}
      />
    )
  }

  dataAdvFilter() {
    if (this._advFilter) {
      this._advFilter.show()
    } else {
      const that = this
      renderRbcomp(
        <AdvFilter title={$L('数据过滤条件')} inModal={true} canNoFilters={true} entity={this.props.sourceEntity} filter={that._advFilter__data} confirm={(f) => that.saveAdvFilter(f)} />,
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
      return RbHighbar.create($L('请选择源字段'))
    }

    const items = this.state.items || []
    let exists = items.find((x) => x.targetField === tf)
    if (exists) return RbHighbar.create($L('目标字段已添加'))

    items.push({ targetField: tf, calcMode: calc, sourceField: sf, sourceFormula: formula })
    this.setState({ items: items }, () => $(this._$sourceFormula).empty())
  }

  delItem(targetField) {
    const itemsNew = (this.state.items || []).filter((x) => x.targetField !== targetField)
    this.setState({ items: itemsNew })
  }

  addGroupField() {
    const groupFields = [...(this.state.groupFields || [])]
    const tgf = $(this._$targetGroupField).val()
    const sgf = $(this._$sourceGroupField).val()
    if (!tgf) return RbHighbar.create($L('请选择目标字段'))
    if (!sgf) return RbHighbar.create($L('请选择源字段'))

    let exists = groupFields.find((x) => x.targetField === tgf)
    if (exists) return RbHighbar.create($L('目标字段已添加'))
    exists = groupFields.find((x) => x.sourceField === sgf)
    if (exists) return RbHighbar.create($L('源字段已添加'))

    groupFields.push({ targetField: tgf, sourceField: sgf })
    this.setState({ groupFields })
  }

  delGroupField(targetField) {
    const groupFields = this.state.groupFields.filter((x) => x.targetField !== targetField)
    this.setState({ groupFields })
  }

  buildContent() {
    const content = {
      targetEntity: $(this._$targetEntity).val(),
      items: this.state.items || [],
      dataFilter: this._advFilter__data,
      autoCreate: $(this._$autoCreate).prop('checked'),
      groupFields: this.state.groupFields || [],
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
  const x = fields.find((x) => x[0] === field)
  return x ? x[1] : `[${field.toUpperCase()}]`
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  renderRbcomp(<ContentGroupAggregation {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
    $('#react-content [data-toggle="tooltip"]').tooltip()
  })
}
