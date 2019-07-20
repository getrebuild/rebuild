/* eslint-disable react/jsx-no-target-blank */
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
      {this.state.state === 1 && this.renderStateDraft()}
      {this.state.state === 2 && this.renderStateProcessing()}
      {this.state.state === 10 && this.renderStateApproved()}
      {this.state.state === 11 && this.renderStateRejected()}
    </div>)
  }

  renderStateDraft() {
    return (<div className="alert alert-warning">
      <button className="close btn btn-secondary" onClick={this.submit}>提交</button>
      <div className="icon"><span className="zmdi zmdi-info-outline"></span></div>
      <div className="message">当前记录尚未提交审批，请在信息完善后尽快提交</div>
    </div>)
  }

  renderStateProcessing() {
    $('.J_edit,.J_delete,.J_add-slave').attr('disabled', true)
    let aMsg = '当前记录正在审批中'
    if (this.state.imApprover) {
      if (this.state.imApproveSatate === 1) aMsg = '当前记录正在等待你审批'
      else if (this.state.imApproveSatate === 10) aMsg = '你已审批同意，正在等待他人审批'
      else if (this.state.imApproveSatate === 11) aMsg = '你已驳回审批'
    }
    return (<div className="alert alert-warning">
      <button className="close btn btn-secondary" onClick={this.viewSteps}>详情</button>
      {(this.state.imApprover && this.state.imApproveSatate === 1) && <button className="close btn btn-secondary" onClick={this.approve}>审批</button>}
      <div className="icon"><span className="zmdi zmdi-hourglass-alt"></span></div>
      <div className="message">{aMsg}</div>
    </div>)
  }

  renderStateApproved() {
    $('.J_edit,.J_delete,.J_add-slave').attr('disabled', true)
    return (<div className="alert alert-success">
      <button className="close btn btn-secondary" onClick={this.viewSteps}>详情</button>
      <div className="icon"><span className="zmdi zmdi-check"></span></div>
      <div className="message">当前记录已审批完成</div>
    </div>)
  }

  renderStateRejected() {
    return (<div className="alert alert-danger">
      <button className="close btn btn-secondary" onClick={this.viewSteps}>详情</button>
      <button className="close btn btn-secondary" onClick={this.submit}>再次提交</button>
      <div className="icon"><span className="zmdi zmdi-close-circle-o"></span></div>
      <div className="message">审批被驳回，可在信息完善后再次提交</div>
    </div>)
  }

  componentDidMount() {
    $.get(`${rb.baseUrl}/app/entity/approval/state?record=${this.props.id}`, (res) => {
      this.setState(res.data)
    })
  }

  submit = () => {
    let that = this
    if (this._submitForm) this._submitForm.show()
    else renderRbcomp(<ApprovalSubmitForm id={this.props.id} />, null, function () { that._submitForm = this })
  }
  approve = () => {
    let that = this
    if (this._approveForm) this._approveForm.show()
    else renderRbcomp(<ApprovalApproveForm id={this.props.id} approval={this.state.approvalId} />, null, function () { that._approveForm = this })
  }
  viewSteps = () => {
    let that = this
    if (this._stepViewer) this._stepViewer.show()
    else renderRbcomp(<ApprovalStepViewer id={this.props.id} approval={this.state.approvalId} />, null, function () { that._stepViewer = this })
  }
}

// 审批人/抄送人选择
class ApprovalUsersForm extends RbFormHandler {
  constructor(props) {
    super(props)
  }

  renderUsers() {
    let approverHas = (this.state.nextApprovers || []).length > 0 || this.state.approverSelfSelecting
    let ccHas = (this.state.nextCcs || []).length > 0 || this.state.ccSelfSelecting
    return (<div>
      {approverHas && <div className="form-group">
        <label><i className="zmdi zmdi-account zicon" /> {`${this._approverLabel || '审批人'} (${this.state.signMode === 'AND' ? '会签' : '或签'})`}</label>
        <div>
          {(this.state.nextApprovers || []).map((item) => {
            return <UserShow key={'AU' + item[0]} id={item[0]} name={item[1]} showName={true} />
          })}
        </div>
        {this.state.approverSelfSelecting && <div>
          <UserSelector ref={(c) => this._approverSelect = c} />
        </div>}
      </div>}
      {ccHas && <div className="form-group">
        <label><i className="zmdi zmdi-mail-send zicon" /> 本次审批结果将抄送给</label>
        <div>
          {(this.state.nextCcs || []).map((item) => {
            return <UserShow key={'CU' + item[0]} id={item[0]} name={item[1]} showName={true} />
          })}
        </div>
        {this.state.ccSelfSelecting && <div>
          <UserSelector ref={(c) => this._ccSelect = c} />
        </div>}
      </div>}
    </div>)
  }

  getSelectUsers() {
    let selectUsers = {
      selectApprovers: this.state.approverSelfSelecting ? this._approverSelect.getSelected() : [],
      selectCcs: this.state.ccSelfSelecting ? this._ccSelect.getSelected() : []
    }
    if (this.state.isLastStep !== true) {
      if ((this.state.nextApprovers || []).length === 0 && selectUsers.selectApprovers.length === 0) {
        RbHighbar.create('请选择审批人')
        return false
      }
    }
    return selectUsers
  }

  getNextStep(approval) {
    $.get(`${rb.baseUrl}/app/entity/approval/fetch-nextstep?record=${this.props.id}&approval=${approval || this.props.approval}`, (res) => {
      this.setState(res.data)
    })
  }
}

// 审核提交
class ApprovalSubmitForm extends ApprovalUsersForm {
  constructor(props) {
    super(props)
    this.state.approvals = []
  }

  render() {
    return <RbModal ref={(c) => this._dlg = c} title="提交审批" width="600" disposeOnHide={this.props.disposeOnHide === true}>
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
                <a href={`${rb.baseUrl}/p/commons/approval-preview?id=${item.id}`} target="_blank"><i className="zmdi zmdi-usb zmdi-hc-rotate-180"></i> 流程图</a>
              </div>)
            })}
          </div>
        </div>
        {this.renderUsers()}
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
          this.getNextStep(res.data[0].id)
        })
      } else {
        this.setState({ approvals: null, useApproval: null })
      }
    })
  }

  handleChangeAfter(id, val) {
    if (id === 'useApproval') this.getNextStep(val)
  }

  post() {
    if (!this.state.useApproval) {
      RbHighbar.create('无可用流程，请联系管理员配置')
      return
    }
    let selectUsers = this.getSelectUsers()
    if (!selectUsers) return

    this.disabled(true)
    $.post(`${rb.baseUrl}/app/entity/approval/submit?record=${this.props.id}&approval=${this.state.useApproval}`, JSON.stringify(selectUsers), (res) => {
      if (res.error_code > 0) RbHighbar.error(res.error_msg)
      else {
        RbHighbar.success('审批已提交')
        this.hide()
        setTimeout(() => {
          if (window.RbViewPage) window.RbViewPage.reload()
          if (window.RbListPage) window.RbListPage.reload()
          else location.reload()
        }, 1000)
      }
      this.disabled()
    })
  }
}

// 审批
class ApprovalApproveForm extends ApprovalUsersForm {
  constructor(props) {
    super(props)
    this._approverLabel = '下一审批人'
  }

  render() {
    return <RbModal ref={(c) => this._dlg = c} title="审批" width="600">
      <div className="form approval-form">
        <div className="form-group">
          <label>批注</label>
          <textarea className="form-control form-control-sm row3x" name="remark" placeholder="输入批注 (可选)" value={this.state.remark || ''} onChange={this.handleChange} />
        </div>
        {this.renderUsers()}
        <div className="dialog-footer">
          <button type="button" className="btn btn-primary btn-space" onClick={() => this.post(10)}>同意</button>
          <button type="button" className="btn btn-danger bordered btn-space" onClick={() => this.post(11)}>驳回</button>
        </div>
      </div>
    </RbModal>
  }

  componentDidMount() {
    this.getNextStep()
  }

  post(state) {
    let selectUsers = this.getSelectUsers()
    if (!selectUsers) return
    let pdata = { remark: this.state.remark || '', selectUsers: selectUsers }

    this.disabled(true)
    $.post(`${rb.baseUrl}/app/entity/approval/approve?record=${this.props.id}&state=${state}`, JSON.stringify(pdata), (res) => {
      if (res.error_code > 0) RbHighbar.error(res.error_msg)
      else {
        RbHighbar.success('审批已' + (state === 10 ? '同意' : '驳回'))
        setTimeout(() => {
          if (window.RbViewPage) window.RbViewPage.reload()
          else location.reload()
        }, 1000)
      }
      this.disabled()
    })
  }
}

// 已审批步骤查看
class ApprovalStepViewer extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    return (
      <div className="modal" ref={(c) => this._dlg = c} tabIndex="-1">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={this.hide}><span className="zmdi zmdi-close" /></button>
            </div>
            <div className="modal-body approved-steps-body">
              {!this.state.steps && <RbSpinner fully={true} />}
              <ul className="timeline approved-steps">
                {(this.state.steps || []).map((item, idx) => {
                  return idx === 0 ? this.renderSubmitter(item, idx) : this.renderApprovers(item, idx)
                })}
              </ul>
            </div>
          </div>
        </div>
      </div>
    )
  }

  renderSubmitter(s, idx) {
    return <li className="timeline-item state0" key={`step-${idx}`}>
      {this.__formatTime(s.createdOn)}
      <div className="timeline-content">
        <div className="timeline-avatar"><img src={`${rb.baseUrl}/account/user-avatar/${s.submitter}`} /></div>
        <div className="timeline-header">
          <p className="timeline-activity">由 {s.submitter === rb.currentUser ? '你' : s.submitterName} 提交审批</p>
        </div>
      </div>
    </li>
  }
  renderApprovers(s, idx) {
    let k = 'step-' + idx + '-'
    return <div key={k} className={s.length > 1 ? 'joint0' : ''}>
      {s.map((item, idx2) => {
        let approverName = item.approver === rb.currentUser ? '你' : item.approverName
        let aMsg = `等待 ${approverName} 审批`
        if (item.state >= 10) aMsg = `由 ${approverName} ${item.state === 10 ? '审批同意' : '驳回审批'}`

        return <li className={'timeline-item state' + item.state} key={k + idx2}>
          {this.__formatTime(item.approvedTime || item.createdOn)}
          <div className="timeline-date">{}</div>
          <div className="timeline-content">
            <div className="timeline-avatar"><img src={`${rb.baseUrl}/account/user-avatar/${item.approver}`} /></div>
            <div className="timeline-header">
              <p className="timeline-activity">{aMsg}</p>
              {item.remark && <blockquote className="blockquote timeline-blockquote mb-0">
                <p>{item.remark}</p>
              </blockquote>}
            </div>
          </div>
        </li>
      })}
    </div>
  }

  __formatTime(time) {
    time = time.split(' ')
    return <div className="timeline-date">{time[1]}<span>{time[0]}</span></div>
  }

  componentDidMount() {
    this.show()
    $.get(`${rb.baseUrl}/app/entity/approval/fetch-workedsteps?record=${this.props.id}`, (res) => {
      if (!res.data || res.data.length === 0) {
        RbHighbar.create('未查询到流程详情')
        this.hide()
        this.__noStepFound = true
      } else this.setState({ steps: res.data })
    })
  }

  hide = () => $(this._dlg).modal('hide')
  show = () => {
    if (this.__noStepFound === true) {
      RbHighbar.create('未查询到流程详情')
      this.hide()
    } else $(this._dlg).modal({ show: true, keyboard: true })
  }
}