/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
// @see rb-datalist.common.js

const wpc = window.__PageConfig || {}

// ~~ 列表操作

const RbListPage = {
  _RbList: null,

  /**
   * @param {JSON} config DataList config
   * @param {Object} entity [Name, Label, Icon]
   * @param {JSON} ep Privileges of the entity
   */
  init: function (config, entity, ep) {
    renderRbcomp(<RbList config={config} uncheckbox={config.uncheckbox} />, 'react-list', function () {
      RbListPage._RbList = this

      if (window.FrontJS) {
        window.FrontJS.DataList._trigger('open', [this])
      }
    })

    const that = this

    $('.J_view').on('click', () => {
      const ids = this._RbList.getSelectedIds()
      if (ids.length >= 1) {
        location.hash = `!/View/${entity[0]}/${ids[0]}`
        RbViewModal.create({ id: ids[0], entity: entity[0] })
      }
    })
    $('.J_edit').on('click', () => {
      const ids = this._RbList.getSelectedIds()
      if (ids.length >= 1) {
        RbFormModal.create({ id: ids[0], title: $L('编辑%s', entity[1]), entity: entity[0], icon: entity[2], showExtraButton: true }, true)
      }
    })
    $('.J_delete').on('click', () => {
      if ($('.J_delete').attr('disabled')) return
      const ids = this._RbList.getSelectedIds()
      if (ids.length < 1) return
      const needEntity = wpc.type === 'DetailList' || wpc.type === 'DetailView' ? null : entity[0]
      renderRbcomp(
        <DeleteConfirm
          ids={ids}
          entity={needEntity}
          deleteAfter={() => {
            that._RbList.reload()
          }}
        />
      )
    })
    $('.J_assign').on('click', () => {
      if ($('.J_assign').attr('disabled')) return
      const ids = this._RbList.getSelectedIds()
      ids.length > 0 && DlgAssign.create({ entity: entity[0], ids: ids })
    })
    $('.J_share').on('click', () => {
      if ($('.J_share').attr('disabled')) return
      const ids = this._RbList.getSelectedIds()
      ids.length > 0 && DlgShare.create({ entity: entity[0], ids: ids })
    })
    $('.J_unshare').on('click', () => {
      if ($('.J_unshare').attr('disabled')) return
      const ids = this._RbList.getSelectedIds()
      ids.length > 0 && DlgUnshare.create({ entity: entity[0], ids: ids })
    })
    $('.J_columns').on('click', () => RbModal.create(`/p/general/list-fields?entity=${entity[0]}`, $L('设置列显示')))

    // Privileges
    if (ep) {
      if (ep.C === false) $('.J_new, .J_new_group').remove()
      if (ep.D === false) $('.J_delete').remove()
      if (ep.U === false) $('.J_edit, .J_batch-update').remove()
      if (ep.A !== true) $('.J_assign').remove()
      if (ep.S !== true) $('.J_share, .J_unshare').remove()
      $cleanMenu('.J_action')
      $('.dataTables_oper.invisible2').removeClass('invisible2')
    }

    // Filter Pane
    const $fp = $('.quick-filter-pane>span')
    if ($fp[0]) {
      // eslint-disable-next-line react/jsx-no-undef
      renderRbcomp(<AdvFilterPane entity={entity[0]} fields={wpc.paneFields} onSearch={(s) => RbListPage._RbList.search(s)} />, $fp[0])
      // $('.dataTables_filter .input-search').hide()
    }

    typeof window.startTour === 'function' && window.startTour(1000)
  },

  reload() {
    this._RbList.reload()
  },
}

// ~~ 视图

class RbViewModal extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, inLoad: true, isHide: true, destroy: false }

    this._mcWidth = props.subView === true ? 1344 : 1404
    if ($(window).width() < 1464) this._mcWidth -= 184
  }

  render() {
    return this.state.destroy ? null : (
      <div className="modal-wrapper">
        <div className="modal rbview" ref={(c) => (this._$rbview = c)}>
          <div className="modal-dialog">
            <div className="modal-content" style={{ width: this._mcWidth }}>
              <div className={`modal-body iframe rb-loading ${this.state.inLoad === true && 'rb-loading-active'}`}>
                <iframe
                  data-subview={this.props.subView || false}
                  ref={(c) => (this._iframe = c)}
                  className={this.state.isHide ? 'invisible' : ''}
                  src={this.state.showAfterUrl || 'about:blank'}
                  frameBorder="0"
                  scrolling="no"
                />
                <RbSpinner />
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const $root = $(this._$rbview)
    const $rootp = $root.parent().parent()
    const $mc = $root.find('.modal-content')
    const that = this
    $root
      .on('hidden.bs.modal', function () {
        $mc.css({ 'margin-right': -1500 })
        that.setState({ inLoad: true, isHide: true })
        if (!$keepModalOpen()) location.hash = '!/View/'

        // SubView 子视图不保持
        if (that.state.disposeOnHide === true) {
          $root.modal('dispose')
          that.setState({ destroy: true }, () => {
            RbViewModal.holder(that.state.id, 'DISPOSE')
            $unmount($rootp)
          })
        }
      })
      .on('shown.bs.modal', function () {
        $mc.css('margin-right', 0)
        if (that._urlChanged === false) {
          const w = $mc.find('iframe')[0].contentWindow
          // 检查数据
          if (w.RbViewPage && w.RbViewPage._RbViewForm) w.RbViewPage._RbViewForm.showAgain(that)
        }

        const $mcbd = $('body>.modal-backdrop.show')
        if ($mcbd[0]) {
          $mcbd.addClass('o')
          $mcbd.eq(0).removeClass('o')
        }
      })
    this.show()
  }

  hideLoading() {
    this.setState({ inLoad: false, isHide: false })
  }

  showLoading() {
    this.setState({ inLoad: true, isHide: true })
  }

  show(url, option) {
    let urlChanged = true
    if (url && url === this.state.url) urlChanged = false
    option = option || {}
    url = url || this.state.url

    this._urlChanged = urlChanged
    this.setState({ ...option, url: url, inLoad: urlChanged, isHide: urlChanged }, () => {
      $(this._$rbview).modal({ show: true, backdrop: true, keyboard: false })
      setTimeout(() => {
        this.setState({ showAfterUrl: this.state.url })
      }, 210) // 0.2s in rb-page.css '.rbview.show .modal-content'
    })
  }

  hide() {
    $(this._$rbview).modal('hide')
  }

  // -- Usage

  static mode = 1

  /**
   * @param {*} props
   * @param {Boolean} subView
   */
  static create(props, subView) {
    if (props.id) props.id = props.id.toLowerCase()

    this.__HOLDERs = this.__HOLDERs || {}
    this.__HOLDERsStack = this.__HOLDERsStack || []
    const that = this
    const viewUrl = `${rb.baseUrl}/app/${props.entity}/view/${props.id}`

    if (subView) {
      renderRbcomp(<RbViewModal url={viewUrl} id={props.id} disposeOnHide subView />, function () {
        that.__HOLDERs[props.id] = this
        that.__HOLDERsStack.push(this)
      })
    } else {
      if (this.__HOLDER) {
        this.__HOLDER.show(viewUrl)
        this.__HOLDERs[props.id] = this.__HOLDER
      } else {
        renderRbcomp(<RbViewModal url={viewUrl} id={props.id} />, function () {
          that.__HOLDERs[props.id] = this
          that.__HOLDERsStack.push(this)
          that.__HOLDER = this
        })
      }
    }
  }

  /**
   * 获取视图
   * @param {*} id
   * @param {*} action [DISPOSE|HIDE|LOADING]
   */
  static holder(id, action) {
    if (action === 'DISPOSE') {
      delete this.__HOLDERs[id]
      this.__HOLDERsStack.pop() // 销毁后替换
      this.__HOLDERsStack.forEach((x) => {
        if (x.props.id === id) this.__HOLDERs[id] = x
      })
    } else if (action === 'HIDE') {
      this.__HOLDERs[id] && this.__HOLDERs[id].hide()
    } else if (action === 'LOADING') {
      this.__HOLDERs[id] && this.__HOLDERs[id].showLoading()
    } else {
      return this.__HOLDERs[id]
    }
  }

  /**
   * 当前激活主视图
   */
  static currentHolder(reload) {
    if (reload && this.__HOLDER) {
      this.__HOLDER.showLoading()
      this.__HOLDER._iframe.contentWindow.location.reload()
    }
    return this.__HOLDER
  }
}

$(document).ready(() => {
  window.RbListCommon && window.RbListCommon.init(wpc)

  const viewHash = (location.hash || '').split('/')
  if (['RecordList', 'DetailList'].includes(wpc.type) && viewHash.length === 4 && viewHash[1] === 'View' && $regex.isId(viewHash[3])) {
    setTimeout(() => RbViewModal.create({ entity: viewHash[2], id: viewHash[3] }), 500)
  }

  // ASIDE
  if (wpc.advListAsideShows && wpc.advListAsideShows.length > 0) {
    wpc.advListAsideShows.forEach((item) => {
      $(`<li class="nav-item"><a class="nav-link" href="#${item[2]}" data-toggle="tab">${item[0]}</a></li>`).appendTo('#asideShows')
    })

    // eslint-disable-next-line no-undef
    if ($('#asideCharts')[0]) ChartsWidget.init()
    // eslint-disable-next-line no-undef
    if ($('#asideCategory')[0]) CategoryWidget.init()

    const $wtab = $('.page-aside.widgets .nav a:eq(0)')
    if ($wtab.length > 0) {
      $('.page-aside.widgets .ph-item.rb').remove()
      $wtab.trigger('click')
    }

    $('.side-toggle').on('click', () => {
      const $el = $('.rb-aside').toggleClass('rb-aside-collapsed')
      $.cookie('rb.asideCollapsed', $el.hasClass('rb-aside-collapsed'), { expires: 180 })
    })

    const $content = $('.page-aside .tab-content')
    $addResizeHandler(() => {
      $content.height($(window).height() - 135)
      $content.perfectScrollbar('update')
    })()
  }

  // v3.8, v3.9
  wpc.easyAction && window.EasyAction4List && window.EasyAction4List.init(wpc.easyAction)
})

// v4.1 AI
window.attachAibotPageData = function (cb) {
  renderRbcomp(
    // eslint-disable-next-line react/jsx-no-undef
    <DlgAttachRecordList
      onConfirm={(s) => {
        const qe = RbListPage._RbList.getLastQueryEntry()
        qe._dataRange = s
        typeof cb === 'function' && cb({ listFilter: qe, name })
      }}
    />
  )
}
