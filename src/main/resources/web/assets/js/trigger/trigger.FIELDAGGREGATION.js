/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global FormulaAggregation, ActionContentSpec */

const CALC_MODES2 = {
  ...FormulaAggregation.CALC_MODES,
  RBJOIN: $L('连接'),
  RBJOIN2: $L('去重连接'),
  RBJOIN3: $L('去重连接*N'),
}

const __LAB_MATCHFIELDS = false

// ~~ 字段聚合
class ContentFieldAggregation extends ActionContentSpec {
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
                      <optgroup label={$L('通过规则匹配')}>
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

          {__LAB_MATCHFIELDS && (
            <div className={`form-group row ${this.state.showMatchFields ? '' : 'hide'}`}>
              <label className="col-md-12 col-lg-3 col-form-label text-lg-right"></label>
              <div className="col-md-12 col-lg-9">
                <div>
                  <h5 className="mt-0 text-bold">{$L('目标实体/记录匹配规则')} (LAB)</h5>
                  <textarea className="formula-code" style={{ height: 72 }} ref={(c) => (this._$matchFields = c)} placeholder="## [{ sourceField:XXX, targetField:XXX }]" />
                </div>
              </div>
            </div>
          )}

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
                            <span className="badge badge-warning">{FormulaAggregation.getLabel(item.targetField, this.__targetFieldsCache)}</span>
                          </div>
                          <div className="col-2">
                            <i className="zmdi zmdi-forward zmdi-hc-rotate-180" />
                            <span className="badge badge-warning">{CALC_MODES2[item.calcMode]}</span>
                          </div>
                          <div className="col-5 del-wrap">
                            <span className="badge badge-warning">
                              {item.calcMode === 'FORMULA'
                                ? FormulaAggregation.textFormula(item.sourceFormula, this.__sourceFieldsCache)
                                : FormulaAggregation.getLabel(item.sourceField, this.__sourceFieldsCache)}
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
                  <select className="form-control form-control-sm" ref={(c) => (this._$calcMode = c)}>
                    {(this.state.calcModes || []).map((item) => {
                      return (
                        <option key={item} value={item}>
                          {CALC_MODES2[item]}
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
                          <option key={item.name} value={item.name}>
                            {item.label}
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
              <div className="form-text mt-0">{$L('符合聚合数据条件的记录才会被聚合')}</div>
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
              <div className="mt-2 bosskey-show">
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._$stopPropagation = c)} />
                  <span className="custom-control-label">{$L('禁用传播')} (LAB)</span>
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

    $.get(`/admin/robot/trigger/field-aggregation-entities?source=${this.props.sourceEntity}&matchfields=${__LAB_MATCHFIELDS}`, (res) => {
      this.setState({ targetEntities: res.data || [] }, () => {
        const $s2te = $(this._$targetEntity)
          .select2({ placeholder: $L('选择目标实体') })
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
      if (content.stopPropagation === true) $(this._$stopPropagation).attr('checked', true).parents('.bosskey-show').removeClass('bosskey-show')
      this.saveAdvFilter(content.dataFilter)
      $(this._$matchFields).val(content.targetEntityMatchFields || null)

      // eslint-disable-next-line no-undef
      DlgSpecFields.render(content)
    }
  }

  _changeTargetEntity() {
    const teSplit = ($(this._$targetEntity).val() || '').split('.')
    if (!teSplit || !teSplit[1]) return
    // 清空现有规则
    this.setState({ items: [] })

    if (__LAB_MATCHFIELDS) this.setState({ showMatchFields: teSplit[0] === '$' })

    $.get(`/admin/robot/trigger/field-aggregation-fields?source=${this.props.sourceEntity}&target=${teSplit[1]}`, (res) => {
      this.setState({ hasWarning: res.data.hadApproval ? $L('目标实体已启用审批流程，可能影响源实体操作 (触发动作)，建议启用“允许强制更新”') : null })

      this.__sourceFieldsCache = res.data.source
      this.__targetFieldsCache = res.data.target

      if (this.state.sourceFields) {
        this.setState({ sourceFields: res.data.source }, () => $(this._$sourceField).trigger('change'))
      } else {
        // init
        this.setState({ sourceFields: res.data.source }, () => {
          let $s2sf, $s2cm, $s2tf

          $s2sf = $(this._$sourceField)
            .select2({ placeholder: $L('选择聚合字段') })
            .on('change', (e) => {
              let sf = e.target.value
              sf = this.__sourceFieldsCache.find((x) => x.name === sf)
              if (!sf) return

              let cmAllow = Object.keys(FormulaAggregation.CALC_MODES)
              if (['DATE', 'DATETIME'].includes(sf.type)) {
                cmAllow = ['MAX', 'MIN', 'COUNT', 'COUNT2', 'RBJOIN', 'RBJOIN2', 'RBJOIN3', 'FORMULA']
              } else if (!['DATE', 'DATETIME', 'NUMBER', 'DECIMAL'].includes(sf.type)) {
                cmAllow = ['COUNT', 'COUNT2', 'RBJOIN', 'RBJOIN2', 'RBJOIN3', 'FORMULA']
              }

              this.setState({ calcModes: cmAllow }, () => $s2cm.trigger('change'))
            })

          $s2cm = $(this._$calcMode)
            .select2({ placeholder: $L('选择聚合方式') })
            .on('change', (e) => {
              const cm = e.target.value
              this.setState({ calcMode: cm })

              let sf = $s2sf.val()
              sf = this.__sourceFieldsCache.find((x) => x.name === sf)
              if (!sf) return

              let tfAllow = this.__targetFieldsCache.filter((x) => ['NUMBER', 'DECIMAL'].includes(x.type))
              if ('RBJOIN' === cm || 'RBJOIN2' === cm || 'RBJOIN3' === cm) {
                tfAllow = this.__targetFieldsCache.filter((x) => {
                  if ('NTEXT' === x.type) return true
                  if ('N2NREFERENCE' === x.type) return sf.ref && sf.ref[0] === x.ref[0]
                  if ('FILE' === x.type) return true
                  return false
                })
              } else if (['DATE', 'DATETIME'].includes(sf.type) && !['COUNT', 'COUNT2', 'FORMULA'].includes(cm)) {
                tfAllow = this.__targetFieldsCache.filter((x) => ['DATE', 'DATETIME'].includes(x.type))
              }

              this.setState({ targetFields: tfAllow })
            })

          $s2tf = $(this._$targetField)
            .select2({ placeholder: $L('选择目标字段') })
            .on('change', () => {})

          // 优先显示
          const useNum = this.__sourceFieldsCache.find((x) => ['NUMBER', 'DECIMAL'].includes(x.type))
          if (useNum) {
            $s2sf.val(useNum.name)
          } else {
            const useDate = this.__sourceFieldsCache.find((x) => ['DATE', 'DATETIME'].includes(x.type))
            if (useDate) $s2sf.val(useDate.name)
          }

          $s2sf.trigger('change')

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
    const sfAllow = this.__sourceFieldsCache.filter((x) => x.type === 'NUMBER' || x.type === 'DECIMAL')
    renderRbcomp(
      <FormulaAggregation
        fields={sfAllow}
        onConfirm={(v) => {
          $(this._$sourceFormula).attr('data-v', v).text(FormulaAggregation.textFormula(v, this.__sourceFieldsCache))
        }}
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
        <AdvFilter title={$L('聚合数据条件')} inModal canNoFilters entity={this.props.sourceEntity} filter={that._advFilter__data} confirm={(f) => that.saveAdvFilter(f)} />,
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
    if (exists) return RbHighbar.create($L('目标字段已添加'))

    items.push({ targetField: tf, calcMode: calc, sourceField: sf, sourceFormula: formula })
    this.setState({ items: items }, () => $(this._$sourceFormula).empty())
  }

  delItem(targetField) {
    const itemsNew = (this.state.items || []).filter((x) => x.targetField !== targetField)
    this.setState({ items: itemsNew })
  }

  buildContent() {
    const content = {
      targetEntity: $(this._$targetEntity).val(),
      targetEntityMatchFields: $(this._$matchFields).val() || null,
      items: this.state.items || [],
      readonlyFields: $(this._$readonlyFields).prop('checked'),
      forceUpdate: $(this._$forceUpdate).prop('checked'),
      stopPropagation: $(this._$stopPropagation).prop('checked'),
      dataFilter: this._advFilter__data,
    }

    if (!content.targetEntity) {
      RbHighbar.create($L('请选择目标实体'))
      return false
    }
    if (content.items.length === 0) {
      RbHighbar.create($L('请至少添加 1 个聚合规则'))
      return false
    }

    if (this.state.showMatchFields) {
      let badFormat = !content.targetEntityMatchFields
      if (!badFormat) {
        try {
          badFormat = JSON.parse(content.targetEntityMatchFields)
          badFormat = !$.isArray(badFormat)
        } catch (err) {
          badFormat = true
        }
      }
      if (badFormat) {
        RbHighbar.create($L('请正确填写目标实体/记录匹配规则'))
        return false
      }
    }

    return content
  }
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  renderRbcomp(<ContentFieldAggregation {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
    $('#react-content [data-toggle="tooltip"]').tooltip()
  })

  // 指定字段
  $('.when-update a.hide').removeClass('hide')

  // eslint-disable-next-line no-undef
  useExecManual()
}
