/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

let _principal
let _members
let _planList

$(document).ready(() => {
  renderRbcomp(<UserSelector hideDepartment={true} hideRole={true} hideTeam={true} multiple={false} closeOnSelect={true} />, 'principal',
    function () {
      _principal = this
      _initUserComp(wpc.principal, this)
    })
  renderRbcomp(<UserSelector />, 'members',
    function () {
      _members = this
      _initUserComp(wpc.members, this)
    })

  renderRbcomp(<PlanList />, 'plans', function () { _planList = this })
  $('.J_add-plan').click(() => renderRbcomp(<PlanEdit projectId={wpc.id} />))

  const $btn = $('.J_save').click(() => {
    const _data = {
      principal: _principal.val().join(','),
      members: _members.val().join(',')
    }
    _data.metadata = { id: wpc.id }
    _data._sorts = _planList.sortsVal().join('>')

    $btn.button('loading')
    $.post('/admin/projects/updates', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) location.href = '../projects'
      else RbHighbar.error(res.error_msg)
      $btn.button('reset')
    })
  })
})

const _initUserComp = function (users, comp) {
  if (users) {
    $.post('/commons/search/user-selector', JSON.stringify(users.split(',')), (res) => {
      if (res.error_code === 0 && res.data.length > 0) comp.setState({ selected: res.data })
    })
  }
}

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
            <div className="card" key={`plan-${item[0]}`} data-id={item[0]}>
              <div className="card-body">
                <h5 className="text-truncate text-bold">{item[1]}</h5>
              </div>
              <div className="card-footer card-footer-contrast">
                <a onClick={() => this._handleEdit(item)}><i className="zmdi zmdi-edit"></i></a>
                <a onClick={() => this._handleDelete(item[0])} className="danger"><i className="zmdi zmdi-delete"></i></a>
              </div>
            </div>
          )
        })}
      </React.Fragment>
    )
  }

  componentDidMount() {
    $.get(`/admin/projects/plan-list?project=${wpc.id}`, (res) => {
      this.setState({ plans: res.data }, () => {
        $('#plans').sortable({
          handle: '.card-body',
          axis: 'x',
        }).disableSelection()

        const s = []
        $('#plans .card').each(function () { s.push($(this).data('id')) })
        this._sortsOld = s
      })
    })
  }

  _handleEdit(item) {
    renderRbcomp(<PlanEdit id={item[0]} planName={item[1]} />)
  }

  _handleDelete(planId) {
    RbAlert.create('只有空面板才能被删除。确认删除吗？', {
      type: 'danger',
      confirmText: '删除',
      confirm: function () {
        this.disabled(true)
        $.post(`/app/entity/record-delete?id=${planId}`, (res) => {
          if (res.error_code === 0) {
            RbHighbar.success('面板已删除')
            setTimeout(() => location.reload(), 500)
          } else RbHighbar.error(res.error_msg)
        })
      }
    })
  }

  sortsVal() {
    const s = []
    $('#plans .card').each(function () { s.push($(this).data('id')) })
    if (s.join(',') === this._sortsOld.join(',')) return []
    else return s
  }
}

// 编辑面板
class PlanEdit extends RbFormHandler {

  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    return (
      <RbModal title={`${this.props.id ? '修改' : '添加'}面板`} ref={(c) => this._dlg = c}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">面板名称</label>
            <div className="col-sm-7">
              <input className="form-control form-control-sm" value={this.state.planName || ''} data-id="planName" onChange={this.handleChange} maxLength="60" autoFocus />
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => this._btns = c}>
              <button className="btn btn-primary" type="button" onClick={this.save}>确定</button>
              <a className="btn btn-link" onClick={this.hide}>取消</a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  save = (e) => {
    e.preventDefault()
    if (!this.state.planName) return RbHighbar.create('请输入面板名称')

    const _data = {
      planName: this.state.planName
    }
    if (!this.props.id) {
      _data.projectId = this.props.projectId
      _data.seq = _planList.sortVal().length + 1
    }
    _data.metadata = { entity: 'ProjectPlanConfig', id: this.props.id || null }

    this.disabled(true)
    $.post('/app/entity/record-save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        RbHighbar.success('面板已添加')
        setTimeout(() => location.reload(), 500)
      } else RbHighbar.error(res.error_msg)
    })
  }
}