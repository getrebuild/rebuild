/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */

// ~~ 分派
class DlgAssign extends RbModalHandler {
  constructor(props) {
    super(props)
    this.onView = !!window.RbViewPage
    this._Props = ['assign', 'Assign', 'A']
  }

  render() {
    return (
      <RbModal title={$L(this._Props[1])} ref={(c) => (this._dlg = c)}>
        <div className="form">
          {this.onView === true ? null : (
            <div className="form-group row pb-0">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('SomeWhichRecords,' + this._Props[1])}</label>
              <div className="col-sm-7">
                <div className="form-control-plaintext">{`${$L('DatasSelected')} (${$L('XItem').replace('%d', this.state.ids.length)})`}</div>
              </div>
            </div>
          )}
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('SomeToWho,' + this._Props[1])}</label>
            <div className="col-sm-7">{this._useUserSelector()}</div>
          </div>
          {this.state.cascadesShow !== true ? (
            <div className="form-group row">
              <div className="col-sm-7 offset-sm-3">
                <a href="#" onClick={this._showCascade}>
                  {$L('SameSomeRelatedRecords,' + this._Props[1])}
                </a>
              </div>
            </div>
          ) : (
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('SelectSome,RelatedRecord')}</label>
              <div className="col-sm-7">
                <select className="form-control form-control-sm" ref={(c) => (this._cascades = c)}>
                  {(this.state.cascadesEntity || []).map((item) => {
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
              <label className="col-sm-3 col-form-label text-sm-right"></label>
              <div className="col-sm-7">
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._withUpdate = c)} />
                  <span className="custom-control-label">{$L('ShareWithUpdate')}</span>
                </label>
              </div>
            </div>
          )}
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary btn-space" type="button" onClick={() => this.post()}>
                {$L('Confirm')}
              </button>
              <a className="btn btn-link btn-space" onClick={() => this.hide()}>
                {$L('Cancel')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  _showCascade = () => {
    event && event.preventDefault()
    $.get(`/commons/metadata/references?entity=${this.props.entity}&permission=${this._Props[2]}`, (res) => {
      this.setState({ cascadesShow: true, cascadesEntity: res.data }, () => {
        const defaultSelected = []
        res.data.forEach((item) => defaultSelected.push(item[0]))

        $(this._cascades)
          .select2({
            multiple: true,
            placeholder: $L('SelectCasEntity'),
          })
          .val(defaultSelected)
          .trigger('change')
      })
    })
  }

  _useUserSelector() {
    return <UserSelector hideDepartment={true} hideRole={true} hideTeam={true} multiple={false} ref={(c) => (this._UserSelector = c)} />
  }

  post() {
    let users = this._UserSelector.val()
    if (!users || users.length === 0) return RbHighbar.create($L('PlsSelectSomeToWho,' + this._Props[1]))
    if ($.type(users) === 'array') users = users.join(',')
    const cass = this.state.cascadesShow === true ? $(this._cascades).val().join(',') : ''
    const withUpdate = $(this._withUpdate).prop('checked')

    const $btn = $(this._btns).find('.btn').button('loading')
    $.post(`/app/entity/record-${this._Props[0]}?id=${this.state.ids.join(',')}&cascades=${cass}&to=${users}&withUpdate=${withUpdate || ''}`, (res) => {
      if (res.error_code === 0) {
        this.setState({ cascadesShow: false })
        this._UserSelector.clearSelection()
        $(this._cascades).val(null).trigger('change')
        $(this._withUpdate).prop('checked', false)

        this.hide()
        RbHighbar.success($L('SomeSuccess,' + this._Props[1]))

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
      renderRbcomp(<DlgAssign {...props} />, null, function () {
        that.__HOLDER = this
      })
  }
}

// ~~ 共享
class DlgShare extends DlgAssign {
  constructor(props) {
    super(props)
    this._Props = ['share', 'Share', 'S']
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
      renderRbcomp(<DlgShare {...props} />, null, function () {
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
      <RbModal title={$L('UnShare')} ref={(c) => (this._dlg = c)}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('SomeWhichRecords,UnShare')}</label>
            <div className="col-sm-7">
              <div className="form-control-plaintext">{`${$L('DatasSelected')} (${$L('XItem').replace('%d', this.state.ids.length)})`}</div>
            </div>
          </div>
          <div className="form-group row pt-0 pb-0">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('CancelWhichUsers')}</label>
            <div className="col-sm-7">
              <div className="mt-1">
                <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-2">
                  <input className="custom-control-input" name="whichUsers" type="radio" checked={this.state.whichUsers === 'ALL'} onChange={() => this.whichMode(true)} />
                  <span className="custom-control-label">{$L('AllUser')}</span>
                </label>
                <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-2">
                  <input className="custom-control-input" name="whichUsers" type="radio" checked={this.state.whichUsers === 'SPEC'} onChange={() => this.whichMode()} />
                  <span className="custom-control-label">{$L('SpecUser')}</span>
                </label>
              </div>
              <div className={'mb-2 ' + (this.state.whichUsers === 'ALL' ? 'hide' : '')}>
                <UserSelector ref={(c) => (this._UserSelector = c)} />
              </div>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary btn-space" type="button" onClick={() => this.post()}>
                {$L('Confirm')}
              </button>
              <a className="btn btn-link btn-space" onClick={() => this.hide()}>
                {$L('Cancel')}
              </a>
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
        RbHighbar.create($L('PlsSelectSome,CancelWhichUsers'))
        return
      }
      users = users.join(',')
    }

    const $btns = $(this._btns).find('.btn').button('loading')
    $.post(`/app/entity/record-unshare-batch?id=${this.state.ids.join(',')}&to=${users}`, (res) => {
      if (res.error_code === 0) {
        this._UserSelector.clearSelection()

        this.hide()
        RbHighbar.success($L('SomeSuccess,UnShare'))

        setTimeout(() => {
          if (window.RbListPage) RbListPage._RbList.reload()
        }, 500)
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
      renderRbcomp(<DlgUnshare {...props} />, null, function () {
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
      <RbModal title={$L('ShareUsers')} ref={(c) => (this._dlg = c)}>
        <div className="sharing-list">
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
                      {(item[4] & 8) !== 0 && <span className="badge badge-light">{$L('Read')}</span>}
                      {(item[4] & 4) !== 0 && <span className="badge badge-light">{$L('Update')}</span>}
                    </td>
                    <td className="text-right text-muted" title={item[2]}>
                      {$L('ByXSharedOnX').replace('%s', item[3]).replace('%s', $fromNow(item[2]))}
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
          {this.props.unshare === true && (
            <button className="btn btn-primary btn-space" type="button" onClick={() => this.post()}>
              {$L('UnShare')}
            </button>
          )}
          <button className="btn btn-secondary btn-space" type="button" onClick={() => this.hide()}>
            {$L('Cancel')}
          </button>
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
    if (s.length === 0) return RbHighbar.create($L('PlsSelectSome,CancelWhichUsers'))

    const $btn = $(this._btns).button('loading')
    $.post(`/app/entity/record-unshare?id=${s.join(',')}&record=${this.props.id}`, (res) => {
      if (res.error_code === 0) {
        this.hide()
        RbHighbar.success($L('SomeSuccess,UnShare'))
        setTimeout(() => window.RbViewPage && window.RbViewPage.reload(), 200)
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
      renderRbcomp(<DlgShareManager {...props} />, null, function () {
        that.__HOLDER = this
      })
  }
}
