/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global FIELD_TYPES */

// ~~ 新建实体
// eslint-disable-next-line no-unused-vars
class FieldNew2 extends RbModalHandler {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    const ftKeys = Object.keys(FIELD_TYPES)
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$L('添加字段')} disposeOnHide>
        <div>
          <form>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('字段名称')}</label>
              <div className="col-sm-7">
                <input className="form-control form-control-sm" type="text" maxLength="40" ref={(c) => (this._$fieldLabel = c)} />
              </div>
            </div>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('字段类型')}</label>
              <div className="col-sm-7">
                <select className="form-control form-control-sm" ref={(c) => (this._$type = c)} defaultValue={this.props.fieldType}>
                  {ftKeys.map((key) => {
                    if (FIELD_TYPES[key][2]) return null
                    return (
                      <option key={key} value={key}>
                        {FIELD_TYPES[key][0]}
                      </option>
                    )
                  })}
                </select>
              </div>
            </div>
            <div className={`form-group row ${this.state.fieldType === 'REFERENCE' || this.state.fieldType === 'N2NREFERENCE' ? '' : 'hide'}`}>
              <label className="col-sm-3 col-form-label text-sm-right">{$L('选择引用实体')}</label>
              <div className="col-sm-7">
                <select className="form-control form-control-sm" ref={(c) => (this._$refEntity = c)}>
                  {this.state.refEntities &&
                    this.state.refEntities.map((item) => {
                      if (item.entityName === 'Team') return null
                      return (
                        <option key={item.entityName} value={item.entityName}>
                          {item.entityLabel}
                        </option>
                      )
                    })}
                </select>
              </div>
            </div>
            <div className={`form-group row ${this.state.fieldType === 'CLASSIFICATION' ? '' : 'hide'}`}>
              <label className="col-sm-3 col-form-label text-sm-right">{$L('选择分类数据')}</label>
              <div className="col-sm-7">
                <select className="form-control form-control-sm" ref={(c) => (this._$refClass = c)}>
                  {this.state.refClasses &&
                    this.state.refClasses.map((item) => {
                      return (
                        <option key={item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                </select>
              </div>
            </div>
            <div className={`form-group row ${this.state.fieldType === 'STATE' ? '' : 'hide'}`}>
              <label className="col-sm-3 col-form-label text-sm-right">{$L('状态类 (StateSpec)')}</label>
              <div className="col-sm-7">
                <input className="form-control form-control-sm" type="text" placeholder="com.rebuild.core.support.state.HowtoState" ref={(c) => (this._$stateClass = c)} />
              </div>
            </div>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('备注')}</label>
              <div className="col-sm-7">
                <textarea className="form-control form-control-sm row2x" maxLength="100" placeholder={$L('(选填)')} ref={(c) => (this._$comments = c)} />
              </div>
            </div>
            <div className="form-group row pb-1">
              <label className="col-sm-3 col-form-label text-sm-right"></label>
              <div className="col-sm-7">
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._$saveAndNew = c)} />
                  <span className="custom-control-label">{$L('继续添加下一个')}</span>
                </label>
              </div>
            </div>
            <div className="form-group row footer">
              <div className="col-sm-7 offset-sm-3" ref={(c) => (this._$btns = c)}>
                <button className="btn btn-primary" type="button" onClick={() => this.postNew()}>
                  {$L('确定')}
                </button>
                <button className="btn btn-link" type="button" onClick={() => this.hide()}>
                  {$L('取消')}
                </button>
              </div>
            </div>
          </form>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    $(this._$type)
      .select2({ allowClear: false })
      .on('change', (e) => {
        this.setState({ fieldType: e.target.value })
      })

    $.get('/admin/entity/entity-list?detail=true&bizz=true', (res) => {
      this.setState({ refEntities: res.data || [] })

      $.get('/admin/metadata/classification/list', (res2) => {
        this.setState({ refClasses: res2.data || [] }, () => {
          $([this._$refEntity, this._$refClass]).select2({ allowClear: false })
        })
      })
    })

    if (this.props.fieldType) {
      $(this._$saveAndNew).attr('checked', true)
      $(this._$saveAndNew).parents('.form-group').hide()
      $(this._$type).parents('.form-group').hide()
    }

    setTimeout(() => $(this._$fieldLabel).focus(), 200)
  }

  postNew() {
    const fieldLabel = $val(this._$fieldLabel),
      type = $val(this._$type),
      comments = $val(this._$comments),
      refEntity = $val(this._$refEntity),
      refClassification = $val(this._$refClass),
      stateClass = $val(this._$stateClass) || 'com.rebuild.core.support.state.HowtoState'
    if (!fieldLabel) return RbHighbar.create($L('请输入字段名称'))

    if ((type === 'REFERENCE' || type === 'N2NREFERENCE') && !refEntity) {
      return RbHighbar.create($L('请选择引用实体'))
    } else if (type === 'CLASSIFICATION' && !refClassification) {
      return RbHighbar.create($L('请选择分类数据'))
    } else if (type === 'STATE' && !stateClass) {
      return RbHighbar.create($L('请输入状态类 (StateSpec)'))
    }

    const data = {
      entity: this.props.entity,
      label: fieldLabel,
      type: type,
      comments: comments,
      refEntity: refEntity,
      refClassification: refClassification,
      stateClass: stateClass,
    }

    const $btn = $(this._$btns).find('.btn').button('loading')
    $.post('/admin/entity/field-new', JSON.stringify(data), (res) => {
      $btn.button('reset')

      if (res.error_code === 0) {
        if ($val(this._$saveAndNew)) {
          RbHighbar.success($L('字段已添加'))
          $([this._$fieldLabel, this._$comments]).val('')
          $(this._$fieldLabel).focus()

          // @see fields.html
          typeof window.loadFields === 'function' && window.loadFields()
          // @see `form-design.js`
          typeof window.add2Layout === 'function' && window.add2Layout(res.data, this)
        } else {
          location.href = `${rb.baseUrl}/admin/entity/${data.entity}/field/${res.data}`
        }
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}
