/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global autosize, EMOJIS, SimpleMDE */

const wpc = window.__PageConfig

const __HOLDER = {
  hide: () => window.close(),
  setLoadingState: () => {},
}
const __TaskViewer = parent && parent.TaskViewModal ? parent.TaskViewModal.__HOLDER : __HOLDER

let __TaskContent
let __TaskComment

$(document).ready(() => {
  const editable = wpc.isMember && wpc.projectStatus !== 2

  renderRbcomp(<TaskForm id={wpc.taskId} editable={editable} />, 'task-contents', function () {
    __TaskContent = this
  })
  if (editable) {
    renderRbcomp(<TaskComment id={wpc.taskId} call={() => __TaskContent.refreshComments()} />, 'task-comment', function () {
      __TaskComment = this
    })
  }

  $('.J_close').click(() => __TaskViewer.hide())
  $('.J_reload').click(() => {
    __TaskViewer.setLoadingState(true)
    location.reload()
  })
})

// 任务表单
class TaskForm extends React.Component {
  state = { ...this.props, priority: 1 }

  render() {
    return (
      <div className="rbview-form task-form">
        <div className="form-group row pt-0">
          <div className="col-10">
            <ValueTaskName taskName={this.state.taskName} $$$parent={this} />
          </div>
          {this.props.editable && this.state.isManageable && (
            <div className="col-2 text-right">
              <button className="btn btn-secondary" style={{ minWidth: 80, marginTop: 2 }} data-toggle="dropdown">
                {$L('操作')} <i className="icon zmdi zmdi-more-vert" />
              </button>
              <div className="dropdown-menu dropdown-menu-right">
                <a className="dropdown-item" onClick={() => this._handleDelete()}>
                  <i className="icon zmdi zmdi-delete" />
                  {$L('删除')}
                </a>
              </div>
            </div>
          )}
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">
            <i className="icon zmdi zmdi-square-o" /> {$L('状态')}
          </label>
          <div className="col-12 col-sm-9">
            <ValueStatus status={this.state.status} readonly={this.state.planFlow === 2} $$$parent={this} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">
            <i className="icon zmdi zmdi-account-o" /> {$L('执行人')}
          </label>
          <div className="col-12 col-sm-9">
            <ValueExecutor executor={this.state.executor} $$$parent={this} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">
            <i className="icon zmdi zmdi-time" /> {$L('到期时间')}
          </label>
          <div className="col-12 col-sm-9">
            <ValueDeadline deadline={this.state.deadline} $$$parent={this} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">
            <i className="icon zmdi zmdi-comment-more" /> {$L('详情')}
          </label>
          <div className="col-12 col-sm-9">
            <ValueDescription description={this.state.description} $$$parent={this} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">
            <i className="icon zmdi zmdi-circle-o" /> {$L('优先级')}
          </label>
          <div className="col-12 col-sm-9">
            <ValuePriority priority={this.state.priority} $$$parent={this} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">
            <i className="icon zmdi zmdi-label" /> {$L('标签')}
          </label>
          <div className="col-12 col-sm-9">{this.state.projectId && <ValueTags tags={this.state.tags} projectId={this.state.projectId} taskid={this.props.id} $$$parent={this} />}</div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">
            <i className="icon zmdi zmdi-attachment-alt zmdi-hc-rotate-45 mt-1" /> {$L('附件')}
          </label>
          <div className="col-12 col-sm-9">
            <ValueAttachments attachments={this.state.attachments} $$$parent={this} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">
            <i className="icon zmdi zmdi-link fs-19 up-1" /> {$L('关联记录')}
          </label>
          <div className="col-12 col-sm-9">
            {this.state.projectId && <ValueRelatedRecord relatedRecord={this.state.relatedRecord} relatedRecordData={this.state.relatedRecordData} $$$parent={this} />}
          </div>
        </div>
        <TaskCommentsList taskid={this.props.id} ref={(c) => (this._TaskCommentsList = c)} editable={this.props.editable} />
      </div>
    )
  }

  componentDidMount() {
    __TaskViewer.setLoadingState(false)
    this.fetch()
  }

  fetch() {
    $.get(`/project/tasks/details?task=${this.props.id}`, (res) => {
      if (res.error_code === 0) this.setState({ ...res.data }, () => $(this._status).prop('checked', this.state.status === 1))
      else RbHighbar.error(res.error_msg)
    })
  }

  _handleDelete() {
    const that = this
    RbAlert.create($L('确认删除此任务？'), {
      type: 'danger',
      confirmText: $L('删除'),
      confirm: function () {
        this.disabled(true)
        $.post(`/app/entity/common-delete?id=${that.props.id}`, (res) => {
          if (res.error_code === 0) {
            this.hide()
            RbHighbar.success($L('任务已删除'))
            __TaskViewer.refreshTask('DELETE')
            __TaskViewer.hide()
          } else RbHighbar.error(res.error_msg)
        })
      },
    })
  }

  refreshComments() {
    this._TaskCommentsList.fetchComments()
  }
}

// 字段基类
class ValueComp extends React.Component {
  state = { ...this.props }

  render() {
    return this.props.$$$parent.props.editable ? this.renderElement() : this.renderViewElement()
  }

  renderElement() {
    return <div />
  }

  renderViewElement() {
    return this.renderElement()
  }

  UNSAFE_componentWillReceiveProps = (props) => this.setState(props)

  // 即时保存
  handleChange(e, call) {
    const name = e.target.name
    const value = e.target.value
    const valueOld = this.state[name]
    if ($same(value, valueOld)) {
      typeof call === 'function' && call()
      return
    }
    this.setState({ [name]: value })

    const data = {
      [name]: value,
      metadata: { id: this.props.$$$parent.props.id },
    }

    $.post('/app/entity/common-save', JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        __TaskViewer.refreshTask && __TaskViewer.refreshTask(name === 'projectPlanId' ? value : null)
        typeof call === 'function' && call(true)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

// 任务标题
class ValueTaskName extends ValueComp {
  state = { ...this.props }

  renderElement() {
    return this.props.$$$parent.props.editable && this.state.editMode ? (
      <input
        type="text"
        className="task-title"
        name="taskName"
        defaultValue={this.state.taskName}
        ref={(c) => (this._taskName = c)}
        onBlur={(e) => this.handleChange(e)}
        onKeyDown={(e) => e.keyCode === 13 && e.target.blur()}
        disabled={!this.props.$$$parent.props.editable}
      />
    ) : (
      this.renderViewElement()
    )
  }

  renderViewElement() {
    const editable = this.props.$$$parent.props.editable
    return (
      <div className={`task-title text-break ${editable ? 'hover' : ''}`} onClick={() => editable && this.setState({ editMode: true }, () => this._taskName.focus())}>
        {this.state.taskName}
      </div>
    )
  }

  handleChange(e) {
    const value = e.target.value
    if (!value) {
      RbHighbar.create($L('任务名称不能为空'))
      this._taskName.focus()
    } else {
      super.handleChange(e, () => this.setState({ editMode: false }))
    }
  }
}

// 状态和面板
class ValueStatus extends ValueComp {
  state = { ...this.props }

  renderElement() {
    return (
      <div className="form-control-plaintext">
        <span className="status-checkbox">
          <label className="custom-control custom-checkbox custom-control-inline bw-bold" onClick={(e) => $stopEvent(e)}>
            <input className="custom-control-input" type="checkbox" disabled={this.props.readonly} ref={(c) => (this._status = c)} onChange={(e) => this._handleChangeStatus(e)} />
            <span className="custom-control-label">{this.state.status > 0 ? $L('已完成') : $L('未完成')}</span>
          </label>
        </span>
      </div>
    )
  }

  renderViewElement() {
    return <div className="form-control-plaintext">{this.state.status > 0 ? $L('已完成') : $L('未完成')}</div>
  }

  _handleChangeStatus(e) {
    this.handleChange({ target: { name: 'status', value: e.target.checked ? 1 : 0 } }, () => this.props.$$$parent.fetch())
  }

  // preProps, preState, spanshot
  componentDidUpdate() {
    $(this._status).prop('checked', this.state.status > 0)
  }
}

// 执行者
// TODO 执行者仅允许选择成员 ???
class ValueExecutor extends ValueComp {
  state = { ...this.props }

  renderElement() {
    return (
      <React.Fragment>
        {this.state.executor ? (
          <div className="executor-show">
            <UserSelector
              hideDepartment
              hideRole
              hideTeam
              multiple={false}
              ref={(c) => (this._UserSelector = c)}
              compToggle={
                <span
                  data-toggle="dropdown"
                  style={{
                    height: 30,
                    display: 'inline-block',
                  }}>
                  {this._renderValue()}
                </span>
              }
              onSelectItem={(s) => this.handleChange(s)}
            />
            <a className="close" onClick={() => this.handleChange(null)} title={$L('移除')}>
              <i className="zmdi zmdi-close" />
            </a>
          </div>
        ) : (
          <div className="form-control-plaintext">
            <UserSelector
              hideDepartment
              hideRole
              hideTeam
              multiple={false}
              ref={(c) => (this._UserSelector = c)}
              compToggle={
                <a className="tag-value arrow placeholder" data-toggle="dropdown">
                  {$L('选择执行人')}
                </a>
              }
              onSelectItem={(s) => this.handleChange(s)}
            />
          </div>
        )}
      </React.Fragment>
    )
  }

  renderViewElement() {
    return this._renderValue() || <div className="form-control-plaintext text-muted">{$L('无')}</div>
  }

  _renderValue(call) {
    if (this.state.executor && typeof this.state.executor === 'object') {
      return <UserShow id={this.state.executor[0]} name={this.state.executor[1]} showName onClick={() => typeof call === 'function' && call()} />
    } else {
      return null
    }
  }

  handleChange(value) {
    super.handleChange({ target: { name: 'executor', value: value ? value.id : null } }, () => {
      this.setState({ executor: value ? [value.id, value.text] : null })
      !value && this._UserSelector.clearSelection()
    })
  }
}

// 截至时间
class ValueDeadline extends ValueComp {
  state = { ...this.props }

  renderElement() {
    return (
      <div className="form-control-plaintext" ref={(c) => (this._deadline = c)}>
        <a className={`tag-value arrow ${this.state.deadline ? 'plaintext' : 'placeholder'}`} name="deadline" title={this.state.deadline}>
          {this._renderValue($L('选择到期时间'))}
        </a>
      </div>
    )
  }

  renderViewElement() {
    return this.state.deadline ? <div className="form-control-plaintext">{this._renderValue()}</div> : <div className="form-control-plaintext text-muted">{$L('无')}</div>
  }

  _renderValue(defaultValue) {
    return this.state.deadline ? `${this.state.deadline.substr(0, 16)} (${$fromNow(this.state.deadline)})` : defaultValue
  }

  componentDidMount() {
    $(this._deadline)
      .find('.tag-value')
      .datetimepicker({
        startDate: new Date(),
        clearBtn: true,
      })
      .on('changeDate', (e) => {
        this.handleChange({
          target: {
            name: 'deadline',
            value: e.date ? moment(e.date).format('YYYY-MM-DD HH:mm:ss') : null,
          },
        })
      })
  }
}

// 描述
class ValueDescription extends ValueComp {
  state = { ...this.props }

  renderElement() {
    if (this.state.editMode) {
      return (
        <div className="form-control-plaintext">
          <textarea defaultValue={this.state.description || ''} ref={(c) => (this._editor = c)} />
          <input type="file" className="hide" accept="image/*" ref={(c) => (this._fieldValue__upload = c)} />
          <div className="mt-2 text-right">
            <button onClick={() => this._handleEditMode(false)} className="btn btn-sm btn-link mr-1">
              {$L('取消')}
            </button>
            <button className="btn btn-sm btn-primary" onClick={() => this.handleChange()}>
              {$L('确定')}
            </button>
          </div>
        </div>
      )
    } else {
      const ps = {
        className: 'form-control-plaintext mdedit-content hover',
        onClick: () => this._handleEditMode(true),
      }

      if (this.state.description) {
        return <div {...ps} dangerouslySetInnerHTML={{ __html: SimpleMDE.prototype.markdown(this.state.description) }} />
      } else {
        return (
          <div {...ps}>
            <span className="text-muted">{$L('无')}</span>
          </div>
        )
      }
    }
  }

  renderViewElement() {
    if (this.state.description) {
      return <div className="form-control-plaintext mdedit-content" dangerouslySetInnerHTML={{ __html: SimpleMDE.prototype.markdown(this.state.description) }} />
    } else {
      return (
        <div className="form-control-plaintext mdedit-content">
          <span className="text-muted">{$L('无')}</span>
        </div>
      )
    }
  }

  _handleEditMode(editMode) {
    if (!editMode && this._simplemde) {
      this._simplemde.toTextArea()
      this._simplemde = null
    }

    this.setState({ editMode: editMode }, () => {
      if (this.state.editMode) {
        const mde = new SimpleMDE({
          element: this._editor,
          status: false,
          autoDownloadFontAwesome: false,
          spellChecker: false,
          // eslint-disable-next-line no-undef
          toolbar: DEFAULT_MDE_TOOLBAR(this),
        })
        this._simplemde = mde

        $createUploader(this._fieldValue__upload, null, (res) => {
          const pos = mde.codemirror.getCursor()
          mde.codemirror.setSelection(pos, pos)
          mde.codemirror.replaceSelection(`![](${rb.baseUrl}/filex/img/${res.key})`)
        })
        mde.codemirror.focus()
        mde.codemirror.setCursor(mde.codemirror.lineCount(), 0) // cursor at end
      }
    })
  }

  handleChange() {
    const value = this._simplemde.value()
    super.handleChange({ target: { name: 'description', value: value } }, () => this.setState({ description: value, editMode: false }))
  }
}

const __PRIORITIES = {
  0: $L('较低'),
  1: $L('普通'),
  2: $L('紧急'),
  3: $L('非常紧急'),
}

// 优先级
class ValuePriority extends ValueComp {
  state = { ...this.props }

  renderElement() {
    return (
      <div className="form-control-plaintext">
        <a className={`tag-value arrow priority-${this.state.priority}`} data-toggle="dropdown">
          {__PRIORITIES[this.state.priority]}
        </a>
        <div className="dropdown-menu">
          <a className="dropdown-item text-muted" onClick={() => this.handleChange(0)}>
            {__PRIORITIES[0]}
          </a>
          <a className="dropdown-item text-primary" onClick={() => this.handleChange(1)}>
            {__PRIORITIES[1]}
          </a>
          <a className="dropdown-item text-warning" onClick={() => this.handleChange(2)}>
            {__PRIORITIES[2]}
          </a>
          <a className="dropdown-item text-danger" onClick={() => this.handleChange(3)}>
            {__PRIORITIES[3]}
          </a>
        </div>
      </div>
    )
  }

  renderViewElement() {
    return (
      <div className="form-control-plaintext">
        <span className={`tag-value arrow priority-${this.state.priority}`}>{__PRIORITIES[this.state.priority]}</span>
      </div>
    )
  }

  handleChange(value) {
    super.handleChange({ target: { name: 'priority', value: value } })
  }
}

// 附件
class ValueAttachments extends ValueComp {
  state = { ...this.props }

  renderElement() {
    return (
      <React.Fragment>
        <div className="form-control-plaintext">
          <input type="file" className="inputfile" id="attachments" ref={(c) => (this._attachments = c)} />
          <label htmlFor="attachments" style={{ padding: 0, border: 0, lineHeight: 1, marginBottom: 0 }}>
            <a className="tag-value upload hover">+ {$L('上传')}</a>
          </label>
        </div>
        {this._renderValue(true)}
      </React.Fragment>
    )
  }

  renderViewElement() {
    return this._renderValue() || <div className="form-control-plaintext text-muted">{$L('无')}</div>
  }

  _renderValue(del) {
    return (
      this.state.attachments && (
        <div className="file-field attachments">
          {this.state.attachments.map((item) => {
            const fileName = $fileCutName(item)
            return (
              <a key={`file-${item}`} className="img-thumbnail" title={fileName} onClick={() => (parent || window).RbPreview.create([item])}>
                <i className="file-icon" data-type={$fileExtName(fileName)} />
                <span>{fileName}</span>
                {del && (
                  <b title={$L('删除')} onClick={(e) => this._deleteAttachment(item, e)}>
                    <span className="zmdi zmdi-close" />
                  </b>
                )}
              </a>
            )
          })}
        </div>
      )
    )
  }

  _deleteAttachment(item, e) {
    $stopEvent(e)
    const that = this
    RbAlert.create($L('确认删除此附件？'), {
      type: 'danger',
      confirmText: $L('删除'),
      confirm: function () {
        this.hide()
        const s = that.state.attachments.filter((x) => x !== item)
        that.handleChange({ target: { name: 'attachments', value: s } })
      },
    })
  }

  componentDidMount() {
    $createUploader(
      this._attachments,
      () => $mp.start(),
      (res) => {
        $mp.end()
        const s = (this.state.attachments || []).slice(0)
        s.push(res.key)
        this.handleChange({ target: { name: 'attachments', value: s } })
      }
    )
  }
}

// 标签
class ValueTags extends ValueComp {
  state = { ...this.props }

  renderElement() {
    return this._render(true)
  }

  renderViewElement() {
    return this._render(false)
  }

  _render(editable) {
    const tags = this.state.tags || []
    return (
      <div className="form-control-plaintext task-tags">
        <React.Fragment>
          {tags.map((item) => {
            const colorStyle = { color: item.color, borderColor: item.color }
            return (
              <span className="tag-value" key={item.rid} style={colorStyle}>
                {item.name}
                {editable && (
                  <a title={$L('移除')} onClick={() => this._delRelated(item.rid)}>
                    <i className="zmdi zmdi-close" />
                  </a>
                )}
              </span>
            )
          })}
        </React.Fragment>
        {editable ? (
          <span className="dropdown" ref={(c) => (this._dropdown = c)}>
            <a className="tag-add" title={$L('点击添加')} data-toggle="dropdown">
              <i className="zmdi zmdi-plus" />
            </a>
            <div className="dropdown-menu dropdown-menu-right tags">
              {<ValueTagsEditor ref={(c) => (this._ValueTagsEditor = c)} projectId={this.props.projectId} taskid={this.props.taskid} $$$parent={this} />}
            </div>
          </span>
        ) : (
          tags.length === 0 && (
            <span className="text-muted" style={{ display: 'inline-block', paddingTop: 5 }}>
              {$L('无')}
            </span>
          )
        )}
      </div>
    )
  }

  componentDidMount() {
    const that = this
    $unhideDropdown(this._dropdown).on({
      'hiden.bs.dropdown': function () {
        that._ValueTagsEditor.toggleEditMode(false)
      },
    })
  }

  _delRelated(rid) {
    $.post(`/project/tags/related-del?rid=${rid}`, (res) => {
      if (res.error_code === 0) {
        const tagsNew = this.state.tags.filter((item) => item.rid !== rid)
        this.setState({ tags: tagsNew })
        this.refreshTags()
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }

  refreshTags() {
    $.get(`/project/tags/task-tags?task=${this.props.taskid}`, (res) => {
      this.setState({ tags: res.data || [] })
    })
    __TaskViewer.refreshTask && __TaskViewer.refreshTask()
  }
}

class ValueTagsEditor extends React.Component {
  static _COLORS = ['#4285f4', '#34a853', '#6a70b8', '#009c95', '#fbbc05', '#ea4335']

  state = { ...this.props, editMode: false, useColor: ValueTagsEditor._COLORS[0] }

  render() {
    return (
      <div>
        {this._renderList()}
        {this._renderEditor()}
      </div>
    )
  }

  _renderList() {
    return (
      <div className={`tags-list ${this.state.editMode ? 'hide' : ''}`}>
        <div className="search-tag pt-2 pb-2 pl-3 pr-3">
          <div className="input-group input-search w-100">
            <input
              type="text"
              className="form-control"
              maxLength="20"
              placeholder={$L('搜索')}
              ref={(c) => (this._searchTagName = c)}
              value={this.state.searchTagName || ''}
              onChange={(e) => this.setState({ searchTagName: e.target.value })}
              onKeyDown={(e) => e.keyCode === 13 && this.toggleEditMode(true)}
            />
            <span className="input-group-btn">
              <button className="btn btn-secondary" type="button" onClick={() => this.toggleEditMode(true)}>
                <i className="icon zmdi zmdi-plus text-primary" />
              </button>
            </span>
          </div>
        </div>
        <div className="rb-scroller" ref={(c) => (this._scroller = c)}>
          {(this.state.tagList || []).map((item) => {
            if (!this.state.searchTagName || item.name.includes(this.state.searchTagName)) {
              const colorStyle = { backgroundColor: item.color }
              return (
                <li className="dropdown-item" key={item.id} onClick={() => this._saveRelated(item)}>
                  <i style={colorStyle} />
                  <span>{item.name}</span>
                  {item.isManageable && (
                    <a onClick={() => this.toggleEditMode(true, item)} title={$L('编辑')}>
                      <i className="zmdi zmdi-edit" />
                    </a>
                  )}
                </li>
              )
            } else {
              return null
            }
          })}
        </div>
      </div>
    )
  }

  _renderEditor() {
    return (
      <div className={`tags-editor ${this.state.editMode ? '' : 'hide'}`}>
        <h5 className="pt-2 pb-2 pl-3 pr-3 m-0">
          <a onClick={() => this.toggleEditMode(false)}>
            <i className="zmdi zmdi-chevron-left" />
          </a>
          {this.state.tagId ? $L('编辑标签') : $L('添加标签')}
        </h5>
        <div className="p-3 pt-0">
          <div>
            <input
              type="text"
              className="form-control form-control-sm"
              maxLength="20"
              ref={(c) => (this._createTagName = c)}
              value={this.state.createTagName || ''}
              onChange={(e) => this.setState({ createTagName: e.target.value })}
              onKeyDown={(e) => e.keyCode === 13 && this._saveTag()}
            />
          </div>
          <div className="colors pt-2 pb-2 text-center">
            {ValueTagsEditor._COLORS.map((color) => {
              return (
                <a key={color} style={{ backgroundColor: color }} onClick={() => this.setState({ useColor: color })}>
                  {this.state.useColor === color && <i className="zmdi zmdi-check" />}
                </a>
              )
            })}
          </div>
          <div className="row">
            <div className="col">
              <button typeof="button" className="btn btn-primary w-100" onClick={() => this._saveTag()}>
                {$L('确定')}
              </button>
            </div>
            {this.state.tagId && (
              <div className="col pl-0">
                <button typeof="button" className="btn btn-danger btn-outline w-100" onClick={() => this._deleteTag(this.state.tagId)}>
                  {$L('删除')}
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    )
  }

  toggleEditMode(editMode, tag) {
    let state = { editMode: editMode }
    if (typeof tag === 'object') {
      state = { ...state, createTagName: tag.name, tagId: tag.id, useColor: tag.color }
    } else {
      state = { ...state, createTagName: this.state.searchTagName || '', tagId: null, useColor: ValueTagsEditor._COLORS[0] }
    }

    this.setState(state)
    setTimeout(() => {
      if (this.state.editMode) $(this._createTagName).focus()
      else $(this._searchTagName).focus()
    }, 100)
  }

  componentDidMount() {
    $(this._scroller).perfectScrollbar()
    setTimeout(() => {
      $(this._searchTagName).focus()
    }, 1000)

    this.fetchTags()
  }

  fetchTags() {
    $.get(`/project/tags/list?project=${this.props.projectId}`, (res) => {
      this.setState({ tagList: res.data || [] })
    })
  }

  _deleteTag(tagId) {
    const that = this
    RbAlert.create($L('确认删除此标签？'), {
      confirm: function () {
        this.disabled(true)
        $.post(`/app/entity/common-delete?id=${tagId}`, () => {
          that.fetchTags()
          that.toggleEditMode(false)
          that.props.$$$parent.refreshTags()
          this.hide()
        })
      },
    })
  }

  _saveTag() {
    const data = {
      tagName: this.state.createTagName,
      color: this.state.useColor,
      projectId: this.props.projectId,
      metadata: { entity: 'ProjectTaskTag', id: this.state.tagId },
    }

    $.post(`/project/tags/create?task2=${this.props.taskId}`, JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        this.fetchTags()
        this.toggleEditMode(false)
        this.props.$$$parent.refreshTags()
        this.setState({ createTagName: null, useColor: ValueTagsEditor._COLORS[0] })
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }

  _saveRelated(tag) {
    $.post(`/project/tags/related-add?task=${this.props.taskid}&tag=${tag.id}`, (res) => {
      if (res.error_code === 0) {
        res.data.rid && this.props.$$$parent.refreshTags()
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

// 关联记录
class ValueRelatedRecord extends ValueComp {
  state = { ...this.props }

  renderElement() {
    if (this.state.editMode) {
      return (
        <div className="row">
          <div className="col-9">
            <AnyRecordSelector initValue={this.state.relatedRecordData} ref={(c) => (this._relatedRecord = c)} />
          </div>
          <div className="col-3" style={{ paddingTop: '0.18rem' }}>
            <button onClick={() => this.setState({ editMode: false })} className="btn btn-sm btn-link mr-1">
              {$L('取消')}
            </button>
            <button className="btn btn-sm btn-primary" onClick={() => this.handleChange()}>
              {$L('确定')}
            </button>
          </div>
        </div>
      )
    } else {
      return this.renderViewElement(true)
    }
  }

  renderViewElement(useEdit) {
    const data = this.state.relatedRecordData
    if (data) {
      return (
        <div className={`form-control-plaintext ${useEdit ? 'hover' : ''}`} onClick={() => useEdit && this.setState({ editMode: true })}>
          <a href={`${rb.baseUrl}/app/list-and-view?id=${data.id}`} title={$L('查看记录')} target="_blank" onClick={(e) => $stopEvent(e)}>
            {data.text}
          </a>
        </div>
      )
    } else {
      return (
        <div className={`form-control-plaintext text-muted ${useEdit ? 'hover' : ''}`} onClick={() => useEdit && this.setState({ editMode: true })}>
          {$L('无')}
        </div>
      )
    }
  }

  handleChange() {
    const value = this._relatedRecord.value() || null
    super.handleChange({ target: { name: 'relatedRecord', value: value ? value.id : null } }, () => {
      this.setState({ editMode: false, relatedRecordData: value })
    })
  }
}

// --

// 评论列表
class TaskCommentsList extends React.Component {
  state = { ...this.props }

  render() {
    if ((this.state.comments || []).length === 0) return null
    return (
      <div className="comment-list-wrap">
        <h5 className="text-bold">
          <i className="zmdi zmdi-comments label-icon down-2" />
          {$L('评论列表')} ({this.state.comments.length})
        </h5>
        <div className="feeds-list comment-list">
          {this.state.comments.map((item) => {
            const id = `comment-${item.id}`
            return (
              <div key={id} id={id}>
                <div className="feeds">
                  <div className="user">
                    <a className="user-show">
                      <div className="avatar">
                        <img alt="Avatar" src={`${rb.baseUrl}/account/user-avatar/${item.createdBy[0]}`} />
                      </div>
                    </a>
                  </div>
                  <div className="content">
                    <div className="meta">
                      <a>{item.createdBy[1]}</a>
                    </div>
                    {RichTextEditor.renderRichContent(item)}
                    <div className="actions">
                      <div className="float-left text-muted fs-12 time">
                        <DateShow date={item.createdOn} />
                      </div>
                      {this.props.editable && (
                        <ul className="list-unstyled m-0">
                          {item.isManageable && (
                            <li className="list-inline-item mr-3">
                              <a href="#" onClick={() => this._handleDelete(item)} className="fixed-icon danger-hover">
                                <i className="zmdi zmdi-delete" /> {$L('删除')}
                              </a>
                            </li>
                          )}
                          <li className="list-inline-item">
                            <a href="#" onClick={() => this._handleReply(item)} className="fixed-icon">
                              <i className="zmdi zmdi-mail-reply" /> {$L('回复')}
                            </a>
                          </li>
                        </ul>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      </div>
    )
  }

  componentDidMount = () => this.fetchComments()

  fetchComments() {
    $.get(`/project/comments/list?task=${this.props.taskid}`, (res) => this.setState({ comments: res.data }))
  }

  _handleReply(item) {
    __TaskComment.commentState(true, `@${item.createdBy[1]} : `)
  }

  _handleDelete(item) {
    const that = this
    RbAlert.create($L('确认删除此评论？'), {
      type: 'danger',
      confirmText: $L('删除'),
      confirm: function () {
        $.post(`/app/entity/common-delete?id=${item.id}`, (res) => {
          this.hide()
          if (res.error_code !== 0) return RbHighbar.error(res.error_msg)
          const ss = that.state.comments.filter((x) => x.id !== item.id)
          that.setState({ comments: ss })
        })
      },
    })
  }
}

// 任务评论
class TaskComment extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="comments">
        <div className="comment-reply">
          <div onClick={() => this.commentState(true)} className={`reply-mask ${this.state.openComment && 'hide'}`}>
            {$L('添加评论')}
          </div>
          <span className={`${!this.state.openComment && 'hide'}`}>
            <RichTextEditor placeholder={$L('添加评论')} ref={(c) => (this._RichTextEditor = c)} />
            <div className="mt-2 text-right" ref={(c) => (this._btns = c)}>
              <button onClick={() => this.commentState(false)} className="btn btn-sm btn-link mr-1">
                {$L('取消')}
              </button>
              <button className="btn btn-sm btn-primary" ref={(c) => (this._btn = c)} onClick={() => this._post()}>
                {$L('评论')}
              </button>
            </div>
          </span>
        </div>
      </div>
    )
  }

  commentState = (state, initValue) => {
    this.setState({ openComment: state }, () => this.state.openComment && this._RichTextEditor.focus(initValue))
  }

  _post() {
    const _data = this._RichTextEditor.vals()
    if (!_data.content) return RbHighbar.create($L('请输入评论内容'))

    _data.taskId = this.props.id
    _data.metadata = { entity: 'ProjectTaskComment' }

    $.post('/app/entity/common-save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        this._RichTextEditor.reset()
        this.commentState(false)
        typeof this.props.call === 'function' && this.props.call()
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

// ~ 编辑框
// @see feeds-post.js
class RichTextEditor extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }

    this.__es = []
    for (let k in EMOJIS) {
      const item = EMOJIS[k]
      this.__es.push(
        <a key={`em-${item}`} title={k} onClick={() => this._selectEmoji(k)}>
          <img src={`${rb.baseUrl}/assets/img/emoji/${item}`} alt={k} />
        </a>
      )
    }
  }

  render() {
    return (
      <React.Fragment>
        <div className={`rich-editor ${this.state.focus ? 'active' : ''}`}>
          <textarea
            ref={(c) => (this._$editor = c)}
            placeholder={this.props.placeholder}
            maxLength="1000"
            onFocus={() => this.setState({ focus: true })}
            onBlur={() => this.setState({ focus: false })}
            onKeyDown={(e) => this._handleInputAt(e)}
            defaultValue={this.props.initValue}
          />

          <div className="action-btns">
            <ul className="list-unstyled list-inline m-0 p-0">
              <li className="list-inline-item use-dropdown">
                <a title={$L('表情')} data-toggle="dropdown">
                  <i className="zmdi zmdi-mood" />
                </a>
                <div className="dropdown-menu">
                  <div className="emoji-wrapper">{this.__es}</div>
                </div>
              </li>
              <li className="list-inline-item">
                <UserSelector
                  hideDepartment
                  hideRole
                  hideTeam
                  hideSelection
                  multiple={false}
                  ref={(c) => (this._UserSelector = c)}
                  compToggle={
                    <a title={'@' + $L('用户')} data-toggle="dropdown">
                      <i className="zmdi at-text">@</i>
                    </a>
                  }
                  targetInput={this._$editor}
                  onSelectItem={this._selectAtUser}
                />
              </li>
              <li className="list-inline-item">
                <a title={$L('附件')} onClick={() => this._$fileInput.click()}>
                  <i className="zmdi zmdi-attachment-alt zmdi-hc-rotate-45" />
                </a>
              </li>
            </ul>
          </div>
        </div>
        <span className="hide">
          <input type="file" ref={(c) => (this._$fileInput = c)} />
        </span>

        {(this.state.files || []).length > 0 && (
          <div className="attachment">
            <div className="file-field attachments">
              {(this.state.files || []).map((item) => {
                const fileName = $fileCutName(item)
                return (
                  <div key={'file-' + item} className="img-thumbnail" title={fileName}>
                    <i className="file-icon" data-type={$fileExtName(fileName)} />
                    <span>{fileName}</span>
                    <b title={$L('移除')} onClick={() => this._removeFile(item)}>
                      <span className="zmdi zmdi-close" />
                    </b>
                  </div>
                )
              })}
            </div>
          </div>
        )}
      </React.Fragment>
    )
  }

  UNSAFE_componentWillReceiveProps = (props) => this.setState(props)

  componentDidMount() {
    autosize(this._$editor)
    setTimeout(() => this.props.initValue && autosize.update(this._$editor), 200)

    let mp
    const mp_end = function () {
      if (mp) mp.end()
      mp = null
    }

    $createUploader(
      this._$fileInput,
      (res) => {
        if (!mp) mp = new Mprogress({ template: 1, start: true })
        mp.set(res.percent / 100)
      },
      (res) => {
        mp_end()
        const files = this.state.files || []
        files.push(res.key)
        this.setState({ files: files })
      },
      () => mp_end()
    )
  }

  _selectEmoji(emoji) {
    $(this._$editor).insertAtCursor(`[${emoji}] `)
    this.setState({ showEmoji: false })
  }

  _selectAtUser = (s) => {
    const text = this.__lastInputKey === '@' ? `${s.text} ` : `@${s.text} `
    $(this._$editor).insertAtCursor(text)
    this.setState({ showAtUser: false })
  }

  _handleInputAt(e) {
    if (this._handleInput__Timer) {
      clearTimeout(this._handleInput__Timer)
      this._handleInput__Timer = null
    }

    this.__lastInputKey = e.key
    if (e.key === '@') {
      this._handleInput__Timer = setTimeout(() => this._UserSelector.toggle('show'), 400)
    }
  }

  _removeFile(file) {
    const files = this.state.files
    files.remove(file)
    this.setState({ files: files })
  }

  val() {
    return $(this._$editor).val()
  }

  vals() {
    return {
      content: this.val(),
      attachments: this.state.files,
    }
  }

  focus(initValue) {
    if (typeof initValue !== 'undefined') {
      setTimeout(() => autosize.update(this._$editor), 100)
      $(this._$editor).val(initValue)
    }
    $(this._$editor).selectRange(9999, 9999) // Move to last
  }

  reset() {
    $(this._$editor).val('')
    autosize.update(this._$editor)
    this.setState({ files: null, images: null })
  }

  /**
   * 渲染内容
   * @param {*} data
   */
  static renderRichContent(data) {
    // 表情和换行不在后台转换，因为不同客户端所需的格式不同
    const contentHtml = data.content ? $converEmoji(data.content.replace(/\n/g, '<br />')) : $L('点击添加')
    return (
      <div className="rich-content">
        <div className="texts text-break" dangerouslySetInnerHTML={{ __html: contentHtml }} />
        {(data.attachments || []).length > 0 && (
          <div className="file-field">
            {data.attachments.map((item) => {
              const fileName = $fileCutName(item)
              return (
                <a key={'file-' + item} title={fileName} onClick={() => (parent || window).RbPreview.create(item)} className="img-thumbnail">
                  <i className="file-icon" data-type={$fileExtName(fileName)} />
                  <span>{fileName}</span>
                </a>
              )
            })}
          </div>
        )}
      </div>
    )
  }
}
