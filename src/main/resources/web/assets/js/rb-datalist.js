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
        window.FrontJS.DataList._trigger('open', [])
      }
    })

    const that = this

    $('.J_edit').on('click', () => {
      const ids = this._RbList.getSelectedIds()
      if (ids.length >= 1) {
        RbFormModal.create({ id: ids[0], title: $L('编辑%s', entity[1]), entity: entity[0], icon: entity[2] })
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

    $('.J_view').on('click', () => {
      const ids = this._RbList.getSelectedIds()
      if (ids.length >= 1) {
        location.hash = `!/View/${entity[0]}/${ids[0]}`
        RbViewModal.create({ id: ids[0], entity: entity[0] })
      }
    })

    $('.J_columns').on('click', () => RbModal.create(`/p/general/list-fields?entity=${entity[0]}`, $L('设置列显示')))

    // 权限实体才有

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

    // Privileges
    if (ep) {
      if (ep.C === false) $('.J_new').remove()
      if (ep.D === false) $('.J_delete').remove()
      if (ep.U === false) $('.J_edit, .J_batch').remove()
      if (ep.A !== true) $('.J_assign').remove()
      if (ep.S !== true) $('.J_share, .J_unshare').remove()
      $cleanMenu('.J_action')
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
    this.mcWidth = this.props.subView === true ? 1344 : 1404
    if ($(window).width() < 1464) this.mcWidth -= 184
  }

  render() {
    if (this.state.destroy) return null

    return (
      <div className="modal-wrapper">
        <div className="modal rbview" ref={(c) => (this._rbview = c)}>
          <div className="modal-dialog">
            <div className="modal-content" style={{ width: this.mcWidth }}>
              <div className={`modal-body iframe rb-loading ${this.state.inLoad === true && 'rb-loading-active'}`}>
                <iframe ref={(c) => (this._iframe = c)} className={this.state.isHide ? 'invisible' : ''} src={this.state.showAfterUrl || 'about:blank'} frameBorder="0" scrolling="no" />
                <RbSpinner />
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const $root = $(this._rbview)
    const rootWrap = $root.parent().parent()
    const mc = $root.find('.modal-content')
    const that = this
    $root
      .on('hidden.bs.modal', function () {
        mc.css({ 'margin-right': -1500 })
        that.setState({ inLoad: true, isHide: true })
        if (!$keepModalOpen()) location.hash = '!/View/'

        // SubView 子视图不保持
        if (that.state.disposeOnHide === true) {
          $root.modal('dispose')
          that.setState({ destroy: true }, () => {
            RbViewModal.holder(that.state.id, 'DISPOSE')
            $unmount(rootWrap)
          })
        }
      })
      .on('shown.bs.modal', function () {
        mc.css('margin-right', 0)
        if (that.__urlChanged === false) {
          const cw = mc.find('iframe')[0].contentWindow
          if (cw.RbViewPage && cw.RbViewPage._RbViewForm) cw.RbViewPage._RbViewForm.showAgain(that)
          this.__urlChanged = true
        }

        const mcs = $('body>.modal-backdrop.show')
        if (mcs.length > 1) {
          mcs.addClass('o')
          mcs.eq(0).removeClass('o')
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

  show(url, ext) {
    let urlChanged = true
    if (url && url === this.state.url) urlChanged = false
    ext = ext || {}
    url = url || this.state.url

    this.__urlChanged = urlChanged
    this.setState({ ...ext, url: url, inLoad: urlChanged, isHide: urlChanged }, () => {
      $(this._rbview).modal({ show: true, backdrop: true, keyboard: false })
      setTimeout(() => {
        this.setState({ showAfterUrl: this.state.url })
      }, 210) // 0.2s in rb-page.css '.rbview.show .modal-content'
    })
  }

  hide() {
    $(this._rbview).modal('hide')
  }

  // -- Usage

  static mode = 1

  /**
   * @param {*} props
   * @param {Boolean} subView
   */
  static create(props, subView) {
    this.__HOLDERs = this.__HOLDERs || {}
    this.__HOLDERsStack = this.__HOLDERsStack || []
    const that = this
    const viewUrl = `${rb.baseUrl}/app/${props.entity}/view/${props.id}`

    if (subView) {
      renderRbcomp(<RbViewModal url={viewUrl} id={props.id} disposeOnHide={true} subView={true} />, null, function () {
        that.__HOLDERs[props.id] = this
        that.__HOLDERsStack.push(this)
      })
    } else {
      if (this.__HOLDER) {
        this.__HOLDER.show(viewUrl)
        this.__HOLDERs[props.id] = this.__HOLDER
      } else {
        renderRbcomp(<RbViewModal url={viewUrl} id={props.id} />, null, function () {
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

// 复写
window.chart_remove = function (box) {
  box.parent().animate({ opacity: 0 }, function () {
    box.parent().remove()
    ChartsWidget.saveWidget()
  })
}

// 列表图表部件
const ChartsWidget = {
  init: function () {
    // eslint-disable-next-line no-undef
    ECHART_BASE.grid = { left: 40, right: 20, top: 30, bottom: 20 }

    $('.J_load-charts').on('click', () => {
      this._chartLoaded !== true && this.loadWidget()
    })
    $('.J_add-chart').on('click', () => this.showChartSelect())

    $('.charts-wrap')
      .sortable({
        handle: '.chart-title',
        axis: 'y',
        update: () => ChartsWidget.saveWidget(),
      })
      .disableSelection()
  },

  showChartSelect: function () {
    if (this.__chartSelect) {
      this.__chartSelect.show()
      this.__chartSelect.setState({ appended: ChartsWidget.__currentCharts() })
      return
    }

    // eslint-disable-next-line react/jsx-no-undef
    renderRbcomp(<ChartSelect select={(c) => this.renderChart(c, true)} entity={wpc.entity[0]} />, null, function () {
      ChartsWidget.__chartSelect = this
      this.setState({ appended: ChartsWidget.__currentCharts() })
    })
  },

  renderChart: function (chart, append) {
    const $w = $(`<div id="chart-${chart.chart}"></div>`).appendTo('.charts-wrap')
    // eslint-disable-next-line no-undef
    renderRbcomp(detectChart({ ...chart, editable: true }, chart.chart), $w, function () {
      if (append) ChartsWidget.saveWidget()
    })
  },

  loadWidget: function () {
    $.get(`/app/${wpc.entity[0]}/widget-charts`, (res) => {
      this._chartLoaded = true
      this.__config = res.data || {}
      res.data && $(res.data.config).each((idx, chart) => this.renderChart(chart))
    })
  },

  saveWidget: function () {
    const charts = this.__currentCharts(true)
    $.post(`/app/${wpc.entity[0]}/widget-charts?id=${this.__config.id || ''}`, JSON.stringify(charts), (res) => {
      ChartsWidget.__config.id = res.data
      $('.page-aside .tab-content').perfectScrollbar('update')
    })
  },

  __currentCharts: function (o) {
    const charts = []
    $('.charts-wrap>div').each(function () {
      const id = $(this).attr('id').substr(6)
      if (o) charts.push({ chart: id })
      else charts.push(id)
    })
    return charts
  },
}

// 分类
const CategoryWidget = {
  init() {
    $('.J_load-category').on('click', () => {
      this._loaded !== true && this.loadCategory()
    })

    this._$wrap = $('<div></div>').appendTo('#asideCategory')
    $(`<div class="dropdown-item active" data-id="$ALL$">${$L('全部数据')}</div>`).appendTo(this._$wrap)
  },

  loadCategory() {
    this._loaded = true
    $.get(`/app/${wpc.entity[0]}/widget-category-data`, (res) => {
      res.data &&
        res.data.forEach((item) => {
          $(`<div class="dropdown-item" data-id="${item.id}">${item.label}</div>`).appendTo(this._$wrap)
        })

      const $items = this._$wrap.find('.dropdown-item').on('click', function () {
        $items.removeClass('active')
        $(this).addClass('active')

        // Clean via
        $('.J_via-filter').remove()

        const v = $(this).data('id')
        if (v === '$ALL$') wpc.protocolFilter = null
        else wpc.protocolFilter = `category:${wpc.entity[0]}:${v}`

        RbListPage.reload()
      })
    })
  },
}

$(document).ready(() => {
  window.RbListCommon && window.RbListCommon.init(wpc)

  const viewHash = (location.hash || '').split('/')
  if ((wpc.type === 'RecordList' || wpc.type === 'DetailList') && viewHash.length === 4 && viewHash[1] === 'View' && viewHash[3].length === 20) {
    setTimeout(() => RbViewModal.create({ entity: viewHash[2], id: viewHash[3] }), 500)
  }

  // ASIDE
  if ($('#asideFilters, #asideWidgets, #asideCategory').length > 0) {
    $('.side-toggle').on('click', () => {
      const $el = $('.rb-aside').toggleClass('rb-aside-collapsed')
      $.cookie('rb.asideCollapsed', $el.hasClass('rb-aside-collapsed'), { expires: 180 })
    })

    const $content = $('.page-aside .tab-content')
    $addResizeHandler(() => {
      $content.height($(window).height() - 147)
      $content.perfectScrollbar('update')
    })()

    if ($('#asideWidgets').length > 0) ChartsWidget.init()
    if ($('#asideCategory').length > 0) CategoryWidget.init()
  }

  const $wtab = $('.page-aside.widgets .nav a:eq(0)')
  if ($wtab.length > 0) {
    $('.page-aside.widgets .ph-item.rb').remove()
    $wtab.trigger('click')
  }
})
