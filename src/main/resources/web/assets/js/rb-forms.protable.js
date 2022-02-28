/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global detectElement, TYPE_DIVIDER */
/* eslint-disable no-unused-vars */

// ~~ 高级表格

const COL_WIDTH = 170

class ProTable extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    const formFields = this.state.formFields || []
    const details = this.state.details || []

    // fixed 模式大概 5 个字段
    const ww = $(window).width()
    const fw = ww > 1064 ? 994 : ww - 70
    const fixed = COL_WIDTH * formFields.length + (38 + 88) > fw

    const colStyles = { minWidth: COL_WIDTH }

    return (
      <div className={`protable rb-scroller ${fixed && 'column-fixed-pin'}`} ref={(c) => (this._$scroller = c)}>
        <table className={`table table-sm ${!fixed && 'table-fixed'}`}>
          <thead>
            <tr>
              <th className="col-index" />
              {formFields.map((item) => {
                return (
                  <th key={item.field} data-field={item.field} style={colStyles}>
                    {item.label}
                    <i className="dividing hide" />
                  </th>
                )
              })}
              <td className={`col-action ${fixed && 'column-fixed'}`} />
            </tr>
          </thead>
          <tbody>
            {(this.state.inlineForms || []).map((form, idx) => {
              const key = form.key
              return (
                <tr key={`inline-${key}`}>
                  <th className="col-index">{details.length + idx + 1}</th>
                  {form}

                  <td className={`col-action ${fixed && 'column-fixed'}`}>
                    <button className="btn btn-light hide" title={$L('编辑')} onClick={() => this.editLine(key)}>
                      <i className="icon zmdi zmdi-edit fs-14" />
                    </button>
                    <button className="btn btn-light" title={$L('移除')} onClick={() => this.removeLine(key)}>
                      <i className="icon zmdi zmdi-close fs-16 text-bold" />
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    )
  }

  componentDidMount() {
    const entity = this.props.entity
    const initialValue = {
      '$MAINID$': this.props.mainid || '$MAINID$',
    }

    $.post(`/app/${entity.entity}/form-model?id=`, JSON.stringify(initialValue), (res) => {
      this._initModel = res.data // 新建用
      this.setState({ formFields: res.data.elements }, () => {
        $(this._$scroller).perfectScrollbar()
      })

      // 编辑
      if (this.props.mainid) {
        $.get(`/app/${entity.entity}/detail-models?mainid=${this.props.mainid}`, (res) => {
          res.data.forEach((item) => this.addLine(item))
        })
      }
    })
  }

  addNew() {
    this.addLine(this._initModel)
  }

  addLine(model) {
    const key = `form-${model.id ? model.id : $random()}`
    const ref = React.createRef()
    const FORM = (
      <InlineForm entity={this.props.entity.entity} id={model.id} rawModel={model} $$$parent={this} key={key} ref={ref}>
        {model.elements.map((item) => {
          return detectElement({ ...item, colspan: 4 })
        })}
      </InlineForm>
    )

    const forms = this.state.inlineForms || []
    forms.push(FORM)
    this.setState({ inlineForms: forms }, () => {
      const refs = this._inlineFormsRefs || []
      refs.push(ref)
      this._inlineFormsRefs = refs
    })
  }

  removeLine(key) {
    const forms = this.state.inlineForms.filter((c) => {
      if (c.key === key && c.props.id) {
        const d = this._deletes || []
        d.push(c.props.id)
        this._deletes = d
      }
      return c.key !== key
    })
    this.setState({ inlineForms: forms })
  }

  editLine(id) {}

  buildFormData() {
    const datas = []
    let error = null

    ;(this._inlineFormsRefs || []).forEach((item) => {
      const f = item.current
      if (!f) return

      const d = f.buildFormData()
      if (!d || typeof d === 'string') {
        if (!error) error = d
      } else if (Object.keys(d).length > 0) {
        datas.push(d)
      }
    })

    if (error) {
      RbHighbar.create(error)
      return null
    }

    // 删除
    if (this._deletes) {
      this._deletes.forEach((item) => {
        const d = {
          metadata: {
            entity: this.props.entity.entity,
            id: item,
            delete: true,
          },
        }
        datas.push(d)
      })
    }

    return datas
  }
}

class InlineForm extends RbForm {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <React.Fragment>
        {this.props.children.map((fieldComp) => {
          const refid = fieldComp.props.field === TYPE_DIVIDER ? null : `fieldcomp-${fieldComp.props.field}`
          return (
            <td key={`td-${refid}`} ref={(c) => (this._$ref = c)}>
              {React.cloneElement(fieldComp, { $$$parent: this, ref: refid })}
            </td>
          )
        })}
      </React.Fragment>
    )
  }

  buildFormData() {
    const $idx = $(this._$ref).parent().find('th.col-index').removeAttr('title')

    const data = {}
    let error = null
    for (let k in this.__FormData) {
      const err = this.__FormData[k].error
      if (err) {
        error = err
        $idx.attr('title', err)
        break
      } else {
        data[k] = this.__FormData[k].value
      }
    }

    if (error) return error

    // 未修改
    if (Object.keys(data).length > 0) {
      data.metadata = {
        entity: this.state.entity,
        id: this.state.id || null,
      }
    }
    return data
  }
}
