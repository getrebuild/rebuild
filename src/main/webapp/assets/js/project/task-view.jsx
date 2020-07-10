/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

const activeTaskView = window.parent && window.parent.activeTaskView ? window.parent.activeTaskView
  : { hide: () => window.close(), setLoadingState: () => { /* NOOP */ }, refreshTask: () => { /* NOOP */ } }

$(document).ready(() => {
  renderRbcomp(<TaskContent id={wpc.taskId} />, 'task-contents')
  renderRbcomp(<TaskComments id={wpc.taskId} />, 'task-comments')

  $('.J_close').click(() => activeTaskView.hide())
  $('.J_reload').click(() => {
    activeTaskView.setLoadingState(true)
    location.reload()
  })
})

const __PRIORITIES = { 0: '较低', 1: '普通', 2: '紧急', 3: '非常紧急' }

// 任务详情
class TaskContent extends React.Component {
  state = { ...this.props, priority: 1 }

  render() {
    const stateOfPlans = this.state.stateOfPlans || []
    return (
      <div className="task-form">
        <div className="form-group row pt-0">
          <div className="col-12">
            <input type="text" className="form-control task-title" name="taskName" value={this.state.taskName || ''}
              onChange={(e) => this._handleChange(e, true)} onBlur={(e) => this._handleChange(e)} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">状态</label>
          <div className="col-12 col-sm-9">
            <div className="form-control-plaintext">
              <a className="tag-value arrow plaintext" data-toggle="dropdown">{this.state.projectPlanId ? stateOfPlans.find(x => x.id === this.state.projectPlanId).text : null}</a>
              <div className="dropdown-menu">
                {stateOfPlans.map((item) => {
                  return <a key={`plan-${item.id}`} className="dropdown-item" onClick={() => this._handleChangePlan(item.id)}>{item.text}</a>
                })}
              </div>
            </div>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">执行者</label>
          <div className="col-12 col-sm-9">
            <UserSelector hideDepartment={true} hideRole={true} hideTeam={true} multiple={false} closeOnSelect={true} ref={(c) => this._executor = c} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">时间</label>
          <div className="col-12 col-sm-9">
            <div className="input-group input-group-sm">
              <input type="text" className="form-control form-control-sm" placeholder="开始时间" />
              <div className="input-group-prepend input-group-append">
                <span className="input-group-text">至</span>
              </div>
              <input type="text" className="form-control form-control-sm" placeholder="截至时间" />
            </div>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">备注</label>
          <div className="col-12 col-sm-9">
            <textarea className="form-control form-control-sm row3x"></textarea>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">优先级</label>
          <div className="col-12 col-sm-9">
            <div className="form-control-plaintext">
              <a className={`tag-value arrow priority-${this.state.priority}`} data-toggle="dropdown">{__PRIORITIES[this.state.priority]}</a>
              <div className="dropdown-menu">
                <a className="dropdown-item text-muted" onClick={() => this._handleChangePriority(0)}>较低</a>
                <a className="dropdown-item text-primary" onClick={() => this._handleChangePriority(1)}>普通</a>
                <a className="dropdown-item text-warning" onClick={() => this._handleChangePriority(2)}>紧急</a>
                <a className="dropdown-item text-danger" onClick={() => this._handleChangePriority(3)}>非常紧急</a>
              </div>
            </div>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">标签</label>
          <div className="col-12 col-sm-9">
            <div className="form-control-plaintext tags">
              {(this.state.tags || []).map((item) => {
                return <span className="tag-value" key={`tag-${item.id}`} data-id={item.id}>{item.text}</span>
              })}
              <a className="tag-value">+ 添加标签</a>
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    this.fetch()
    activeTaskView.setLoadingState(false)
  }

  fetch() {
    $.get(`/project/tasks/detail?task=${this.props.id}`, (res) => {
      if (res.error_code === 0) this.setState({ ...res.data })
      else RbHighbar.error(res.error_msg)
    })
  }

  // 即时保存

  _handleChange(e, unsave) {
    const name = e.target.name
    const value = e.target.value
    this.setState({ [name]: value })

    if (unsave) return

    const data = {
      [name]: value,
      metadata: { id: this.props.id }
    }
    $.post('/project/tasks/post', JSON.stringify(data), (res) => {
      if (res.error_code === 0) activeTaskView.refreshTask()
      else RbHighbar.error(res.error_msg)
    })
  }
  _handleChangePlan = (val) => this._handleChange({ target: { name: 'projectPlanId', value: val } })
  _handleChangePriority = (val) => this._handleChange({ target: { name: 'priority', value: val } })

}

// 任务评论
class TaskComments extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div>
        评论区
      </div>
    )
  }
}
