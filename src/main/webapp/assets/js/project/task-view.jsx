/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global autosize */

const wpc = window.__PageConfig

const __TaskViewer = parent && parent.TaskViewModal ? parent.TaskViewModal.__HOLDER
  : { hide: () => window.close() }

$(document).ready(() => {
  renderRbcomp(<TaskContent id={wpc.taskId} />, 'task-contents')
  // renderRbcomp(<TaskComments id={wpc.taskId} />, 'task-comments')

  $('.J_close').click(() => parent.RbV.hide())
  $('.J_reload').click(() => {
    __TaskViewer.setLoadingState(true)
    location.reload()
  })
})

const __PRIORITIES = { 0: '较低', 1: '普通', 2: '紧急', 3: '非常紧急' }
const __NOTNULL = ['taskName']

// 任务详情
class TaskContent extends React.Component {
  state = { ...this.props, priority: 1 }

  render() {
    const stateOfPlans = this.state.stateOfPlans || []
    return (
      <div className="rbview-form task-form">
        <div className="form-group row pt-0">
          <div className="col-10">
            <input type="text" className="task-title" name="taskName" value={this.state.taskName || ''}
              onChange={(e) => this._handleChange(e, true)} onBlur={(e) => this._handleChange(e)} onKeyDown={(e) => this._enterKey(e)} />
          </div>
          <div className="col-2 text-right">
            <button className="btn btn-secondary" style={{ minWidth: 0, marginTop: 2 }} data-toggle="dropdown">操作 <i className="icon zmdi zmdi-more-vert"></i></button>
            <div className="dropdown-menu dropdown-menu-right">
              <a className="dropdown-item text-muted" onClick={() => this._handleDelete()}>删除</a>
            </div>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label"><i className="icon zmdi zmdi-square-o" /> 状态</label>
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
          <label className="col-12 col-sm-3 col-form-label"><i className="icon zmdi zmdi-account-o" /> 执行人</label>
          <div className="col-12 col-sm-9">
            <React.Fragment>
              {this.state.executor ?
                (
                  <div className="executor-show">
                    <UserShow id={this.state.executor[0]} name={this.state.executor[1]} showName={true} onClick={() => this._UserSelector.openDropdown()} />
                    <a className="close close-circle" onClick={() => this._handleChangeExecutor(null)} title="移除执行人">&times;</a>
                  </div>
                )
                : <div className="form-control-plaintext"><a className="tag-value arrow placeholder" onClick={() => this._UserSelector.openDropdown()}>选择执行人</a></div>
              }
            </React.Fragment>
            <div className="mount">
              <UserSelector hideDepartment={true} hideRole={true} hideTeam={true} hideSelection={true} multiple={false} closeOnSelect={true} onSelectItem={this._handleChangeExecutor} ref={(c) => this._UserSelector = c} />
            </div>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label"><i className="icon zmdi zmdi-time" /> 截至时间</label>
          <div className="col-12 col-sm-9">
            <div className="form-control-plaintext" ref={(c) => this._dates = c}>
              <a className={`tag-value arrow ${this.state.deadline ? 'plaintext' : 'placeholder'}`} name="deadline" title={this.state.deadline}>{$fromNow(this.state.deadline) || '选择截至时间'}</a>
            </div>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label"><i className="icon zmdi zmdi-comment-more" /> 备注</label>
          <div className="col-12 col-sm-9">
            <div >
              <textarea className="task-desc" name="description" value={this.state.description || ''} maxLength="2000" placeholder="添加备注" ref={(c) => this._description = c}
                onChange={(e) => this._handleChange(e, true)} onBlur={(e) => this._handleChange(e)} onKeyDown={(e) => this._enterKey(e)} />
            </div>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label"><i className="icon zmdi zmdi-circle-o" /> 优先级</label>
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
        <div className="form-group row hide">
          <label className="col-12 col-sm-3 col-form-label"><i className="icon zmdi zmdi-label" /> 标签</label>
          <div className="col-12 col-sm-9">
            <div className="form-control-plaintext tags">
              {(this.state.tags || []).map((item) => {
                return <span className="tag-value" key={`tag-${item.id}`} data-id={item.id}>{item.text}</span>
              })}
              <a className="tag-value">+ 添加标签</a>
            </div>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label"><i className="icon zmdi zmdi-attachment-alt pl-1" /> 附件</label>
          <div className="col-12 col-sm-9">
            <div className="form-control-plaintext">
              <input type="file" className="inputfile" id="attachments" ref={(c) => this._attachments = c} data-maxsize="102400000" />
              <label htmlFor="attachments" style={{ padding: 0, border: 0, lineHeight: 1 }}><a className="tag-value">+ 上传</a></label>
            </div>
            <div className="file-field attachments">
              {(this.state.attachments || []).map((item) => {
                const fileName = $fileCutName(item)
                return (
                  <a key={`file-${item}`} className="img-thumbnail" title={fileName} onClick={() => (parent || window).RbPreview.create([item])}>
                    <i className="file-icon" data-type={$fileExtName(fileName)} /><span>{fileName}</span>
                    <b title="删除" onClick={(e) => this._deleteAttachment(item, e)}><span className="zmdi zmdi-delete"></span></b>
                  </a>
                )
              })}
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    __TaskViewer.setLoadingState(false)
    this.fetch()

    $(this._dates).find('.tag-value').datetimepicker({
      startDate: new Date(),
      clearBtn: true,
    }).on('changeDate', (e) => {
      this._handleChange({ target: { name: e.currentTarget.name, value: e.date ? moment(e.date).format('YYYY-MM-DD HH:mm:ss') : null } })
    })

    autosize(this._description)

    $createUploader(this._attachments,
      () => $mp.start(),
      (res) => {
        $mp.end()
        let s = (this.state.attachments || []).join(',')
        s = s.split(',')
        s.push(res.key)
        console.log(s)
        this._handleChange({ target: { name: 'attachments', value: s } })
      })
  }

  fetch() {
    $.get(`/project/tasks/details?task=${this.props.id}`, (res) => {
      if (res.error_code === 0) this.setState({ ...res.data })
      else RbHighbar.error(res.error_msg)
    })
  }

  _handleDelete() {
    const that = this
    RbAlert.create('确认删除此任务吗？', {
      type: 'danger',
      confirmText: '删除',
      confirm: function () {
        this.disabled(true)
        $.post(`/project/tasks/delete?task=${that.props.id}`, (res) => {
          if (res.error_code === 0) {
            this.hide()
            RbHighbar.success('任务已删除')
            __TaskViewer.refreshTask('DELETE')
            __TaskViewer.hide()
          } else RbHighbar.error(res.error_msg)
        })
      }
    })
  }

  // 即时保存

  _handleChange(e, unsave) {
    const name = e.target.name
    const value = e.target.value
    const valueOld = this.state[name]
    if ($same(value, valueOld)) return
    this.setState({ [name]: value })

    if (unsave) return
    if (!value && __NOTNULL.includes(name)) return RbHighbar.create('不允许为空')

    const data = {
      [name]: $.type(value) === 'array' ? value.join(',') : value,
      metadata: { id: this.props.id }
    }
    $.post('/project/tasks/post', JSON.stringify(data), (res) => {
      if (res.error_code === 0) __TaskViewer.refreshTask(name === 'projectPlanId' ? value : null)
      else RbHighbar.error(res.error_msg)
    })
  }
  _handleChangePlan = (val) => this._handleChange({ target: { name: 'projectPlanId', value: val } })
  _handleChangePriority = (val) => this._handleChange({ target: { name: 'priority', value: val } })
  _handleChangeExecutor = (val) => {
    this._handleChange({ target: { name: 'executor', value: val ? val.id : null } })
    this.setState({ executor: val ? [val.id, val.text] : null })
  }

  _deleteAttachment(item, e) {
    $stopEvent(e)
    const that = this
    RbAlert.create('确认删除此附件？', {
      confirm: function () {
        this.hide()
        const s = that.state.attachments.filter(x => x !== item)
        that._handleChange({ target: { name: 'attachments', value: s } })
      }
    })

  }

  _enterKey(e) {
    if (e.keyCode === 13) e.target.blur()
  }
}

// // 任务评论
// class TaskComments extends React.Component {
//   state = { ...this.props }

//   render() {
//     return (
//       <div>
//         评论区
//       </div>
//     )
//   }
// }
