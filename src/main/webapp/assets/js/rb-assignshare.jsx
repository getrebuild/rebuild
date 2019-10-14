/* eslint-disable no-unused-vars */
/* eslint-disable react/prop-types */
// ~~ 分派
class DlgAssign extends RbModalHandler {
  constructor(props) {
    super(props)
    this.onView = !!window.RbViewPage
    this.types = ['assign', '分派']
  }
  render() {
    return (<RbModal title={this.types[1]} ref={(c) => this._dlg = c}>
      <div className="form">
        {this.onView === true ? null : (
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{this.types[1]}哪些记录</label>
            <div className="col-sm-7">
              <div className="form-control-plaintext">{'选中的记录 (' + this.state.ids.length + '条)'}</div>
            </div>
          </div>
        )}
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{this.types[1]}给谁</label>
          <div className="col-sm-7">
            <select className="form-control form-control-sm" ref={(c) => this._toUser = c} />
          </div>
        </div>
        {this.state.cascadesShow !== true ? (
          <div className="form-group row">
            <div className="col-sm-7 offset-sm-3"><a href="#" onClick={this.showCascades}>同时{this.types[1]}关联记录</a></div>
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
    </RbModal>)
  }
  componentDidMount() {
    $initUserSelect2(this._toUser, this.types[2] === true)
  }
  componentWillUnmount() {
    $(this._toUser, this._cascades).select2('destroy')
  }
  showCascades = () => {
    event.preventDefault()
    $.get(rb.baseUrl + '/commons/metadata/references?entity=' + this.props.entity, (res) => {
      this.setState({ cascadesShow: true, cascadesEntity: res.data }, () => {
        $(this._cascades).select2({
          multiple: true,
          placeholder: '选择关联实体 (可选)'
        }).val(null).trigger('change')
      })
    })
  }

  post() {
    let users = $(this._toUser).val()
    if (!users || users.length === 0) { RbHighbar.create('请选择' + this.types[1] + '给谁'); return }
    if ($.type(users) === 'array') users = users.join(',')
    let cass = this.state.cascadesShow === true ? $(this._cascades).val().join(',') : ''

    let btns = $(this._btns).find('.btn').button('loading')
    $.post(`${rb.baseUrl}/app/entity/record-${this.types[0]}?id=${this.state.ids.join(',')}&cascades=${cass}&to=${users}`, (res) => {
      if (res.error_code === 0) {
        this.setState({ cascadesShow: false })
        $(this._toUser, this._cascades).val(null).trigger('change')

        this.hide()
        let affected = res.data.assigned || res.data.shared || 0
        if (affected > 0 && rb.env === 'dev') RbHighbar.success('已成功' + this.types[1] + ' ' + affected + ' 条记录')
        else RbHighbar.success('记录已' + this.types[1])

        setTimeout(() => {
          if (window.RbListPage) RbListPage._RbList.reload()
          if (window.RbViewPage) location.reload()
        }, 500)
      } else {
        RbHighbar.error(res.error_msg)
      }
      btns.button('reset')
    })
  }

  // -- Usage
  /**
   * @param {*} props 
   */
  static create(props) {
    let that = this
    if (that.__HOLDER) that.__HOLDER.show(props)
    else renderRbcomp(<DlgAssign {...props} />, null, function () { that.__HOLDER = this })
  }
}

// ~~ 共享
class DlgShare extends DlgAssign {
  constructor(props) {
    super(props)
    this.types = ['share', '共享', true]
  }

  // -- Usage
  /**
   * @param {*} props 
   */
  static create(props) {
    let that = this
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
    return (<RbModal title="取消共享" ref={(c) => this._dlg = c}>
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
              <label className="custom-control custom-control-sm custom-radio custom-control-inline">
                <input className="custom-control-input" name="whichUsers" type="radio" checked={this.state.whichUsers === 'ALL'} onChange={() => this.whichMode(true)} />
                <span className="custom-control-label">全部用户</span>
              </label>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline">
                <input className="custom-control-input" name="whichUsers" type="radio" checked={this.state.whichUsers === 'SPEC'} onChange={() => this.whichMode()} />
                <span className="custom-control-label">指定用户</span>
              </label>
            </div>
          </div>
        </div>
        <div className={'form-group row pt-0 ' + (this.state.whichUsers === 'ALL' ? 'hide' : '')}>
          <label className="col-sm-3 col-form-label text-sm-right"></label>
          <div className="col-sm-7">
            <select className="form-control form-control-sm" ref={(c) => this._toUser = c} />
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3" ref={(c) => this._btns = c}>
            <button className="btn btn-primary btn-space" type="button" data-loading-text="请稍后" onClick={() => this.post()}>确定</button>
            <a className="btn btn-link btn-space" onClick={() => this.hide()}>取消</a>
          </div>
        </div>
      </div>
    </RbModal>)
  }
  componentWillUnmount() {
    if (this.__select2) $(this._toUser).select2('destroy')
  }
  whichMode(isAll) {
    this.setState({ whichUsers: isAll === true ? 'ALL' : 'SPEC' }, () => {
      if (isAll !== true && !this.__select2) {
        this.__select2 = $initUserSelect2(this._toUser, true)
      }
    })
  }

  post() {
    let users = $(this._toUser).val()
    if (this.state.whichUsers === 'ALL') {
      users = '$ALL$'
    } else {
      if (!users || users.length === 0) { RbHighbar.create('请选择' + this.types[1] + '给谁'); return }
      users = users.join(',')
    }

    let btns = $(this._btns).find('.btn').button('loading')
    $.post(`${rb.baseUrl}/app/entity/record-unshare-batch?id=${this.state.ids.join(',')}&to=${users}`, (res) => {
      if (res.error_code === 0) {
        $(this._toUser).val(null).trigger('change')

        this.hide()
        if (res.data.unshared > 0 && rb.env === 'dev') RbHighbar.success('成功取消共享 ' + res.data.unshared + ' 条记录')
        else RbHighbar.success('已取消共享')

        setTimeout(() => {
          if (window.RbListPage) RbListPage._RbList.reload()
        }, 500)
      } else {
        RbHighbar.error(res.error_msg)
      }
      btns.button('reset')
    })
  }

  // -- Usage
  /**
   * @param {*} props 
   */
  static create(props) {
    let that = this
    if (that.__HOLDER) that.__HOLDER.show(props)
    else renderRbcomp(<DlgUnshare {...props} />, null, function () { that.__HOLDER = this })
  }
}

// ~~ 管理共享
class DlgShareManager extends RbModalHandler {
  constructor(props) {
    super(props)
    this.state.selectAccess = []
  }
  render() {
    return (<RbModal title={(this.props.unshare === true ? '管理' : '') + '共享用户'} ref={(c) => this._dlg = c}>
      <div className="sharing-list">
        <ul className="list-unstyled list-inline">
          {(this.state.sharingList || []).map((item) => {
            return (<li className="list-inline-item" key={'user-' + item[1]}>
              <div onClick={() => this.clickUser(item[1])} title={'由 ' + item[3] + ' 共享于 ' + item[2]}>
                <UserShow id={item[0][0]} name={item[0][1]} showName={true} />
                {this.state.selectAccess.contains(item[1]) && <i className="zmdi zmdi-check-circle" />}
              </div>
            </li>)
          })}
        </ul>
        <div className="dialog-footer" ref={(c) => this._btns = c}>
          {this.props.unshare === true && <button className="btn btn-primary btn-space" type="button" onClick={() => this.post()}>取消共享</button>}
          <button className="btn btn-secondary btn-space" type="button" onClick={() => this.hide()}>取消</button>
        </div>
      </div>
    </RbModal>)
  }
  componentDidMount() {
    $.get(`${rb.baseUrl}/app/entity/shared-list?id=${this.props.id}`, (res) => {
      this.setState({ sharingList: res.data })
    })
  }
  clickUser(id) {
    if (this.props.unshare !== true) return
    let s = this.state.selectAccess
    if (s.contains(id)) s.remove(id)
    else s.push(id)
    this.setState({ selectAccess: s })
  }
  post() {
    let s = this.state.selectAccess
    if (s.length === 0) { RbHighbar.create('请选择需要取消共享的用户'); return }

    let btns = $(this._btns).button('loading')
    $.post(`${rb.baseUrl}/app/entity/record-unshare?id=${s.join(',')}&record=${this.props.id}`, (res) => {
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
      btns.button('reset')
    })
  }

  // -- Usage
  /**
   * @param {*} id 
   * @param {*} unshare 
   */
  static create(id, unshare) {
    let props = { id: id, unshare: unshare !== false }
    let that = this
    if (that.__HOLDER) that.__HOLDER.show(props)
    else renderRbcomp(<DlgShareManager {...props} />, null, function () { that.__HOLDER = this })
  }
}