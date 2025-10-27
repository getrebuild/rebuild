/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global autosize */

// ~ 动态发布
// eslint-disable-next-line no-unused-vars
class FeedsPost extends React.Component {
  state = { ...this.props, type: 1 }

  render() {
    const activeType = this.state.type
    const activeClass = 'text-primary text-bold'

    return (
      <div className="feeds-post">
        <ul className="list-unstyled list-inline mb-1 pl-1" ref={(c) => (this._$activeType = c)}>
          <li className="list-inline-item">
            <a onClick={() => this.setState({ type: 1 })} className={`${activeType === 1 ? activeClass : ''}`}>
              {$L('动态')}
            </a>
          </li>
          <li className="list-inline-item">
            <a onClick={() => this.setState({ type: 2 })} className={`${activeType === 2 ? activeClass : ''}`}>
              {$L('跟进')}
            </a>
          </li>
          <li className="list-inline-item">
            <a onClick={() => this.setState({ type: 4 })} className={`${activeType === 4 ? activeClass : ''}`}>
              {$L('日程')}
            </a>
          </li>
          {rb.isAdminUser && (
            <li className="list-inline-item">
              <a onClick={() => this.setState({ type: 3 })} className={`${activeType === 3 ? activeClass : ''}`}>
                {$L('公告')}
              </a>
            </li>
          )}
        </ul>
        <div className="arrow_box" ref={(c) => (this._$activeArrow = c)} />

        <div>
          <FeedsEditor ref={(c) => (this._FeedsEditor = c)} type={activeType} />
        </div>

        <div className="mt-3">
          <div className="float-right">
            <button className="btn btn-primary" ref={(c) => (this._$btn = c)} onClick={this._post}>
              <i className="mdi mdi-send icon mr-1" />
              {$L('发布')}
            </button>
          </div>
          <div className="float-right mr-4">
            <FeedsScope ref={(c) => (this.__FeedsScope = c)} />
          </div>
          <div className="clearfix" />
        </div>
      </div>
    )
  }

  componentDidUpdate(prevProps, prevState) {
    if (prevState.type !== this.state.type) {
      const pos = $(this._$activeType).find('.text-primary').position()
      $(this._$activeArrow).css('margin-left', pos.left - 30)
      setTimeout(() => this._FeedsEditor.focus(), 200)
    }
  }

  componentDidMount = () => $('#rb-feeds').attr('class', '')

  _post = () => {
    const _data = this._FeedsEditor.vals()
    if (!_data) return
    if (!_data.content) {
      if (_data.images && _data.images.length) _data.content = $fileCutName(_data.images[0])
      else if (_data.attachments && _data.attachments.length) _data.content = $fileCutName(_data.attachments[0])
      // v4.1
      if (!_data.content) return RbHighbar.create($L('请输入动态内容'))
    }

    _data.scope = this.__FeedsScope.val()
    if (_data.scope === false) return

    _data.type = this.state.type
    _data.metadata = { entity: 'Feeds', id: this.props.id }

    const $btn = $(this._$btn).button('loading')
    $.post('/feeds/post/publish', JSON.stringify(_data), (res) => {
      $btn.button('reset')
      if (res.error_code > 0) return RbHighbar.error(res.error_msg)

      this._FeedsEditor.reset()
      typeof this.props.call === 'function' && this.props.call()
    })
  }
}

// ~ 动态范围
class FeedsScope extends React.Component {
  constructor(props) {
    super(props)
    this.state = { scope: 'ALL', ...props }
    this._$items = {}
  }

  render() {
    return (
      <div className="btn-group border-0">
        <button className="btn btn-scope btn-link" data-toggle="dropdown" ref={(c) => (this._$btn = c)}>
          <i className="icon up-1 zmdi zmdi-chart-donut" />
          {$L('公开')}
        </button>
        <div className="dropdown-menu dropdown-menu-right">
          <a className="dropdown-item" onClick={this._selectScope} data-scope="ALL" title={$L('全部人员可见')} ref={(c) => (this._$items['ALL'] = c)}>
            <i className="icon up-1 zmdi zmdi-chart-donut" />
            {$L('公开')}
          </a>
          <a className="dropdown-item" onClick={this._selectScope} data-scope="SELF" title={$L('仅自己可见')} ref={(c) => (this._$items['SELF'] = c)}>
            <i className="icon up-1 zmdi zmdi-lock" />
            {$L('私密')}
          </a>
          <a className="dropdown-item" onClick={this._selectScope} data-scope="GROUP" title={$L('团队成员可见')} ref={(c) => (this._$items['GROUP'] = c)}>
            <i className="icon up-1 mdi mdi-briefcase-account" />
            {$L('团队')}
          </a>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const iv = this.props.initValue
    if (iv) {
      if (iv === 'ALL' || iv === 'SELF') {
        $(this._$btn).html($(this._$items[iv]).html())
      } else {
        this._renderGroupScope({ id: iv[0], name: iv[1] })
      }
    }
  }

  _selectScope = (e) => {
    const $target = e.target
    this.setState({ scope: $target.dataset.scope }, () => {
      $(this._$btn).html($($target).html())

      if (this.state.scope === 'GROUP') {
        if (this.__group) this._renderGroupScope(this.__group)
        const that = this
        if (this.__SelectGroup) {
          this.__SelectGroup.show()
        } else {
          renderRbcomp(<SelectGroup call={this._renderGroupScope} />, function () {
            that.__SelectGroup = this
          })
        }
      }
    })
  }

  _renderGroupScope = (item) => {
    if (!item) return
    $(this._$btn).html(`<i class="icon up-1 zmdi zmdi-accounts"></i>${item.name}`)
    this.__group = item
  }

  val() {
    let scope = this.state.scope
    if (scope === 'GROUP') {
      if (!this.__group) {
        RbHighbar.create($L('请选择团队'))
        return false
      }
      scope = this.__group.id
    }
    return scope
  }
}

// ~ 动态编辑框
class FeedsEditor extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }

    this.__es = []
    for (let k in RBEMOJIS) {
      const item = RBEMOJIS[k]
      this.__es.push(
        <a key={`em-${item}`} title={k} onClick={() => this._selectEmoji(k)}>
          <img src={`${rb.baseUrl}/assets/img/emoji/${item}`} alt={k} />
        </a>
      )
    }
  }

  render() {
    // 日程已完成
    const isFinish = this.state.type === 4 && this.props.contentMore && this.props.contentMore.finishTime

    return (
      <RF>
        {isFinish && <RbAlertBox message={$L('此日程已完成，修改后你需要重新将其完成')} />}

        <div className={`rich-editor ${this.state.focus ? 'active' : ''}`} ref={(c) => (this._$dropArea = c)}>
          <textarea
            ref={(c) => (this._$editor = c)}
            placeholder={this.props.placeholder}
            maxLength="2000"
            onFocus={() => this.setState({ focus: true })}
            onBlur={() => this.setState({ focus: false })}
            onKeyUp={(e) => this._handleInputAt(e)}
            defaultValue={this.props.initValue}
          />
          <div className="action-btns">
            <ul className="list-unstyled list-inline m-0 p-0">
              <li className="list-inline-item use-dropdown">
                <a onClick={() => this.setState({ renderEmoji: true })} title={$L('表情')} data-toggle="dropdown">
                  <i className="mdi mdi-emoticon-excited-outline" />
                </a>
                <div className="dropdown-menu">{this.state.renderEmoji && <div className="emoji-wrapper">{this.__es}</div>}</div>
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
                    <a title={`@${$L('用户')}`} data-toggle="dropdown" className="at-user pt-0">
                      <i className="mdi mdi-at" />
                    </a>
                  }
                  targetInput={this._$editor}
                  onSelectItem={this._selectAtUser}
                  requestAtAll
                />
              </li>
              <li className="list-inline-item">
                <a title={$L('图片')} onClick={() => this._$imageInput.click()}>
                  <i className="mdi mdi-image-outline" />
                </a>
              </li>
              <li className="list-inline-item">
                <a title={$L('附件')} onClick={() => this._$fileInput.click()}>
                  <i className="mdi mdi-attachment mdi-rotate-315" />
                </a>
              </li>
            </ul>
          </div>
        </div>
        <span className="hide">
          <input type="file" ref={(c) => (this._$fileInput = c)} multiple />
          <input type="file" ref={(c) => (this._$imageInput = c)} multiple accept="image/*" />
        </span>

        {this.state.type === 4 && <ScheduleOptions ref={(c) => (this._scheduleOptions = c)} initValue={this.state.contentMore} contentMore={this.state.contentMore} />}

        {(this.state.type === 2 || this.state.type === 4) && (
          <div className="feed-options related">
            <dl className="row">
              <dt className="col-12 col-lg-3 pt-2">{$L('关联记录')}</dt>
              <dd className="col-12 col-lg-9">
                <AnyRecordSelector ref={(c) => (this._selectRelated = c)} initValue={this.state.relatedRecord} />
              </dd>
            </dl>
          </div>
        )}

        {this.state.type === 3 && <AnnouncementOptions ref={(c) => (this._announcementOptions = c)} initValue={this.state.contentMore} />}

        {((this.state.images || []).length > 0 || (this.state.files || []).length > 0) && (
          <div className="attachment">
            <div className="img-field">
              {(this.state.images || []).map((item, idx) => {
                return (
                  <span key={`img-${item}`}>
                    <a title={$fileCutName(item)} className="img-thumbnail img-upload" onClick={() => this._filePreview(this.state.images, idx)}>
                      <img src={`${rb.baseUrl}/filex/img/${item}?imageView2/2/w/300/interlace/1/q/100`} />
                      <b title={$L('移除')} onClick={(e) => this._removeImage(item, e)}>
                        <span className="zmdi zmdi-close" />
                      </b>
                    </a>
                  </span>
                )
              })}
            </div>
            <div className="file-field">
              {(this.state.files || []).map((item) => {
                return <FileShow file={item} key={item} removeHandle={() => this._removeFile(item)} />
              })}
            </div>
          </div>
        )}
      </RF>
    )
  }

  UNSAFE_componentWillReceiveProps = (props) => this.setState(props)

  componentDidMount() {
    autosize(this._$editor)
    setTimeout(() => this.props.initValue && autosize.update(this._$editor), 200)

    const that = this
    const _complete = function (res, name) {
      const fs = that.state[name] || []
      let hasByName = $fileCutName(res.key)
      hasByName = fs.find((x) => $fileCutName(x) === hasByName)
      if (!hasByName) {
        fs.push(res.key)
        that.setState({ [name]: fs })
      }
    }

    $multipleUploader(this._$imageInput, (res) => _complete(res, 'images'))
    $multipleUploader(this._$fileInput, (res) => _complete(res, 'files'))

    function _dropUpload(files) {
      if (!files || files.length === 0) return false

      let $imgOrFile = that._$imageInput
      for (let file of files) {
        if ($isImage(file.name));
        else $imgOrFile = that._$fileInput
      }

      $imgOrFile.files = files
      $($imgOrFile).trigger('change')
    }
    $dropUpload(this._$dropArea, this._$editor, _dropUpload)
  }

  componentWillUnmount() {
    // super.componentWillUnmount()
    $(this._$editor).off('paste.file')
  }

  _filePreview(urlKey, idx) {
    const p = parent || window
    p.RbPreview.create(urlKey, idx)
  }

  _selectEmoji(emoji) {
    $(this._$editor).insertAtCursor(`[${emoji}] `)
    this.setState({ showEmoji: false })
  }

  _selectAtUser = (s) => {
    const text = this.__lastInputKey === '@' ? `${s.text} ` : `@${s.text} `
    $(this._$editor).insertAtCursor(text)
    this.setState({ showAtUser: false })
    this.__lastInputKey = null
  }

  _handleInputAt(e) {
    if (e.key === 'Shift' || e.key === 'Process') return true

    if (this._handleInput__Timer) {
      clearTimeout(this._handleInput__Timer)
      this._handleInput__Timer = null
    }

    if (e.key === '@') {
      this.__lastInputKey = '@'
      this._handleInput__Timer = setTimeout(() => this._UserSelector.toggle('show'), 200)
    } else {
      this.__lastInputKey = null
    }
  }

  _removeImage(image, e) {
    e && $stopEvent(e, true)
    const images = this.state.images
    images.remove(image)
    this.setState({ images: images })
  }

  _removeFile(file, e) {
    e && $stopEvent(e, true)
    const files = this.state.files
    files.remove(file)
    this.setState({ files: files })
  }

  val() {
    return $(this._$editor).val()
  }

  vals() {
    const vals = {
      content: this.val(),
      images: this.state.images,
      attachments: this.state.files,
    }

    if ((this.state.type === 2 || this.state.type === 4) && this._selectRelated) {
      vals.relatedRecord = this._selectRelated.val()
    }

    if (this.state.type === 3 && this._announcementOptions) {
      vals.contentMore = this._announcementOptions.val()
      if (!vals.contentMore) return
    }

    if (this.state.type === 4 && this._scheduleOptions) {
      vals.contentMore = this._scheduleOptions.val()
      if (!vals.contentMore) return
      vals.scheduleTime = vals.contentMore.scheduleTime + ':00'
    }

    return vals
  }

  focus = () => $(this._$editor).selectRange(9999, 9999) // Move to last

  reset = () => {
    $(this._$editor).val('')
    autosize.update(this._$editor)
    if (this._selectRelated) this._selectRelated.reset()
    if (this._announcementOptions) this._announcementOptions.reset()
    if (this._scheduleOptions) this._scheduleOptions.reset()
    this.setState({ files: null, images: null })
  }
}

// ~ 选择团队
class SelectGroup extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="modal select-list" ref={(c) => (this._dlg = c)} tabIndex="-1">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={this.hide} title={`${$L('关闭')} (Esc)`}>
                <i className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <h5 className="mt-0 text-bold">{$L('选择团队')}</h5>
              {this.state.groups && this.state.groups.length === 0 && <p className="text-muted">{$L('未加入任何团队')}</p>}
              <div>
                <ul className="list-unstyled">
                  {(this.state.groups || []).map((item) => {
                    return (
                      <li key={`team-${item.id}`}>
                        <a className="text-truncate" onClick={() => this._handleClick(item)}>
                          {item.name}
                          <i className="zmdi zmdi-check" />
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
    $.get('/feeds/group/group-list?self=yes', (res) => this.setState({ groups: res.data }))
    $(this._dlg).modal({ show: true, keyboard: true })
  }

  hide = () => $(this._dlg).modal('hide')
  show = () => $(this._dlg).modal('show')

  _handleClick = (item) => {
    this.hide()
    this.props.call && this.props.call(item)
  }
}

// 公告选项
class AnnouncementOptions extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="feed-options announcement">
        <dl className="row mb-1">
          <dt className="col-12 col-lg-3">{$L('同时公示在')}</dt>
          <dd className="col-12 col-lg-9 mb-0" ref={(c) => (this._$showWhere = c)}>
            <label className="custom-control custom-checkbox custom-control-inline">
              <input className="custom-control-input" name="showOn" type="checkbox" value={1} disabled={this.props.readonly} />
              <span className="custom-control-label">{$L('动态页')}</span>
            </label>
            <label className="custom-control custom-checkbox custom-control-inline">
              <input className="custom-control-input" name="showOn" type="checkbox" value={2} disabled={this.props.readonly} />
              <span className="custom-control-label">{$L('首页')}</span>
            </label>
            <label className="custom-control custom-checkbox custom-control-inline">
              <input className="custom-control-input" name="showOn" type="checkbox" value={4} disabled={this.props.readonly} />
              <span className="custom-control-label">
                {$L('登录页')} <i className="zmdi zmdi-help zicon down-3" data-toggle="tooltip" title={$L('选择登录页公示请注意不要发布敏感信息')} />
              </span>
            </label>
          </dd>
        </dl>
        <dl className="row">
          <dt className="col-12 col-lg-3 pt-2">{$L('公示时间')}</dt>
          <dd className="col-12 col-lg-9" ref={(c) => (this._$showTime = c)}>
            <div className="input-group">
              <input type="text" className="form-control form-control-sm" placeholder={$L('现在')} />
              <div className="input-group-prepend input-group-append">
                <span className="input-group-text">{$L('至')}</span>
              </div>
              <input type="text" className="form-control form-control-sm" placeholder={$L('选择结束时间')} />
            </div>
          </dd>
        </dl>
        <dl className="row mb-1">
          <dt className="col-12 col-lg-3">{$L('要求已读')}</dt>
          <dd className="col-12 col-lg-9 mb-0" ref={(c) => (this._$reqRead = c)}>
            <label className="custom-control custom-checkbox custom-control-inline">
              <input className="custom-control-input" name="reqRead" type="checkbox" disabled={this.props.readonly} />
              <span className="custom-control-label">{$L('是')}</span>
            </label>
          </dd>
        </dl>
      </div>
    )
  }

  componentDidMount() {
    $(this._$showTime).find('.form-control').datetimepicker()
    $(this._$showWhere).find('.zicon').tooltip()

    const initValue = this.props.initValue
    if (initValue) {
      $(this._$showTime)
        .find('.form-control:eq(0)')
        .val(initValue.timeStart || '')
      $(this._$showTime)
        .find('.form-control:eq(1)')
        .val(initValue.timeEnd || '')
      $(this._$showWhere)
        .find('input')
        .each(function () {
          if ((~~$(this).val() & initValue.showWhere) !== 0) $(this).prop('checked', true)
        })
      $(this._$reqRead)
        .find('input')
        .prop('checked', initValue.reqRead === true)
    }
  }

  componentWillUnmount() {
    $(this._$showTime).find('.form-control').datetimepicker('remove')
  }

  val() {
    let where = 0
    $(this._$showWhere)
      .find('input:checked')
      .each(function () {
        where += ~~$(this).val()
      })

    const timeStart = $(this._$showTime).find('.form-control:eq(0)').val()
    const timeEnd = $(this._$showTime).find('.form-control:eq(1)').val()
    if (where > 0 && !timeEnd) {
      RbHighbar.create($L('请选择结束时间'))
      return
    }

    return {
      timeStart: timeStart || null,
      timeEnd: timeEnd,
      showWhere: where,
      reqRead: $(this._$reqRead).find('input')[0].checked,
    }
  }

  reset() {
    $(this._$showTime).find('.form-control').val('')
    $(this._$showWhere).find('input').prop('checked', false)
    $(this._$reqRead).find('input').prop('checked', false)
  }
}

// 日程选项
class ScheduleOptions extends React.Component {
  state = { ...this.props }

  render() {
    const wpc = window.__PageConfig
    return (
      <div className="feed-options schedule">
        <dl className="row">
          <dt className="col-12 col-lg-3 pt-2">{$L('日程时间')}</dt>
          <dd className="col-12 col-lg-9" ref={(c) => (this._$scheduleTime = c)}>
            <input type="text" className="form-control form-control-sm" placeholder={$L('选择日程时间')} />
          </dd>
        </dl>
        <dl className="row mb-1">
          <dt className="col-12 col-lg-3">{$L('发送提醒给我')}</dt>
          <dd className="col-12 col-lg-9 mb-0" ref={(c) => (this._$scheduleRemind = c)}>
            <label className="custom-control custom-checkbox custom-control-inline">
              <input className="custom-control-input" name="remindOn" type="checkbox" value={1} disabled={this.props.readonly} />
              <span className="custom-control-label">{$L('通知')}</span>
            </label>
            <label className="custom-control custom-checkbox custom-control-inline">
              <input className="custom-control-input" name="remindOn" type="checkbox" value={2} disabled={this.props.readonly} />
              <span className="custom-control-label">
                {$L('邮件')}
                {!$isTrue(wpc.serviceMail) && <span className="text-danger fs-12"> ({$L('不可用')})</span>}
              </span>
            </label>
            <label className="custom-control custom-checkbox custom-control-inline">
              <input className="custom-control-input" name="remindOn" type="checkbox" value={4} disabled={this.props.readonly} />
              <span className="custom-control-label">
                {$L('短信')}
                {!$isTrue(wpc.serviceSms) && <span className="text-danger fs-12"> ({$L('不可用')})</span>}
              </span>
            </label>
          </dd>
        </dl>
      </div>
    )
  }

  componentDidMount() {
    $(this._$scheduleTime).find('.form-control').datetimepicker()

    const initValue = this.props.initValue
    if (initValue) {
      $(this._$scheduleTime).find('.form-control').val(initValue.scheduleTime)
      $(this._$scheduleRemind)
        .find('input')
        .each(function () {
          if ((~~$(this).val() & initValue.scheduleRemind) !== 0) $(this).prop('checked', true)
        })
    }
  }

  componentWillUnmount() {
    $(this._$scheduleTime).find('.form-control').datetimepicker('remove')
  }

  val() {
    let remind = 0
    $(this._$scheduleRemind)
      .find('input:checked')
      .each(function () {
        remind += ~~$(this).val()
      })
    const time = $(this._$scheduleTime).find('.form-control:eq(0)').val()
    if (!time) {
      RbHighbar.create($L('请选择日程时间'))
      return
    }

    return {
      scheduleTime: time,
      scheduleRemind: remind,
    }
  }

  reset() {
    $(this._$scheduleTime).find('.form-control').val('')
    $(this._$scheduleRemind).find('input').prop('checked', false)
  }
}

// ~~ 新建/编辑动态
// 新建主要从记录视图新建相关
// eslint-disable-next-line no-unused-vars
class FeedEditorDlg extends RbModalHandler {
  constructor(props) {
    super(props)
    this.state = { type: props.type }
  }

  render() {
    const _data = {
      initValue: (this.props.content || '').replace(/<\/?.+?>/g, ''),
      images: this.props.images,
      files: this.props.attachments,
      relatedRecord: this.props.relatedRecord,
      contentMore: this.props.contentMore,
      type: this.state.type,
    }

    const activeType = this.state.type
    const activeClass = 'text-primary text-bold'
    const scope = (this.props.scopeRaw || '').length > 10 /*ID*/ ? this.props.scope : this.props.scopeRaw

    return (
      <RbModal ref={(c) => (this._dlg = c)} title={this.props.id ? $L('编辑动态') : $L('新建动态')} icon="chart-donut" disposeOnHide>
        <div className={`feeds-post p-0 ml-3 mr-3 ${this.props.id ? 'mt-2' : 'mt-1'}`}>
          {!this.props.id && (
            <RF>
              <ul className="list-unstyled list-inline mb-1 pl-1" ref={(c) => (this._$activeType = c)}>
                <li className="list-inline-item">
                  <a onClick={() => this._clickTypeTab(2)} className={`${activeType === 2 ? activeClass : ''}`}>
                    {$L('跟进')}
                  </a>
                </li>
                <li className="list-inline-item">
                  <a onClick={() => this._clickTypeTab(4)} className={`${activeType === 4 ? activeClass : ''}`}>
                    {$L('日程')}
                  </a>
                </li>
              </ul>
              <div className="arrow_box" ref={(c) => (this._$activeArrow = c)} />
            </RF>
          )}

          <div>
            <FeedsEditor ref={(c) => (this._FeedsEditor = c)} {..._data} />
          </div>
        </div>

        <div className="mt-4 mr-1 text-right" ref={(c) => (this._$btn = c)}>
          <FeedsScope ref={(c) => (this.__FeedsScope = c)} initValue={scope} />
          <button className="btn btn-primary btn-space ml-4" type="button" onClick={this._post}>
            {this.props.id ? $L('保存') : $L('发布')}
          </button>
          <button className="btn btn-secondary btn-space ml-1 mr-2" type="button" onClick={this.hide}>
            {$L('取消')}
          </button>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    if (this.props.id) {
      setTimeout(() => this._FeedsEditor._$editor.focus(), 100)
    } else {
      this._clickTypeTab(this.state.type)
    }
  }

  _clickTypeTab(type) {
    this.setState({ type: type }, () => {
      const pos = $(this._$activeType).find('.text-primary').position()
      $(this._$activeArrow).css('margin-left', pos.left - 20)
      setTimeout(() => this._FeedsEditor._$editor.focus(), 200)
    })
  }

  _post = () => {
    const _data = this._FeedsEditor.vals()
    if (!_data) return
    if (!_data.content) return RbHighbar.create($L('请输入动态内容'))

    _data.scope = this.__FeedsScope.val()
    if (_data.scope === false) return

    // 新建
    if (!this.props.id) {
      _data.type = this.state.type
    }

    _data.metadata = { entity: 'Feeds', id: this.props.id || null }

    const $btn = $(this._$btn).find('.btn').button('loading')
    $.post('/feeds/post/publish', JSON.stringify(_data), (res) => {
      $btn.button('reset')
      if (res.error_code > 0) return RbHighbar.error(res.error_msg)

      this.hide()
      typeof this.props.call === 'function' && this.props.call()
    })
  }
}
