/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global detectElement, TYPE_DIVIDER */
/* eslint-disable no-unused-vars */

// ~~ 高级表格

class ProTable extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    const showFields = this.state.fields || []
    const details = this.state.details || []

    return (
      <div className="protable">
        <table className="table table-fixed table-sm">
          <thead>
            <tr>
              <th className="col-index" />
              {showFields.map((item) => {
                return (
                  <th key={item.field} data-field={item.field}>
                    {item.label}
                  </th>
                )
              })}
              <th className="col-action" />
            </tr>
          </thead>
          <tbody>
            {details.map((item, idx) => {
              const tds = []
              for (let i = 0; i < showFields.length - 1; i++) {
                tds.push(<td>{item[i]}</td>)
              }

              return (
                <tr key={`inline-${idx}`}>
                  <th>{idx + 1}</th>
                  {tds}

                  <td className="col-action">
                    <button className="btn btn-light" title={$L('编辑')} onClick={() => this.editLine()}>
                      <i className="icon zmdi zmdi-border-color" />
                    </button>
                    <button className="btn btn-light danger-hover" title={$L('删除')} onClick={() => this.deleteLine()}>
                      <i className="icon zmdi zmdi-delete" />
                    </button>
                  </td>
                </tr>
              )
            })}

            {(this.state.editForms || []).map((inlineForm, idx) => {
              const key = inlineForm.key
              return (
                <tr key={`inline-${key}`}>
                  <th>{details.length + idx + 1}</th>
                  {inlineForm}

                  <td className="col-action">
                    <button className="btn btn-light danger-hover" title={$L('删除')} onClick={() => this.removeLine(key)}>
                      <i className="icon zmdi zmdi-delete" />
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>

        <div className="protable-footer">
          <button className="btn btn-light" onClick={() => this.addNew()}>
            {$L('添加明细')}
          </button>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const entity = this.props.entity
    const initialValue = {
      '$MAINID$': this.props.mainid || '$MAINID$',
    }

    $.post(`/app/${entity.entity}/form-model`, JSON.stringify(initialValue), (res) => {
      this._rawModel = res.data
      this.setState({ fields: res.data.elements })
    })
  }

  addNew() {
    const key = `form-${$random()}`
    const FORM = (
      <ProTableForm entity={this.props.entity.entity} rawModel={this._rawModel} $$$parent={this} key={key}>
        {this._rawModel.elements.map((item) => {
          return detectElement({ ...item, colspan: 4 })
        })}
      </ProTableForm>
    )

    const forms = this.state.editForms || []
    forms.push(FORM)
    this.setState({ editForms: forms })
  }

  removeLine(key) {
    const forms = this.state.editForms.filter((c) => c.key !== key)
    this.setState({ editForms: forms })
  }

  editLine(id) {}

  deleteLine(id) {}
}

class ProTableForm extends RbForm {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <React.Fragment>
        {this.props.children.map((fieldComp) => {
          const refid = fieldComp.props.field === TYPE_DIVIDER ? null : `fieldcomp-${fieldComp.props.field}`
          return <td key={`td-${refid}`}>{React.cloneElement(fieldComp, { $$$parent: this, ref: refid })}</td>
        })}
      </React.Fragment>
    )
  }

  componentDidMount() {
    // TODO
  }
}
