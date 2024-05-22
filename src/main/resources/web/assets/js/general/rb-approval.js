/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */

// 审批流程
class ApprovalProcessor extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    return (
      <div className="approval-toolbar">
        {this.state.state === 1 && this.renderStateDraft()}
        {this.state.state === 2 && this.renderStateProcessing()}
        {this.state.state === 10 && this.renderStateApproved()}
        {this.state.state === 11 && this.renderStateRejected()}
        {this.state.state === 12 && this.renderStateCanceled()}
        {this.state.state === 13 && this.renderStateRevoked()}
      </div>
    )
  }

  renderStateDraft() {
    return (
      <div className="alert alert-warning shadow-sm">
        <span className="close">
          <button className="btn btn-secondary" onClick={this.submit}>
            {$L('提交')}
          </button>
        </span>

        <div className="icon">
          <span className="zmdi zmdi-info-outline" />
        </div>
        <div className="message"> {$L('当前记录尚未提交审批，请在信息完善后尽快提交')}</div>
      </div>
    )
  }

  renderStateProcessing() {
    window.RbViewPage && window.RbViewPage.setReadonly(true)

    let aMsg = $L('当前记录正在审批中')
    if (this.state.imApprover) {
      if (this.state.imApproveSatate === 1) aMsg = $L('当前记录正在等待你审批')
      else if (this.state.imApproveSatate === 10) aMsg = $L('你已审批同意，正在等待其他人审批')
      else if (this.state.imApproveSatate === 11) aMsg = $L('你已驳回审批')
    }

    return (
      <div className="alert alert-warning shadow-sm">
        <span className="close">
          {this.state.imApprover && this.state.imApproveSatate === 1 && (
            <button className="btn btn-secondary" onClick={this.approve}>
              {$L('审批')}
            </button>
          )}
          {(this.state.canCancel || this.state.canUrge) && (
            <RF>
              {this.state.canUrge && (
                <button className="btn btn-secondary" onClick={this.urge}>
                  {$L('催审')}
                </button>
              )}
              {this.state.canCancel && (
                <button className="btn btn-secondary" onClick={this.cancel}>
                  {$L('撤回')}
                </button>
              )}
            </RF>
          )}
          <button className="btn btn-secondary" onClick={this.viewSteps}>
            {$L('详情')}
          </button>
        </span>

        <div className="icon">
          <span className="zmdi zmdi-hourglass-alt" />
        </div>
        <div className="message">{aMsg}</div>
      </div>
    )
  }

  renderStateApproved() {
    window.RbViewPage && window.RbViewPage.setReadonly(true)

    return (
      <div className="alert alert-success shadow-sm">
        <span className="close">
          {(rb.isAdminUser || this.state.canRevoke) && (
            <button className="btn btn-secondary" onClick={this.revoke}>
              {$L('撤销')}
            </button>
          )}
          <button className="btn btn-secondary" onClick={this.viewSteps}>
            {$L('详情')}
          </button>
        </span>

        <div className="icon">
          <span className="zmdi zmdi-check" />
        </div>
        <div className="message">{$L('当前记录已审批完成')}</div>
      </div>
    )
  }

  renderStateRejected() {
    return (
      <div className="alert alert-danger shadow-sm">
        <span className="close">
          <button className="btn btn-secondary" onClick={this.submit}>
            {$L('再次提交')}
          </button>
          <button className="btn btn-secondary" onClick={this.viewSteps}>
            {$L('详情')}
          </button>
        </span>

        <div className="icon">
          <span className="zmdi zmdi-close-circle-o" />
        </div>
        <div className="message">{$L('审批被驳回，可在信息完善后再次提交')}</div>
      </div>
    )
  }

  renderStateCanceled() {
    return (
      <div className="alert alert-warning shadow-sm">
        <span className="close">
          <button className="btn btn-secondary" onClick={this.submit}>
            {$L('再次提交')}
          </button>
          <button className="btn btn-secondary" onClick={this.viewSteps}>
            {$L('详情')}
          </button>
        </span>

        <div className="icon">
          <span className="zmdi zmdi-rotate-left" />
        </div>
        <div className="message">{$L('审批已撤回，请在信息完善后再次提交')}</div>
      </div>
    )
  }

  renderStateRevoked() {
    return (
      <div className="alert alert-warning shadow-sm">
        <span className="close">
          <button className="btn btn-secondary" onClick={this.submit}>
            {$L('再次提交')}
          </button>
          <button className="btn btn-secondary" onClick={this.viewSteps}>
            {$L('详情')}
          </button>
        </span>

        <div className="icon">
          <span className="zmdi zmdi-rotate-left" />
        </div>
        <div className="message">{$L('审批已撤销，请在信息完善后再次提交')}</div>
      </div>
    )
  }

  componentDidMount() {
    $.get(`/app/entity/approval/state?record=${this.props.id}`, (res) => {
      if (res.error_code === 0 && res.data) {
        this.setState(res.data)
      } else {
        RbHighbar.error($L('无法获取审批状态'))
      }
    })
  }

  submit = () => {
    const that = this
    if (this._SubmitForm) {
      // this._SubmitForm.show(null, () => that._SubmitForm.reload())
      this._SubmitForm.show()
    } else {
      renderRbcomp(<ApprovalSubmitForm id={this.props.id} />, function () {
        that._SubmitForm = this
      })
    }
  }

  approve = () => {
    const that = this
    if (this._ApproveForm) {
      this._ApproveForm.show()
    } else {
      renderRbcomp(<ApprovalApproveForm id={this.props.id} approval={this.state.approvalId} entity={this.props.entity} $$$parent={this} />, function () {
        that._ApproveForm = this
      })
    }
  }

  cancel = () => {
    const that = this
    RbAlert.create($L('将要撤回已提交审批。是否继续？'), {
      confirm: function () {
        this.disabled(true, true)
        $.post(`/app/entity/approval/cancel?record=${that.props.id}`, (res) => {
          if (res.error_code > 0) RbHighbar.error(res.error_msg)
          else _reloadAndTips(this, $L('审批已撤回'))
          this.disabled()
        })
      },
    })
  }

  urge = () => {
    const that = this
    RbAlert.create($L('将向当前审批人发送催审通知。是否继续？'), {
      confirm: function () {
        this.disabled(true)
        $.post(`/app/entity/approval/urge?record=${that.props.id}`, (res) => {
          if (res.error_code > 0) {
            RbHighbar.create(res.error_msg)
            this.disabled()
          } else {
            RbHighbar.success($L('通知已发送'))
            this.hide()
          }
        })
      },
    })
  }

  revoke = () => {
    const that = this
    RbAlert.create($L('将要撤销已通过审批。是否继续？'), {
      type: 'danger',
      confirm: function () {
        this.disabled(true, true)
        $.post(`/app/entity/approval/revoke?record=${that.props.id}`, (res) => {
          if (res.error_code > 0) RbHighbar.error(res.error_msg)
          else _reloadAndTips(this, $L('审批已撤销'))
          this.disabled()
        })
      },
    })
  }

  viewSteps = () => {
    const that = this
    if (this._ApprovalStepViewer) {
      this._ApprovalStepViewer.show()
    } else {
      renderRbcomp(<ApprovalStepViewer id={this.props.id} approval={this.state.approvalId} $$$parent={this} />, function () {
        that._ApprovalStepViewer = this
      })
    }
  }
}

// 审批人/抄送人选择
class ApprovalUsersForm extends RbFormHandler {
  renderUsers() {
    if (!this.state.isLoaded) return null

    if (this.state.hasError) {
      return (
        <div className="form-group">
          <RbAlertBox message={this.state.hasError} type="danger" />
        </div>
      )
    }

    const approverHas = (this.state.nextApprovers || []).length > 0 || this.state.approverSelfSelecting
    const ccHas = (this.state.nextCcs || []).length > 0 || this.state.ccSelfSelecting

    return (
      <RF>
        {approverHas ? (
          <div className="form-group">
            <label>
              <i className="zmdi zmdi-account zicon" /> {`${this._approverLabel || $L('下一审批人')} (${this.state.signMode === 'AND' ? $L('会签') : $L('或签')})`}
            </label>
            <div>
              {(this.state.nextApprovers || []).map((item) => {
                return <UserShow key={item[0]} id={item[0]} name={item[1]} showName />
              })}
            </div>
            {this.state.approverSelfSelecting && (
              <div>
                <UserSelector ref={(c) => (this._approverSelector = c)} />
              </div>
            )}
          </div>
        ) : (
          !this.state.isLastStep && (
            <div className="form-group">
              <RbAlertBox message={$L('当前审批流程无可用审批人')} />
            </div>
          )
        )}
        {ccHas && (
          <div className="form-group">
            <label>
              <i className="zmdi zmdi-mail-send zicon" /> {this._ccLabel || $L('本次审批结果将抄送给')}
            </label>
            <div>
              {(this.state.nextCcs || []).map((item) => {
                return <UserShow key={item[0]} id={item[0]} name={item[1]} showName />
              })}
            </div>
            {this.state.ccSelfSelecting && (
              <div>
                <UserSelector ref={(c) => (this._ccSelector = c)} />
              </div>
            )}

            {(this.state.nextCcAccounts || []).length > 0 && (
              <div className="mt-2 cc-accounts">
                <span className="mr-1">{$L('及以下外部人员')}</span>
                {this.state.nextCcAccounts.map((me) => {
                  return <a key={me}>{me}</a>
                })}
              </div>
            )}
          </div>
        )}
      </RF>
    )
  }

  getSelectUsers() {
    const selectUsers = {
      selectApprovers: this.state.approverSelfSelecting ? this._approverSelector.getSelected() : [],
      selectCcs: this.state.ccSelfSelecting ? this._ccSelector.getSelected() : [],
    }

    if (!this.state.isLastStep) {
      if ((this.state.nextApprovers || []).length === 0 && selectUsers.selectApprovers.length === 0) {
        RbHighbar.create($L('请选择审批人'))
        return false
      }
    }
    return selectUsers
  }

  getNextStep(approval) {
    $.get(`/app/entity/approval/fetch-nextstep?record=${this.props.id}&approval=${approval || this.props.approval}`, (res) => {
      this.setState({ isLoaded: true })
      if (res.error_code === 0) {
        this.setState({ ...res.data, hasError: null })
      } else {
        this.setState({ hasError: res.error_msg })
      }
    })
  }
}

// 审批提交
class ApprovalSubmitForm extends ApprovalUsersForm {
  constructor(props) {
    super(props)
    this._ccLabel = $L('本次提交将抄送给')
    this._approverLabel = $L('审批人')
    this.state.approvals = []
  }

  render() {
    const approvals = this.state.approvals || []

    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$L('提交审批')}>
        <div className="form approval-form">
          <div className="form-group">
            <label>{$L('选择审批流程')}</label>
            <div className="approval-list">
              {!this.state.approvals && (
                <p className="text-muted">
                  {$L('无适用流程')}
                  {rb.isAdminUser && (
                    <a className="icon-link ml-1" target="_blank" href={`${rb.baseUrl}/admin/robot/approvals`}>
                      <i className="zmdi zmdi-settings" /> {$L('点击配置')}
                    </a>
                  )}
                </p>
              )}
              {approvals.map((item) => {
                return (
                  <div key={item.id}>
                    <label className="custom-control custom-control-sm custom-radio mb-0">
                      <input className="custom-control-input" type="radio" name="useApproval" value={item.id} onChange={this.handleChange} checked={this.state.useApproval === item.id} />
                      <span className="custom-control-label">{item.name}</span>
                    </label>
                    <a href={`${rb.baseUrl}/app/RobotApprovalConfig/view/${item.id}`} target="_blank">
                      <i className="icon mdi mdi-progress-check fs-14" /> {$L('流程详情')}
                    </a>
                  </div>
                )
              })}
            </div>
          </div>
          {approvals.length > 0 && this.renderUsers()}
        </div>
        <div className="dialog-footer" ref={(c) => (this._btns = c)}>
          <button type="button" className="btn btn-secondary btn-space mr-2" onClick={this.hide}>
            {$L('取消')}
          </button>
          <button type="button" className="btn btn-primary btn-space mr-1" onClick={() => this.post()}>
            {$L('提交')}
          </button>
        </div>
      </RbModal>
    )
  }

  componentDidMount = () => this.reload()

  reload() {
    $.get(`/app/entity/approval/workable?record=${this.props.id}`, (res) => {
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
    if (!this.state.useApproval) return RbHighbar.create($L('请选择审批流程'))
    const selectUsers = this.getSelectUsers()
    if (!selectUsers) return

    this.disabled(true)
    $.post(`/app/entity/approval/submit?record=${this.props.id}&approval=${this.state.useApproval}`, JSON.stringify(selectUsers), (res) => {
      if (res.error_code > 0) RbHighbar.error(res.error_msg)
      else _reloadAndTips(this, $L('审批已提交'))
      this.disabled()
    })
  }
}

// 审批
class ApprovalApproveForm extends ApprovalUsersForm {
  render() {
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$L('审批')}>
        <div className="form approval-form">
          {this.state.bizMessage && (
            <div className="form-group">
              <RbAlertBox message={this.state.bizMessage} onClose={() => this.setState({ bizMessage: null })} />
            </div>
          )}

          {this.state.aform && this.renderLiteForm()}

          <div className="form-group">
            <label>{$L('批注')}</label>
            <textarea className="form-control form-control-sm row2x" name="remark" placeholder={$L('输入批注 (可选)')} value={this.state.remark || ''} onChange={this.handleChange} maxLength="600" />
          </div>

          {this.renderUsers()}
        </div>

        <div className="dialog-footer" ref={(c) => (this._btns = c)}>
          {this.props.$$$parent && (
            <div className="float-left">
              <button
                className="btn btn-link btn-sm pl-1"
                onClick={() => {
                  this.props.$$$parent.viewSteps()
                  // this.hide()
                }}>
                <i className="zmdi zmdi-time mr-1" />
                {$L('审批详情')}
              </button>
            </div>
          )}

          {(this.state.allowReferral || this.state.allowCountersign) && (
            <div className="btn-group btn-space mr-2">
              <button className="btn btn-secondary dropdown-toggle w-auto" data-toggle="dropdown" title={$L('更多操作')}>
                <i className="icon zmdi zmdi-more-vert" />
              </button>
              <div className="dropdown-menu dropdown-menu-right">
                {this.state.allowReferral && (
                  <a className="dropdown-item" onClick={() => this._handleReferral()}>
                    <i className="icon mdi mdi-account-arrow-right-outline" /> {$L('转审')}
                  </a>
                )}
                {this.state.allowCountersign && (
                  <a className="dropdown-item" onClick={() => this._handleCountersign()}>
                    <i className="icon mdi mdi-account-multiple-plus-outline" /> {$L('加签')}
                  </a>
                )}
              </div>
            </div>
          )}

          <button type="button" className="btn btn-primary btn-space mr-2" onClick={() => this.post(10)} disabled={!!this.state.hasError}>
            {$L('同意')}
          </button>
          <button type="button" className="btn btn-danger btn-outline btn-space mr-1" onClick={() => this.post(11)} disabled={!!this.state.hasError}>
            {$L('驳回')}
          </button>
        </div>
      </RbModal>
    )
  }

  renderLiteForm() {
    const fake = {
      state: { id: this.props.id },
    }

    // @see rb-forms.append.js LiteFormModal#create

    return (
      <div className="form-group">
        <label>{$L('信息完善 (驳回时无需填写)')}</label>
        <LiteForm entity={this.props.entity} id={this.props.id} rawModel={{}} $$$parent={fake} ref={(c) => (this._LiteForm = c)}>
          {this.state.aform.map((item) => {
            item.isFull = true
            delete item.referenceQuickNew // v35
            // eslint-disable-next-line no-undef
            return detectElement(item)
          })}
        </LiteForm>
      </div>
    )
  }

  componentDidMount = () => this.getNextStep()

  post(state) {
    const that = this
    if (state === 11 && this.state.isRejectStep) {
      this.disabled(true)
      $.get(`/app/entity/approval/fetch-backsteps?record=${this.props.id}`, (res) => {
        this.disabled()

        const ss = res.data || []
        RbAlert.create(
          <RF>
            <b>{$L('请选择驳回方式')}</b>
            <div className="widget-sm mt-3">
              <select className="form-control form-control-sm" defaultValue={ss.length > 0 ? ss[ss.length - 1].node : '0'}>
                <option value="0">{$L('整体驳回')}</option>
                {ss.length > 0 && (
                  <optgroup label={$L('退回至')}>
                    {ss.map((s) => {
                      return (
                        <option key={s.node} value={s.node}>
                          {s.nodeName}
                        </option>
                      )
                    })}
                  </optgroup>
                )}
              </select>
            </div>
          </RF>,
          {
            onConfirm: function () {
              this.disabled(true, true)
              const node = $(this._element).find('select').val()
              that.post2(state, node === '0' ? null : node, this)
            },
            onRendered: function () {
              $(this._element).find('select').select2({
                allowClear: false,
              })
            },
          }
        )
      })
    } else {
      this.post2(state, null)
    }
  }

  post2(state, rejectNode, _alert) {
    let aformData = {}
    if (this.state.aform && state === 10) {
      aformData = this._LiteForm.buildFormData()
      if (aformData === false) return
    }

    let selectUsers
    if (state === 10) {
      selectUsers = this.getSelectUsers()
      if (!selectUsers) return
    }

    const data = {
      remark: this.state.remark || '',
      selectUsers: selectUsers,
      aformData: aformData,
      useGroup: this.state.useGroup,
    }

    _alert && _alert.disabled(true, true)
    this.disabled(true)

    $.post(`/app/entity/approval/approve?record=${this.props.id}&state=${state}&rejectNode=${rejectNode || ''}`, JSON.stringify(data), (res) => {
      _alert && _alert.disabled()
      this.disabled()

      if (res.error_code === 498) {
        this.setState({ bizMessage: res.error_msg })
        this.getNextStep()
      } else if (res.error_code > 0) {
        RbHighbar.error(res.error_msg)
      } else {
        _alert && _alert.hide(true)
        _reloadAndTips(this, state === 10 ? $L('审批已同意') : rejectNode ? $L('审批已退回') : $L('审批已驳回'))
        typeof this.props.call === 'function' && this.props.call()
      }
    })
  }

  _handleReferral() {
    renderRbcomp(
      <ApproveFormExtAction
        title={$L('转审给谁')}
        tips={$L('请选择转审给谁')}
        onConfirm={(s, _alert) => {
          _alert.disabled(true)
          $.post(`/app/entity/approval/referral?record=${this.props.id}&to=${s[0]}`, (res) => {
            _alert.disabled()

            if (res.error_code === 0) {
              _alert.hide()
              _reloadAndTips(this, $L('已转审'))
              typeof this.props.call === 'function' && this.props.call()
            } else {
              RbHighbar.error(res.error_msg)
            }
          })
        }}
      />
    )
  }

  _handleCountersign() {
    renderRbcomp(
      <ApproveFormExtAction
        multiple
        title={$L('加签哪些用户')}
        tips={$L('请选择加签哪些用户')}
        onConfirm={(s, _alert) => {
          _alert.disabled(true)
          $.post(`/app/entity/approval/countersign?record=${this.props.id}&to=${s.join(',')}`, (res) => {
            _alert.disabled()

            if (res.error_code === 0) {
              _alert.hide()
              _reloadAndTips(this, $L('已加签'))
              typeof this.props.call === 'function' && this.props.call()
            } else {
              RbHighbar.error(res.error_msg)
            }
          })
        }}
      />
    )
  }
}

class ApproveFormExtAction extends RbAlert {
  renderContent() {
    return (
      <form className="rbalert-form-sm">
        <div className="form-group">
          <label className="text-bold">{this.props.title}</label>
          <UserSelector hideDepartment hideRole hideTeam multiple={this.props.multiple === true} ref={(c) => (this._UserSelector = c)} />
        </div>
        <div className="form-group mb-1">
          <button
            disabled={this.state.disable}
            type="button"
            className="btn btn-space btn-primary"
            onClick={() => {
              const s = this._UserSelector.val()
              if (s.length === 0) return RbHighbar.create(this.props.tips)
              else this.props.onConfirm(s, this)
            }}>
            {$L('确定')}
          </button>
        </div>
      </form>
    )
  }
}

const STATE_NAMES = {
  10: $L('审批同意'),
  11: $L('审批驳回'),
  12: $L('审批撤回'),
  13: $L('审批撤销'),
  21: $L('退回'),
}

// 已审批步骤查看
class ApprovalStepViewer extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    const stateLast = this.state.steps ? this.state.steps[0].approvalState : 0

    return (
      <div className="modal" ref={(c) => (this._dlg = c)} style={{ zIndex: 1051 }}>
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={this.hide}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body approved-steps-body">
              {!this.state.steps && <RbSpinner fully />}
              <ul className="timeline approved-steps">
                {(this.state.steps || []).map((item, idx) => {
                  return idx === 0 ? this.renderSubmitter(item) : this.renderApprover(item, stateLast)
                })}
                {stateLast >= 10 && (
                  <li className="timeline-item last" key="step-last">
                    <span>{stateLast === 13 || stateLast === 12 ? $L('重审') : $L('结束')}</span>
                  </li>
                )}
              </ul>
            </div>
          </div>
        </div>
      </div>
    )
  }

  renderSubmitter(s) {
    return (
      <li className="timeline-item state0" key="step-submit">
        {this._formatTime(s.createdOn)}
        <div className="timeline-content">
          <div className="timeline-avatar">
            <img src={`${rb.baseUrl}/account/user-avatar/${s.submitter}`} alt="Avatar" />
          </div>
          <div className="timeline-header">
            <p className="timeline-activity">{$L('由 %s 提交审批', s.submitter === rb.currentUser ? $L('你') : s.submitterName)}</p>
            {s.approvalName && (
              <blockquote className="blockquote timeline-blockquote mb-0">
                <p>
                  <a target="_blank" href={`${rb.baseUrl}/app/RobotApprovalConfig/view/${s.approvalId}`}>
                    <i className="mdi mdi-progress-check" /> {s.approvalName}
                  </a>
                </p>
              </blockquote>
            )}
          </div>
        </div>
      </li>
    )
  }

  renderApprover(s, stateLast) {
    const sss = []
    let nodeState = 0
    if (s[0].signMode === 'OR') {
      s.forEach((item) => {
        if (item.state >= 10) nodeState = item.state
      })
    }

    s.forEach((item) => {
      const approverName = item.approver === rb.currentUser ? $L('你') : item.approverName
      let aMsg = $L('等待 %s 审批', approverName)
      if (item.state >= 10) aMsg = $L('由 %s %s', approverName, STATE_NAMES[item.state] || item.state)
      if ((nodeState >= 10 || stateLast >= 10) && item.state < 10) aMsg = `${approverName} ${$L('未进行审批')}`

      // 待我审批
      if (item.approver === rb.currentUser && item.state === 1 && stateLast === 2) {
        aMsg = (
          <RF>
            {$L('等待 你 ')}
            <a
              href="###"
              onClick={(e) => {
                $stopEvent(e, true)
                if (this.props.$$$parent) {
                  this.props.$$$parent.approve()
                  this.hide()
                }
              }}>
              {$L('审批')}
            </a>
          </RF>
        )
      }

      sss.push(
        <li className={`timeline-item state${item.state}`} key={`step-${$random()}`}>
          {this._formatTime(item.approvedTime || item.createdOn)}
          <div className="timeline-content">
            <div className="timeline-avatar">
              <img src={`${rb.baseUrl}/account/user-avatar/${item.approver}`} alt="Avatar" />
            </div>
            <div className="timeline-header">
              <p className="timeline-activity">
                {aMsg}
                {item.referralFrom && (
                  <span className="badge badge-warning" title={$L('由 %s 转审', item.referralFrom)}>
                    {$L('转审')}
                  </span>
                )}
                {item.countersignFrom && (
                  <span className="badge badge-warning" title={$L('由 %s 加签', item.countersignFrom)}>
                    {$L('加签')}
                  </span>
                )}
                {item.batchMode && (
                  <span className="badge badge-warning" title={$L('批量审批')}>
                    {$L('批量')}
                  </span>
                )}
              </p>
              {(item.druation || item.druation === 0) && (
                <span className="float-right badge badge-light badge-pill cursor-help" title={$L('审批时长')}>
                  {$sec2Time(item.druation)}
                </span>
              )}
              {item.remark && (
                <blockquote className="blockquote timeline-blockquote mb-0">
                  <p className="text-wrap">{item.remark}</p>
                </blockquote>
              )}
              {item.state >= 10 && (item.ccUsers || []).length + (item.ccAccounts || []).length > 0 && (
                <blockquote className="blockquote timeline-blockquote mb-0 cc">
                  <p className="text-wrap">
                    <span className="mr-1">
                      <i className="zmdi zmdi-mail-send mr-1" />
                      {$L('已抄送')}
                    </span>
                    {(item.ccUsers || []).map((item) => {
                      return <a key={item}>{item}</a>
                    })}
                    {(item.ccAccounts || []).map((item) => {
                      return (
                        <a key={item} title={$L('外部人员')}>
                          {item}
                        </a>
                      )
                    })}
                  </p>
                </blockquote>
              )}
            </div>
          </div>
        </li>
      )
    })

    if (sss.length < 2) {
      return (
        <RF key={`step-${$random()}`}>
          {s[0].nodeName && <strong className="mb-1">{s[0].nodeName}</strong>}
          {sss}
        </RF>
      )
    }

    const sm = s[0].signMode
    const clazz = sm === 'OR' || sm === 'AND' ? 'joint' : 'no-joint'
    return (
      <RF key={`step-${$random()}`}>
        {s[0].nodeName && <strong className="mb-1">{s[0].nodeName}</strong>}
        <div className={clazz} _title={sm === 'OR' ? $L('或签') : sm === 'AND' ? $L('会签') : null} key={`step-${$random()}`}>
          {sss}
        </div>
      </RF>
    )
  }

  _formatTime(time) {
    time = time.split(' ')
    return (
      <div className="timeline-date">
        {time[1]}
        <div>{time[0]}</div>
      </div>
    )
  }

  componentDidMount() {
    this.show()
    $.get(`/app/entity/approval/fetch-workedsteps?record=${this.props.id}`, (res) => {
      if (!res.data || res.data.length === 0) {
        RbHighbar.create($L('未查询到流程详情'))
        this.hide()
        this.__noStepFound = true
      } else {
        this.setState({ steps: res.data })
      }
    })
  }

  hide = () => $(this._dlg).modal('hide')
  show = () => {
    if (this.__noStepFound === true) {
      RbHighbar.create($L('未查询到流程详情'))
      this.hide()
    } else {
      $(this._dlg).modal({ show: true, keyboard: true })
    }
  }
}

// 刷新页面
const _reloadAndTips = function (dlg, msg) {
  dlg && dlg.hide(true)
  msg && RbHighbar.success(msg)

  // 保持当前视图
  const keepViewId = dlg && parent && parent.RbViewModal && parent.RbViewModal.mode === 2 ? dlg.props.id : null

  setTimeout(() => {
    if (window.RbViewPage) window.RbViewPage.reload()
    if (window.RbListPage) window.RbListPage.reload()
    else if (parent.RbListPage) parent.RbListPage.reload(keepViewId)
  }, 1000)
}
