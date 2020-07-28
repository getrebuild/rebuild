/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */

// ~~ 分派
class DlgAssign extends RbModalHandler {

  constructor(props) {
    super(props)
    this.onView = !!window.RbViewPage
    this.specs = ['assign', '分派']
  }

  render() {
    return (
      <RbModal title={this.specs[1]} ref={(c) => this._dlg = c}>
        <div className="form">
          {this.onView === true ? null : (
            <div className="form-group row pb-0">
              <label className="col-sm-3 col-form-label text-sm-right">{this.specs[1]}哪些记录</label>
              <div className="col-sm-7">
                <div className="form-control-plaintext">{'选中的记录 (' + this.state.ids.length + '条)'}</div>
              </div>
            </div>
          )}
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{this.specs[1]}给谁</label>
            <div className="col-sm-7">
              {this._useUserSelector()}
            </div>
          </div>
          {this.state.cascadesShow !== true ? (
            <div className="form-group row">
              <div className="col-sm-7 offset-sm-3"><a href="#" onClick={this._showCascade}>同时{this.specs[1]}关联记录</a></div>
            </div>
          ) : (<div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">选择关联记录</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref={(c) => this._cascades = c}>
                {(this.state.cascadesEntity || []).map((item) => {
                  return <option key={'option-' + item[0]} value={item[0]}>{item[1]}</option>
                })}
              </select>
            </div>
          </div>)}
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => this._btns = c}>
              <button className="btn btn-primary btn-space" type="button" data-loading-text="请稍后" onClick={() => this.post()}>确定</button>
              <a className="btn btn-link btn-space" onClick={() => this.hide()}>取消</a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  _showCascade = () => {
    event && event.preventDefault()
    $.get(`/commons/metadata/references?entity=${this.props.entity}`, (res) => {
      this.setState({ cascadesShow: true, cascadesEntity: res.data }, () => {
        $(this._cascades).select2({
          multiple: true,
          placeholder: '选择关联实体 (可选)'
        }).val(null).trigger('change')
      })
    })
  }

  _useUserSelector() {
    return <UserSelector hideDepartment={true} hideRole={true} hideTeam={true} multiple={false} ref={(c) => this._UserSelector = c} />
  }

  post() {
    let users = this._UserSelector.val()
    if (!users || users.length === 0) { RbHighbar.create('请选择' + this.specs[1] + '给谁'); return }
    if ($.type(users) === 'array') users = users.join(',')
    const cass = this.state.cascadesShow === true ? $(this._cascades).val().join(',') : ''

    const $btns = $(this._btns).find('.btn').button('loading')
    $.post(`/app/entity/record-${this.specs[0]}?id=${this.state.ids.join(',')}&cascades=${cass}&to=${users}`, (res) => {
      if (res.error_code === 0) {
        this.setState({ cascadesShow: false })
        this._UserSelector.clearSelection()
        $(this._cascades).val(null).trigger('change')

        this.hide()
        const affected = res.data.assigned || res.data.shared || 0
        if (affected > 0 && rb.env === 'dev') RbHighbar.success('已成功' + this.specs[1] + ' ' + affected + ' 条记录')
        else RbHighbar.success('记录已' + this.specs[1])

        setTimeout(() => {
          if (window.RbListPage) RbListPage._RbList.reload()
          if (window.RbViewPage) location.reload()
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
    else renderRbcomp(<DlgAssign {...props} />, null, function () { that.__HOLDER = this })
  }
}

// ~~ 共享
class DlgShare extends DlgAssign {

  constructor(props) {
    super(props)
    this.specs = ['share', '共享']
  }

  _useUserSelector() {
    return <UserSelector ref={(c) => this._UserSelector = c} />
  }

  // -- Usage
  /**
   * @param {*} props 
   */
  static create(props) {
    const that = this
    if (that.__HOLDER2) that.__HOLDER2.show(props)
    else renderRbcomp(<DlgShare {...props} />, null, function () { that.__HOLDER2 = this })
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
      <RbModal title="取消共享" ref={(c) => this._dlg = c}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">取消共享哪些记录</label>
            <div className="col-sm-7">
              <div className="form-control-plaintext">{'选中的记录 (' + this.state.ids.length + '条)'}</div>
            </div>
          </div>
          <div className="form-group row pt-0 pb-0">
            <label className="col-sm-3 col-form-label text-sm-right">取消哪些用户</label>
            <div className="col-sm-7">
              <div className="mt-1">
                <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-2">
                  <input className="custom-control-input" name="whichUsers" type="radio" checked={this.state.whichUsers === 'ALL'} onChange={() => this.whichMode(true)} />
                  <span className="custom-control-label">全部用户</span>
                </label>
                <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-2">
                  <input className="custom-control-input" name="whichUsers" type="radio" checked={this.state.whichUsers === 'SPEC'} onChange={() => this.whichMode()} />
                  <span className="custom-control-label">指定用户</span>
                </label>
              </div>
              <div className={'mb-2 ' + (this.state.whichUsers === 'ALL' ? 'hide' : '')}>
                <UserSelector ref={(c) => this._UserSelector = c} />
              </div>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => this._btns = c}>
              <button className="btn btn-primary btn-space" type="button" data-loading-text="请稍后" onClick={() => this.post()}>确定</button>
              <a className="btn btn-link btn-space" onClick={() => this.hide()}>取消</a>
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
      if (!users || users.length === 0) { RbHighbar.create('请选择取消用户'); return }
      users = users.join(',')
    }

    const $btns = $(this._btns).find('.btn').button('loading')
    $.post(`/app/entity/record-unshare-batch?id=${this.state.ids.join(',')}&to=${users}`, (res) => {
      if (res.error_code === 0) {
        this._UserSelector.clearSelection()

        this.hide()
        if (res.data.unshared > 0 && rb.env === 'dev') RbHighbar.success('成功取消共享 ' + res.data.unshared + ' 条记录')
        else RbHighbar.success('已取消共享')

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
    else renderRbcomp(<DlgUnshare {...props} />, null, function () { that.__HOLDER = this })
  }
}

// ~~ 管理共享
class DlgShareManager extends RbModalHandler {

  constructor(props) {
    super(props)
    this.state.selectedAccess = []
  }

  render() {
    return (
      <RbModal title={(this.props.unshare === true ? '管理' : '') + '共享用户'} ref={(c) => this._dlg = c}>
        <div className="sharing-list">
          <ul className="list-unstyled list-inline">
            {(this.state.sharingList || []).map((item) => {
              return (
                <li className={`list-inline-item ${this.state.selectedAccess.includes(item[1]) ? 'active' : ''}`} key={`access-${item[1]}`}>
                  <div onClick={() => this.clickUser(item[1])} title={`由 ${item[3]} 共享于 ${item[2]}`}>
                    <UserShow id={item[0][0]} name={item[0][1]} showName={true} />
                    <i className="zmdi zmdi-check" />
                  </div>
                </li>
              )
            })}
          </ul>
        </div>
        <div className="dialog-footer" ref={(c) => this._btns = c}>
          {this.props.unshare === true && <button className="btn btn-primary btn-space" type="button" onClick={() => this.post()}>取消共享</button>}
          <button className="btn btn-secondary btn-space" type="button" onClick={() => this.hide()}>取消</button>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    $.get(`/app/entity/shared-list?id=${this.props.id}`, (res) => {
      this.setState({ sharingList: res.data })
    })
  }

  clickUser(id) {
    if (this.props.unshare !== true) return
    const s = this.state.selectedAccess
    if (s.includes(id)) s.remove(id)
    else s.push(id)
    this.setState({ selectedAccess: s })
  }

  post() {
    const s = this.state.selectedAccess
    if (s.length === 0) { RbHighbar.create('请选择需要取消共享的用户'); return }

    const $btns = $(this._btns).button('loading')
    $.post(`/app/entity/record-unshare?id=${s.join(',')}&record=${this.props.id}`, (res) => {
      if (res.error_code === 0) {
        this.hide()
        if (rb.env === 'dev') RbHighbar.success('已取消 ' + res.data.unshared + ' 位用户的共享')
        else RbHighbar.success('共享已取消')
        setTimeout(() => {
          if (window.RbViewPage) location.reload()
        }, 500)
      } else {
        RbHighbar.error(res.error_msg)
      }
      $btns.button('reset')
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
    else renderRbcomp(<DlgShareManager {...props} />, null, function () { that.__HOLDER = this })
  }
}