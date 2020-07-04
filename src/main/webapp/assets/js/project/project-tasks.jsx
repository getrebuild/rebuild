/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global TaskViewer */

const wpc = window.__PageConfig

$(document).ready(() => {
  $('.side-toggle').click(() => $('.rb-aside').toggleClass('rb-aside-collapsed'))
  const $content = $('.page-aside .tab-content')
  const $boxes = $('#plan-boxes')
  $addResizeHandler(() => {
    const wh = $(window).height()
    $content.height(wh - 147)
    $content.perfectScrollbar('update')
    $boxes.height(wh - 128)
  })()

  renderRbcomp(<PlanBoxes plans={wpc.projectPlans} />, 'plan-boxes')

  let viewHash = location.hash
  if (viewHash && viewHash.startsWith('#!/View/ProjectTask/')) {
    viewHash = viewHash.split('/')
    if (viewHash.length === 4 && viewHash[3].length === 20) {
      setTimeout(() => renderRbcomp(<TaskViewer id={viewHash[3]} />), 500)
    }
  }
})

// 面板组件引用
const __PlanRefs = {}
// 任务组件引用
const __TaskRefs = {}

// 任务面板列表
class PlanBoxes extends React.Component {

  constructor(props) {
    super(props)
  }

  render() {
    return (
      <React.Fragment>
        {this.props.plans.map((item) => {
          return <PlanBox key={`plan-${item.id}`} id={item.id} planName={item.planName} />
        })}
      </React.Fragment>
    )
  }

  componentDidMount() {
    $('.J_project-load').remove()
    __boxesDrag()

    // 拖动排序&换面板
    $('.task-list').sortable({
      connectWith: '.task-list',
      containment: document.body,
      helper: 'clone',
      zIndex: 1999,
      appendTo: '#plan-boxes',
      items: '>.task-card',
      placeholder: 'task-card highlight',
      start: function (event, ui) {
        ui.placeholder.height(ui.helper.height())
      },
      update: function (event, ui) {
        const prevSeq = ~~(ui.item.prev('.task-card').attr('data-seq') || 0)
        const nextSeq = ~~(ui.item.next('.task-card').attr('data-seq') || -1)
        let seq = ~~(prevSeq + (nextSeq - prevSeq) / 2)
        // At last
        if (nextSeq === -1) seq = -1

        let newPlanId = ui.item.parent('.task-list').attr('data-planid')
        const oldPlanId = ui.item.attr('data-planid')
        if (newPlanId === oldPlanId) {
          newPlanId = undefined
        }

        const taskid = ui.item.data('taskid')
        __taskPost(taskid, { seq: seq, projectPlanId: newPlanId }, () => {
          __TaskRefs[taskid].refresh(newPlanId)
          if (newPlanId) {
            __PlanRefs[oldPlanId].refresh()
            __PlanRefs[newPlanId].refresh()
          }
        })
      }
    }).disableSelection()
  }
}

// 任务面板
class PlanBox extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="plan-box-wrapper">
        <div className="plan-box">
          <div className="plan-header">
            <h5 className="plan-title">{this.props.planName}<small> · {this.state.taskNum || 0}</small></h5>
          </div>
          <div className="task-list rb-scroller" ref={(c) => this._scroller = c} data-planid={this.props.id}>
            {(this.state.tasks || []).map((item) => {
              return <Task data={item} planid={this.props.id} key={`task-${item.id}`} />
            })}
            {this.state.taskNum === 0 && <div className="no-tasks">暂无任务</div>}
          </div>
          {this.state.newMode ?
            <div className="task-card newtask">
              <div className="task-card-body">
                <div>
                  <textarea className="form-control form-control-sm row2x" placeholder="输入标题以新建任务" ref={(c) => this._taskName = c}
                    onKeyDown={(e) => e.keyCode === 13 && this._handleCreateTask()}
                    autoFocus />
                </div>
                <div>
                  <label className="mb-1">选择负责人</label>
                  <UserSelector hideDepartment={true} hideRole={true} hideTeam={true} multiple={false} closeOnSelect={true} ref={(c) => this._executor = c} />
                </div>
                <div>
                  <label className="mb-1">选择参与人</label>
                  <UserSelector ref={(c) => this._partners = c} />
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
          }
        </div>
      </div >
    )
  }

  componentDidMount() {
    __PlanRefs[this.props.id] = this
    this.loadTasks()

    const $scroller = $(this._scroller).perfectScrollbar()
    $addResizeHandler(() => {
      $scroller.css({ 'max-height': $(window).height() - 230 })
      $scroller.perfectScrollbar('update')
    })()
  }

  // 加载任务列表
  loadTasks() {
    $.get(`/project/tasks/list?plan=${this.props.id}`, (res) => {
      if (res.error_code === 0) {
        this.setState({ tasks: [], taskNum: 0 },
          () => this.setState({ tasks: res.data.tasks, taskNum: res.data.count }))
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }

  refresh() {
    const $scroller = $(this._scroller).perfectScrollbar('update')
    this.setState({ taskNum: $scroller.find('.task-card').length })
  }

  _handleAddTask() {
    this.setState({ newMode: true }, () => {
      const $boxes = $('#plan-boxes')
      $boxes.animate({ scrollTop: $boxes.height() - 100 }, 400)
    })
  }

  _handleCreateTask() {
    const _data = {
      taskName: $val(this._taskName),
      executor: this._executor.val().join(','),
      partners: this._partners.val().join(','),
      projectId: wpc.id,
      projectPlanId: this.props.id,
      taskNumber: 0
    }
    _data.metadata = { entity: 'ProjectTask' }

    const $btn = $(this._btn).button('loading')
    $.post('/project/tasks/post', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        this.setState({ newMode: false }, () => this.loadTasks())
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
    const data = this.state.data
    return (
      <div className={`task-card content status-${data.status}`}
        data-seq={data.seq} data-taskid={data.id} data-planid={this.state.planid} onClick={() => this._openView()}>
        <div className="task-card-body">
          <div className="task-content-wrapper">
            <div className="task-status">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline" onClick={(e) => $stopEvent(e)}>
                <input className="custom-control-input" type="checkbox" defaultChecked={data.status === 1} onChange={(e) => this._toggleStatus(e)} />
                <span className="custom-control-label"></span>
              </label>
            </div>
            <div className="task-content">
              <div className="task-title text-wrap">{data.taskName}</div>
              <div className="task-time">创建于 <span title={data.createdOn}>{$fromNow(data.createdOn)}</span></div>
              {data.endTime &&
                <div className="task-time">完成于 <span title={data.endTime}>{$fromNow(data.endTime)}</span></div>}
              <div className="task-more">
                {data.executor && (
                  <a className="avatar float-left" title={`负责人 ${data.executor[1]}`}>
                    <img src={`${rb.baseUrl}/account/user-avatar/${data.executor[0]}`} />
                  </a>
                )}
                <span className="badge float-right">{data.taskNumber}</span>
                <div className="clearfix"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    __TaskRefs[this.props.data.id] = this
  }

  refresh(newPlanId) {
    $.get(`/project/tasks/get?task=${this.props.data.id}`, (res) => {
      this.setState({ data: res.data })
      if (newPlanId) this.setState({ planid: newPlanId })
    })
  }

  _toggleStatus(e) {
    __taskPost(this.props.data.id, { status: e.currentTarget.checked ? 1 : 0 }, () => {
      __PlanRefs[this.props.planid].loadTasks()
    })
  }

  _openView() {
    renderRbcomp(<TaskViewer id={this.props.data.id} />)
  }
}

// 修改任务信息
const __taskPost = function (id, data, call) {
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