/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global autosize, EMOJIS */

// ~ 动态发布
// eslint-disable-next-line no-unused-vars
class FeedsPost extends React.Component {
  state = { ...this.props, type: 1 }

  render() {
    const activeType = this.state.type
    const activeClass = 'text-primary text-bold'
    return (
      <div className="feeds-post">
        <ul className="list-unstyled list-inline mb-1 pl-1" ref={(c) => (this._activeType = c)}>
          <li className="list-inline-item">
            <a onClick={() => this.setState({ type: 1 })} className={`${activeType === 1 ? activeClass : ''}`}>
              {$L('FeedsType1')}
            </a>
          </li>
          <li className="list-inline-item">
            <a onClick={() => this.setState({ type: 2 })} className={`${activeType === 2 ? activeClass : ''}`}>
              {$L('FeedsType2')}
            </a>
          </li>
          <li className="list-inline-item">
            <a onClick={() => this.setState({ type: 4 })} className={`${activeType === 4 ? activeClass : ''}`}>
              {$L('FeedsType4')}
            </a>
          </li>
          {rb.isAdminUser && (
            <li className="list-inline-item">
              <a onClick={() => this.setState({ type: 3 })} className={`${activeType === 3 ? activeClass : ''}`}>
                {$L('FeedsType3')}
              </a>
            </li>
          )}
        </ul>
        <div className="arrow_box" ref={(c) => (this._activeArrow = c)}></div>
        <div>
          <FeedsEditor ref={(c) => (this._editor = c)} type={activeType} />
        </div>
        <div className="mt-3">
          <div className="float-right">
            <button className="btn btn-primary" ref={(c) => (this._btn = c)} onClick={this._post}>
              {$L('Publish')}
            </button>
          </div>
          <div className="float-right mr-4">
            <div className="btn-group" style={{ border: '0 none' }}>
              <button className="btn btn-scope btn-link" data-toggle="dropdown" ref={(c) => (this._scopeBtn = c)}>
                <i className="icon up-1 zmdi zmdi-chart-donut" />
                {$L('Public')}
              </button>
              <div className="dropdown-menu dropdown-menu-right">
                <a className="dropdown-item" onClick={this._selectScope} data-scope="ALL" title={$L('FeedsScopeAll')}>
                  <i className="icon up-1 zmdi zmdi-chart-donut" />
                  {$L('Public')}
                </a>
                <a className="dropdown-item" onClick={this._selectScope} data-scope="SELF" title={$L('FeedsScopePrivate')}>
                  <i className="icon up-1 zmdi zmdi-lock" />
                  {$L('FeedsTabPrivate')}
                </a>
                <a className="dropdown-item" onClick={this._selectScope} data-scope="GROUP" title={$L('FeedsScopeGroup')}>
                  <i className="icon up-1 zmdi zmdi-accounts" />
                  {$L('Team')}
                </a>
              </div>
            </div>
          </div>
          <div className="clearfix"></div>
        </div>
      </div>
    )
  }

  componentDidUpdate(prevProps, prevState) {
    if (prevState.type !== this.state.type) {
      const pos = $(this._activeType).find('.text-primary').position()
      $(this._activeArrow).css('margin-left', pos.left - 30)
    }
  }

  componentDidMount = () => $('#rb-feeds').attr('class', '')

  _selectScope = (e) => {
    const target = e.target
    this.setState({ scope: target.dataset.scope }, () => {
      $(this._scopeBtn).html($(target).html())
      if (this.state.scope === 'GROUP') {
        if (this.__group) this._renderGroupScope(this.__group)
        const that = this
        if (this.__selectGroup) this.__selectGroup.show()
        else
          renderRbcomp(<SelectGroup call={this._renderGroupScope} />, null, function () {
            that.__selectGroup = this
          })
      }
    })
  }
  _renderGroupScope = (item) => {
    if (!item) return
    $(this._scopeBtn).html(`<i class="icon up-1 zmdi zmdi-accounts"></i>${item.name}`)
    this.__group = item
  }

  _post = () => {
    const _data = this._editor.vals()
    if (!_data) return
    if (!_data.content) {
      RbHighbar.create($L('PlsInputSome,FeedsContent'))
      return
    }

    _data.scope = this.state.scope
    if (_data.scope === 'GROUP') {
      if (!this.__group) {
        RbHighbar.create($L('PlsSelectSome,e.Team'))
        return
      }
      _data.scope = this.__group.id
    }

    _data.type = this.state.type
    _data.metadata = { entity: 'Feeds', id: this.props.id }

    const btn = $(this._btn).button('loading')
    $.post('/feeds/post/publish', JSON.stringify(_data), (res) => {
      btn.button('reset')
      if (res.error_msg > 0) {
        RbHighbar.error(res.error_msg)
        return
      }
      this._editor.reset()
      this.props.call && this.props.call()
    })
  }
}

// ~ 动态编辑框
class FeedsEditor extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }

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
    // 日程已完成
    const isFinish = this.state.type === 4 && this.props.contentMore && this.props.contentMore.finishTime

    return (
      <React.Fragment>
        {isFinish && <RbAlertBox message={$L('ReFinshScheduleConfirm')} />}
        <div className={`rich-editor ${this.state.focus ? 'active' : ''}`}>
          <textarea
            ref={(c) => (this._editor = c)}
            placeholder={this.props.placeholder}
            maxLength="2000"
            onFocus={() => this.setState({ focus: true })}
            onBlur={() => this.setState({ focus: false })}
            defaultValue={this.props.initValue}
          />
          <div className="action-btns">
            <ul className="list-unstyled list-inline m-0 p-0">
              <li className="list-inline-item use-dropdown">
                <a onClick={() => this.setState({ renderEmoji: true })} title={$L('Emoji')} data-toggle="dropdown">
                  <i className="zmdi zmdi-mood" />
                </a>
                <div className="dropdown-menu">{this.state.renderEmoji && <div className="emoji-wrapper">{this.__es}</div>}</div>
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
                    <a title={`@${$L('User')}`} data-toggle="dropdown">
                      <i className="zmdi at-text">@</i>
                    </a>
                  }
                  onSelectItem={this._selectAtUser}
                />
              </li>
              <li className="list-inline-item">
                <a title={$L('Image')} onClick={() => this._imageInput.click()}>
                  <i className="zmdi zmdi-image-o" />
                </a>
              </li>
              <li className="list-inline-item">
                <a title={$L('Attachment')} onClick={() => this._fileInput.click()} style={{ marginLeft: -5 }}>
                  <i className="zmdi zmdi-attachment-alt zmdi-hc-rotate-45" />
                </a>
              </li>
            </ul>
          </div>
        </div>
        {this.state.type === 4 && <ScheduleOptions ref={(c) => (this._scheduleOptions = c)} initValue={this.state.contentMore} contentMore={this.state.contentMore} />}
        {(this.state.type === 2 || this.state.type === 4) && <SelectRelated ref={(c) => (this._selectRelated = c)} initValue={this.state.relatedRecord} />}
        {this.state.type === 3 && <AnnouncementOptions ref={(c) => (this._announcementOptions = c)} initValue={this.state.contentMore} />}
        {((this.state.images || []).length > 0 || (this.state.files || []).length > 0) && (
          <div className="attachment">
            <div className="img-field">
              {(this.state.images || []).map((item) => {
                return (
                  <span key={'img-' + item}>
                    <a title={$fileCutName(item)} className="img-thumbnail img-upload">
                      <img src={`${rb.baseUrl}/filex/img/${item}?imageView2/2/w/100/interlace/1/q/100`} />
                      <b title={$L('Remove')} onClick={() => this._removeImage(item)}>
                        <span className="zmdi zmdi-close"></span>
                      </b>
                    </a>
                  </span>
                )
              })}
            </div>
            <div className="file-field">
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
          <input type="file" ref={(c) => (this._imageInput = c)} accept="image/*" data-maxsize="10240000" />
        </span>
      </React.Fragment>
    )
  }

  UNSAFE_componentWillReceiveProps = (props) => this.setState(props)

  componentDidMount() {
    autosize(this._editor)
    setTimeout(() => this.props.initValue && autosize.update(this._editor), 200)

    let mp
    const mp_end = function () {
      if (mp) mp.end()
      mp = null
    }

    $createUploader(
      this._imageInput,
      (res) => {
        if (!mp) mp = new Mprogress({ template: 1, start: true })
        mp.set(res.percent / 100)
      },
      (res) => {
        mp_end()
        const images = this.state.images || []
        images.push(res.key)
        this.setState({ images: images })
      },
      () => mp_end()
    )

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
    $(this._editor).insertAtCursor(`[${emoji}]`)
    this.setState({ showEmoji: false })
  }

  _selectAtUser = (s) => {
    $(this._editor).insertAtCursor(`@${s.text} `)
    this.setState({ showAtUser: false })
  }

  _removeImage(image) {
    const images = this.state.images
    images.remove(image)
    this.setState({ images: images })
  }

  _removeFile(file) {
    const files = this.state.files
    files.remove(file)
    this.setState({ files: files })
  }

  val() {
    return $(this._editor).val()
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

  focus = () => $(this._editor).selectRange(9999, 9999) // Move to last

  reset = () => {
    $(this._editor).val('')
    autosize.update(this._editor)
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
              <button className="close" type="button" onClick={this.hide}>
                <i className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <h5 className="mt-0 text-bold">{$L('SelectSome,e.Team')}</h5>
              {this.state.groups && this.state.groups.length === 0 && <p className="text-muted">{$L('YouNotJoinAnyTeams')}</p>}
              <div>
                <ul className="list-unstyled">
                  {(this.state.groups || []).map((item) => {
                    return (
                      <li key={'g-' + item.id}>
                        <a className="text-truncate" onClick={() => this._handleClick(item)}>
                          {item.name}
                          <i className="zmdi zmdi-check"></i>
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
    $.get('/feeds/group/group-list', (res) => this.setState({ groups: res.data }))
    $(this._dlg).modal({ show: true, keyboard: true })
  }

  hide = () => $(this._dlg).modal('hide')
  show = () => $(this._dlg).modal('show')

  _handleClick = (item) => {
    this.hide()
    this.props.call && this.props.call(item)
  }
}

// ~ 选择相关记录
class SelectRelated extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="feed-options related">
        <dl className="row">
          <dt className="col-12 col-lg-3 pt-2">{$L('ReleatedRecord')}</dt>
          <dd className="col-12 col-lg-9">
            <span className="float-left" style={{ width: 200 }}>
              <select className="form-control form-control-sm" ref={(c) => (this._entity = c)}>
                {(this.state.entities || []).length === 0 && <option>{$L('NoAnySome,Entity')}</option>}
                {(this.state.entities || []).map((item) => {
                  return (
                    <option key={item.name} value={item.name}>
                      {item.label}
                    </option>
                  )
                })}
              </select>
            </span>
            <span className="float-left pl-1 related-record">
              <select className="form-control form-control-sm float-left" ref={(c) => (this._record = c)} />
            </span>
            <div className="clearfix"></div>
          </dd>
        </dl>
      </div>
    )
  }

  componentDidMount() {
    $.get('/commons/metadata/entities', (res) => {
      if (!res.data || res.data.length === 0) {
        $(this._entity).attr('disabled', true)
        $(this._record).attr('disabled', true)
        return
      }
      this.setState({ entities: res.data }, () => {
        $(this._entity)
          .select2({
            allowClear: false,
          })
          .on('change', () => {
            $(this._record).val(null).trigger('change')
          })

        // 编辑时
        if (this.props.initValue) {
          $(this._entity).val(this.props.initValue.entity).trigger('change')
          const option = new Option(this.props.initValue.text, this.props.initValue.id, true, true)
          $(this._record).append(option)
        }
      })
    })

    const that = this
    let search_input = null
    $(this._record).select2({
      placeholder: `${$L('SelectSome,ReleatedRecord')} (${$L('Optional')})`,
      minimumInputLength: 0,
      maximumSelectionLength: 1,
      ajax: {
        url: '/commons/search/search',
        delay: 300,
        data: function (params) {
          search_input = params.term
          return { entity: $(that._entity).val(), q: params.term }
        },
        processResults: function (data) {
          return { results: data.data }
        },
      },
      language: {
        noResults: () => {
          return (search_input || '').length > 0 ? $L('NoResults') : $L('InputForSearch')
        },
        inputTooShort: () => {
          return $L('InputForSearch')
        },
        searching: () => {
          return $L('Searching')
        },
        maximumSelected: () => {
          return $L('OnlyXSelected').replace('%d', 1)
        },
      },
    })
  }

  val() {
    return $(this._record).val()
  }

  reset = () => $(this._record).val(null).trigger('change')
}

// 公告选项
class AnnouncementOptions extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="feed-options announcement">
        <dl className="row mb-1">
          <dt className="col-12 col-lg-3">{$L('SameAnnouncementPos')}</dt>
          <dd className="col-12 col-lg-9 mb-0" ref={(c) => (this._showWhere = c)}>
            <label className="custom-control custom-checkbox custom-control-inline">
              <input className="custom-control-input" name="showOn" type="checkbox" value={1} disabled={this.props.readonly} />
              <span className="custom-control-label">{$L('AnnouncementPos1')}</span>
            </label>
            <label className="custom-control custom-checkbox custom-control-inline">
              <input className="custom-control-input" name="showOn" type="checkbox" value={2} disabled={this.props.readonly} />
              <span className="custom-control-label">{$L('AnnouncementPos2')}</span>
            </label>
            <label className="custom-control custom-checkbox custom-control-inline">
              <input className="custom-control-input" name="showOn" type="checkbox" value={4} disabled={this.props.readonly} />
              <span className="custom-control-label">
                {$L('AnnouncementPos4')} <i className="zmdi zmdi-help zicon down-3" data-toggle="tooltip" title={$L('AnnouncementPos4Tips')} />
              </span>
            </label>
          </dd>
        </dl>
        <dl className="row">
          <dt className="col-12 col-lg-3 pt-2"> {$L('AnnouncementTime')}</dt>
          <dd className="col-12 col-lg-9" ref={(c) => (this._showTime = c)}>
            <div className="input-group">
              <input type="text" className="form-control form-control-sm" placeholder={$L('Now')} />
              <div className="input-group-prepend input-group-append">
                <span className="input-group-text">{$L('To')}</span>
              </div>
              <input type="text" className="form-control form-control-sm" placeholder={$L('SelectSome,EndTime')} />
            </div>
          </dd>
        </dl>
      </div>
    )
  }

  componentDidMount() {
    $(this._showTime).find('.form-control').datetimepicker()
    $(this._showWhere).find('.zicon').tooltip()

    const initValue = this.props.initValue
    if (initValue) {
      $(this._showTime)
        .find('.form-control:eq(0)')
        .val(initValue.timeStart || '')
      $(this._showTime)
        .find('.form-control:eq(1)')
        .val(initValue.timeEnd || '')
      $(this._showWhere)
        .find('input')
        .each(function () {
          if ((~~$(this).val() & initValue.showWhere) !== 0) $(this).prop('checked', true)
        })
    }
  }

  componentWillUnmount() {
    $(this._showTime).find('.form-control').datetimepicker('remove')
  }

  val() {
    let where = 0
    $(this._showWhere)
      .find('input:checked')
      .each(function () {
        where += ~~$(this).val()
      })

    const timeStart = $(this._showTime).find('.form-control:eq(0)').val()
    const timeEnd = $(this._showTime).find('.form-control:eq(1)').val()
    if (where > 0 && !timeEnd) {
      RbHighbar.create($L('PlsSelectSome,EndTime'))
      return
    }

    return {
      timeStart: timeStart || null,
      timeEnd: timeEnd,
      showWhere: where,
    }
  }

  reset() {
    $(this._showTime).find('.form-control').val('')
    $(this._showWhere).find('input').prop('checked', false)
  }
}

// 日程选项
class ScheduleOptions extends React.Component {
  state = { ...this.props }

  render() {
    const email = window.__USER_EMAIL
    const mobile = window.__USER_MOBILE
    return (
      <div className="feed-options schedule">
        <dl className="row">
          <dt className="col-12 col-lg-3 pt-2">{$L('ScheduleTime')}</dt>
          <dd className="col-12 col-lg-9" ref={(c) => (this._scheduleTime = c)}>
            <input type="text" className="form-control form-control-sm" placeholder={$L('SelectSome,ScheduleTime')} />
          </dd>
        </dl>
        <dl className="row mb-1">
          <dt className="col-12 col-lg-3">{$L('SendRemindToMe')}</dt>
          <dd className="col-12 col-lg-9 mb-0" ref={(c) => (this._scheduleRemind = c)}>
            <label className="custom-control custom-checkbox custom-control-inline">
              <input className="custom-control-input" name="showOn" type="checkbox" value={1} disabled={this.props.readonly} />
              <span className="custom-control-label">{$L('Notification')}</span>
            </label>
            <label className="custom-control custom-checkbox custom-control-inline" title={email}>
              <input className="custom-control-input" name="showOn" type="checkbox" value={2} disabled={this.props.readonly} />
              <span className="custom-control-label">
                {$L('Mail')}
                {!email && <span> ({$L('Unavailable')})</span>}
              </span>
            </label>
            <label className="custom-control custom-checkbox custom-control-inline" title={mobile}>
              <input className="custom-control-input" name="showOn" type="checkbox" value={4} disabled={this.props.readonly} />
              <span className="custom-control-label">
                {$L('Sms')}
                {!mobile && <span> ({$L('Unavailable')})</span>}
              </span>
            </label>
          </dd>
        </dl>
      </div>
    )
  }

  componentDidMount() {
    $(this._scheduleTime).find('.form-control').datetimepicker()
    const initValue = this.props.initValue
    if (initValue) {
      $(this._scheduleTime).find('.form-control').val(initValue.scheduleTime)
      $(this._scheduleRemind)
        .find('input')
        .each(function () {
          if ((~~$(this).val() & initValue.scheduleRemind) !== 0) $(this).prop('checked', true)
        })
    }
  }

  componentWillUnmount() {
    $(this._scheduleTime).find('.form-control').datetimepicker('remove')
  }

  val() {
    let remind = 0
    $(this._scheduleRemind)
      .find('input:checked')
      .each(function () {
        remind += ~~$(this).val()
      })
    const time = $(this._scheduleTime).find('.form-control:eq(0)').val()
    if (!time) {
      RbHighbar.create($L('PlsSelectSome,ScheduleTime'))
      return
    }

    return {
      scheduleTime: time,
      scheduleRemind: remind,
    }
  }

  reset() {
    $(this._scheduleTime).find('.form-control').val('')
    $(this._scheduleRemind).find('input').prop('checked', false)
  }
}

// ~~ 编辑动态
// eslint-disable-next-line no-unused-vars
class FeedsEditDlg extends RbModalHandler {
  constructor(props) {
    super(props)
  }

  render() {
    const _data = {
      initValue: this.props.content.replace(/<\/?.+?>/g, ''),
      type: this.props.type,
      images: this.props.images,
      files: this.props.attachments,
      relatedRecord: this.props.relatedRecord,
      contentMore: this.props.contentMore,
    }
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$L('EditSome,e.Feeds')} disposeOnHide={true}>
        <div className="m-1">
          <FeedsEditor ref={(c) => (this._editor = c)} {..._data} />
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

  _post = () => {
    const _data = this._editor.vals()
    if (!_data) return
    if (!_data.content) {
      RbHighbar.create($L('PlsInputSome,FeedsContent'))
      return
    }
    _data.metadata = { entity: 'Feeds', id: this.props.id }

    const btns = $(this._btns).find('.btn').button('loading')
    $.post('/feeds/post/publish', JSON.stringify(_data), (res) => {
      btns.button('reset')
      if (res.error_msg > 0) {
        RbHighbar.error(res.error_msg)
        return
      }
      this.hide()
      this.props.call && this.props.call()
    })
  }
}
