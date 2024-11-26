/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */

// ~~ 分配
class DlgAssign extends RbModalHandler {
  constructor(props) {
    super(props)
    this.onView = !!window.RbViewPage
    this._Props = ['assign', $L('分配'), 'A']
  }

  render() {
    return (
      <RbModal title={this._Props[1]} ref={(c) => (this._dlg = c)}>
        <div className="form">
          {this.onView === true ? null : (
            <div className="form-group row pb-0">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('%s哪些记录', this._Props[1])}</label>
              <div className="col-sm-7">
                <div className="form-control-plaintext">{`${$L('选中的记录')} (${$L('%d 条', this.state.ids.length)})`}</div>
              </div>
            </div>
          )}
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('%s给谁', this._Props[1])}</label>
            <div className="col-sm-7">{this._useUserSelector()}</div>
          </div>
          {this.state.cascadesShow !== true ? (
            <div className="form-group row">
              <div className="col-sm-7 offset-sm-3">
                <a href="#" onClick={(e) => this._showCascade(e)}>
                  {$L('同时%s相关记录', this._Props[1])}
                </a>
              </div>
            </div>
          ) : (
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('选择相关记录')}</label>
              <div className="col-sm-7">
                <select className="form-control form-control-sm" ref={(c) => (this._$cascades = c)}>
                  {(this.state.cascadesEntity || []).map((item) => {
                    if ($isSysMask(item[1])) return null
                    return (
                      <option key={item[0]} value={item[0]}>
                        {item[1]}
                      </option>
                    )
                  })}
                </select>
              </div>
            </div>
          )}
          {this._Props[0] === 'share' && (
            <div className="form-group row pt-1">
              <label className="col-sm-3 col-form-label text-sm-right" />
              <div className="col-sm-7">
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._withUpdate = c)} />
                  <span className="custom-control-label">{$L('允许编辑 (不勾选则仅共享读取权限)')}</span>
                </label>
              </div>
            </div>
          )}
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={() => this.post()}>
                {$L('确定')}
              </button>
              <button type="button" className="btn btn-link" onClick={() => this.hide()}>
                {$L('取消')}
              </button>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  _showCascade(e) {
    e && e.preventDefault()
    $.get(`/commons/metadata/references?entity=${this.props.entity}&permission=${this._Props[2]}`, (res) => {
      this.setState({ cascadesShow: true, cascadesEntity: res.data }, () => {
        const defaultSelected = []
        res.data.forEach((item) => defaultSelected.push(item[0]))

        $(this._$cascades)
          .select2({
            multiple: true,
            placeholder: $L('选择'),
          })
          .val(defaultSelected)
          .trigger('change')
      })
    })
  }

  _useUserSelector() {
    return <UserSelector hideDepartment hideRole hideTeam multiple={false} ref={(c) => (this._UserSelector = c)} />
  }

  _reset() {
    this.setState({ cascadesShow: false })
    this._UserSelector.clearSelection()
    $(this._$cascades).val(null).trigger('change')
    $(this._withUpdate).prop('checked', false)
  }

  post() {
    let users = this._UserSelector.val()
    if (!users || users.length === 0) return RbHighbar.create($L('请选择%s给谁', this._Props[1]))
    if (Array.isArray(users)) users = users.join(',')
    const cas = this.state.cascadesShow === true ? $(this._$cascades).val().join(',') : ''
    const withUpdate = $(this._withUpdate).prop('checked')

    const $btn = $(this._btns).find('.btn').button('loading')
    $.post(`/app/entity/record-${this._Props[0]}?id=${this.state.ids.join(',')}&cascades=${cas}&to=${users}&withUpdate=${withUpdate || ''}`, (res) => {
      if (res.error_code === 0) {
        const _data = res.data
        if (this._Props[0] === 'assign') {
          if (_data.assigned >= _data.requests) RbHighbar.success($L('分配成功'))
          else if (_data.assigned === 0) RbHighbar.error($L('无法分配记录'))
          else RbHighbar.success($L('成功分配 %d 条记录', _data.assigned))
        } else if (this._Props[0] === 'share') {
          if (_data.shared >= _data.requests) RbHighbar.success($L('共享成功'))
          else if (_data.shared === 0) RbHighbar.error($L('无法共享记录'))
          else RbHighbar.success($L('成功共享 %d 条记录', _data.shared))
        }

        this._reset()
        this.hide()

        setTimeout(() => {
          if (window.RbListPage) RbListPage._RbList.reload()
          if (window.RbViewPage) window.RbViewPage.reload()
        }, 200)
      } else {
        RbHighbar.error(res.error_msg)
      }
      $btn.button('reset')
    })
  }

  // -- Usage
  /**
   * @param {*} props
   */
  static create(props) {
    const that = this
    if (that.__HOLDER) that.__HOLDER.show(props)
    else
      renderRbcomp(<DlgAssign {...props} />, function () {
        that.__HOLDER = this
      })
  }
}

// ~~ 共享
class DlgShare extends DlgAssign {
  constructor(props) {
    super(props)
    this._Props = ['share', $L('共享'), 'S']
  }

  _useUserSelector() {
    return <UserSelector ref={(c) => (this._UserSelector = c)} />
  }

  // -- Usage
  /**
   * @param {*} props
   */
  static create(props) {
    const that = this
    if (that.__HOLDER2) that.__HOLDER2.show(props)
    else
      renderRbcomp(<DlgShare {...props} />, function () {
        that.__HOLDER2 = this
      })
  }
}

// ~~ 取消共享（批量模式）
class DlgUnshare extends RbModalHandler {
  constructor(props) {
    super(props)
    this.state.whichUsers = 'ALL'
  }

  render() {
    return (
      <RbModal title={$L('取消共享')} ref={(c) => (this._dlg = c)}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('取消共享哪些记录')}</label>
            <div className="col-sm-7">
              <div className="form-control-plaintext">{`${$L('选中的记录')} (${$L('%d 条', this.state.ids.length)})`}</div>
            </div>
          </div>
          <div className="form-group row pt-0 pb-0">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('取消哪些用户')}</label>
            <div className="col-sm-7">
              <div className="mt-1">
                <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-2">
                  <input className="custom-control-input" name="whichUsers" type="radio" checked={this.state.whichUsers === 'ALL'} onChange={() => this.whichMode(true)} />
                  <span className="custom-control-label">{$L('全部用户')}</span>
                </label>
                <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-2">
                  <input className="custom-control-input" name="whichUsers" type="radio" checked={this.state.whichUsers === 'SPEC'} onChange={() => this.whichMode()} />
                  <span className="custom-control-label">{$L('指定用户')}</span>
                </label>
              </div>
              <div className={`mb-2 ${this.state.whichUsers === 'ALL' ? 'hide' : ''}`}>
                <UserSelector ref={(c) => (this._UserSelector = c)} />
              </div>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={() => this.post()}>
                {$L('确定')}
              </button>
              <button type="button" className="btn btn-link" onClick={() => this.hide()}>
                {$L('取消')}
              </button>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  whichMode(isAll) {
    this.setState({ whichUsers: isAll === true ? 'ALL' : 'SPEC' })
  }

  post() {
    let users = this._UserSelector.val()
    if (this.state.whichUsers === 'ALL') {
      users = '$ALL$'
    } else {
      if (!users || users.length === 0) {
        RbHighbar.create($L('请选择取消哪些用户'))
        return
      }
      users = users.join(',')
    }

    const $btns = $(this._btns).find('.btn').button('loading')
    $.post(`/app/entity/record-unshare-batch?id=${this.state.ids.join(',')}&to=${users}`, (res) => {
      if (res.error_code === 0) {
        const _data = res.data
        if (_data.unshared >= _data.requests) RbHighbar.success($L('取消共享成功'))
        else if (_data.unshared === 0) RbHighbar.success($L('无法取消共享'))
        else RbHighbar.success($L('成功取消共享 %d 条记录', _data.unshared))

        this._UserSelector.clearSelection()
        this.hide()

        setTimeout(() => {
          if (window.RbListPage) RbListPage._RbList.reload()
        }, 200)
      } else {
        RbHighbar.error(res.error_msg)
      }
      $btns.button('reset')
    })
  }

  // -- Usage
  /**
   * @param {*} props
   */
  static create(props) {
    const that = this
    if (that.__HOLDER) that.__HOLDER.show(props)
    else
      renderRbcomp(<DlgUnshare {...props} />, function () {
        that.__HOLDER = this
      })
  }
}

// ~~ 管理共享
class DlgShareManager extends RbModalHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <RbModal title={$L('共享用户')} ref={(c) => (this._dlg = c)}>
        <div className="shares-list ml-1 mr-1">
          <table className="table table-hover">
            <tbody ref={(c) => (this._tbody = c)}>
              {(this.state.sharingList || []).map((item) => {
                return (
                  <tr key={item[1]}>
                    <td className="user-avatar cell-detail user-info">
                      <img src={`${rb.baseUrl}/account/user-avatar/${item[0][0]}`} alt="Avatar" />
                      <span>{item[0][1]}</span>
                    </td>
                    <td className="text-right">
                      {(item[4] & 4) !== 0 && <span className="badge badge-light text-danger">{$L('编辑')}</span>}
                      {(item[4] & 8) !== 0 && <span className="badge badge-light">{$L('读取')}</span>}
                    </td>
                    <td className="text-right text-muted" title={item[2]}>
                      {$L('由 %s 共享于 %s', item[3], $fromNow(item[2]))}
                    </td>
                    <td className="actions text-right">
                      <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                        <input className="custom-control-input" type="checkbox" defaultChecked={false} data-id={item[1]} />
                        <span className="custom-control-label">&nbsp;</span>
                      </label>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
        <div className="dialog-footer" ref={(c) => (this._btns = c)}>
          <button className="btn btn-secondary btn-space" type="button" onClick={() => this.hide()}>
            {$L('取消')}
          </button>
          {this.props.unshare === true && (
            <button className="btn btn-primary btn-space ml-1 mr-1" type="button" onClick={() => this.post()}>
              {$L('取消共享')}
            </button>
          )}
        </div>
      </RbModal>
    )
  }

  _componentDidMount() {
    $.get(`/app/entity/shared-list?id=${this.props.id}`, (res) => this.setState({ sharingList: res.data || [] }))
  }
  componentDidMount = () => this._componentDidMount()

  post() {
    const s = []
    $(this._tbody)
      .find('input:checked')
      .each((idx, item) => s.push($(item).data('id')))
    if (s.length === 0) return RbHighbar.create($L('请选择取消哪些用户'))

    const $btn = $(this._btns).button('loading')
    $.post(`/app/entity/record-unshare?id=${s.join(',')}&record=${this.props.id}`, (res) => {
      if (res.error_code === 0) {
        RbHighbar.success($L('取消共享成功'))
        this.hide()

        setTimeout(() => {
          window.RbViewPage && window.RbViewPage.reload()
        }, 200)
      } else {
        RbHighbar.error(res.error_msg)
      }
      $btn.button('reset')
    })
  }

  // -- Usage
  /**
   * @param {*} id
   * @param {*} unshare
   */
  static create(id, unshare) {
    const props = { id: id, unshare: unshare !== false }
    const that = this
    if (that.__HOLDER) that.__HOLDER.show(props)
    else
      renderRbcomp(<DlgShareManager {...props} />, function () {
        that.__HOLDER = this
      })
  }
}

// ~~ 记录转换
class DlgTransform extends RbModalHandler {
  constructor(props) {
    super(props)
    this.state.transType = 0
  }

  render() {
    return (
      <RbModal title={$L('转换记录')} className="sm-height" ref={(c) => (this._dlg = c)} disposeOnHide>
        <div className="form">
          <div className="form-group row pb-1">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('转换为')}</label>
            <div className="col-sm-7" style={{ paddingTop: 6 }}>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input" type="radio" name="transType" checked={this.state.transType === 0} onChange={() => this.setState({ transType: 0 })} />
                <span className="custom-control-label">{$L('新记录')}</span>
              </label>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input J_word4" type="radio" name="transType" checked={this.state.transType === 1} onChange={() => this.setState({ transType: 1 })} />
                <span className="custom-control-label">{$L('已有记录')}</span>
              </label>
            </div>
          </div>
          <div className={`form-group row ${this.state.transType !== 1 && 'hide'}`}>
            <label className="col-sm-3 col-form-label text-sm-right">{$L('选择已有记录')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref={(c) => (this._$existsRecord = c)}></select>
            </div>
          </div>
          {this.props.mainEntity && (
            <div className={`form-group row ${this.state.transType === 1 && 'hide'}`}>
              <label className="col-sm-3 col-form-label text-sm-right">{$L('选择主记录')}</label>
              <div className="col-sm-7">
                <select className="form-control form-control-sm" ref={(c) => (this._$mainRecord = c)}></select>
                <p className="form-text">{$L('转换新明细记录时需要选择主记录')}</p>
              </div>
            </div>
          )}
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._$btn = c)}>
              <button className="btn btn-primary" type="button" onClick={() => this.post()}>
                {$L('确定')}
              </button>
              <button className="btn btn-primary btn-outline ml-2" type="button" onClick={() => this.post(true)}>
                {$L('预览')}
              </button>
              <button type="button" className="btn btn-link" onClick={() => this.hide()}>
                {$L('取消')}
              </button>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    $initReferenceSelect2(this._$existsRecord, {
      placeholder: $L('选择'),
      entity: this.props.entity,
      searchType: 'search',
    })
    if (this.props.existsRecord) {
      $.get(`/commons/frontjs/ref-label?id=${this.props.existsRecord}`, (res) => {
        const o = new Option(res.data, this.props.existsRecord, true, true)
        $(this._$existsRecord).append(o).trigger('change')
        this.setState({ transType: 1 })
      })
    }

    if (this.props.mainEntity) {
      $initReferenceSelect2(this._$mainRecord, {
        placeholder: $L('选择'),
        entity: this.props.entity,
        name: `${this.props.mainEntity}Id`,
      })
      if (this.props.mainRecord) {
        $.get(`/commons/frontjs/ref-label?id=${this.props.mainRecord}`, (res) => {
          const o = new Option(res.data, this.props.mainRecord, true, true)
          $(this._$mainRecord).append(o).trigger('change')
        })
      }
    }
  }

  post(preview) {
    const props = this.props
    const _post = {
      transid: props.transid,
      sourceRecord: props.sourceRecord,
      existsRecord: this.state.transType === 1 ? $(this._$existsRecord).val() || null : null,
      mainRecord: this.state.transType === 0 ? $(this._$mainRecord).val() || null : null,
      preview: preview || false,
    }

    if (this.state.transType === 1 && !_post.existsRecord) {
      return RbHighbar.createl('请选择已有记录')
    }
    if (props.mainEntity) {
      if (this.state.transType !== 1 && !_post.mainRecord) {
        return RbHighbar.createl('请选择主记录')
      }
    }
    if (_post.sourceRecord === _post.existsRecord) {
      return RbHighbar.createl('已有记录不能是当前记录')
    }

    const $btn = $(this._$btn).find('.btn').button('loading')
    $.post('/app/entity/extras/transform39', JSON.stringify(_post), (res) => {
      $btn.button('reset')
      if (res.error_code === 0) {
        this.reset()
        this.hide(true)

        if (_post.preview) {
          const modalProps = { title: $L('新建%s', props.entityLabel), entity: props.entity, icon: props.icon, initialFormModel: res.data }
          if (_post.existsRecord) {
            modalProps.title = $L('编辑%s', props.entityLabel)
            modalProps.id = _post.existsRecord
          }
          // form
          RbFormModal.create(modalProps, true)
        } else {
          setTimeout(() => {
            if (window.RbViewPage) window.RbViewPage.clickView(`#!/View/${this.props.entity}/${res.data}`)
            else window.open(`${rb.baseUrl}/app/${this.props.entity}/view/${res.data}`)
          }, 200)
        }
      } else {
        res.error_code === 400 ? RbHighbar.create(res.error_msg) : RbHighbar.error(res.error_msg)
      }
    })
  }

  reset() {
    this.setState({ transType: 0 })
    this._$existsRecord && $(this._$existsRecord).val(null).trigger('change')
    this._$mainRecord && $(this._$mainRecord).val(null).trigger('change')
  }
}
