
$(document).ready(function () {
  $('.J_add').click(() => { renderRbcomp(<ApprovalEdit />) })
  renderRbcomp(<ApprovalList />, 'dataList')
})

class ApprovalList extends ConfigList {
  constructor(props) {
    super(props)
    this.requestUrl = `${rb.baseUrl}/admin/robot/approval/list`
  }
  render() {
    return <React.Fragment>
      {(this.state.data || []).map((item) => {
        return <tr key={'k-' + item[0]}>
          <td><a href={`approval/${item[0]}`}>{item[3]}</a></td>
          <td>{item[2] || item[1]}</td>
          <td>{item[4] ? <span className="badge badge-warning font-weight-light">否</span> : <span className="badge badge-success font-weight-light">是</span>}</td>
          <td>{item[5]}</td>
          <td className="actions">
            <a className="icon" title="修改" onClick={() => this.handleEdit(item)}><i className="zmdi zmdi-edit" /></a>
            <a className="icon" title="删除" onClick={() => this.handleDelete(item[0])}><i className="zmdi zmdi-delete" /></a>
          </td>
        </tr>
      })}
    </React.Fragment>
  }

  handleEdit(item) {
    renderRbcomp(<ApprovalEdit id={item[0]} name={item[3]} isDisabled={item[4]} />)
  }
  handleDelete(id) {
    let handle = super.handleDelete
    RbAlert.create('若流程正在使用则不能删除，建议你将其禁用。<br>确认删除此审批流程吗？', {
      html: true,
      type: 'danger',
      confirmText: '删除',
      confirm: function () {
        this.disabled(true)
        handle(id)
      }
    })
  }
}

class ApprovalEdit extends ConfigFormDlg {
  constructor(props) {
    super(props)
    this.subtitle = '审批流程'
  }
  renderFrom() {
    return <React.Fragment>
      <div className="form-group row">
        <label className="col-sm-3 col-form-label text-sm-right">流程名称</label>
        <div className="col-sm-7">
          <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} />
        </div>
      </div>
      {!this.props.id && <div className="form-group row">
        <label className="col-sm-3 col-form-label text-sm-right">选择应用实体</label>
        <div className="col-sm-7">
          <select className="form-control form-control-sm" ref={(c) => this._entity = c}>
            {(this.state.entities || []).map((item) => {
              return <option key={'e-' + item.name} value={item.name}>{item.label}</option>
            })}
          </select>
        </div>
      </div>
      }
      {this.props.id && <div className="form-group row">
        <div className="col-sm-7 offset-sm-3">
          <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
            <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
            <span className="custom-control-label">是否禁用 <i ref={(c) => this._tooltip = c} className="zmdi zmdi-help zicon" title="禁用后正在使用此流程的审批记录不受影响"></i></span>
          </label>
        </div>
      </div>
      }
    </React.Fragment>
  }
  componentDidMount() {
    super.componentDidMount()
    if (this.props.id) $(this._tooltip).tooltip()
  }

  confirm = () => {
    let post = { name: this.state['name'] }
    if (!post.name) { RbHighbar.create('请输入流程名称'); return }
    if (!this.props.id) {
      post.belongEntity = this.__select2.val()
      if (!post.belongEntity) { RbHighbar.create('请选择应用实体'); return }
    } else {
      post.isDisabled = this.state.isDisabled === true
    }
    post.metadata = { entity: 'RobotApprovalConfig', id: this.props.id || null }

    this.disabled(true)
    $.post(rb.baseUrl + '/app/entity/record-save', JSON.stringify(post), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) location.reload()
        else location.href = 'approval/' + res.data.id
      } else RbHighbar.error(res.error_msg)
      this.disabled()
    })
  }
}