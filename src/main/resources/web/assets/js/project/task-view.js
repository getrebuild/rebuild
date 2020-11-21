/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global autosize, EMOJIS */

const wpc = window.__PageConfig

const __HOLDER = {
  hide: () => window.close(),
  setLoadingState: () => {},
}
const __TaskViewer = parent && parent.TaskViewModal ? parent.TaskViewModal.__HOLDER : __HOLDER

let __TaskContent
let __TaskComment

$(document).ready(() => {
  renderRbcomp(<TaskForm id={wpc.taskId} editable={wpc.isMember} manageable={wpc.isManageable} />, 'task-contents', function () {
    __TaskContent = this
  })
  if (wpc.isMember) {
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
          {this.props.editable && this.props.manageable && (
            <div className="col-2 text-right">
              <button className="btn btn-secondary" style={{ minWidth: 80, marginTop: 2 }} data-toggle="dropdown">
                {$L('Operation')} <i className="icon zmdi zmdi-more-vert"></i>
              </button>
              <div className="dropdown-menu dropdown-menu-right">
                <a className="dropdown-item" onClick={() => this._handleDelete()}>
                  {$L('Delete')}
                </a>
              </div>
            </div>
          )}
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">
            <i className="icon zmdi zmdi-square-o" /> {$L('Status')}
          </label>
          <div className="col-12 col-sm-9">
            <ValueStatus status={this.state.status} $$$parent={this} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">
            <i className="icon zmdi zmdi-account-o" /> {$L('Executor')}
          </label>
          <div className="col-12 col-sm-9">
            <ValueExecutor executor={this.state.executor} $$$parent={this} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">
            <i className="icon zmdi zmdi-time" /> {$L('Deadline')}
          </label>
          <div className="col-12 col-sm-9">
            <ValueDeadline deadline={this.state.deadline} $$$parent={this} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">
            <i className="icon zmdi zmdi-comment-more" /> {$L('Description')}
          </label>
          <div className="col-12 col-sm-9">
            <ValueDescription description={this.state.description} $$$parent={this} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">
            <i className="icon zmdi zmdi-circle-o" /> {$L('Priority')}
          </label>
          <div className="col-12 col-sm-9">
            <ValuePriority priority={this.state.priority} $$$parent={this} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">
            <i className="icon zmdi zmdi-attachment-alt zmdi-hc-rotate-45 mt-1" /> {$L('TaskTag')}
          </label>
          <div className="col-12 col-sm-9">{this.state.projectId && <ValueTags tags={this.state.tags} projectId={this.state.projectId} taskid={this.props.id} $$$parent={this} />}</div>
        </div>
        <div className="form-group row">
          <label className="col-12 col-sm-3 col-form-label">
            <i className="icon zmdi zmdi-attachment-alt zmdi-hc-rotate-45 mt-1" /> {$L('Attachment')}
          </label>
          <div className="col-12 col-sm-9">
            <ValueAttachments attachments={this.state.attachments} $$$parent={this} />
          </div>
        </div>
        <TaskCommentsList taskid={this.props.id} ref={(c) => (this._TaskCommentsList = c)} />
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
    RbAlert.create($L('DeleteSomeConfirm,Task'), {
      type: 'danger',
      confirmText: $L('Delete'),
      confirm: function () {
        this.disabled(true)
        $.post(`/app/entity/common-delete?id=${that.props.id}`, (res) => {
          if (res.error_code === 0) {
            this.hide()
            RbHighbar.success($L('SomeDeleted,Task'))
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
      } else RbHighbar.error(res.error_msg)
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
      <div className={`task-title ${editable ? 'hover' : ''}`} onClick={() => editable && this.setState({ editMode: true }, () => this._taskName.focus())}>
        {this.state.taskName}
      </div>
    )
  }

  handleChange(e) {
    const value = e.target.value
    if (!value) {
      RbHighbar.create($L('SomeNotEmpty,TaskName'))
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
          <label className="custom-control custom-checkbox custom-control-inline" onClick={(e) => $stopEvent(e)}>
            <input className="custom-control-input" type="checkbox" ref={(c) => (this._status = c)} onChange={(e) => this._handleChangeStatus(e)} />
            <span className="custom-control-label">{$L(this.state.status > 0 ? 'Finished' : 'UnFinished')}</span>
          </label>
        </span>
      </div>
    )
  }

  renderViewElement() {
    return <div className="form-control-plaintext">{$L(this.state.status > 0 ? 'Finished' : 'UnFinished')}</div>
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
class ValueExecutor extends ValueComp {
  state = { ...this.props }

  renderElement() {
    return (
      <React.Fragment>
        {this.state.executor ? (
          <div className="executor-show">
            <UserSelector
              hideDepartment={true}
              hideRole={true}
              hideTeam={true}
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
            <a className="close" onClick={() => this.handleChange(null)} title={$L('RemoveSome,Executor')}>
              &times;
            </a>
          </div>
        ) : (
          <div className="form-control-plaintext">
            <UserSelector
              hideDepartment={true}
              hideRole={true}
              hideTeam={true}
              multiple={false}
              ref={(c) => (this._UserSelector = c)}
              compToggle={
                <a className="tag-value arrow placeholder" data-toggle="dropdown">
                  {$L('SelectSome,Executor')}
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
    return this._renderValue() || <div className="form-control-plaintext text-muted">{$L('Null')}</div>
  }

  _renderValue(call) {
    return typeof this.state.executor === 'object' && <UserShow id={this.state.executor[0]} name={this.state.executor[1]} showName onClick={() => typeof call === 'function' && call()} />
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
          {this._renderValue($L('SelectSome,Deadline'))}
        </a>
      </div>
    )
  }

  renderViewElement() {
    return this._renderValue(<div className="form-control-plaintext text-muted">{$L('Null')}</div>)
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
          <TextEditor hideToolbar={true} ref={(c) => (this._editor = c)} />
          <div className="mt-2 text-right">
            <button onClick={() => this._handleEditMode(false)} className="btn btn-sm btn-link mr-1">
              {$L('Cancel')}
            </button>
            <button className="btn btn-sm btn-primary" onClick={() => this.handleChange()}>
              {$L('Confirm')}
            </button>
          </div>
        </div>
      )
    } else {
      return (
        <div className="form-control-plaintext desc hover" onClick={() => this._handleEditMode(true)}>
          {this.state.description ? TextEditor.renderRichContent({ content: this.state.description }) : <span className="text-muted">{$L('ClickAdd')}</span>}
        </div>
      )
    }
  }

  renderViewElement() {
    return (
      <div className="form-control-plaintext desc">{this.state.description ? TextEditor.renderRichContent({ content: this.state.description }) : <span className="text-muted">{$L('Null')}</span>}</div>
    )
  }

  _handleEditMode(editMode) {
    this.setState({ editMode: editMode }, () => this.state.editMode && this._editor.focus(this.state.description))
  }

  handleChange() {
    const value = this._editor.val()
    super.handleChange({ target: { name: 'description', value: value } }, () => this.setState({ description: value, editMode: false }))
  }
}

const __PRIORITIES = {
  0: $L('TaskPriority0'),
  1: $L('TaskPriority1'),
  2: $L('TaskPriority2'),
  3: $L('TaskPriority3'),
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
        <span className={`tag-value arrow priority-${this.state.priority}`} data-toggle="dropdown">
          {__PRIORITIES[this.state.priority]}
        </span>
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
          <input type="file" className="inputfile" id="attachments" ref={(c) => (this._attachments = c)} data-maxsize="102400000" />
          <label htmlFor="attachments" style={{ padding: 0, border: 0, lineHeight: 1 }}>
            <a className="tag-value upload hover">+ {$L('Upload')}</a>
          </label>
        </div>
        {this._renderValue(true)}
      </React.Fragment>
    )
  }

  renderViewElement() {
    return this._renderValue() || <div className="form-control-plaintext text-muted">{$L('Null')}</div>
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
                  <b title={$L('Delete')} onClick={(e) => this._deleteAttachment(item, e)}>
                    <span className="zmdi zmdi-delete"></span>
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
    RbAlert.create($L('DeleteSomeConfirm,Attachment'), {
      type: 'danger',
      confirmText: $L('Delete'),
      confirm: function () {
        this.hide()
        const s = that.state.attachments.filter((x) => x !== item)
        that.handleChange({ target: { name: 'attachments', value: s } })
      },
    })
  }

  componentDidMount() {
    let mp = false
    $createUploader(
      this._attachments,
      () => {
        if (!mp) {
          $mp.start()
          mp = true
        }
      },
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
class ValueTags extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="form-control-plaintext task-tags">
        <React.Fragment>
          {(this.state.tags || []).map((item) => {
            const _color = { color: item.color, borderColor: item.color }
            return (
              <span className="tag-value" key={item.id} style={_color}>
                {item.name}
                <a title={$L('Remove')} onClick={() => this._delRelated(item.rid)}>
                  <i className="zmdi zmdi-close"></i>
                </a>
              </span>
            )
          })}
        </React.Fragment>
        <span className="dropdown" ref={(c) => (this._dropdown = c)}>
          <a className="tag-add" title={$L('ClickAdd')} data-toggle="dropdown">
            <i className="zmdi zmdi-plus"></i>
          </a>
          <div className="dropdown-menu tags">{<ValueTagsEditor ref={(c) => (this._ValueTagsEditor = c)} projectId={this.props.projectId} taskid={this.props.taskid} />}</div>
        </span>
      </div>
    )
  }

  componentDidMount() {
    const that = this
    $(this._dropdown).on({
      'hide.bs.dropdown': function (e) {
        if (!e.clickEvent || !e.clickEvent.target) return
        const $target = $(e.clickEvent.target)
        if ($target.hasClass('dropdown-menu') || $target.parents('.dropdown-menu').length === 1) {
          return false
        }
      },
      'hiden.bs.dropdown': function () {
        that._ValueTagsEditor.toggleEditMode(false)
      },
    })
  }

  _delRelated(rid) {
    $.post(`/project/tags/related-del?relation=${rid}`, (res) => {
      if (res.error_code === 0) {
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
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
          <div className="input-group">
            <input
              type="text"
              className="form-control form-control-sm"
              placeholder={$L('Search')}
              ref={(c) => (this._searchTagName = c)}
              value={this.state.searchTagName || ''}
              onChange={(e) => this.setState({ searchTagName: e.target.value })}
            />
            <span className="input-group-append">
              <button className="btn btn-secondary" type="button" onClick={() => this.setState({ editMode: true })}>
                <i className="icon zmdi zmdi-plus"></i>
              </button>
            </span>
          </div>
        </div>
        <div className="rb-scroller" ref={(c) => (this._scroller = c)}>
          {(this.state.tagList || []).map((item) => {
            if (!this.state.searchTagName || item.name.includes(this.state.searchTagName)) {
              return (
                <li className="dropdown-item" key={item.id} onClick={() => this._saveRelated(item.id)}>
                  {item.name}
                  <a onClick={() => this.toggleEditMode(true, item)} title={$L('Edit')}>
                    <i className="zmdi zmdi-edit"></i>
                  </a>
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
          {$L(this.state.tagId ? 'EditSome,TaskTag' : 'AddSome,TaskTag')}
        </h5>
        <div className="p-3 pt-0">
          <div>
            <input
              type="text"
              className="form-control form-control-sm"
              ref={(c) => (this._createTagName = c)}
              value={this.state.createTagName || ''}
              onChange={(e) => this.setState({ createTagName: e.target.value })}
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
          <div>
            <button typeof="button" className="btn btn-primary w-100" onClick={() => this.saveTag()}>
              {$L('Confirm')}
            </button>
          </div>
        </div>
      </div>
    )
  }

  toggleEditMode(editMode, tag) {
    let state = { editMode: editMode }
    if (typeof tag === 'object') {
      state = { ...state, tagId: tag.id, createTagName: tag.name, useColor: tag.color }
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

  saveTag() {
    const data = {
      tagName: this.state.createTagName,
      color: this.state.useColor,
      projectId: this.props.projectId,
      metadata: { entity: 'ProjectTaskTag', id: this.state.tagId },
    }

    $.post(`/project/tags/create?task=${this.props.taskId2}`, JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        this.fetchTags()
        this.toggleEditMode(false)
        this.setState({ createTagName: null, useColor: ValueTagsEditor._COLORS[0] })
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }

  _saveRelated(tagId) {
    $.post(`/project/tags/related-add?task=${this.props.taskid}&tag=${tagId}`, (res) => {
      if (res.error_code === 0) {
      } else {
        RbHighbar.error(res.error_msg)
      }
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
        <h4>{$L('SomeList,Comment')}</h4>
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
                    {TextEditor.renderRichContent(item)}
                    <div className="actions">
                      <div className="float-left text-muted fs-12 time">
                        <DateShow date={item.createdOn} />
                      </div>
                      <ul className="list-unstyled m-0">
                        {item.self && (
                          <li className="list-inline-item mr-2">
                            <a href="#" onClick={() => this._handleDelete(item)} className="fixed-icon">
                              <i className="zmdi zmdi-delete" /> {$L('Delete')}
                            </a>
                          </li>
                        )}
                        <li className="list-inline-item">
                          <a href="#" onClick={() => this._handleReply(item)} className="fixed-icon">
                            <i className="zmdi zmdi-mail-reply" /> {$L('Reply')}
                          </a>
                        </li>
                      </ul>
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
    __TaskComment.commentState(true, `@${item.createdBy[1]} `)
  }

  _handleDelete(item) {
    const that = this
    RbAlert.create($L('DeleteSomeConfirm,Comment'), {
      type: 'danger',
      confirmText: $L('Delete'),
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
            {$L('AddSome,Comment')}
          </div>
          <span className={`${!this.state.openComment && 'hide'}`}>
            <TextEditor placeholder={$L('AddSome,Comment')} ref={(c) => (this._editor = c)} />
            <div className="mt-2 text-right" ref={(c) => (this._btns = c)}>
              <button onClick={() => this.commentState(false)} className="btn btn-sm btn-link">
                {$L('Cancel')}
              </button>
              <button className="btn btn-sm btn-primary" ref={(c) => (this._btn = c)} onClick={() => this._post()}>
                {$L('Comment')}
              </button>
            </div>
          </span>
        </div>
      </div>
    )
  }

  commentState = (state, initValue) => {
    this.setState({ openComment: state }, () => this.state.openComment && this._editor.focus(initValue))
  }

  _post() {
    const _data = this._editor.vals()
    if (!_data.content) return RbHighbar.create($L('PlsInputSome,CommentContent'))

    _data.taskId = this.props.id
    _data.metadata = { entity: 'ProjectTaskComment' }

    $.post('/app/entity/common-save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        this._editor.reset()
        this.commentState(false)
        typeof this.props.call === 'function' && this.props.call()
      } else RbHighbar.error(res.error_msg)
    })
  }
}

// ~ 编辑框
// @see feeds-post.js
class TextEditor extends React.Component {
  state = { ...this.props }

  constructor(props) {
    super(props)

    this.__es = []
    for (let k in EMOJIS) {
      const item = EMOJIS[k]
      this.__es.push(
        <a key={`em-${item}`} title={k} onClick={() => this._selectEmoji(k)}>
          <img src={`${rb.baseUrl}/assets/img/emoji/${item}`} />
        </a>
      )
    }
  }

  render() {
    return (
      <React.Fragment>
        <div className={`rich-editor ${this.state.focus ? 'active' : ''}`}>
          <textarea
            ref={(c) => (this._textarea = c)}
            placeholder={this.props.placeholder}
            maxLength="2000"
            onFocus={() => this.setState({ focus: true })}
            onBlur={() => this.setState({ focus: false })}
            defaultValue={this.props.initValue}
          />
          {!this.props.hideToolbar && (
            <div className="action-btns">
              <ul className="list-unstyled list-inline m-0 p-0">
                <li className="list-inline-item use-dropdown">
                  <a title={$L('Emoji')} data-toggle="dropdown">
                    <i className="zmdi zmdi-mood" />
                  </a>
                  <div className="dropdown-menu">
                    <div className="emoji-wrapper">{this.__es}</div>
                  </div>
                </li>
                <li className="list-inline-item">
                  <UserSelector
                    hideDepartment={true}
                    hideRole={true}
                    hideTeam={true}
                    hideSelection={true}
                    multiple={false}
                    ref={(c) => (this._UserSelector = c)}
                    compToggle={
                      <a title={'@' + $L('User')} data-toggle="dropdown">
                        <i className="zmdi at-text">@</i>
                      </a>
                    }
                    onSelectItem={this._selectAtUser}
                  />
                </li>
                <li className="list-inline-item">
                  <a title={$L('Attachment')} onClick={() => this._fileInput.click()}>
                    <i className="zmdi zmdi-attachment-alt zmdi-hc-rotate-45" />
                  </a>
                </li>
              </ul>
            </div>
          )}
        </div>
        {(this.state.files || []).length > 0 && (
          <div className="attachment">
            <div className="file-field attachments">
              {(this.state.files || []).map((item) => {
                const fileName = $fileCutName(item)
                return (
                  <div key={'file-' + item} className="img-thumbnail" title={fileName}>
                    <i className="file-icon" data-type={$fileExtName(fileName)} />
                    <span>{fileName}</span>
                    <b title={$L('Remove')} onClick={() => this._removeFile(item)}>
                      <span className="zmdi zmdi-close"></span>
                    </b>
                  </div>
                )
              })}
            </div>
          </div>
        )}
        <span className="hide">
          <input type="file" ref={(c) => (this._fileInput = c)} data-maxsize="102400000" />
        </span>
      </React.Fragment>
    )
  }

  UNSAFE_componentWillReceiveProps = (props) => this.setState(props)

  componentDidMount() {
    if (this.hideToolbar) return

    autosize(this._editor)
    setTimeout(() => this.props.initValue && autosize.update(this._editor), 200)

    let mp
    const mp_end = function () {
      if (mp) mp.end()
      mp = null
    }

    $createUploader(
      this._fileInput,
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
    $(this._textarea).insertAtCursor(`[${emoji}]`)
    this.setState({ showEmoji: false })
  }

  _selectAtUser = (s) => {
    $(this._textarea).insertAtCursor(`@${s.text} `)
    this.setState({ showAtUser: false })
  }

  _removeFile(file) {
    const files = this.state.files
    files.remove(file)
    this.setState({ files: files })
  }

  val() {
    return $(this._textarea).val()
  }

  vals() {
    return {
      content: this.val(),
      attachments: this.state.files,
    }
  }

  focus(initValue) {
    if (typeof initValue !== 'undefined') {
      setTimeout(() => autosize.update(this._textarea), 100)
      $(this._textarea).val(initValue)
    }
    $(this._textarea).selectRange(9999, 9999) // Move to last
  }

  reset() {
    $(this._textarea).val('')
    autosize.update(this._textarea)
    this.setState({ files: null, images: null })
  }

  /**
   * 渲染内容
   * @param {*} data
   */
  static renderRichContent(data) {
    // 表情和换行不在后台转换，因为不同客户端所需的格式不同
    const contentHtml = data.content ? $converEmoji(data.content.replace(/\n/g, '<br />')) : $L('ClickAdd')
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
