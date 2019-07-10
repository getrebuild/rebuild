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
    this.state.approvals = []
  }
  render() {
    let hasCC = (this.state.nextCcs || []).length > 0 || this.state.ccSelfSelecting
    return <RbModal ref={(c) => this._dlg = c} title="提交审批" width="600">
      <div className="form approval-form">
        <div className="form-group">
          <label>选择审批流程</label>
          <div className="approval-list">
            {!this.state.approvals && <strong className="text-danger">无可用流程，请联系管理员配置</strong>}
            {(this.state.approvals || []).map((item) => {
              return (<div key={'A' + item.id}>
                <label className="custom-control custom-control-sm custom-radio mb-0">
                  <input className="custom-control-input" type="radio" name="useApproval" value={item.id} onChange={this.handleChange} checked={this.state.useApproval === item.id} />
                  <span className="custom-control-label">{item.name}</span>
                </label>
                <a href="javascript:;">流程详情</a>
              </div>)
            })}
          </div>
        </div>
        <div className="form-group">
          <label><i className="zmdi zmdi-account zicon" /> 审批人</label>
          <div>
            {(this.state.nextApprovers || []).map((item) => {
              return <UserShow key={'AU' + item[0]} id={item[0]} name={item[1]} showName={true} />
            })}
          </div>
          {this.state.approverSelfSelecting && <div>
            <UserSelector ref={(c) => this._approverSelect = c} />
          </div>}
        </div>
        {hasCC && <div className="form-group">
          <label><i className="zmdi zmdi-mail-send zicon" /> 抄送给</label>
          <div>
            {(this.state.nextCcs || []).map((item) => {
              return <UserShow key={'CU' + item[0]} id={item[0]} name={item[1]} showName={true} />
            })}
          </div>
          {this.state.approverSelfSelecting && <div>
            <UserSelector ref={(c) => this._ccSelect = c} />
          </div>}
        </div>}
        <div className="dialog-footer" ref={(c) => this._btns = c}>
          <button type="button" className="btn btn-primary btn-space" onClick={() => this.post()}>提交</button>
          <button type="button" className="btn btn-secondary btn-space" onClick={this.hide}>取消</button>
        </div>
      </div>
    </RbModal >
  }
  componentDidMount() {
    $.get(`${rb.baseUrl}/app/entity/approval/workable?record=${this.props.id}`, (res) => {
      if (res.data && res.data.length > 0) {
        this.setState({ approvals: res.data, useApproval: res.data[0].id }, () => {
          this.showNextStep(res.data[0].id)
        })
      } else {
        this.setState({ approvals: null, useApproval: null })
      }
    })
  }
  handleChangeAfter(id, val) {
    if (id === 'useApproval') this.showNextStep(val)
  }
  showNextStep(approval) {
    $.get(`${rb.baseUrl}/app/entity/approval/nextstep-gets?record=${this.props.id}&approval=${approval}`, (res) => {
      this.setState(res.data)
    })
  }
  post() {
    if (!this.state.useApproval) {
      rb.highbar('无可用流程，请联系管理员配置')
      return
    }

    let selectUsers = {
      selectApprovers: this.state.approverSelfSelecting ? this._approverSelect.getSelected() : [],
      selectCcs: this.state.ccSelfSelecting ? this._ccSelect.getSelected() : []
    }
    if ((this.state.nextApprovers || []).length === 0 && selectUsers.selectApprovers.length === 0) {
      rb.highbar('请选择审批人')
      return
    }

    this.disabled(true)
    $.post(`${rb.baseUrl}/app/entity/approval/submit?record=${this.props.id}&approval=${this.state.useApproval}`, JSON.stringify(selectUsers), (res) => {
      if (res.error_code > 0) rb.hberror(res.error_msg)
      else {
        rb.hbsuccess('审批已提交')
        setTimeout(() => location.reload(), 1000)
      }
      this.disabled(false)
    })
  }
}

// 审批
class ApprovalForm extends RbFormHandler {
  constructor(props) {
    super(props)
  }
  render() {
    return <RbModal ref={(c) => this._dlg = c} title="审批" width="600">
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