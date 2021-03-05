/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global RepeatedViewer */

const wpc = window.__PageConfig || {}

//~~ 视图
class RbViewForm extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }

    this.onViewEditable = wpc.onViewEditable !== false
    this.__FormData = {}
  }

  render() {
    return (
      <div className="rbview-form" ref={(c) => (this._viewForm = c)}>
        {this.state.formComponent}
      </div>
    )
  }

  componentDidMount() {
    $.get(`/app/${this.props.entity}/view-model?id=${this.props.id}`, (res) => {
      // 有错误
      if (res.error_code > 0 || !!res.data.error) {
        const err = res.data.error || res.error_msg
        this.renderViewError(err)
        return
      }

      let hadApproval = res.data.hadApproval
      if (wpc.type === 'DetailView') {
        if (hadApproval === 2) $('.J_edit, .J_delete').attr({ disabled: true, title: $L('SomeInApproval,MainRecord') })
        else if (hadApproval === 10) $('.J_edit, .J_delete').remove()
        hadApproval = null
      }

      const VFORM = (
        <div>
          {hadApproval && <ApprovalProcessor id={this.props.id} entity={this.props.entity} />}
          <div className="row">
            {res.data.elements.map((item) => {
              item.$$$parent = this
              return detectViewElement(item)
            })}
          </div>
        </div>
      )
      this.setState({ formComponent: VFORM }, () => this.hideLoading())
      this.__lastModified = res.data.lastModified || 0
    })
  }

  renderViewError(message) {
    this.setState({ formComponent: _renderError(message) }, () => this.hideLoading())
    $('.view-operating .view-action').empty()
  }

  hideLoading() {
    const ph = parent && parent.RbViewModal ? parent.RbViewModal.holder(this.state.id) : null
    ph && ph.hideLoading()
  }

  showAgain = (handle) => this.checkDrityData(handle)

  // 脏数据检查
  checkDrityData(handle) {
    if (!this.__lastModified || !this.state.id) return
    $.get(`/app/entity/extras/record-last-modified?id=${this.state.id}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.lastModified !== this.__lastModified) {
          handle && handle.showLoading()
          setTimeout(() => location.reload(), window.VIEW_LOAD_DELAY || 200)
        }
      } else if (res.error_msg === 'NO_EXISTS') {
        this.renderViewError($L('ThisRecordDeleted'))
        $('.view-operating').empty()
      }
    })
  }

  // see RbForm in `rb-forms.js`

  setFieldValue(field, value, error) {
    this.__FormData[field] = { value: value, error: error }
    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log('FV ... ' + JSON.stringify(this.__FormData))
  }

  setFieldUnchanged(field) {
    delete this.__FormData[field]
    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log('FV ... ' + JSON.stringify(this.__FormData))
  }

  // 保存单个字段值
  saveSingleFieldValue(fieldComp) {
    setTimeout(() => this._saveSingleFieldValue(fieldComp), 30)
  }

  _saveSingleFieldValue(fieldComp) {
    const fieldName = fieldComp.props.field
    const fieldValue = this.__FormData[fieldName]
    // Unchanged
    if (!fieldValue) {
      fieldComp.toggleEditMode(false)
      return
    }
    if (fieldValue.error) return RbHighbar.create(fieldValue.error)

    const data = {
      metadata: { entity: this.props.entity, id: this.props.id },
      [fieldName]: fieldValue.value,
    }

    const $btns = $(fieldComp._fieldText).find('.edit-oper .btn').button('loading')
    $.post('/app/entity/record-save?single=true', JSON.stringify(data), (res) => {
      $btns.button('reset')
      if (res.error_code === 0) {
        this.setFieldUnchanged(fieldName)
        fieldComp.toggleEditMode(false, res.data[fieldName])
        // 刷新列表
        parent && parent.RbListPage && parent.RbListPage.reload()
      } else if (res.error_code === 499) {
        // 有重复
        renderRbcomp(<RepeatedViewer entity={this.props.entity} data={res.data} />)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

const detectViewElement = function (item) {
  if (!window.detectElement) throw 'detectElement undef'
  item.onView = true
  item.editMode = false
  item.key = `col-${item.field === '$DIVIDER$' ? $random() : item.field}`
  return (
    <div className={`col-12 col-sm-${item.isFull ? 12 : 6}`} key={item.key}>
      {window.detectElement(item)}
    </div>
  )
}

const _renderError = (message) => {
  return (
    <div className="alert alert-danger alert-icon mt-5 w-75" style={{ margin: '0 auto' }}>
      <div className="icon">
        <i className="zmdi zmdi-alert-triangle"></i>
      </div>
      <div className="message" dangerouslySetInnerHTML={{ __html: `<strong>${$L('Opps')}!</strong> ${message}` }}></div>
    </div>
  )
}

// 选择报表
class SelectReport extends React.Component {
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
              <h5 className="mt-0 text-bold">{$L('SelectSome,Report')}</h5>
              {this.state.reports && this.state.reports.length === 0 && (
                <p className="text-muted">
                  {$L('NoAnySome,Report')}
                  {rb.isAdminUser && (
                    <a className="icon-link ml-1" target="_blank" href={`${rb.baseUrl}/admin/datas/data-reports`}>
                      <i className="zmdi zmdi-settings"></i> {$L('ClickConf')}
                    </a>
                  )}
                </p>
              )}
              <div>
                <ul className="list-unstyled">
                  {(this.state.reports || []).map((item) => {
                    const reportUrl = `${rb.baseUrl}/app/${this.props.entity}/report/export?report=${item.id}&record=${this.props.id}&attname=${$encode(item.name)}`
                    return (
                      <li key={'r-' + item.id}>
                        <a target="_blank" href={reportUrl} className="text-truncate">
                          {item.name}
                          <i className="zmdi zmdi-download"></i>
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
    $.get(`/app/${this.props.entity}/report/available`, (res) => this.setState({ reports: res.data }))
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
    const that = this
    renderRbcomp(<SelectReport entity={entity} id={id} />, null, function () {
      that.__cached = this
    })
  }
}

// ~ 相关项列表
class RelatedList extends React.Component {
  state = { ...this.props, viewOpens: {}, viewComponents: {} }

  render() {
    return (
      <div className={`related-list ${!this.state.list ? 'rb-loading rb-loading-active' : ''}`}>
        {!this.state.list && <RbSpinner />}
        {this.state.list && this.state.list.length === 0 && (
          <div className="list-nodata">
            <span className="zmdi zmdi-info-outline" />
            <p>{$L('NoData')}</p>
          </div>
        )}
        {(this.state.list || []).map((item) => {
          return (
            <div className={`card ${this.state.viewOpens[item[0]] ? 'active' : ''}`} key={item[0]} ref={`item-${item[0]}`}>
              <div className="row header-title" onClick={() => this._toggleInsideView(item[0])}>
                <div className="col-10">
                  <a href={`#!/View/${this.props.entity.split('.')[0]}/${item[0]}`} onClick={(e) => this._handleView(e)} title={$L('Open')}>
                    {item[1]}
                  </a>
                </div>
                <div className="col-2 text-right">
                  <span className="fs-12 text-muted" title={`${$L('f.modifiedOn')} ${item[2]}`}>
                    {$fromNow(item[2])}
                  </span>
                </div>
              </div>
              <div className="rbview-form inside">{this.state.viewComponents[item[0]] || <RbSpinner fully={true} />}</div>
            </div>
          )
        })}
        {this.state.showMores && (
          <div className="text-center load-mores">
            <div>
              <button type="button" className="btn btn-secondary" onClick={() => this.fetchList(1)}>
                {$L('LoadMore')}
              </button>
            </div>
          </div>
        )}
      </div>
    )
  }

  componentDidMount = () => this.fetchList()

  fetchList(append) {
    this.__pageNo = this.__pageNo || 1
    if (append) this.__pageNo += append
    const pageSize = 5

    $.get(`/app/entity/related-list?mainid=${this.props.main}&related=${this.props.entity}&pageNo=${this.__pageNo}&pageSize=${pageSize}`, (res) => {
      const data = res.data.data || []
      const list = (this.state.list || []).concat(data)

      this.setState({ list: list, showMores: data.length >= pageSize }, () => {
        if (this.props.autoExpand) {
          data.forEach((item) => {
            // eslint-disable-next-line react/no-string-refs
            const $H = $(this.refs[`item-${item[0]}`]).find('.header-title')
            if ($H.length > 0) $H[0].click()
          })
        }
      })
    })
  }

  _handleView(e) {
    e.preventDefault()
    $stopEvent(e)
    RbViewPage.clickView(e.currentTarget)
  }

  _toggleInsideView(id) {
    const viewOpens = this.state.viewOpens
    viewOpens[id] = !viewOpens[id]
    this.setState({ viewOpens: viewOpens })

    // 加载视图
    const viewComponents = this.state.viewComponents
    if (!viewComponents[id]) {
      $.get(`/app/${this.props.entity.split('.')[0]}/view-model?id=${id}`, (res) => {
        if (res.error_code > 0 || !!res.data.error) {
          const err = res.data.error || res.error_msg
          viewComponents[id] = _renderError(err)
        } else {
          viewComponents[id] = (
            <div className="row">
              {res.data.elements.map((item) => {
                item.$$$parent = this
                return detectViewElement(item)
              })}
            </div>
          )
        }
        this.setState({ viewComponents: viewComponents })
      })
    }
  }
}

const FeedsList = window.FeedsList || React.Component
// ~ 跟进列表
// eslint-disable-next-line no-undef
class ReducedFeedsList extends FeedsList {
  state = { ...this.props }

  render() {
    return (
      <div className={`related-list ${!this.state.data ? 'rb-loading rb-loading-active' : ''}`}>
        {!this.state.data && <RbSpinner />}
        {this.state.data && this.state.data.length === 0 && (
          <div className="list-nodata">
            <span className="zmdi zmdi-chart-donut" />
            <p>{$L('NoSome,FeedsType2')}</p>
          </div>
        )}
        <div className="feeds-list inview">
          {(this.state.data || []).map((item) => {
            return this.renderItem({ ...item, self: false })
          })}
        </div>
        {this.state.showMores && (
          <div className="text-center load-mores">
            <div>
              <button type="button" className="btn btn-secondary" onClick={() => this.fetchFeeds(1)}>
                {$L('LoadMore')}
              </button>
            </div>
          </div>
        )}
      </div>
    )
  }

  fetchFeeds(append) {
    const filter = {
      entity: 'Feeds',
      equation: '(1 OR 2) AND 3',
      items: [
        { field: 'type', op: 'EQ', value: 2 },
        { field: 'type', op: 'EQ', value: 4 },
        { field: 'relatedRecord', op: 'EQ', value: wpc.recordId },
      ],
    }

    this.__pageNo = this.__pageNo || 1
    if (append) this.__pageNo += append
    const pageSize = 20

    $.post(`/feeds/feeds-list?pageNo=${this.__pageNo}&sort=&type=&foucs=&pageSize=${pageSize}`, JSON.stringify(filter), (res) => {
      const data = (res.data || {}).data || []
      const list = (this.state.data || []).concat(data)
      this.setState({ data: list, showMores: data.length >= pageSize })
    })
  }

  _toggleComment(feeds) {
    return window.open(`${rb.baseUrl}/app/list-and-view?id=${feeds}`)
  }
}

class MixRelatedList extends React.Component {
  state = { ...this.props }

  render() {
    const entity = this.props.entity.split('.')[0]
    if (entity === 'Feeds') {
      return <ReducedFeedsList {...this.props} fetchNow={true} />
    } else {
      return <RelatedList {...this.props} />
    }
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

    renderRbcomp(<RbViewForm entity={entity[0]} id={id} />, 'tab-rbview', function () {
      RbViewPage._RbViewForm = this
    })

    $('.J_close').click(() => this.hide())
    $('.J_reload').click(() => this.reload())

    const that = this

    $('.J_delete').click(function () {
      if ($(this).attr('disabled')) return
      const needEntity = wpc.type === 'DetailList' || wpc.type === 'DetailView' ? null : entity[0]
      renderRbcomp(
        <DeleteConfirm
          id={that.__id}
          entity={needEntity}
          deleteAfter={() => {
            // 刷新主视图
            parent && parent.RbViewModal && parent.RbViewModal.currentHolder(true)
            that.hide(true)
          }}
        />
      )
    })
    $('.J_edit').click(() =>
      RbFormModal.create({
        id: id,
        title: $L('EditSome').replace('{0}', entity[1]),
        entity: entity[0],
        icon: entity[2],
      })
    )
    $('.J_assign').click(() => DlgAssign.create({ entity: entity[0], ids: [id] }))
    $('.J_share').click(() => DlgShare.create({ entity: entity[0], ids: [id] }))
    $('.J_report').click(() => SelectReport.create(entity[0], id))
    $('.J_add-detail').click(function () {
      const iv = { $MAINID$: id }
      const $this = $(this)
      RbFormModal.create({
        title: $L('AddDetail'),
        entity: $this.data('entity'),
        icon: $this.data('icon'),
        initialValue: iv,
      })
    })

    if (wpc.transformTos && wpc.transformTos.length > 0) {
      this.initTrans(wpc.transformTos)
      $('.J_trans').removeClass('hide')
    } else {
      $('.J_trans').remove()
    }

    // Privileges
    if (ep) {
      if (ep.D === false) $('.J_delete').remove()
      if (ep.U === false) $('.J_edit, .J_add-detail').remove()
      if (ep.A !== true) $('.J_assign').remove()
      if (ep.S !== true) $('.J_share').remove()
    }

    // Clean buttons
    that.cleanViewActionButton()

    that.initRecordMeta()
    that.initHistory()
  },

  // 元数据
  initRecordMeta() {
    $.get(`/app/entity/extras/record-meta?id=${this.__id}`, (res) => {
      // 如果出错就清空操作区
      if (res.error_code !== 0) {
        $('.view-operating').empty()
        return
      }

      for (let k in res.data) {
        const v = res.data[k]
        if (!v) continue
        const $el = $(`.J_${k}`)
        if ($el.length === 0) continue

        if (k === 'owningUser') {
          renderRbcomp(<UserShow id={v[0]} name={v[1]} showName={true} deptName={v[2]} onClick={() => this.clickViewUser(v[0])} />, $el[0])
        } else if (k === 'sharingList') {
          const $list = $('<ul class="list-unstyled list-inline mb-0"></ul>').appendTo($('.J_sharingList').empty())
          const that = this
          $(v).each(function () {
            const _this = this
            const $item = $('<li class="list-inline-item"></li>').appendTo($list)
            renderRbcomp(<UserShow id={_this[0]} name={_this[1]} onClick={() => that.clickViewUser(_this[0])} />, $item[0])
          })

          if (this.__ep && this.__ep.S === true) {
            const $op = $('<li class="list-inline-item"></li>').appendTo($list)[0]
            if (v.length === 0) {
              renderRbcomp(
                <UserShow
                  name={$L('AddSome,Share')}
                  icon="zmdi zmdi-plus"
                  onClick={() => {
                    $('.J_share').trigger('click')
                  }}
                />,
                $op
              )
            } else {
              renderRbcomp(<UserShow name={$L('SomeManage,ShareUsers')} icon="zmdi zmdi-more" onClick={() => DlgShareManager.create(this.__id)} />, $op)
            }
          } else if (v.length > 0) {
            const $op = $('<li class="list-inline-item"></li>').appendTo($list)[0]
            renderRbcomp(<UserShow name={$L('ViewSome,ShareUsers')} icon="zmdi zmdi-more" onClick={() => DlgShareManager.create(this.__id, false)} />, $op)
          } else {
            $('.J_sharingList').parent().remove()
          }
        } else if (k === 'createdOn' || k === 'modifiedOn') {
          renderRbcomp(<DateShow date={v} />, $el[0])
        } else {
          $(`<span>${v}</span>`).appendTo($el.empty())
        }
      }

      // If PlainEntity
      if (!res.data.owningUser) $('.view-user').remove()
    })
  },

  // 修改历史
  initHistory() {
    const $into = $('.view-history .view-history-items')
    if ($into.length === 0) return

    $.get(`/app/entity/extras/record-history?id=${this.__id}`, (res) => {
      if (res.error_code !== 0) return
      $into.empty()
      res.data.forEach((item, idx) => {
        const content = $LF('ViewHistoryContent', $fromNow(item.revisionOn)).replace('$USER$', item.revisionBy[1]).replace('$ACTION$', item.revisionType)
        const $item = $(`<li>${content}</li>`).appendTo($into)
        $item.find('b:eq(0)').attr('title', item.revisionOn)
        if (idx > 9) $item.addClass('hide')
      })

      if (res.data.length > 10) {
        $into.after(`<a href="javascript:;" class="J_mores">${$L('LoadMore')}</a>`)
        $('.view-history .J_mores').click(function () {
          $into.find('li.hide').removeClass('hide')
          $(this).addClass('hide')
        })
      }
    })
  },

  // 相关项

  // 列表
  initVTabs(config) {
    const that = this
    that.__vtabEntities = []
    $(config).each(function () {
      const entity = this.entity // Entity.Field
      that.__vtabEntities.push(entity)

      const tabId = 'tab-' + entity.replace('.', '--') // `.` is JS keyword
      const tabNav = $(`<li class="nav-item"><a class="nav-link" href="#${tabId}" data-toggle="tab" title="${this.entityLabel}">${this.entityLabel}</a></li>`).appendTo('.nav-tabs')
      const tabPane = $(`<div class="tab-pane" id="${tabId}"></div>`).appendTo('.tab-content')
      tabNav.find('a').click(function () {
        tabPane.find('.related-list').length === 0 && renderRbcomp(<MixRelatedList entity={entity} main={that.__id} autoExpand={$isTrue(wpc.viewTabsAutoExpand)} />, tabPane)
      })
    })
    this.updateVTabs()

    // for Admin
    if (rb.isAdminUser) {
      $('.J_view-addons').click(function () {
        const type = $(this).data('type')
        RbModal.create(`/p/admin/metadata/view-addons?entity=${that.__entity[0]}&type=${type}`, $L('ConfSome,' + (type === 'TAB' ? 'ViewShowAddon' : 'ViewNewAddon')))
      })
    }
  },

  // 记录数量
  updateVTabs(specEntities) {
    specEntities = specEntities || this.__vtabEntities
    if (!specEntities || specEntities.length === 0) return
    $.get(`/app/entity/related-counts?mainid=${this.__id}&relateds=${specEntities.join(',')}`, function (res) {
      for (let k in res.data || {}) {
        if (~~res.data[k] > 0) {
          const tabNav = $('.nav-tabs a[href="#tab-' + k.replace('.', '--') + '"]')
          if (tabNav.find('.badge').length > 0) tabNav.find('.badge').text(res.data[k])
          else $('<span class="badge badge-pill badge-primary">' + res.data[k] + '</span>').appendTo(tabNav)
        }
      }
    })
  },

  // 新建
  initVAdds(config) {
    const that = this
    $(config).each(function () {
      const e = this
      const title = $L('NewSome').replace('{0}', e.entityLabel)
      const $item = $(`<a class="dropdown-item"><i class="icon zmdi zmdi-${e.icon}"></i>${title}</a>`)
      $item.click(function () {
        const iv = {}
        const entity = e.entity.split('.')
        if (entity.length > 1) iv[entity[1]] = that.__id
        else iv['&' + that.__entity[0]] = that.__id
        RbFormModal.create({ title: `${title}`, entity: entity[0], icon: e.icon, initialValue: iv })
      })
      $('.J_adds .dropdown-divider').before($item)
    })
  },

  // 转换
  initTrans(config) {
    const that = this
    config.forEach((item) => {
      const $item = $(`<a class="dropdown-item"><i class="icon zmdi zmdi-${item.icon}"></i>${item.entityLabel}</a>`)
      $item.click(() => {
        const alert = $L('TransformAsTips').replace('%s', `[ ${item.entityLabel} ]`)
        RbAlert.create(alert, {
          confirm: function () {
            this.disabled(true)
            $.post(`/app/entity/extras/transform?transid=${item.transid}&source=${that.__id}`, (res) => {
              this.hide()
              if (res.error_code === 0) {
                RbHighbar.success($L('SomeSuccess,Transform'))
                setTimeout(() => that.clickView(`!#/View/${item.entity}/${res.data}`), 200)
              } else if (res.error_code === 400) {
                RbHighbar.create(res.error_msg)
              } else {
                RbHighbar.error(res.error_msg)
              }
            })
          },
        })
      })
      $('.J_trans .dropdown-divider').before($item)
    })
  },

  // 通过父级页面打开
  clickView(target) {
    if (parent && parent.RbViewModal) {
      // `#!/View/{entity}/{id}`
      const viewUrl = typeof target === 'string' ? target : $(target).attr('href')
      if (!viewUrl) {
        console.warn('Bad view target : ', target)
        return
      }
      const urlSpec = viewUrl.split('/')
      parent.RbViewModal.create({ entity: urlSpec[2], id: urlSpec[3] }, true)
    }
    return false
  },
  clickViewUser(id) {
    return this.clickView('#!/View/User/' + id)
  },

  // 清理操作按钮
  cleanViewActionButton() {
    $setTimeout(
      () => {
        $cleanMenu('.view-action .J_mores')
        $cleanMenu('.view-action .J_adds')
        $cleanMenu('.view-action .J_trans')
        $('.view-action .col-lg-6').each(function () {
          if ($(this).children().length === 0) $(this).remove()
        })
        if ($('.view-action').children().length === 0) $('.view-action').addClass('empty').empty()
      },
      100,
      'cleanViewActionButton'
    )
  },

  // 隐藏划出的 View
  hide(reload) {
    if (parent && parent !== window) {
      parent && parent.RbViewModal && parent.RbViewModal.holder(this.__id, 'HIDE')
      if (reload === true) {
        if (parent.RbListPage) parent.RbListPage.reload()
        else setTimeout(() => parent.location.reload(), 200)
      }
    } else {
      window.close() // Maybe unclose
    }
  },

  // 重新加載
  reload() {
    parent && parent.RbViewModal && parent.RbViewModal.holder(this.__id, 'LOADING')
    setTimeout(() => location.reload(), 20)
  },

  // 记录只读
  setReadonly() {
    $(this._RbViewForm._viewForm).addClass('readonly')
    $('.J_edit, .J_delete, .J_add-detail').remove()
    this.cleanViewActionButton()
  },
}

// init
$(document).ready(function () {
  if (wpc.entity) {
    RbViewPage.init(wpc.recordId, wpc.entity, wpc.privileges)
    if (wpc.viewTabs) RbViewPage.initVTabs(wpc.viewTabs)
    if (wpc.viewAdds) RbViewPage.initVAdds(wpc.viewAdds)
  }
})
