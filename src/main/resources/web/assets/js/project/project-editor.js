/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

let _Principal
let _Members
let _PlanList

$(document).ready(() => {
  renderRbcomp(<UserSelector hideDepartment={true} hideRole={true} hideTeam={true} multiple={false} defaultValue={wpc.principal} />, 'principal', function () {
    _Principal = this
  })
  renderRbcomp(<UserSelector defaultValue={wpc.members} />, 'members', function () {
    _Members = this
  })

  if (wpc.scope === 2) $('#scope_2').attr('checked', true)

  renderRbcomp(<PlanList />, 'plans', function () {
    _PlanList = this
  })
  $('.J_add-plan').click(() => renderRbcomp(<PlanEdit projectId={wpc.id} flowNexts={_PlanList.getPlans()} seq={_PlanList.getMaxSeq() + 1000} />))

  const $btn = $('.J_save').click(() => {
    const data = {
      scope: $('#scope_2').prop('checked') ? 2 : 1,
      principal: _Principal.val().join(','),
      members: _Members.val().join(','),
      metadata: { id: wpc.id },
    }
    if (!data.members) return RbHighbar.create($L('PlsSelectSome,Members'))

    $btn.button('loading')
    $.post('/admin/projects/post', JSON.stringify(data), (res) => {
      if (res.error_code === 0) location.href = '../projects'
      else RbHighbar.error(res.error_msg)
      $btn.button('reset')
    })
  })
})

// 面板列表
class PlanList extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    return (
      <React.Fragment>
        {(this.state.plans || []).map((item) => {
          return (
            <div className="card" key={`plan-${item[0]}`} data-id={item[0]} data-seq={item[4]}>
              <div className="card-body">
                <h5 className="text-truncate text-bold">{item[1]}</h5>
              </div>
              <div className="card-footer card-footer-contrast">
                <a onClick={() => this._handleEdit(item)}>
                  <i className="zmdi zmdi-edit"></i>
                </a>
                <a onClick={() => this._handleDelete(item[0])} className="danger">
                  <i className="zmdi zmdi-delete"></i>
                </a>
              </div>
            </div>
          )
        })}
        {this.state.plans && this.state.plans.length === 0 && <p className="text-muted m-3">{$L('PlsAddSome,ProjectPlan')}</p>}
      </React.Fragment>
    )
  }

  componentDidMount() {
    this.loadPlans()
  }

  loadPlans() {
    const that = this
    $.get(`/admin/projects/plan-list?project=${wpc.id}`, (res) => {
      this.setState({ plans: res.data }, () => {
        $('#plans')
          .sortable({
            handle: '.card-body',
            axis: 'x',
            update: function (event, ui) {
              const prevSeq = ~~(ui.item.prev('.card').attr('data-seq') || 0)
              const nextSeq = ~~(ui.item.next('.card').attr('data-seq') || -1)
              let seq = ~~(prevSeq + (nextSeq - prevSeq) / 2)
              if (nextSeq === -1) seq = that.getMaxSeq() + 1000

              const _data = {
                seq: seq,
                metadata: { id: ui.item.data('id') },
              }
              $.post('/admin/projects/post', JSON.stringify(_data), (res) => {
                if (res.error_code === 0) that.loadPlans()
                else RbHighbar.error(res.error_msg)
              })
            },
          })
          .disableSelection()
      })
    })
  }

  getPlans() {
    return this.state.plans
  }

  getMaxSeq() {
    let seq = 0
    this.state.plans.forEach((item) => {
      if (item[4] > seq) seq = item[4]
    })
    return seq
  }

  _handleEdit(item) {
    const otherPlans = this.state.plans.filter((x) => {
      return x[0] !== item[0]
    })
    renderRbcomp(<PlanEdit id={item[0]} planName={item[1]} flowStatus={item[2]} flowNexts={otherPlans} selectedFlowNexts={(item[3] || '').split(',')} />)
  }

  _handleDelete(planId) {
    const that = this
    RbAlert.create($L('DeletePlanConfirm'), {
      type: 'danger',
      confirmText: $L('Delete'),
      confirm: function () {
        this.disabled(true)
        $.post(`/app/entity/record-delete?id=${planId}`, (res) => {
          this.hide()
          if (res.error_code === 0) {
            that.loadPlans()
            RbHighbar.success($L('SomeDeleted,ProjectPlan'))
          } else RbHighbar.error(res.error_msg)
        })
      },
    })
  }
}

// 编辑面板
class PlanEdit extends RbFormHandler {
  constructor(props) {
    super(props)
    this.state = { flowStatus: 1, ...props }
  }

  render() {
    const selectedFlowNexts = this.props.selectedFlowNexts || []
    return (
      <RbModal title={`${$L(this.props.id ? 'Modify' : 'Add')}${$L('ProjectPlan')}`} ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('PlanName')}</label>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="planName" value={this.state.planName || ''} onChange={this.handleChange} maxLength="60" autoFocus />
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('PlanStatus')}</label>
            <div className="col-sm-7">
              <label className="custom-control custom-control-sm custom-radio mb-1 mt-1">
                <input className="custom-control-input" type="radio" name="flowStatus" value="1" checked={~~this.state.flowStatus === 1} onChange={this.handleChange} />
                <span className="custom-control-label">
                  {$L('PlanStatus1')} <p className="text-muted mb-0 fs-12">{$L('PlanStatus1Tips')}</p>
                </span>
              </label>
              <label className="custom-control custom-control-sm custom-radio mb-1">
                <input className="custom-control-input" type="radio" name="flowStatus" value="2" checked={~~this.state.flowStatus === 2} onChange={this.handleChange} />
                <span className="custom-control-label">
                  {$L('PlanStatus2')} <p className="text-muted mb-0 fs-12">{$L('PlanStatus2Tips')}</p>
                </span>
              </label>
              <label className="custom-control custom-control-sm custom-radio mb-1">
                <input className="custom-control-input" type="radio" name="flowStatus" value="3" checked={~~this.state.flowStatus === 3} onChange={this.handleChange} />
                <span className="custom-control-label">
                  {$L('PlanStatus3')} <p className="text-muted mb-0 fs-12">{$L('PlanStatus3Tips')}</p>
                </span>
              </label>
            </div>
          </div>
          {this.props.flowNexts && this.props.flowNexts.length > 0 && (
            <div className="form-group row pt-0">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('FlowNexts')}</label>
              <div className="col-sm-7 pt-1" ref={(c) => (this._flowNexts = c)}>
                {this.props.flowNexts.map((item) => {
                  return (
                    <label className="custom-control custom-control-sm custom-checkbox custom-control-inline" key={`plan-${item[0]}`}>
                      <input className="custom-control-input" type="checkbox" value={item[0]} defaultChecked={selectedFlowNexts.includes(item[0])} />
                      <span className="custom-control-label">{item[1]}</span>
                    </label>
                  )
                })}
              </div>
            </div>
          )}
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={this.save}>
                {$L('Confirm')}
              </button>
              <a className="btn btn-link" onClick={this.hide}>
                {$L('Cancel')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  save = () => {
    if (!this.state.planName) return RbHighbar.create($L('PlsInputSome,PlanName'))

    const _data = {
      planName: this.state.planName,
      flowStatus: this.state.flowStatus,
    }
    const flowNexts = []
    $(this._flowNexts)
      .find('input:checked')
      .each((idx, item) => flowNexts.push($(item).val()))
    _data.flowNexts = flowNexts.join(',')

    if (!this.props.id) {
      _data.projectId = this.props.projectId
      _data.seq = this.props.seq
    }
    _data.metadata = { entity: 'ProjectPlanConfig', id: this.props.id || null }

    this.disabled(true)
    $.post('/app/entity/record-save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        this.hide()
        RbHighbar.success('面板已保存')
        _PlanList.loadPlans()
      } else {
        RbHighbar.error(res.error_msg)
        this.disabled()
      }
    })
  }
}
