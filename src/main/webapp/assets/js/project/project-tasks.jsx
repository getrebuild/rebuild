/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

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

  dragMoveX()
})

// 任务面板列表
class PlanBoxes extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <React.Fragment>
        {this.props.plans.map((item) => {
          return <PlanBox key={`plan-${item.id}`} id={item.id} planName={item.planName} />
        })}
      </React.Fragment>
    )
  }
}

// 任务面板
class PlanBox extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="plan-box-wrapper">
        <div className="plan-header">
          <h5 className="plan-title">{this.props.planName}<small> · {this.state.taskNum || 0}</small></h5>
        </div>
        <div className="task-list rb-scroller" ref={(c) => this._scroller = c}>
          {(this.state.tasks || []).map((item) => {
            return <Task data={item} key={`task-${item[2]}`} />
          })}
        </div>
        {this.state.newMode ?
          <div className="task-card newtask">
            <div className="task-card-body">
              <div>
                <textarea className="form-control form-control-sm row2x" placeholder="输入标题以新建任务" ref={(c) => this._taskName = c} autoFocus></textarea>
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
                <button className="btn btn-primary" type="button" ref={(c) => this._btn = c} onClick={() => this._handleAddTask()}>确定</button>
              </div>
            </div>
          </div>
          :
          <div className="task-card newbtn" onClick={() => this.setState({ newMode: true })}>
            <i className="zmdi zmdi-plus"></i>
          </div>
        }
      </div>
    )
  }

  componentDidMount() {
    this._loadTasks()

    const $scroller = $(this._scroller).perfectScrollbar()
    $addResizeHandler(() => {
      $scroller.css({ 'max-height': $(window).height() - 230 })
      $scroller.perfectScrollbar('update')
    })()
  }

  _loadTasks() {
    $.get(`/project/tasks/list?plan=${this.props.id}`, (res) => {
      if (res.error_code === 0) this.setState({ tasks: res.data.tasks, taskNum: res.data.count })
      else RbHighbar.error(res.error_msg)
    })
  }

  _handleAddTask() {
    const _data = {
      taskName: $val(this._taskName),
      executor: this._executor.val().join(','),
      partners: this._partners.val().join(','),
      projectId: wpc.id,
      projectPlanId: this.props.id,
      taskNumber: 0
    }
    _data.metadata = { entity: 'ProjectTask' }
    console.log(_data)

    $(this._btn).button('loading')
    $.post('/project/tasks/post', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        this.setState({ newMode: false })
        this._loadTasks()
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

// 任务
class Task extends React.Component {
  state = { ...this.props }

  render() {
    const data = this.props.data
    return (
      <div className="task-card content">
        <div className="task-card-body">
          <div className="task-content-wrapper">
            <div className="task-status">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline">
                <input className="custom-control-input" type="checkbox" onChange={(e) => this._toggleStatus(e)} />
                <span className="custom-control-label"></span>
              </label>
            </div>
            <div className="task-content">
              <div className="task-title text-wrap">{data[3]}</div>
              <div className="task-time">创建于 {data[4]}</div>
              <div className="task-more">
                {data[6] && <a className="avatar float-left" title={data[6][1]}><img src={`${rb.baseUrl}/account/user-avatar/${data[6][0]}`} /></a>}
                <span className="badge float-right">{data[1]}</span>
                <div className="clearfix"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  _toggleStatus(e) {
  }

}

// 内容拖动
function dragMoveX() {
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
    $boxes.removeClass('move')
  })
  $boxes.on('mouseout', function () {
    flag = false
    $boxes.removeClass('move')
  })
}