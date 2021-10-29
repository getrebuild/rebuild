/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

let __PlanBoxes
let __AdvFilter

$(document).ready(() => {
  // $('.side-toggle').click(() => $('.rb-aside').toggleClass('rb-aside-collapsed'))
  const gs = $decode($urlp('gs', location.hash))
  if (gs) {
    $('.search-input-gs, .J_search .input-search input').val(gs)
    $('.J_search .indicator-primary').removeClass('hide')
  }

  const readonly = !wpc.isMember || ~~wpc.status === 2
  renderRbcomp(<PlanBoxes plans={wpc.projectPlans} readonly={readonly} search={gs} />, 'plan-boxes', function () {
    __PlanBoxes = this
    __draggable()
    $('.J_project-load').remove()

    // 自动打开
    let viewHash = location.hash
    if (viewHash && viewHash.startsWith('#!/View/ProjectTask/')) {
      viewHash = viewHash.split('/')
      if (viewHash.length === 4 && viewHash[3].length === 20) {
        setTimeout(() => TaskViewModal.create(viewHash[3]), 500)
      }
    }
  })

  // 排序
  $('.J_sorts .dropdown-item').click(function () {
    const $this = $(this)
    $('.J_sorts .btn span').text($this.text())
    __PlanBoxes.setState({ sort: $this.data('sort') })
  })

  // 搜索
  const $search = $('.J_search .input-search')
  const $btn2 = $search.find('.input-group-btn .btn').on('click', () => {
    __PlanBoxes.setState({ search: $search.find('input').val() || null })
  })
  const $input2 = $search.find('input').on('keydown', (e) => {
    e.keyCode === 13 && $btn2.trigger('click')
  })
  $search.find('.btn-input-clear').on('click', () => {
    $input2.val('')
    $btn2.trigger('click')
  })

  $unhideDropdown('.J_search').on({
    'shown.bs.dropdown': function () {
      $search.find('input')[0].focus()
    },
  })

  // 高级查询
  const confirmFilter = function (s) {
    __PlanBoxes.setState({ filter: s })
  }
  $('.J_filter').click(() => {
    if (__AdvFilter) {
      __AdvFilter.show()
    } else {
      renderRbcomp(<AdvFilter title={$L('高级查询')} entity="ProjectTask" inModal canNoFilters confirm={confirmFilter} />, null, function () {
        __AdvFilter = this
      })
    }
  })
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
          return (
            <PlanBox
              key={`plan-${item.id}`}
              readonly={this.props.readonly}
              id={item.id}
              planName={item.planName}
              flowStatus={item.flowStatus}
              flowNexts={item.flowNexts}
              sort={this.state.sort}
              search={this.state.search}
              filter={this.state.filter}
            />
          )
        })}
      </React.Fragment>
    )
  }

  componentDidMount() {
    if (this.props.readonly) return

    let startState = 0
    // 拖动排序&换面板
    $('.task-list')
      .sortable({
        connectWith: '.task-list',
        containment: document.body,
        helper: 'clone',
        zIndex: 1999,
        appendTo: '#plan-boxes',
        items: '>.task-card',
        placeholder: 'task-card highlight',
        revert: false,
        delay: 200,
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

          // 面板变化
          if (planidNew) {
            const flowNexts = wpc.projectPlans.find((item) => item.id === planidOld).flowNexts || []
            if (!flowNexts.includes(planidNew)) {
              $(this).sortable('cancel')
              RbHighbar.create($L('不允许流转到此面板'))
              return
            }
          }

          const taskid = $item.data('taskid')
          const prevTaskId = $itemPrev.attr('data-taskid')

          const prevSeq = ~~($itemPrev.attr('data-seq') || 0) // 0 = At first
          const nextSeq = ~~($itemNext.attr('data-seq') || -1) // -1 = At last
          let seq
          if (nextSeq === -1) seq = -1
          else if (prevSeq === 0) seq = nextSeq / 2
          else seq = ~~(prevSeq + (nextSeq - prevSeq) / 2)

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
            } else {
              __TaskRefs[taskid].refresh()
            }
          })
        },
      })
      .disableSelection()
  }

  componentDidUpdate(prevProps, prevState) {
    if (prevState.sort !== this.state.sort) {
      $('.task-list').sortable('option', 'disabled', this.state.sort !== 'seq')
    }

    if (this.state.search || (this.state.filter && this.state.filter.items.length > 0)) {
      $('.J_search .indicator-primary').removeClass('hide')
    } else {
      $('.J_search .indicator-primary').addClass('hide')
    }
  }
}

// 任务面板
const __DEFAULT_PAGE_SIZE = 40
class PlanBox extends React.Component {
  state = { ...this.props }

  creatableTask = (this.props.flowStatus === 1 || this.props.flowStatus === 3) && !this.props.readonly
  performableTask = (this.props.flowStatus === 1 || this.props.flowStatus === 3) && !this.props.readonly

  pageNo = 0
  pageSize = __DEFAULT_PAGE_SIZE

  render() {
    return (
      <div className="plan-box-wrapper">
        <div className="plan-box">
          <div className="plan-header">
            <h5 className="plan-title">
              {this.props.planName}
              <small> · {this.state.taskNum || 0}</small>
            </h5>
          </div>
          <div className="task-list rb-scroller" ref={(c) => (this._scroller = c)} data-planid={this.props.id}>
            {(this.state.tasks || []).map((item) => {
              return <Task key={`task-${item.id}`} planid={this.props.id} $$$parent={this} {...item} />
            })}
            {this.state.taskNum === 0 && <div className="no-tasks">{$L('暂无任务')}</div>}
          </div>
          {this.creatableTask &&
            (this.state.newMode ? (
              <div className="task-card newtask">
                <div className="task-card-body">
                  <div>
                    <textarea
                      className="form-control form-control-sm row2x"
                      placeholder={$L('输入标题以新建任务')}
                      ref={(c) => (this._taskName = c)}
                      maxLength="190"
                      autoFocus
                      onKeyDown={(e) => {
                        if (e.keyCode === 13) {
                          this._handleCreateTask()
                          $stopEvent(e)
                          return false
                        }
                      }}
                    />
                  </div>
                  <div>
                    <label className="mb-1">{$L('执行人')}</label>
                    <div>
                      <UserSelector hideDepartment hideRole hideTeam multiple={false} ref={(c) => (this._executor = c)} />
                    </div>
                  </div>
                  <div>
                    <label className="mb-1">{$L('到期时间')}</label>
                    <div>
                      <input type="text" className="form-control form-control-sm" ref={(c) => (this._deadline = c)} />
                    </div>
                  </div>
                  <div className="text-right">
                    <button className="btn btn-link w-auto" type="button" onClick={() => this.setState({ newMode: false })}>
                      {$L('取消')}
                    </button>
                    <button className="btn btn-primary" type="button" ref={(c) => (this._btn = c)} onClick={() => this._handleCreateTask()}>
                      {$L('确定')}
                    </button>
                  </div>
                </div>
              </div>
            ) : (
              <div className="task-card newbtn" onClick={() => this._handleAddTask()}>
                <i className="zmdi zmdi-plus" />
              </div>
            ))}
        </div>
      </div>
    )
  }

  componentDidMount() {
    __PlanRefs[this.props.id] = this
    this.refreshTasks(true)

    const $scroller = $(this._scroller).perfectScrollbar()
    // 滚动加载
    $scroller.on('ps-scroll-down', () => {
      // 全部已加载
      if (this.state.tasks.length >= this.state.taskNum) return

      const scrollerHeight = $scroller[0].scrollHeight
      const top = $scroller.scrollTop() + $scroller.height()
      if (top + 222 > scrollerHeight) {
        $setTimeout(() => this.refreshTasks(true), 200, 'tasks-load-more')
      }
    })

    const $boxes = document.getElementById('plan-boxes')
    $addResizeHandler(() => {
      let mh = $(window).height() - 210 + (this.creatableTask ? 0 : 44)
      if ($boxes.scrollWidth > $boxes.clientWidth) mh -= 13 // 横向滚动条高度

      $scroller.css({ 'max-height': mh })
      $scroller.perfectScrollbar('update')
    })()
  }

  componentDidUpdate(prevProps) {
    if (prevProps.sort !== this.props.sort || prevProps.search !== this.props.search || !$same(prevProps.filter, this.props.filter)) this.refreshTasks()
  }

  // 加载任务列表
  refreshTasks(isAppend) {
    if (isAppend) {
      this.pageNo++
    } else {
      this.pageNo = 1
      this.pageSize = Math.max(this.state.tasks.length + 1, __DEFAULT_PAGE_SIZE)
    }

    const url = `/project/tasks/list?plan=${this.props.id}&sort=${this.props.sort || ''}&search=${$encode(this.props.search || '')}&pageNo=${this.pageNo}&pageSize=${this.pageSize}`
    $.post(url, JSON.stringify(this.props.filter), (res) => {
      if (res.error_code === 0) {
        const ns = isAppend ? (this.state.tasks || []).concat(res.data.tasks) : res.data.tasks
        const _state = { tasks: ns }
        if (res.data.count > -1) _state.taskNum = res.data.count

        this.setState(_state, () => {
          $(this._scroller).perfectScrollbar('update')
          // this._scroller.scrollTop = 99999
        })
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }

  removeTask(taskid) {
    const removedTask = this.state.tasks.find((item) => item.id === taskid)
    const ns = this.state.tasks.filter((item) => item.id !== taskid)
    this.setState({ tasks: ns, taskNum: this.state.taskNum - 1 }, () => $(this._scroller).perfectScrollbar('update'))
    return removedTask
  }

  addTask(taskData, prevTaskId, $itemholder) {
    const ns = []
    if (prevTaskId) {
      this.state.tasks.forEach((item) => {
        ns.push(item)
        if (item.id === prevTaskId) ns.push(taskData)
      })
    } else {
      // At top
      ns.push(taskData)
      this.state.tasks.forEach((item) => ns.push(item))
    }

    this.setState({ tasks: ns, taskNum: this.state.taskNum + 1 }, () => {
      $itemholder && $itemholder.remove()
      __TaskRefs[taskData.id].refresh()
      $(this._scroller).perfectScrollbar('update')
    })
  }

  _handleAddTask() {
    this.setState({ newMode: true }, () => {
      $('html').animate({ scrollTop: 500 }, 200)

      if (this.__datetimepicker) this.__datetimepicker.datetimepicker('remove')
      this.__datetimepicker = $(this._deadline).datetimepicker({
        startDate: new Date(),
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
      taskNumber: 0,
      metadata: { entity: 'ProjectTask' },
    }

    if (!_data.taskName) return RbHighbar.create($L('请输入任务标题'))
    if (_data.deadline) _data.deadline += ':00'

    const $btn = $(this._btn).button('loading')
    $.post('/app/entity/common-save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        this.refreshTasks()

        // reset
        this._executor.clearSelection()
        $([this._taskName, this._deadline]).val('')
        this._taskName.focus()
      } else {
        RbHighbar.error(res.error_msg)
      }
      $btn.button('reset')
    })
  }
}

// 任务卡
class Task extends React.Component {
  state = { ...this.props }

  render() {
    let deadlineState = -1
    // 未完成且设置了到期时间
    if (this.state.status !== 1 && this.state.deadline) {
      if ($expired(this.state.deadline)) deadlineState = 2
      else if ($expired(this.state.deadline, -60 * 60 * 24 * 3)) deadlineState = 1
      else deadlineState = 0
    }

    return (
      <div
        className={`task-card content status-${this.state.status} priority-${this.state.priority}`}
        data-seq={this.state.seq}
        data-taskid={this.state.id}
        data-planid={this.props.planid}
        onClick={() => TaskViewModal.create(this.state.id)}>
        <div className="task-card-body">
          <div className="task-content-wrapper">
            <div className="task-status">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline" onClick={(e) => $stopEvent(e)}>
                <input
                  className="custom-control-input"
                  type="checkbox"
                  defaultChecked={this.state.status === 1}
                  onChange={(e) => this._toggleStatus(e)}
                  disabled={!this.props.$$$parent.performableTask}
                  ref={(c) => (this._$status = c)}
                />
                <span className="custom-control-label" />
              </label>
            </div>
            <div className="task-content">
              <div className="task-title text-wrap">{this.state.taskName}</div>

              {this.state.endTime && (
                <div className="task-time">
                  {$L('完成时间')} <DateShow date={this.state.endTime} />
                </div>
              )}
              {this.state.modifiedOn && (
                <div className="task-time">
                  {$L('更新时间')} <DateShow date={this.state.modifiedOn} />
                </div>
              )}
              {this.state.createdOn && (
                <div className="task-time">
                  {$L('创建时间')} <DateShow date={this.state.createdOn} />
                </div>
              )}
              {deadlineState > -1 && (
                <div className="task-time">
                  <span className={`badge badge-${deadlineState === 2 ? 'danger' : deadlineState === 1 ? 'warning' : 'primary'}`}>
                    {$L('到期时间')} <DateShow date={this.state.deadline} />
                  </span>
                </div>
              )}

              {(this.state.tags || []).length > 0 && (
                <div className="task-tags">
                  {this.state.tags.map((item) => {
                    const colorStyle1 = { color: item.color }
                    const colorStyle2 = { backgroundColor: item.color }
                    return (
                      <a key={item.rid} style={colorStyle1}>
                        <i style={colorStyle2} /> {item.name}
                      </a>
                    )
                  })}
                </div>
              )}

              <div className="task-extras">
                {this.state.createdBy && (
                  <a className="avatar mr-1" title={`${$L('创建人')} ${this.state.createdBy[1]}`}>
                    <img src={`${rb.baseUrl}/account/user-avatar/${this.state.createdBy[0]}`} alt="Avatar" />
                  </a>
                )}
                {this.state.executor && (
                  <a className="avatar mr-1" title={`${$L('执行人')} ${this.state.executor[1]}`}>
                    <img src={`${rb.baseUrl}/account/user-avatar/${this.state.executor[0]}`} alt="Avatar" />
                  </a>
                )}

                {this.state.description && <i className="ml-2 icon zmdi zmdi-comment-more" title={$L('详情')} />}
                {this.state.attachments && <i className="ml-3 icon zmdi zmdi-attachment-alt zmdi-hc-rotate-45" title={$L('附件')} />}

                <span className="badge float-right">{this.state.taskNumber}</span>
                <div className="clearfix" />
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

  componentDidUpdate(prevProps, prevState) {
    if (prevState && prevState.status !== this.state.status) {
      $(this._$status).prop('checked', this.state.status === 1)
    }
    // __TaskRefs[this.props.id] = this
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.setState({ ...nextProps })
  }

  refresh() {
    $.get(`/project/tasks/get?task=${this.props.id}`, (res) => {
      const ns = []
      this.props.$$$parent.state.tasks.forEach((item) => {
        if (item.id === res.data.id) ns.push(res.data)
        else ns.push(item)
      })
      // 委托父级刷新
      this.props.$$$parent.setState({ tasks: ns })
    })
  }

  _toggleStatus(e) {
    const status = e.currentTarget.checked ? 1 : 0
    __saveTask(this.props.id, { status: status }, () => {
      this.setState({ status: status })
      __PlanRefs[this.props.planid].refreshTasks()
    })
  }
}

// 保存任务
const __saveTask = function (id, data, call) {
  data.metadata = { id: id }
  $.post('/app/entity/common-save', JSON.stringify(data), (res) => {
    if (res.error_code !== 0) RbHighbar.error(res.error_msg)
    else typeof call === 'function' && call()
  })
}

// 面板拖动
const __draggable = function () {
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

// 与实体视图兼容（提供给 TaskView 用）
// eslint-disable-next-line no-unused-vars
const RbViewModal = {
  // 获取当前视图
  currentHolder: () => {
    return {
      hideLoading: function () {
        TaskViewModal.__HOLDER && TaskViewModal.__HOLDER.setLoadingState(false)
      },
    }
  },
}

// 任务视图
class TaskViewModal extends React.Component {
  state = { ...this.props, inLoad: true }

  render() {
    return (
      <div className="modal rbview task" ref={(c) => (this._dlg = c)}>
        <div className="modal-dialog">
          <div className="modal-content">
            <div className={'modal-body iframe rb-loading ' + (this.state.inLoad === true && 'rb-loading-active')}>
              <iframe ref={(c) => (this._iframe = c)} className={this.state.isHide ? 'invisible' : ''} src={this.state._taskUrl || ''} frameBorder="0" scrolling="no" />
              <RbSpinner />
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const $dlg = $(this._dlg)
      .on('shown.bs.modal', () => {
        $dlg.find('.modal-content').css('margin-right', 0)
        location.hash = '#!/View/ProjectTask/' + this.props.taskid
      })
      .on('hidden.bs.modal', () => {
        $dlg.find('.modal-content').css('margin-right', -1000)
        location.hash = '#!/View/'
      })

    $dlg.modal({ show: true })
    // fix: 打开视图卡顿
    setTimeout(() => this.setState({ _taskUrl: `${rb.baseUrl}/project/task/${this.state.taskid}` }), 200)
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
   * @param {*} id
   */
  static create(id) {
    renderRbcomp(<TaskViewModal taskid={id} />, null, function () {
      TaskViewModal.__HOLDER = this
    })
  }
}
