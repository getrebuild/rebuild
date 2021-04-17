/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig || {}

// 选择报表
// eslint-disable-next-line no-unused-vars
class SelectReport extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="modal select-list" ref={(c) => (this._dlg = c)} tabIndex="-1">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={this.hide}>
                <i className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <h5 className="mt-0 text-bold">{$L('SelectSome,Report')}</h5>
              {this.state.reports && this.state.reports.length === 0 && (
                <p className="text-muted">
                  {$L('NoAnySome,Report')}
                  {rb.isAdminUser && (
                    <a className="icon-link ml-1" target="_blank" href={`${rb.baseUrl}/admin/data/report-templates`}>
                      <i className="zmdi zmdi-settings"></i> {$L('ClickConf')}
                    </a>
                  )}
                </p>
              )}
              <div>
                <ul className="list-unstyled">
                  {(this.state.reports || []).map((item) => {
                    const reportUrl = `${rb.baseUrl}/app/${this.props.entity}/report/export?report=${item.id}&record=${this.props.id}&attname=${$encode(item.name)}`
                    return (
                      <li key={'r-' + item.id}>
                        <a target="_blank" href={reportUrl} className="text-truncate">
                          {item.name}
                          <i className="zmdi zmdi-download"></i>
                        </a>
                      </li>
                    )
                  })}
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    $.get(`/app/${this.props.entity}/report/available`, (res) => this.setState({ reports: res.data }))
    $(this._dlg).modal({ show: true, keyboard: true })
  }

  hide = () => $(this._dlg).modal('hide')
  show = () => $(this._dlg).modal('show')

  /**
   * @param {*} entity
   * @param {*} id
   */
  static create(entity, id) {
    if (this.__cached) {
      this.__cached.show()
      return
    }
    const that = this
    renderRbcomp(<SelectReport entity={entity} id={id} />, null, function () {
      that.__cached = this
    })
  }
}

const FeedsList = window.FeedsList || React.Component
// ~ 跟进列表
// eslint-disable-next-line no-unused-vars
class LightFeedsList extends FeedsList {
  state = { ...this.props }

  render() {
    return (
      <div className={`related-list ${!this.state.data ? 'rb-loading rb-loading-active' : ''}`}>
        {!this.state.data && <RbSpinner />}
        {this.state.showToolbar && (
          <div className="related-toolbar feeds">
            <div className="row">
              <div className="col">
                <div className="input-group input-search">
                  <input className="form-control" type="text" placeholder={$L('Keyword')} maxLength="40" ref={(c) => (this._quickSearch = c)} onKeyDown={(e) => e.keyCode === 13 && this._search()} />
                  <span className="input-group-btn">
                    <button className="btn btn-secondary" type="button" onClick={() => this._search()}>
                      <i className="icon zmdi zmdi-search" />
                    </button>
                  </span>
                </div>
              </div>
              <div className="col text-right">
                <div className="btn-group">
                  <button type="button" className="btn btn-link" data-toggle="dropdown" aria-expanded="false">
                    {this.state.sortDisplayText || $L('DefaultSort')} <i className="icon zmdi zmdi-chevron-down up-1"></i>
                  </button>
                  <div className="dropdown-menu dropdown-menu-right" x-placement="bottom-end">
                    <a className="dropdown-item" data-sort="newer" onClick={(e) => this._search(e)}>
                      {$L('FeedsSortNewer')}
                    </a>
                    <a className="dropdown-item" data-sort="older" onClick={(e) => this._search(e)}>
                      {$L('FeedsSortOlder')}
                    </a>
                    <a className="dropdown-item" data-sort="modified" onClick={(e) => this._search(e)}>
                      {$L('FeedsSortModified')}
                    </a>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {this.state.data && this.state.data.length === 0 && (
          <div className="list-nodata">
            <span className="zmdi zmdi-chart-donut" />
            <p>{$L('NoSome,e.Feeds')}</p>
          </div>
        )}
        <div className="feeds-list inview">
          {(this.state.data || []).map((item) => {
            return this.renderItem({ ...item, self: false })
          })}
        </div>
        {this.state.showMores && (
          <div className="text-center load-mores">
            <div>
              <button type="button" className="btn btn-secondary" onClick={() => this.fetchFeeds(1)}>
                {$L('LoadMore')}
              </button>
            </div>
          </div>
        )}
      </div>
    )
  }

  fetchFeeds(append) {
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
      const list = append ? (this.state.data || []).concat(data) : data

      // 数据少不显示
      // if (this.state.showToolbar === undefined && data.length >= pageSize) this.setState({ showToolbar: data.length > 0 })
      if (this.state.showToolbar === undefined) this.setState({ showToolbar: data.length > 0 })

      this.setState({ data: list, showMores: data.length >= pageSize })
    })
  }

  _search(e) {
    let sort = null
    if (e && e.currentTarget) {
      sort = $(e.currentTarget).data('sort')
      this.setState({ sortDisplayText: $(e.currentTarget).text() })
    }

    this.__searchSort = sort || this.__searchSort
    this.__searchKey = $(this._quickSearch).val() || ''
    this.__pageNo = 1
    this.fetchFeeds()
  }
}

// 任务列表
// eslint-disable-next-line no-unused-vars
class LightTaskList extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className={`related-list ${!this.state.data ? 'rb-loading rb-loading-active' : ''}`}>
        {!this.state.data && <RbSpinner />}
        {this.state.showToolbar && (
          <div className="related-toolbar feeds">
            <div className="row">
              <div className="col">
                <div className="input-group input-search">
                  <input className="form-control" type="text" placeholder={$L('Keyword')} maxLength="40" ref={(c) => (this._quickSearch = c)} onKeyDown={(e) => e.keyCode === 13 && this._search()} />
                  <span className="input-group-btn">
                    <button className="btn btn-secondary" type="button" onClick={() => this._search()}>
                      <i className="icon zmdi zmdi-search" />
                    </button>
                  </span>
                </div>
              </div>
              <div className="col text-right">
                <div className="btn-group">
                  <button type="button" className="btn btn-link" data-toggle="dropdown" aria-expanded="false">
                    {this.state.sortDisplayText || $L('DefaultSort')} <i className="icon zmdi zmdi-chevron-down up-1"></i>
                  </button>
                  <div className="dropdown-menu dropdown-menu-right" x-placement="bottom-end">
                    <a className="dropdown-item" data-sort="newer" onClick={(e) => this._search(e)}>
                      {$L('FeedsSortNewer')}
                    </a>
                    <a className="dropdown-item" data-sort="older" onClick={(e) => this._search(e)}>
                      {$L('FeedsSortOlder')}
                    </a>
                    <a className="dropdown-item" data-sort="modified" onClick={(e) => this._search(e)}>
                      {$L('FeedsSortModified')}
                    </a>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {this.state.data && this.state.data.length === 0 && (
          <div className="list-nodata">
            <span className="zmdi zmdi-chart-donut" />
            <p>{$L('NoSome,e.ProjectTask')}</p>
          </div>
        )}
        <div className="feeds-list inview">
          {(this.state.data || []).map((item) => {
            return (
              <div className="card" key={item.taskId}>
                <div className="row header-title">{item.taskName}</div>
              </div>
            )
          })}
        </div>
        {this.state.showMores && (
          <div className="text-center load-mores">
            <div>
              <button type="button" className="btn btn-secondary" onClick={() => this.fetchFeeds(1)}>
                {$L('LoadMore')}
              </button>
            </div>
          </div>
        )}
      </div>
    )
  }

  componentDidMount() {
    this.search()
  }

  fetchList(append) {
    this.__pageNo = this.__pageNo || 1
    if (append) this.__pageNo += append
    const pageSize = 20

    $.get(`/project/tasks/related-list?pageNo=${this.__pageNo}&pageSize=${pageSize}&sort=${this.__searchSort || ''}&related=${this.props.mainid}`, (res) => {
      if (res.error_code !== 0) return RbHighbar.error(res.error_msg)

      const data = (res.data || {}).data || []
      const list = append ? (this.state.data || []).concat(data) : data

      // 数据少不显示
      // if (this.state.showToolbar === undefined && data.length >= pageSize) this.setState({ showToolbar: data.length > 0 })
      if (this.state.showToolbar === undefined) this.setState({ showToolbar: data.length > 0 })

      this.setState({ data: list, showMores: data.length >= pageSize })
    })
  }

  search(e) {
    let sort = null
    if (e && e.currentTarget) {
      sort = $(e.currentTarget).data('sort')
      this.setState({ sortDisplayText: $(e.currentTarget).text() })
    }

    this.__searchSort = sort || this.__searchSort
    this.__searchKey = $(this._quickSearch).val() || ''
    this.__pageNo = 1
    this.fetchList()
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
      <RbModal ref={(c) => (this._dlg = c)} title={$L('NewSome,e.ProjectTask')} disposeOnHide={true}>
        <div className="m-1">
          <div className="row">
            <div className="col-6">
              <div className="form-group">
                <label>{$L('f.ProjectTask.projectId')}</label>
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
                <label>{$L('f.ProjectTask.projectPlanId')}</label>
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
            <label>{$L('f.ProjectTask.taskName')}</label>
            <textarea className="form-control form-control-sm row2x" ref={(c) => (this._$title = c)}></textarea>
          </div>
        </div>
        <div className="mt-3 text-right" ref={(c) => (this._btns = c)}>
          <button className="btn btn-primary btn-space" type="button" onClick={this._post}>
            {$L('Save')}
          </button>
          <button className="btn btn-secondary btn-space" type="button" onClick={this.hide}>
            {$L('Cancel')}
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
          placeholder: $L('SelectSome,e.ProjectConfig'),
          allowClear: false,
        })
        .on('change', function () {
          const id = $(this).val()
          const s = ps.find((x) => x.id === id)
          that.setState({ selectProject: s })
        })
        .trigger('change')

      $(this._$plan).select2({
        placeholder: $L('SelectSome,e.ProjectPlanConfig'),
        allowClear: false,
      })
    })
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
