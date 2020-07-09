/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

const activeTaskView = window.parent && window.parent.activeTaskView ? window.parent.activeTaskView
  : { hide: () => window.close(), setLoadingState: () => { /* NOOP */ } }

$(document).ready(() => {
  renderRbcomp(<TaskContent id={wpc.taskId} />, 'task-contents')
  renderRbcomp(<TaskComments id={wpc.taskId} />, 'task-comments')

  $('.J_close').click(() => activeTaskView.hide())
  $('.J_reload').click(() => {
    activeTaskView.setLoadingState(true)
    location.reload()
  })
})

// 任务详情
class TaskContent extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div>
        <div className="form-group row">
          <div className="col-12">
            <input type="text" className="form-control" name="taskName" onChange={(e) => this._handleChange(e)} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">状态</label>
          <div className="col-12 col-sm-9">1</div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">执行者</label>
          <div className="col-12 col-sm-9">2</div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">时间</label>
          <div className="col-12 col-sm-9">3</div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">备注</label>
          <div className="col-12 col-sm-9">4</div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">优先级</label>
          <div className="col-12 col-sm-9">5</div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">标签</label>
          <div className="col-12 col-sm-9">6</div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    activeTaskView.setLoadingState(false)
  }

  _handleChange(e) {
    const name = e.target.name
    const value = e.target.value
    this.setState({ [name]: value })
  }
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
