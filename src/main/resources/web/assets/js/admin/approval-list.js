/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global dlgActionAfter */

$(document).ready(function () {
  $('.J_add').click(() => renderRbcomp(<ApprovalEdit />))
  renderRbcomp(<ApprovalList />, 'dataList')
})

class ApprovalList extends ConfigList {
  constructor(props) {
    super(props)
    this.requestUrl = '/admin/robot/approval/list'
  }

  render() {
    return (
      <React.Fragment>
        {(this.state.data || []).map((item) => {
          return (
            <tr key={'k-' + item[0]}>
              <td>
                <a href={`approval/${item[0]}`}>{item[3]}</a>
              </td>
              <td>{item[2] || item[1]}</td>
              <td>{item[4] ? <span className="badge badge-warning font-weight-light">{$L('否')}</span> : <span className="badge badge-success font-weight-light">{$L('是')}</span>}</td>
              <td>
                <DateShow date={item[5]} />
              </td>
              <td className="actions">
                <a className="icon" title={$L('修改')} onClick={() => this.handleEdit(item)}>
                  <i className="zmdi zmdi-edit" />
                </a>
                <a className="icon danger-hover" title={$L('删除')} onClick={() => this.handleDelete(item[0])}>
                  <i className="zmdi zmdi-delete" />
                </a>
              </td>
            </tr>
          )
        })}
      </React.Fragment>
    )
  }

  handleEdit(item) {
    renderRbcomp(<ApprovalEdit id={item[0]} name={item[3]} isDisabled={item[4]} />)
  }

  handleDelete(id) {
    const handle = super.handleDelete
    RbAlert.create(WrapHtml($L('若流程正在使用则不能删除，建议你将其禁用。[] 确认删除此审批流程吗？')), {
      type: 'danger',
      confirmText: $L('删除'),
      confirm: function () {
        this.disabled(true)
        handle(id, () => dlgActionAfter(this))
      },
    })
  }
}

class ApprovalEdit extends ConfigFormDlg {
  constructor(props) {
    super(props)
    this.subtitle = $L('审批流程')
  }

  renderFrom() {
    return (
      <React.Fragment>
        {!this.props.id && (
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('选择应用实体')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref={(c) => (this._entity = c)}>
                {(this.state.entities || []).map((item) => {
                  return (
                    <option key={'e-' + item.name} value={item.name}>
                      {item.label}
                    </option>
                  )
                })}
              </select>
            </div>
          </div>
        )}
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('名称')}</label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} />
          </div>
        </div>
        {this.props.id && (
          <div className="form-group row">
            <div className="col-sm-7 offset-sm-3">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
                <span className="custom-control-label">
                  {$L('是否禁用')}
                  <i ref={(c) => (this._tooltip = c)} className="zmdi zmdi-help zicon" title={$L('禁用后正在使用此流程的审批记录不受影响')}></i>
                </span>
              </label>
            </div>
          </div>
        )}
      </React.Fragment>
    )
  }

  componentDidMount() {
    super.componentDidMount()
    if (this.props.id) $(this._tooltip).tooltip()
  }

  confirm = () => {
    const post = { name: this.state['name'] }
    if (!post.name) return RbHighbar.create($L('请输入名称'))

    if (!this.props.id) {
      post.belongEntity = this.__select2.val()
      if (!post.belongEntity) {
        RbHighbar.create($L('请选择应用实体'))
        return
      }
    } else {
      post.isDisabled = this.state.isDisabled === true
    }
    post.metadata = {
      entity: 'RobotApprovalConfig',
      id: this.props.id || null,
    }

    this.disabled(true)
    $.post('/app/entity/common-save', JSON.stringify(post), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) dlgActionAfter(this)
        else location.href = 'approval/' + res.data.id
      } else {
        RbHighbar.error(res.error_msg)
      }
      this.disabled()
    })
  }
}
