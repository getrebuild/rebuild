/* eslint-disable react/prop-types */
/* eslint-disable react/no-string-refs */
// ~~ 分派
class DlgAssign extends RbModalHandler {
  constructor(props) {
    super(props)
    this.onView = !!window.RbViewPage
    this.type = 'assign'
    this.typeName = '分派'
  }
  render() {
    return (<RbModal title={this.typeName} ref="dlg">
      <div className="form">
        {this.onView === true ? null : (
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{this.typeName}哪些记录</label>
            <div className="col-sm-7">
              <div className="form-control-plaintext">{'选中的记录 (' + this.state.ids.length + '条)'}</div>
            </div>
          </div>
        )}
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{this.typeName}给谁</label>
          <div className="col-sm-7">
            <select className="form-control form-control-sm" ref="toUser" />
          </div>
        </div>
        {this.state.cascadesShow !== true ? (
          <div className="form-group row">
            <div className="col-sm-7 offset-sm-3"><a href="javascript:;" onClick={() => this.showCascades()}>同时{this.typeName}关联记录</a></div>
          </div>
        ) : (
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">选择关联记录</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref="cascades" multiple="multiple">
                {(this.state.cascadesEntity || []).map((item) => { return <option key={'option-' + item[0]} value={item[0]}>{item[1]}</option> })}
              </select>
            </div>
          </div>
        )}
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3" ref="btns">
            <button className="btn btn-primary btn-space" type="button" data-loading-text="请稍后" onClick={() => this.post()}>确定</button>
            <a className="btn btn-link btn-space" onClick={() => this.hide()}>取消</a>
          </div>
        </div>
      </div>
    </RbModal>)
  }
  componentDidMount() {
    $(this.refs['toUser']).select2({
      placeholder: '选择用户',
      multiple: this.multipleUser === true,
      minimumInputLength: 1,
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
  }
  componentWillUnmount() {
    $(this.refs['toUser'], this.refs['cascades']).select2('destroy')
  }
  showCascades() {
    let that = this
    $.get(rb.baseUrl + '/commons/metadata/references?entity=' + this.props.entity, function (res) {
      that.setState({ cascadesShow: true, cascadesEntity: res.data }, function () {
        $(that.refs['cascades']).select2({
          placeholder: '选择关联实体 (可选)'
        }).val(null).trigger('change')
      })
    })
  }

  post() {
    let tous = $(this.refs['toUser']).val()
    if (!tous || tous.length === 0) { rb.highbar('请选择' + this.typeName + '给谁'); return }
    if ($.type(tous) === 'array') tous = tous.join(',')
    let cass = this.state.cascadesShow === true ? $(this.refs['cascades']).val().join(',') : ''

    let btns = $(this.refs['btns']).find('.btn').button('loading')
    $.post(`${rb.baseUrl}/app/entity/record-${this.type}?id=${this.state.ids.join(',')}&cascades=${cass}&to=${tous}`, (res) => {
      if (res.error_code === 0) {
        this.setState({ cascadesShow: false })
        $(this.refs['toUser'], this.refs['cascades']).val(null).trigger('change')

        this.hide()
        let affected = res.data.assigned || res.data.shared || 0
        if (affected > 0 && rb.env === 'dev') rb.hbsuccess('已成功' + this.typeName + ' ' + affected + ' 条记录')
        else rb.hbsuccess('记录已' + this.typeName)

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
    this.type = 'share'
    this.typeName = '共享'
    this.multipleUser = !true  // TODO
  }
}

// ~~ 管理共享
class DlgUnShare extends RbModalHandler {
  constructor(props) {
    super(props)
    this.state.selectAccess = []
  }
  render() {
    return (<RbModal title={(this.props.unshare === true ? '管理' : '') + '共享用户'} ref="dlg">
      <div className="sharing-list">
        <ul className="list-unstyled list-inline">
          {(this.state.sharingList || []).map((item) => {
            return (<li className="list-inline-item" key={'user-' + item[1]}>
              <div onClick={() => this.clickUser(item[1])} title={'由 ' + item[3][0] + ' 共享于 ' + item[2]}>
                <UserShow name={item[0][0]} avatarUrl={item[0][1]} showName={true} />
                {this.state.selectAccess.contains(item[1]) && <i className="zmdi zmdi-delete" />}
              </div>
            </li>)
          })}
        </ul>
        <div className="dialog-footer" ref="btns">
          {this.props.unshare === true && <button className="btn btn-primary btn-space" type="button" onClick={() => this.post()}>取消共享</button>}
          <button className="btn btn-secondary btn-space" type="button" onClick={() => this.hide()}>取消</button>
        </div>
      </div>
    </RbModal>)
  }
  componentDidMount() {
    $.get(`${rb.baseUrl}/app/entity/sharing-list?id=${this.props.id}`, (res) => {
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

    let btns = $(this.refs['btns']).button('loading')
    $.post(`${rb.baseUrl}/app/entity/record-unshare?id=${s.join(',')}&record=${this.props.id}`, (res) => {
      if (res.error_code === 0) {
        this.hide()
        if (rb.env === 'dev') rb.hbsuccess('已取消 ' + res.data.unshared + ' 位用户的共享')
        else rb.hbsuccess('共享已取消')
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

rb.DlgUnShare__holder = null
// @id - record
// @unshare - true or false
rb.DlgUnShare = function (id, unshare) {
  let props = { id: id, unshare: unshare !== false }
  if (rb.DlgUnShare__holder) rb.DlgUnShare__holder.show(props)
  else rb.DlgUnShare__holder = renderRbcomp(<DlgUnShare {...props} />)
  return rb.DlgUnShare__holder
}