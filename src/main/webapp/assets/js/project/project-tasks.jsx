/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

$(document).ready(() => {
  // $('.side-toggle').click(() => $('.rb-aside').toggleClass('rb-aside-collapsed'))

  renderRbcomp(<PlanBoxes plans={wpc.projectPlans} />, 'plan-boxes', () => {
    __boxesDrag()
    $('.J_project-load').remove()
  })

  let viewHash = location.hash
  if (viewHash && viewHash.startsWith('#!/View/ProjectTask/')) {
    viewHash = viewHash.split('/')
    if (viewHash.length === 4 && viewHash[3].length === 20) {
      setTimeout(() => TaskViewModal.create(viewHash[3]), 500)
    }
  }
})

// 面板组件引用
const __PlanRefs = {}
// 任务组件引用
const __TaskRefs = {}

// 任务面板列表
class PlanBoxes extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <React.Fragment>
        {this.props.plans.map((item) => {
          return <PlanBox key={`plan-${item.id}`} id={item.id} planName={item.planName} flowStatus={item.flowStatus} flowNexts={item.flowNexts} />
        })}
      </React.Fragment>
    )
  }

  componentDidMount() {
    let startState = 0
    // 拖动排序&换面板
    $('.task-list').sortable({
      connectWith: '.task-list',
      containment: document.body,
      helper: 'clone',
      zIndex: 1999,
      appendTo: '#plan-boxes',
      items: '>.task-card',
      placeholder: 'task-card highlight',
      revert: false,
      start: function (event, ui) {
        ui.placeholder.height(ui.helper.height())
        startState = 1
      },
      update: function (event, ui) {
        if (startState !== 1) return
        startState = 0

        const $item = ui.item
        const $itemPrev = $item.prev('.task-card')
        const $itemNext = $item.next('.task-card')

        const planidOld = $item.attr('data-planid')
        let planidNew = $item.parent('.task-list').attr('data-planid')
        // Plan unchange
        if (planidOld === planidNew) planidNew = undefined
        console.log('Trigger ... ', $item.data('taskid'), planidOld + ' > ' + planidNew)

        // 面板变化
        if (planidNew) {
          const flowNexts = wpc.projectPlans.find(item => item.id === planidOld).flowNexts
          if (!flowNexts.includes(planidNew)) {
            $(this).sortable('cancel')
            RbHighbar.create('不允许流转到此面板')
            return
          }
        }

        const taskid = $item.data('taskid')
        const prevTaskId = $itemPrev.attr('data-taskid')

        const prevSeq = ~~($itemPrev.attr('data-seq') || 0)
        const nextSeq = ~~($itemNext.attr('data-seq') || -1)  // At last
        const seq = nextSeq === -1 ? -1 : ~~(prevSeq + (nextSeq - prevSeq) / 2)

        // Use state of react for move
        let $itemholder
        if (planidNew) {
          $itemholder = $item.clone()
          $item.hide()
          $item.after($itemholder)
          $(this).sortable('cancel')
        }

        __saveTask(taskid, { seq: seq, projectPlanId: planidNew }, () => {
          if (planidNew) {
            const taskData = __PlanRefs[planidOld].removeTask(taskid)
            if (taskData) __PlanRefs[planidNew].addTask(taskData, prevTaskId, $itemholder)
            else console.error('No taskData : ', taskid)
          } else {
            __TaskRefs[taskid].refresh()
          }
        })
      }

    }).disableSelection()
  }
}

// 任务面板
class PlanBox extends React.Component {
  state = { ...this.props }
  creatableTask = this.props.flowStatus === 1 || this.props.flowStatus === 2

  render() {
    return (
      <div className="plan-box-wrapper">
        <div className="plan-box">
          <div className="plan-header">
            <h5 className="plan-title">{this.props.planName}<small> · {this.state.taskNum || 0}</small></h5>
          </div>
          <div className="task-list rb-scroller" ref={(c) => this._scroller = c} data-planid={this.props.id}>
            {(this.state.tasks || []).map((item) => {
              return <Task key={`task-${item.id}`} planid={this.props.id} $$$parent={this} {...item} />
            })}
            {this.state.taskNum === 0 && <div className="no-tasks">暂无任务</div>}
          </div>
          {this.creatableTask && (
            this.state.newMode ?
              <div className="task-card newtask">
                <div className="task-card-body">
                  <div>
                    <textarea className="form-control form-control-sm row2x" placeholder="输入标题以新建任务" ref={(c) => this._taskName = c}
                      onKeyDown={(e) => e.keyCode === 13 && this._handleCreateTask()}
                      autoFocus />
                  </div>
                  <div>
                    <label className="mb-1">执行人</label>
                    <UserSelector hideDepartment={true} hideRole={true} hideTeam={true} multiple={false} closeOnSelect={true} ref={(c) => this._executor = c} />
                  </div>
                  <div>
                    <label className="mb-1">截至时间</label>
                    <div>
                      <input type="text" className="form-control form-control-sm" ref={(c) => this._deadline = c} />
                    </div>
                  </div>
                  <div className="text-right">
                    <button className="btn btn-link w-auto" type="button" onClick={() => this.setState({ newMode: false })}>取消</button>
                    <button className="btn btn-primary" type="button" ref={(c) => this._btn = c} onClick={() => this._handleCreateTask()}>确定</button>
                  </div>
                </div>
              </div>
              :
              <div className="task-card newbtn" onClick={() => this._handleAddTask()}>
                <i className="zmdi zmdi-plus"></i>
              </div>
          )}
        </div>
      </div >
    )
  }

  componentDidMount() {
    __PlanRefs[this.props.id] = this
    this.refreshTasks()

    const $scroller = $(this._scroller).perfectScrollbar()
    $addResizeHandler(() => {
      $scroller.css({ 'max-height': $(window).height() - 228 + (this.creatableTask ? 0 : 44) })
      $scroller.perfectScrollbar('update')
    })()
  }

  // 加载任务列表
  refreshTasks() {
    $.get(`/project/tasks/list?plan=${this.props.id}`, (res) => {
      if (res.error_code === 0) this.setState({ tasks: res.data.tasks, taskNum: res.data.count })
      else RbHighbar.error(res.error_msg)
    })
  }

  removeTask(taskid) {
    const taskData = this.state.tasks.find(item => item.id === taskid)
    const ns = this.state.tasks.filter(item => item.id !== taskid)
    this.setState({ tasks: ns, taskNum: ns.length },
      () => $(this._scroller).perfectScrollbar('update'))
    return taskData
  }

  addTask(taskData, prevTaskId, $itemholder) {
    console.log('Add Task', prevTaskId, $itemholder, taskData)
    const ns = []
    if (prevTaskId) {
      this.state.tasks.forEach((item) => {
        ns.push(item)
        if (item.id === prevTaskId) ns.push(taskData)
      })
    } else {
      // At top
      ns.push(taskData)
      this.state.tasks.forEach(item => ns.push(item))
    }

    this.setState({ tasks: ns, taskNum: ns.length }, () => {
      $itemholder && $itemholder.remove()
      $(this._scroller).perfectScrollbar('update')
    })
  }

  _handleAddTask() {
    this.setState({ newMode: true }, () => {
      $('html').animate({ scrollTop: 500 }, 200)

      if (this.__datetimepicker) this.__datetimepicker.datetimepicker('remove')
      this.__datetimepicker = $(this._deadline).datetimepicker({
        startDate: new Date()
      })
    })
  }

  _handleCreateTask() {
    const _data = {
      taskName: $val(this._taskName),
      executor: this._executor.val().join(','),
      deadline: this._deadline.value,
      projectId: wpc.id,
      projectPlanId: this.props.id,
      taskNumber: 0
    }
    if (!_data.taskName) return RbHighbar.create('请输入任务标题')
    if (_data.deadline) _data.deadline += ':00'
    _data.metadata = { entity: 'ProjectTask' }

    const $btn = $(this._btn).button('loading')
    $.post('/project/tasks/post', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        this.setState({ newMode: false }, () => this.refreshTasks())
      } else {
        RbHighbar.error(res.error_msg)
        $btn.button('reset')
      }
    })
  }
}

// 任务
class Task extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className={`task-card content status-${this.state.status} priority-${this.state.priority}`}
        data-seq={this.state.seq} data-taskid={this.state.id} data-planid={this.props.planid} onClick={() => TaskViewModal.create(this.state.id)}>
        <div className="task-card-body">
          <div className="task-content-wrapper">
            <div className="task-status">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline" onClick={(e) => $stopEvent(e)}>
                <input className="custom-control-input" type="checkbox" defaultChecked={this.state.status === 1} onChange={(e) => this._toggleStatus(e)} />
                <span className="custom-control-label"></span>
              </label>
            </div>
            <div className="task-content">
              <div className="task-title text-wrap">{this.state.taskName}</div>
              {this.state.endTime
                && <div className="task-time">完成于 <span title={this.state.endTime}>{$fromNow(this.state.endTime)}</span></div>}
              <div className="task-time">创建于 <span title={this.state.createdOn}>{$fromNow(this.state.createdOn)}</span></div>
              {(!this.state.endTime && this.state.deadline)
                && (
                  <div className="task-time">
                    <span className={`badge badge-${this._outDeadline(this.state.deadline) ? 'danger' : 'primary'}`}>
                      截止时间 <span title={this.state.deadline}>{$fromNow(this.state.deadline)}</span>
                    </span>
                  </div>
                )}
              <div className="task-extras">
                {this.state.executor && (
                  <a className="avatar float-left" title={`负责人 ${this.state.executor[1]}`}>
                    <img src={`${rb.baseUrl}/account/user-avatar/${this.state.executor[0]}`} />
                  </a>
                )}
                <span className="badge float-right">{this.state.taskNumber}</span>
                <div className="clearfix"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    __TaskRefs[this.props.id] = this
  }

  refresh() {
    $.get(`/project/tasks/get?task=${this.props.id}`, res => this.setState({ ...res.data }))
  }

  _toggleStatus(e) {
    const status = e.currentTarget.checked ? 1 : 0
    __saveTask(this.props.id, { status: status }, () => {
      this.setState({ status: status })
      __PlanRefs[this.props.planid].refreshTasks()
    })
  }

  _outDeadline(date) {
    return moment(date.split(' ')[0]).isBefore(moment())
  }
}

// 保存任务
const __saveTask = function (id, data, call) {
  data.metadata = { id: id }
  $.post('/project/tasks/post', JSON.stringify(data), (res) => {
    if (res.error_code !== 0) RbHighbar.error(res.error_msg)
    else typeof call === 'function' && call()
  })
}

// 面板拖动
const __boxesDrag = function () {
  let flag
  let downX
  let scrollLeft
  const $boxes = $('#plan-boxes')

  $boxes.on('mousedown', function (event) {
    flag = true
    downX = event.clientX
    scrollLeft = $boxes.scrollLeft()
    $boxes.addClass('move')
  })
  $boxes.on('mousemove', function (event) {
    if (flag) {
      const moveX = event.clientX
      const scrollX = moveX - downX
      $($boxes).scrollLeft(scrollLeft - scrollX)
    }
  })

  $boxes.on('mouseup', function () {
    flag = false
  })
  $boxes.on('mouseout', function () {
    flag = false
  })
}

// --

// 与实体视图兼容
// eslint-disable-next-line no-unused-vars
const RbViewModal = {
  // 获取当前视图
  currentHolder: () => {
    return {
      hideLoading: function () {
        TaskViewModal.__HOLDER && TaskViewModal.__HOLDER.setLoadingState(false)
      }
    }
  }
}

// 任务视图
class TaskViewModal extends React.Component {
  state = { ...this.props, inLoad: true }

  render() {
    return (
      <div className="modal rbview task" ref={(c) => this._dlg = c}>
        <div className="modal-dialog">
          <div className="modal-content">
            <div className={'modal-body iframe rb-loading ' + (this.state.inLoad === true && 'rb-loading-active')}>
              <iframe ref={(c) => this._iframe = c} className={this.state.isHide ? 'invisible' : ''} src={`${rb.baseUrl}/project/task/${this.state.taskid}`} frameBorder="0" scrolling="no"></iframe>
              <RbSpinner />
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const $dlg = $(this._dlg)
    $dlg.on('shown.bs.modal', () => {
      $dlg.find('.modal-content').css('margin-right', 0)
      location.hash = '#!/View/ProjectTask/' + this.props.taskid
    }).on('hidden.bs.modal', () => {
      $dlg.find('.modal-content').css('margin-right', -1000)
      location.hash = '#!/View/'
    })

    $dlg.modal({ show: true })
  }

  setLoadingState(state) {
    state = state === true
    this.setState({ inLoad: state, isHide: state })
  }

  hide() {
    $(this._dlg).modal('hide')
  }

  refreshTask(planChanged) {
    const ref = __TaskRefs[this.state.taskid]
    if (planChanged) {
      ref.props.$$$parent.refreshTasks()
      __PlanRefs[planChanged] && __PlanRefs[planChanged].refreshTasks()
    } else {
      ref.refresh()
    }
  }

  // --

  static __HOLDER
  /**
   * @param {TaskId} id 
   */
  static create(id) {
    renderRbcomp(<TaskViewModal taskid={id} />, null, function () { TaskViewModal.__HOLDER = this })
  }
}