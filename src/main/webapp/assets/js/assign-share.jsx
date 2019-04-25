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
            <div className="col-sm-7 offset-sm-3"><a href="javascript:;" onClick={() => this.showCascades()}>同时{this.types[1]}关联记录</a></div>
          </div>
        ) : (
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">选择关联记录</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref={(c) => this._cascades = c}>
                {(this.state.cascadesEntity || []).map((item) => {
                  return <option key={'option-' + item[0]} value={item[0]}>{item[1]}</option>
                })}
              </select>
            </div>
          </div>
        )}
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
    __initUserSelect2(this._toUser, this.types[2] === true)
  }
  componentWillUnmount() {
    $(this._toUser, this._cascades).select2('destroy')
  }
  showCascades() {
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
    if (!users || users.length === 0) { rb.highbar('请选择' + this.types[1] + '给谁'); return }
    if ($.type(users) === 'array') users = users.join(',')
    let cass = this.state.cascadesShow === true ? $(this._cascades).val().join(',') : ''

    let btns = $(this._btns).find('.btn').button('loading')
    $.post(`${rb.baseUrl}/app/entity/record-${this.types[0]}?id=${this.state.ids.join(',')}&cascades=${cass}&to=${users}`, (res) => {
      if (res.error_code === 0) {
        this.setState({ cascadesShow: false })
        $(this._toUser, this._cascades).val(null).trigger('change')

        this.hide()
        let affected = res.data.assigned || res.data.shared || 0
        if (affected > 0 && rb.env === 'dev') rb.hbsuccess('已成功' + this.types[1] + ' ' + affected + ' 条记录')
        else rb.hbsuccess('记录已' + this.types[1])

        setTimeout(() => {
          if (window.RbListPage) RbListPage._RbList.reload()
          if (window.RbViewPage) location.reload()
        }, 500)
      } else {
        rb.hberror(res.error_msg)
      }
      btns.button('reset')
    })
  }
}

// ~~ 共享
class DlgShare extends DlgAssign {
  constructor(props) {
    super(props)
    this.types = ['share', '共享', true]
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
        this.__select2 = __initUserSelect2(this._toUser, true)
      }
    })
  }

  post() {
    let users = $(this._toUser).val()
    if (this.state.whichUsers === 'ALL') {
      users = '$ALL$'
    } else {
      if (!users || users.length === 0) { rb.highbar('请选择' + this.types[1] + '给谁'); return }
      users = users.join(',')
    }

    let btns = $(this._btns).find('.btn').button('loading')
    $.post(`${rb.baseUrl}/app/entity/record-unshare-batch?id=${this.state.ids.join(',')}&to=${users}`, (res) => {
      if (res.error_code === 0) {
        $(this._toUser).val(null).trigger('change')

        this.hide()
        if (res.data.unshared > 0 && rb.env === 'dev') rb.hbsuccess('成功取消共享 ' + res.data.unshared + ' 条记录')
        else rb.hbsuccess('已取消共享')

        setTimeout(() => {
          if (window.RbListPage) RbListPage._RbList.reload()
        }, 500)
      } else {
        rb.hberror(res.error_msg)
      }
      btns.button('reset')
    })
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
              <div onClick={() => this.clickUser(item[1])} title={'由 ' + item[3][0] + ' 共享于 ' + item[2]}>
                <UserShow name={item[0][0]} avatarUrl={item[0][1]} showName={true} />
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
    if (s.length === 0) { rb.highbar('请选择需要取消共享的用户'); return }

    let btns = $(this._btns).button('loading')
    $.post(`${rb.baseUrl}/app/entity/record-unshare?id=${s.join(',')}&record=${this.props.id}`, (res) => {
      if (res.error_code === 0) {
        this.hide()
        if (rb.env === 'dev') rb.hbsuccess('已取消 ' + res.data.unshared + ' 位用户的共享')
        else rb.hbsuccess('共享已取消')
        setTimeout(() => {
          if (window.RbViewPage) location.reload()
        }, 500)
      } else {
        rb.hberror(res.error_msg)
      }
      btns.button('reset')
    })
  }
}

// 用户选择组件 select2
let __initUserSelect2 = function (el, multiple) {
  let s = $(el).select2({
    placeholder: '选择用户',
    minimumInputLength: 1,
    multiple: multiple === true,
    ajax: {
      url: rb.baseUrl + '/commons/search/search',
      delay: 300,
      data: function (params) {
        let query = {
          entity: 'User',
          qfields: 'loginName,fullName,email,quickCode',
          q: params.term
        }
        return query
      },
      processResults: function (data) {
        let rs = data.data.map((item) => { return item })
        return { results: rs }
      }
    }
  })
  return s
}

// -- Usage

let rb = rb || {}

rb.DlgAssign__holder = null
// @props = { ids, entity }
rb.DlgAssign = function (props) {
  if (rb.DlgAssign__holder) rb.DlgAssign__holder.show(props)
  else rb.DlgAssign__holder = renderRbcomp(<DlgAssign {...props} />)
  return rb.DlgAssign__holder
}

rb.DlgShare__holder = null
// @props = { ids, entity }
rb.DlgShare = function (props) {
  if (rb.DlgShare__holder) rb.DlgShare__holder.show(props)
  else rb.DlgShare__holder = renderRbcomp(<DlgShare {...props} />)
  return rb.DlgShare__holder
}

rb.DlgUnshare__holder = null
// @props = { ids, entity }
rb.DlgUnshare = function (props) {
  if (rb.DlgUnshare__holder) rb.DlgUnshare__holder.show(props)
  else rb.DlgUnshare__holder = renderRbcomp(<DlgUnshare {...props} />)
  return rb.DlgUnshare__holder
}

rb.DlgShareManager__holder = null
// @id - record
// @unshare - true or false
rb.DlgShareManager = function (id, unshare) {
  let props = { id: id, unshare: unshare !== false }
  if (rb.DlgShareManager__holder) rb.DlgShareManager__holder.show(props)
  else rb.DlgShareManager__holder = renderRbcomp(<DlgShareManager {...props} />)
  return rb.DlgShareManager__holder
}