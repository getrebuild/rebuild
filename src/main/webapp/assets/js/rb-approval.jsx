/* eslint-disable react/prop-types */
/* eslint-disable no-unused-vars */
// 审批流程
class ApprovalProcessor extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }
  render() {
    return (<div className="approval-pane">
      {this.state.state === 1 && this.renderStateDarft()}
      {this.state.state === 2 && this.renderStateProcessing()}
      {this.state.state === 10 && this.renderStateApproved()}
      {this.state.state === 11 && this.renderStateRejected()}
    </div>)
  }

  renderStateDarft() {
    return (<div className="alert alert-warning">
      <button className="close btn btn-secondary" onClick={this.openSubmit}>立即提交</button>
      <div className="icon"><span className="zmdi zmdi-info-outline"></span></div>
      <div className="message">
        当前记录尚未提交审批，请在信息完善后尽快提交。
      </div>
    </div>)
  }

  renderStateProcessing() {
  }

  renderStateApproved() {
    <div className="alert alert-success">
      <button className="close btn btn-secondary">审批详情</button>
      <div className="icon"><span className="zmdi zmdi-info-outline"></span></div>
      <div className="message">
        当前记录已审批完成。
      </div>
    </div>
  }

  renderStateRejected() {
    <div className="alert alert-danger">
      <button className="close btn btn-secondary">再次提交</button>
      <div className="icon"><span className="zmdi zmdi-info-outline"></span></div>
      <div className="message">
        审批被驳回，你可在信息完善后重新提交。
      </div>
    </div>
  }

  componentDidMount() {
    $.get(`${rb.baseUrl}/app/entity/approval/state?record=${this.props.id}`, (res) => {
      this.setState(res.data)
    })
  }

  openSubmit = () => {
    if (this._submit) this._submit.show()
    else this._submit = renderRbcomp(<ApprovalSubmit id={this.props.id} />)
  }
  openForm = () => {
    if (this._form) this._form.show()
    else this._form = renderRbcomp(<ApprovalForm id={this.props.id} />)
  }
}

// 提交
class ApprovalSubmit extends RbFormHandler {
  constructor(props) {
    super(props)
  }
  render() {
    return <RbModal ref={(c) => this._dlg = c} title="提交审批" width="500">
      <div className="form approval-form">
        <div className="form-group">
          <label>选择审批流程</label>
          <div>
            {!this.state.approvals && <strong>无可用流程，请联系管理员配置</strong>}
            {(this.state.approvals || []).map((item) => {
              return (<label key={'A' + item.id} className="custom-control custom-control-sm custom-radio mb-2">
                <input className="custom-control-input" type="radio" name="useApproval" value={item.id} onChange={this.handleChange} checked={this.state.useApproval === item.id} />
                <span className="custom-control-label">{item.name}</span>
              </label>)
            })}
          </div>
        </div>
        <div className="dialog-footer">
          <button type="button" className="btn btn-primary btn-space" onClick={() => this.post()}>提交</button>
          <button type="button" className="btn btn-secondary btn-space" onClick={this.hide}>取消</button>
        </div>
      </div>
    </RbModal>
  }
  componentDidMount() {
    $.get(`${rb.baseUrl}/app/entity/approval/workable?record=${this.props.id}`, (res) => {
      if (res.data.length > 0) {
        this.setState({ approvals: res.data, useApproval: res.data[0].id })
      }
    })
  }
  post() {
  }
}

// 审批
class ApprovalForm extends RbFormHandler {
  constructor(props) {
    super(props)
  }
  render() {
    return <RbModal ref={(c) => this._dlg = c} title="审批" width="500">
      <div className="form approval-form">
        <div className="form-group">
          <label>批注</label>
          <textarea className="form-control form-control-sm row3x" name="remark" placeholder="输入批注 ..." value={this.state.remark || ''} onChange={this.handleChange} />
        </div>
        <div className="dialog-footer">
          <button type="button" className="btn btn-primary btn-space" onClick={() => this.post(10)}>同意</button>
          <button type="button" className="btn btn-danger bordered btn-space" onClick={() => this.post(11)}>驳回</button>
        </div>
      </div>
    </RbModal>
  }
  post(state) {
  }
}