/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global RelatedList, FeedsList */

const wpc = window.__PageConfig || {}

// const FeedsList = window.FeedsList || React.Component

// ~ 跟进列表
// eslint-disable-next-line no-unused-vars
class LightFeedsList extends RelatedList {
  constructor(props) {
    super(props)

    // 复写组件
    this.__FeedsList = new FeedsList()
    this.__FeedsList.setState = (s) => this.setState(s)
    this.__FeedsList.fetchFeeds = () => this.fetchData(false)

    this.__listClass = 'feeds-list inview'
    this.__listNoData = (
      <div className="list-nodata">
        <span className="zmdi zmdi-chart-donut" />
        <p>
          {$L('暂无数据')}
          <br />
          {$L('私密动态不支持在相关项中显示')}
        </p>
      </div>
    )
  }

  renderSorts() {
    return (
      <div className="dropdown-menu dropdown-menu-right" x-placement="bottom-end">
        <a className="dropdown-item" data-sort="newer" onClick={(e) => this._search(e)}>
          {$L('最近发布')}
        </a>
        <a className="dropdown-item" data-sort="older" onClick={(e) => this._search(e)}>
          {$L('最早发布')}
        </a>
        <a className="dropdown-item" data-sort="modified" onClick={(e) => this._search(e)}>
          {$L('最近修改')}
        </a>
      </div>
    )
  }

  renderItem(item) {
    return this.__FeedsList.renderItem({ ...item })
  }

  fetchData(append) {
    const filter = {
      entity: 'Feeds',
      equation: '(1 OR 2) AND 3',
      items: [
        { field: 'type', op: 'EQ', value: 2 },
        { field: 'type', op: 'EQ', value: 4 },
        { field: 'relatedRecord', op: 'EQ', value: wpc.recordId },
      ],
    }
    if (this.__searchKey) {
      filter.equation = '(1 OR 2) AND 3 AND 4'
      filter.items.push({ field: 'content', op: 'LK', value: this.__searchKey })
    }

    this.__pageNo = this.__pageNo || 1
    if (append) this.__pageNo += append
    const pageSize = 20

    $.post(`/feeds/feeds-list?pageNo=${this.__pageNo}&pageSize=${pageSize}&sort=${this.__searchSort || ''}&type=&foucs=`, JSON.stringify(filter), (res) => {
      if (res.error_code !== 0) return RbHighbar.error(res.error_msg)

      const data = (res.data || {}).data || []
      const list = append ? (this.state.dataList || []).concat(data) : data
      this.__FeedsList.state = { data: list }
      this.setState({ dataList: list, showMore: data.length >= pageSize }, () => {
        $('.feeds-list.inview .J_relatedRecord a').attr({
          href: 'javascript:;',
          title: '',
        })
      })

      if (this.state.showToolbar === undefined) this.setState({ showToolbar: data.length > 0 })
    })
  }
}

// 任务列表
// eslint-disable-next-line no-unused-vars
class LightTaskList extends RelatedList {
  constructor(props) {
    super(props)

    this.__listClass = 'tasks-list inview'
    this.__listNoData = (
      <div className="list-nodata">
        <span className="zmdi zmdi-shape" />
        <p>
          {$L('暂无数据')}
          <br />
          {$L('可能存在当前用户无访问权限的任务')}
        </p>
      </div>
    )
  }

  renderSorts() {
    return (
      <div className="dropdown-menu dropdown-menu-right" x-placement="bottom-end">
        <a className="dropdown-item" data-sort="seq" onClick={(e) => this._search(e)}>
          {$L('手动拖动')}
        </a>
        <a className="dropdown-item" data-sort="deadline" onClick={(e) => this._search(e)}>
          {$L('最近截至')}
        </a>
        <a className="dropdown-item" data-sort="modifiedOn" onClick={(e) => this._search(e)}>
          {$L('最近修改')}
        </a>
      </div>
    )
  }

  renderItem(item) {
    const readonly = item.planFlow === 2 || !item.projectMember || item.projectStatus === 2

    return (
      <div className={`card priority-${item.priority} status-${item.status}`} key={item.id}>
        <div className="row header-title">
          <div className="col-7 title">
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline ptask">
              <input className="custom-control-input" type="checkbox" defaultChecked={item.status > 0} disabled={readonly} onClick={() => this._toggleStatus(item)} />
              <span className="custom-control-label" />
            </label>
            <a href={`${rb.baseUrl}/app/list-and-view?id=${item.id}`} target="_blank" title={$L('打开')}>
              [{item.taskNumber}] {item.taskName}
            </a>
          </div>
          <div className="col-5 task-meta">
            <div className="row">
              <div className="col-7 pr-0 text-ellipsis">{item.planName}</div>
              <div className="col-5 text-ellipsis">
                {!item.deadline && !item.endTime && (
                  <React.Fragment>
                    <span className="mr-1">{$L('创建时间')}</span>
                    <DateShow date={item.createdOn} />
                  </React.Fragment>
                )}
                {item.endTime && (
                  <React.Fragment>
                    <span className="mr-1">{$L('完成时间')}</span>
                    <DateShow date={item.endTime} />
                  </React.Fragment>
                )}
                {!item.endTime && item.deadline && (
                  <React.Fragment>
                    <span className="mr-1">{$L('到期时间')}</span>
                    <DateShow date={item.deadline} />
                  </React.Fragment>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  fetchData(append) {
    this.__pageNo = this.__pageNo || 1
    if (append) this.__pageNo += append
    const pageSize = 20

    $.get(`/project/tasks/related-list?pageNo=${this.__pageNo}&pageSize=${pageSize}&sort=${this.__searchSort || ''}&related=${this.props.mainid}&search=${$encode(this.__searchKey)}`, (res) => {
      if (res.error_code !== 0) return RbHighbar.error(res.error_msg)

      const data = res.data || []
      const list = append ? (this.state.dataList || []).concat(data) : data
      this.setState({ dataList: list, showMore: data.length >= pageSize })

      if (this.state.showToolbar === undefined) this.setState({ showToolbar: data.length > 0 })
    })
  }

  _toggleStatus(item) {
    const data = {
      status: item.status === 1 ? 0 : 1,
      metadata: { id: item.id },
    }

    $.post('/app/entity/common-save', JSON.stringify(data), (res) => {
      if (res.error_code > 0) return RbHighbar.error(res.error_msg)

      // 获取最新的
      $.get(`/project/tasks/related-list?task=${item.id}&related=${item.id}`, (res) => {
        if (res.error_code === 0) {
          const taskNew = res.data[0]
          const dataListNew = this.state.dataList
          for (let i = 0; i < dataListNew.length; i++) {
            const c = dataListNew[i]
            if (c.id === taskNew.id) {
              dataListNew[i] = taskNew
              break
            }
          }
          this.setState({ dataList: dataListNew })
        }
      })
    })
  }
}

// 任务创建
// eslint-disable-next-line no-unused-vars
class LightTaskDlg extends RbModalHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$L('新建任务')} disposeOnHide={true}>
        <div className="m-1">
          <div className="row">
            <div className="col-6">
              <div className="form-group">
                <label>{$L('项目')}</label>
                <select className="form-control form-control-sm" ref={(c) => (this._$project = c)}>
                  {this.state.projects &&
                    this.state.projects.map((item) => {
                      return (
                        <option key={item.id} value={item.id}>
                          {item.projectName}
                        </option>
                      )
                    })}
                </select>
              </div>
            </div>
            <div className="col-6">
              <div className="form-group">
                <label>{$L('任务面板')}</label>
                <select className="form-control form-control-sm" ref={(c) => (this._$plan = c)}>
                  {this.state.selectProject &&
                    this.state.selectProject.plans &&
                    this.state.selectProject.plans.map((item) => {
                      return (
                        <option key={item.id} value={item.id} disabled={item.flowStatus === 2}>
                          {item.planName}
                        </option>
                      )
                    })}
                </select>
              </div>
            </div>
          </div>
          <div className="form-group">
            <label>{$L('任务标题')}</label>
            <textarea className="form-control form-control-sm row2x" ref={(c) => (this._$title = c)} />
          </div>
        </div>
        <div className="mt-3 text-right" ref={(c) => (this._btns = c)}>
          <button className="btn btn-primary btn-space" type="button" onClick={this._post}>
            {$L('保存')}
          </button>
          <button className="btn btn-secondary btn-space" type="button" onClick={this.hide}>
            {$L('取消')}
          </button>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    $.get('/project/alist', (res) => {
      const ps = res.data || []
      this.setState({ projects: ps })

      const that = this
      $(this._$project)
        .select2({
          placeholder: $L('选择项目'),
          allowClear: false,
        })
        .on('change', function () {
          const id = $(this).val()
          const s = ps.find((x) => x.id === id)
          that.setState({ selectProject: s })
        })
        .trigger('change')

      $(this._$plan).select2({
        placeholder: $L('选择任务面板'),
        allowClear: false,
      })
    })

    setTimeout(() => this._$title.focus(), 100)
  }

  _post = () => {
    const data = {
      taskNumber: 0,
      projectId: $(this._$project).val(),
      projectPlanId: $(this._$plan).val(),
      taskName: $(this._$title).val(),
      relatedRecord: this.props.relatedRecord,
      metadata: { entity: 'ProjectTask' },
    }
    if (!data.projectId) return RbHighbar.create($L('请选择项目'))
    if (!data.projectPlanId) return RbHighbar.create($L('请选择任务面板'))
    if (!data.taskName) return RbHighbar.create($L('请输入任务标题'))

    const $btn = $(this._btns).button('loading')
    $.post('/app/entity/common-save', JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        typeof this.props.call === 'function' && this.props.call()
      } else {
        RbHighbar.error(res.error_msg)
      }
      $btn.button('reset')
    })
  }
}
