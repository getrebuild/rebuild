/* eslint-disable react/jsx-no-target-blank */
/* eslint-disable react/prop-types */
const wpc = window.__PageConfig || {}

//~~ 视图
class RbViewForm extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }
  render() {
    return <div className="rbview-form" ref={(c) => this._viewForm = c}>{this.state.formComponent}</div>
  }
  componentDidMount() {
    $.get(`${rb.baseUrl}/app/${this.props.entity}/view-model?id=${this.props.id}`, (res) => {
      // 有错误
      if (res.error_code > 0 || !!res.data.error) {
        let err = res.data.error || res.error_msg
        this.renderViewError(err)
        return
      }

      let hadApproval = res.data.hadApproval
      if (wpc.type === $pgt.SlaveView) {
        if (hadApproval === 2) $('.J_edit,.J_delete').attr('disabled', true)
        else if (hadApproval === 10) $('.J_edit,.J_delete').remove()
        hadApproval = null
      }

      let vform = (<div>
        {hadApproval && <ApprovalProcessor id={this.props.id} />}
        <div className="row">
          {res.data.elements.map((item) => { return detectViewElement(item) })}
        </div>
      </div>)
      this.setState({ formComponent: vform }, () => {
        this.hideLoading()
      })
      this.__lastModified = res.data.lastModified || 0
    })
  }
  renderViewError(message) {
    let error = <div className="alert alert-danger alert-icon mt-5 w-75" style={{ margin: '0 auto' }}>
      <div className="icon"><i className="zmdi zmdi-alert-triangle"></i></div>
      <div className="message" dangerouslySetInnerHTML={{ __html: '<strong>抱歉!</strong> ' + message }}></div>
    </div>
    this.setState({ formComponent: error }, () => {
      this.hideLoading()
    })
    $('.view-operating .view-action').empty()
  }

  hideLoading() {
    let ph = (parent && parent.RbViewModal) ? parent.RbViewModal.holder(this.state.id) : null
    ph && ph.hideLoading()
    $(this._viewForm).find('.type-NTEXT .form-control-plaintext').perfectScrollbar()
  }

  // 脏数据检查
  showAgain(handle) {
    this.checkDrityData(handle)
  }
  checkDrityData(handle) {
    if (!this.__lastModified || !this.state.id) return
    $.get(`${rb.baseUrl}/app/entity/record-lastModified?id=${this.state.id}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.lastModified !== this.__lastModified) {
          handle && handle.showLoading()
          setTimeout(() => { location.reload() }, window.VIEW_LOAD_DELAY || 200)
        }
      } else if (res.error_msg === 'NO_EXISTS') {
        this.renderViewError('此记录已被删除')
        $('.view-operating').empty()
      }
    })
  }
}

const detectViewElement = function (item) {
  if (!window.detectElement) throw 'detectElement undef'
  item.onView = true
  item.viewMode = true
  item.key = 'col-' + (item.field === '$DIVIDER$' ? $random() : item.field)
  return (<div className={'col-12 col-sm-' + (item.isFull ? 12 : 6)} key={item.key}>{window.detectElement(item)}</div>)
}

// // ~~ 动作
// class RbViewAction extends React.Component {
//   constructor(props) {
//     super(props)
//   }
//   render() {
//     const ep = this.props.ep || {}  // Privileges of current entity/record
//     const viewAdds = wpc.viewAdds || []
//     return <React.Fragment>
//       {ep.U && <div className="col-12 col-lg-6">
//         <button className="btn btn-secondary" type="button" onClick={this.edit}><i className="icon zmdi zmdi-border-color"></i> 编辑</button>
//       </div>}
//       <div className="col-12 col-lg-6 btn-group">
//         <button className="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown">更多 <i className="icon zmdi zmdi-more-vert"></i></button>
//         <div className="dropdown-menu dropdown-menu-right">
//           {ep.D && <a className="dropdown-item" onClick={this.delete}><i className="icon zmdi zmdi-delete"></i> 删除</a>}
//           {ep.A && <a className="dropdown-item" onClick={this.assign}><i className="icon zmdi zmdi-mail-reply-all"></i> 分派</a>}
//           {ep.S && <a className="dropdown-item" onClick={this.share}><i className="icon zmdi zmdi-portable-wifi"></i> 共享</a>}
//           {(ep.D || ep.A || ep.S) && <div className="dropdown-divider"></div>}
//           <a className="dropdown-item" target="_blank" href={`${rb.baseUrl}/app/entity/print?id=${this.props.id}`}><i className="icon zmdi zmdi-print"></i> 打印</a>
//           <a className="dropdown-item" onClick={this.showReports}><i className="icon zmdi zmdi-map"></i> 报表</a>
//         </div>
//       </div>
//       {(wpc.slaveEntity && wpc.slaveEntity[0]) && <div className="col-12 col-lg-6">
//         <button className="btn btn-secondary" type="button" onClick={() => this.slaveAdd(wpc.slaveEntity)}><i className="icon x14 zmdi zmdi-playlist-plus"></i> 添加明细</button>
//       </div>}
//       {(viewAdds.length > 0 || rb.isAdminUser) && <div className="col-12 col-lg-6 btn-group">
//         <button className="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown"><i className="icon zmdi zmdi-plus"></i> 新建相关</button>
//         <div className="dropdown-menu dropdown-menu-right">
//           {viewAdds.map((item) => {
//             return <a key={`vadd-${item[0]}`} className="dropdown-item" onClick={() => this.relatedAdd(item)}><i className={`icon zmdi zmdi-${item[2]}`}></i>新建{item[1]}</a>
//           })}
//           {viewAdds.length > 0 && <div className="dropdown-divider"></div>}
//           <a className="dropdown-item" onClick={() => this.relatedSet()}><i className="icon zmdi zmdi-settings"></i> 配置新建项</a>
//         </div>
//       </div>}
//     </React.Fragment>
//   }
//   componentDidMount() {
//   }
//   edit = () => {
//     const entity = this.props.entity
//     RbFormModal.create({ id: this.props.id, title: `编辑${entity[1]}`, entity: entity[0], icon: entity[2] })
//   }
//   delete = () => {
//     const entity = this.props.entity
//     let needEntity = (wpc.type === 'SlaveList' || wpc.type === 'SlaveView') ? null : entity[0]
//     renderRbcomp(<DeleteConfirm id={this.props.id} entity={needEntity} deleteAfter={() => RbViewPage.hide(true)} />)
//   }
//   assign = () => {
//     DlgAssign.create({ entity: this.props.entity[0], ids: [this.props.id] })
//   }
//   share = () => {
//     DlgShare.create({ entity: this.props.entity[0], ids: [this.props.id] })
//   }
//   slaveAdd(entity) {
//     let iv = { '$MASTER$': this.props.id }
//     RbFormModal.create({ title: '添加明细', entity: entity[0], icon: entity[2], initialValue: iv })
//   }
//   releateAdd(entity) {
//     let iv = {}
//     iv['&' + entity[0]] = this.props.id
//     RbFormModal.create({ title: `新建${entity[1]}`, entity: entity[0], icon: entity[2], initialValue: iv })
//   }
//   relatedSet() {
//   }
//   showReports() {
//   }
// }

// 选择报表
class SelectReport extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }
  render() {
    return (
      <div className="modal select-list" ref={(c) => this._dlg = c} tabIndex="-1">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={this.hide}><i className="zmdi zmdi-close" /></button>
            </div>
            <div className="modal-body">
              <h5 className="mt-0 text-bold">选择报表</h5>
              {(this.state.reports && this.state.reports.length === 0) && <p className="text-muted">无可用报表 {rb.isAdminUser && <a className="icon-link ml-1" target="_blank" href={`${rb.baseUrl}/admin/datas/data-reports`}><i className="zmdi zmdi-settings"></i> 点击配置</a>}</p>}
              <div>
                <ul className="list-unstyled">
                  {(this.state.reports || []).map((item) => {
                    let reportUrl = `${rb.baseUrl}/app/entity/report-export?report=${item.id}&record=${this.props.id}`
                    return <li key={'r-' + item.id}><a target="_blank" href={reportUrl} className="text-truncate">{item.name}<i className="zmdi zmdi-download"></i></a></li>
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
    $.get(`${rb.baseUrl}/app/entity/available-reports?entity=${this.props.entity}`, (res) => {
      this.setState({ reports: res.data })
    })
    $(this._dlg).modal({ show: true, keyboard: true })
  }
  hide = () => $(this._dlg).modal('hide')
  show = () => $(this._dlg).modal('show')

  /**
   * @param {*} entity 
   * @param {*} id 
   */
  static create(entity, id) {
    if (this.__cached) {
      this.__cached.show()
      return
    }
    let that = this
    renderRbcomp(<SelectReport entity={entity} id={id} />, null, function () { that.__cached = this })
  }
}

// ~ 相关项列表
class RelatedList extends React.Component {
  state = { ...this.props }

  render() {
    let _list = this.state.list || []
    return <div className={`related-list ${!this.state.list ? 'rb-loading rb-loading-active' : ''}`}>
      {!this.state.list && <RbSpinner />}
      {(this.state.list && this.state.list.length === 0) && <div className="list-nodata"><span className="zmdi zmdi-info-outline" /><p>暂无相关数据</p></div>}
      {_list.map((item) => {
        return <div className="card" key={`rr-${item[0]}`}>
          <div className="row">
            <div className="col-10">
              <a href={`#!/View/${this.props.entity}/${item[0]}`} onClick={this._handleView}>{item[1]}</a>
            </div>
            <div className="col-2 text-right">
              <span className="fs-12 text-muted" title="最后修改时间">{item[2]}</span>
            </div>
          </div>
        </div>
      })}
      {this.state.showMores
        && <div className="text-center load-mores"><div><button type="button" className="btn btn-secondary" onClick={() => this.loadList(1)}>加载更多</button></div></div>}
    </div>
  }

  componentDidMount = () => this.loadList()
  loadList(plus) {
    this.__pageNo = this.__pageNo || 1
    if (plus) this.__pageNo += plus
    const pageSize = 20
    $.get(`${rb.baseUrl}/app/entity/related-list?masterId=${this.props.master}&related=${this.props.entity}&pageNo=${this.__pageNo}&pageSize=${pageSize}`, (res) => {
      let _data = res.data.data || []
      let _list = this.state.list || []
      _list = _list.concat(_data)
      this.setState({ list: _list, showMores: _data.length >= pageSize })
    })
  }

  _handleView = (e) => {
    e.preventDefault()
    RbViewPage.clickView(e.currentTarget)
  }
}

// 视图页操作类
const RbViewPage = {
  _RbViewForm: null,

  /**
   * @param {*} id Record ID
   * @param {*} entity  [Name, Label, Icon]
   * @param {*} ep  Privileges of this entity
   */
  init(id, entity, ep) {
    this.__id = id
    this.__entity = entity
    this.__ep = ep

    renderRbcomp(<RbViewForm entity={entity[0]} id={id} />, 'tab-rbview', function () { RbViewPage._RbViewForm = this })

    $('.J_close').click(() => this.hide())
    $('.J_reload').click(() => this.reload())

    const that = this

    $('.J_delete').click(function () {
      if ($(this).attr('disabled')) return
      let needEntity = (wpc.type === $pgt.SlaveList || wpc.type === $pgt.SlaveView) ? null : entity[0]
      renderRbcomp(<DeleteConfirm id={that.__id} entity={needEntity} deleteAfter={() => that.hide(true)} />)
    })
    $('.J_edit').click(() => RbFormModal.create({ id: id, title: `编辑${entity[1]}`, entity: entity[0], icon: entity[2] }))
    $('.J_assign').click(() => DlgAssign.create({ entity: entity[0], ids: [id] }))
    $('.J_share').click(() => DlgShare.create({ entity: entity[0], ids: [id] }))
    $('.J_add-slave').click(function () {
      let iv = { '$MASTER$': id }
      let $this = $(this)
      RbFormModal.create({ title: '添加明细', entity: $this.data('entity'), icon: $this.data('icon'), initialValue: iv })
    })
    $('.J_report').click(() => SelectReport.create(entity[0], id))

    // Privileges
    if (ep) {
      if (ep.D === false) $('.J_delete').remove()
      if (ep.U === false) $('.J_edit, .J_add-slave').remove()
      if (ep.A === false) $('.J_assign').remove()
      if (ep.S === false) $('.J_share').remove()
      that.cleanViewActionButton()
    }
  },

  // 记录元数据
  initRecordMeta() {
    $.get(`${rb.baseUrl}/app/entity/record-meta?id=${this.__id}`, (res) => {
      if (res.error_code !== 0) {
        $('.view-operating').empty()
        return
      }
      for (let k in res.data) {
        let v = res.data[k]
        if (!v || v === undefined) return
        if (k === 'owningUser') {
          renderRbcomp(<UserShow id={v[0]} name={v[1]} showName={true} deptName2={v[2]} onClick={() => { this.clickViewUser(v[0]) }} />, $('.J_owningUser')[0])
        } else if (k === 'sharingList') {
          let list = $('<ul class="list-unstyled list-inline mb-0"></ul>').appendTo('.J_sharingList')
          let _this = this
          $(v).each(function () {
            let $v = this
            let item = $('<li class="list-inline-item"></li>').appendTo(list)
            renderRbcomp(<UserShow id={$v[0]} name={$v[1]} onClick={() => { _this.clickViewUser($v[0]) }} />, item[0])
          })

          if (this.__ep && this.__ep.S === true) {
            let item_op = $('<li class="list-inline-item"></li>').appendTo(list)[0]
            if (v.length === 0) renderRbcomp(<UserShow name="添加共享" icon="zmdi zmdi-plus" onClick={() => { $('.J_share').trigger('click') }} />, item_op)
            else renderRbcomp(<UserShow name="管理共享用户" icon="zmdi zmdi-more" onClick={() => { DlgShareManager.create(this.__id) }} />, item_op)
          } else if (v.length > 0) {
            let item_op = $('<li class="list-inline-item"></li>').appendTo(list)[0]
            renderRbcomp(<UserShow name="查看共享用户" icon="zmdi zmdi-more" onClick={() => { DlgShareManager.create(this.__id, false) }} />, item_op)
          } else {
            $('.J_sharingList').parent().remove()
          }
        } else {
          $('<span>' + v + '</span>').appendTo('.J_' + k)
        }
      }
    })
  },

  // 相关项

  // 列表
  initVTabs(config) {
    let that = this
    that.__vtabEntities = []
    $(config).each(function () {
      let entity = this[0]
      that.__vtabEntities.push(entity)
      let tabId = 'tab-' + entity
      let tabNav = $('<li class="nav-item"><a class="nav-link" href="#' + tabId + '" data-toggle="tab">' + this[1] + '</a></li>').appendTo('.nav-tabs')
      let tabPane = $('<div class="tab-pane" id="' + tabId + '"></div>').appendTo('.tab-content')
      tabNav.find('a').click(function () {
        tabPane.find('.related-list').length === 0 && renderRbcomp(<RelatedList entity={entity} master={that.__id} />, tabPane)
      })
    })
    this.updateVTabs()

    // for Admin
    if (rb.isAdminUser) {
      $('.J_view-addons').click(function () {
        let type = $(this).data('type')
        RbModal.create(`${rb.baseUrl}/p/admin/entityhub/view-addons?entity=${that.__entity[0]}&type=${type}`, '配置' + (type === 'TAB' ? '显示项' : '新建项'))
      })
    }
  },

  // 记录数量
  updateVTabs(specEntities) {
    specEntities = specEntities || this.__vtabEntities
    if (!specEntities || specEntities.length === 0) return
    $.get(`${rb.baseUrl}/app/entity/related-counts?masterId=${this.__id}&relateds=${specEntities.join(',')}`, function (res) {
      for (let k in (res.data || {})) {
        if (~~res.data[k] > 0) {
          let tabNav = $('.nav-tabs a[href="#tab-' + k + '"]')
          if (tabNav.find('.badge').length > 0) tabNav.find('.badge').text(res.data[k])
          else $('<span class="badge badge-pill badge-primary">' + res.data[k] + '</span>').appendTo(tabNav)
        }
      }
    })
  },

  // 新建
  initVAdds(config) {
    let that = this
    $(config).each(function () {
      let entity = this
      let item = $('<a class="dropdown-item"><i class="icon zmdi zmdi-' + entity[2] + '"></i>新建' + entity[1] + '</a>')
      item.click(function () {
        let iv = {}
        iv['&' + that.__entity[0]] = that.__id
        RbFormModal.create({ title: `新建${entity[1]}`, entity: entity[0], icon: entity[2], initialValue: iv })
      })
      $('.J_adds .dropdown-divider').before(item)
    })
    this.cleanViewActionButton()
  },

  // 通过父级页面打开

  clickView(el) {
    if (parent && parent.RbViewModal) {
      let viewUrl = $(el).attr('href')
      viewUrl = viewUrl.split('/')
      parent.RbViewModal.create({ entity: viewUrl[2], id: viewUrl[3] }, true)
    }
    return false
  },
  clickViewUser(id) {
    if (parent && parent.RbViewModal) parent.RbViewModal.create({ entity: 'User', id: id }, true)
    return false
  },

  // 清理操作按钮
  cleanViewActionButton() {
    $setTimeout(() => {
      $cleanMenu('.view-action .J_mores')
      $cleanMenu('.view-action .J_adds')
      $('.view-action .col-lg-6').each(function () { if ($(this).children().length === 0) $(this).remove() })
      if ($('.view-action').children().length === 0) $('.view-action').addClass('empty').empty()
    }, 100, 'cleanViewActionButton')
  },

  // 隐藏划出的 View
  hide(reload) {
    (parent && parent.RbViewModal) && parent.RbViewModal.holder(this.__id, 'HIDE')
    if (reload === true) {
      if (parent.RbListPage) parent.RbListPage.reload()
      else setTimeout(() => parent.location.reload(), 200)
    }
  },

  // 重新加載
  reload() {
    (parent && parent.RbViewModal) && parent.RbViewModal.holder(this.__id, 'LOADING')
    setTimeout(() => location.reload(), 20)
  }
}

// Init
$(document).ready(function () {
  if (wpc.entity) {
    RbViewPage.init(wpc.recordId, wpc.entity, wpc.privileges)
    RbViewPage.initRecordMeta()
    if (wpc.viewTabs) RbViewPage.initVTabs(wpc.viewTabs)
    if (wpc.viewAdds) RbViewPage.initVAdds(wpc.viewAdds)
  }
})