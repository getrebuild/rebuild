/*!
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
    this.__FeedsList._inView = true

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
        <a className="dropdown-item" data-sort="newer" onClick={(e) => this.search(e)}>
          {$L('最近发布')}
        </a>
        <a className="dropdown-item" data-sort="older" onClick={(e) => this.search(e)}>
          {$L('最早发布')}
        </a>
        <a className="dropdown-item" data-sort="modified" onClick={(e) => this.search(e)}>
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
      this.setState({ dataList: list, showMore: data.length >= pageSize })
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
        <span className="zmdi zmdi-mdi-checkbox-marked-outline" />
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
        <a className="dropdown-item" data-sort="seq" onClick={(e) => this.search(e)}>
          {$L('手动拖动')}
        </a>
        <a className="dropdown-item" data-sort="deadline" onClick={(e) => this.search(e)}>
          {$L('最近截至')}
        </a>
        <a className="dropdown-item" data-sort="modifiedOn" onClick={(e) => this.search(e)}>
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
          <div className="col-8 title">
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline ptask">
              <input className="custom-control-input" type="checkbox" defaultChecked={item.status > 0} disabled={readonly} onClick={() => this._toggleStatus(item)} />
              <span className="custom-control-label" />
            </label>
            <a href={`${rb.baseUrl}/app/redirect?id=${item.id}`} target="_blank" title={$L('打开')}>
              [{item.taskNumber}] {item.taskName}
            </a>
          </div>
          <div className="col-4 task-meta">
            {item.executor && (
              <span>
                <a className="avatar" title={`${$L('执行人')} ${item.executor[1]}`}>
                  <img src={`${rb.baseUrl}/account/user-avatar/${item.executor[0]}`} alt="Avatar" />
                </a>
              </span>
            )}

            <span title={$L('项目')}>{item.planName}</span>

            {!item.deadline && !item.endTime && (
              <span>
                <span className="mr-1">{$L('创建时间')}</span>
                <DateShow date={item.createdOn} />
              </span>
            )}
            {item.endTime && (
              <span>
                <span className="mr-1">{$L('完成时间')}</span>
                <DateShow date={item.endTime} />
              </span>
            )}
            {!item.endTime && item.deadline && (
              <span>
                <span className="mr-1">{$L('到期时间')}</span>
                <DateShow date={item.deadline} />
              </span>
            )}
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
  render() {
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$L('新建任务')} icon="mdi-checkbox-marked-outline" disposeOnHide>
        <div className="m-2 ml-3 mr-3">
          <div className="row">
            <div className="col pr-2">
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
            <div className="col pl-2">
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
        <div className="mt-3 mr-2 text-right" ref={(c) => (this._btns = c)}>
          <button className="btn btn-primary btn-space" type="button" onClick={this._post}>
            {$L('保存')}
          </button>
          <button className="btn btn-secondary btn-space ml-1" type="button" onClick={this.hide}>
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

// 附件列表
// eslint-disable-next-line no-unused-vars
class LightAttachmentList extends RelatedList {
  constructor(props) {
    super(props)

    this.__listClass = 'file-list inview'
    this.__listNoData = (
      <div className="list-nodata">
        <span className="zmdi zmdi-folder-outline" />
        <p>
          {$L('暂无数据')}
          <br />
          {$L('显示当前记录的所有附件')}
        </p>
      </div>
    )
    this.__listExtraLink = (
      <form method="post" action={`${rb.baseUrl}/files/batch-download`} ref={(c) => (this._$downloadForm = c)} target="_blank">
        <input type="hidden" name="files" />
        <button type="submit" className="btn btn-light w-auto" title={$L('下载全部')} disabled>
          <i className="icon zmdi zmdi-download" />
        </button>
      </form>
    )
  }

  renderSorts() {
    return (
      <div className="dropdown-menu dropdown-menu-right" x-placement="bottom-end">
        <a className="dropdown-item" data-sort="newer" onClick={(e) => this.search(e)}>
          {$L('最近上传')}
        </a>
        <a className="dropdown-item" data-sort="older" onClick={(e) => this.search(e)}>
          {$L('最早上传')}
        </a>
        <a className="dropdown-item" data-sort="size" onClick={(e) => this.search(e)}>
          {$L('文件大小')}
        </a>
      </div>
    )
  }

  renderItem(item) {
    return (
      <div className="file-list-item" key={item.id}>
        <div className="type">
          <i className="file-icon" data-type={item.fileType} />
        </div>
        <div className="detail">
          <a onClick={() => (parent || window).RbPreview.create(item.filePath)} title={$L('预览')}>
            {$fileCutName(item.filePath)}
          </a>
          <div className="extras">
            <span className="fsize">{item.fileSize}</span>
          </div>
        </div>
        <div className="info position-relative">
          <span className="fop-action">
            <a title={$L('下载')} href={`${rb.baseUrl}/filex/download/${item.filePath}?attname=${$fileCutName(item.filePath)}`} target="_blank">
              <i className="icon zmdi zmdi-download fs-17" />
            </a>
            {rb.fileSharable && (
              <a
                title={$L('分享')}
                onClick={(e) => {
                  $stopEvent(e)
                  // eslint-disable-next-line react/jsx-no-undef
                  renderRbcomp(<FileShare file={item.filePath} />)
                }}>
                <i className="icon zmdi zmdi-share up-1" />
              </a>
            )}
          </span>
        </div>
        <div className="info">
          <DateShow date={item.uploadOn} />
        </div>
        <div className="info">{item.uploadBy[1]}</div>
      </div>
    )
  }

  fetchData(append) {
    this.__pageNo = this.__pageNo || 1
    if (append) this.__pageNo += append
    const pageSize = 20

    const relatedId = this.props.mainid
    $.get(
      `/files/list-file?entry=${relatedId.substr(0, 3)}&sort=${this.__searchSort || ''}&q=${$encode(this.__searchKey)}&pageNo=${this.__pageNo}&pageSize=${pageSize}&related=${relatedId}`,
      (res) => {
        if (res.error_code !== 0) return RbHighbar.error(res.error_msg)

        const data = res.data || []
        const list = append ? (this.state.dataList || []).concat(data) : data
        this.setState({ dataList: list, showMore: data.length >= pageSize })

        const files = list.map((item) => item.filePath)
        if (files.length > 0) {
          $(this._$downloadForm).find('input').val(files.join(','))
          $(this._$downloadForm).find('button').attr('disabled', false)
        }
      }
    )
  }

  componentDidMount() {
    // v3.1 有权限才加载
    $.get(`/files/check-readable?id=${this.props.mainid}`, (res) => {
      if (res.data === true) {
        this.fetchData()
      } else {
        this.setState({ dataList: [] }, () => {})
      }
    })
  }
}

// 选择报表
// eslint-disable-next-line no-unused-vars
class SelectReport extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="modal select-list report-list" ref={(c) => (this._dlg = c)} tabIndex="-1">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={this.hide}>
                <i className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <h5 className="mt-0 text-bold">{$L('选择报表')}</h5>
              {this.state.reports && this.state.reports.length === 0 && (
                <p className="text-muted">
                  {$L('暂无报表')}
                  {rb.isAdminUser && (
                    <a className="icon-link ml-2" target="_blank" href={`${rb.baseUrl}/admin/data/report-templates`}>
                      <i className="zmdi zmdi-settings" /> {$L('点击配置')}
                    </a>
                  )}
                </p>
              )}
              <div>
                <ul className="list-unstyled">
                  {(this.state.reports || []).map((item) => {
                    rb._officePreviewUrl = 111
                    const reportUrl = `${rb.baseUrl}/app/${this.props.entity}/report/export?report=${item.id}&record=${this.props.id}`
                    const showPdf = (item.outputType || '').includes('pdf')
                    const showHtml = item.outputType !== 'html5' && (item.outputType || '').includes('html')
                    return (
                      <li key={item.id} className={`${rb._officePreviewUrl && 'has-preview'} ${showPdf && 'has-pdf'} ${showHtml && 'has-html'}`}>
                        <a target="_blank" href={reportUrl} className="text-truncate" title={$L('下载')}>
                          {item.name}
                          <i className="mdi mdi-download" />
                        </a>
                        <span>
                          {showPdf && (
                            <a target="_blank" className="preview" href={`${reportUrl}&output=pdf`} title={$L('查看 PDF')}>
                              <i className="mdi mdi-file-pdf-box fs-18" />
                            </a>
                          )}
                          {showHtml && (
                            <a target="_blank" className="preview" href={`${reportUrl}&output=html`} title={$L('查看 HTML')}>
                              <i className="mdi mdi-language-html5 fs-18" />
                            </a>
                          )}
                          {rb._officePreviewUrl && (
                            <a target="_blank" className="preview" href={`${reportUrl}&output=preview`} title={$L('在线预览')}>
                              <i className="mdi mdi-open-in-new" />
                            </a>
                          )}
                        </span>
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
    $.get(`/app/${this.props.entity}/report/available?record=${this.props.id || ''}`, (res) => this.setState({ reports: res.data }))
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
    renderRbcomp(<SelectReport entity={entity} id={id} />, function () {
      that.__cached = this
    })
  }
}

const _FrontJS = window.FrontJS
const _EasyAction = window.EasyAction
// eslint-disable-next-line no-unused-vars
const EasyAction4View = {
  init(items) {
    if (!(_FrontJS && _EasyAction && items) || !items['view']) return
    const _View = _FrontJS.View
    const viewObj = _View._detectView()
    if (!viewObj) return
    viewObj.__hasEaButton = true

    const id = _View.getCurrentId()
    const _eaView = items['view']
    _eaView.forEach((item) => {
      item = _EasyAction.fixItem(item, _View.getCurrentId())
      if (!item) return

      item.onClick = () => _EasyAction.handleOp(item, id)
      item._eaid = item.id
      item.items &&
        item.items.forEach((itemL2) => {
          itemL2.onClick = (e) => {
            if ($(e.target).attr('disabled')) return
            _EasyAction.handleOp(itemL2, id)
          }
          itemL2._eaid = itemL2.id
        })
      _View.addButton(item)
    })

    // v4.0
    _EasyAction.checkShowFilter(_eaView, id, (res) => {
      $('.view-action')
        .find('button[data-eaid],a[data-eaid]')
        .each((i, b) => {
          const $this = $(b)
          if (res[$this.data('eaid')]) $this.attr('disabled', false)
          else {
            if (b.tagName === 'BUTTON') {
              // $this.parent().addClass('hide')
              $this.parent().remove()
            } else {
              $this.addClass('hide')
            }
          }
        })
    })
  },
}
