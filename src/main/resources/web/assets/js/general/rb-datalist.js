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
    renderRbcomp(<RbList config={config} hideCheckbox={config.hideCheckbox} />, 'react-list', function () {
      RbListPage._RbList = this

      if (window.FrontJS) {
        window.FrontJS.DataList._trigger('open', [this])
      }
    })

    const that = this

    $('.J_view').on('click', () => {
      const ids = this._RbList.getSelectedIds()
      if (ids.length >= 1) RbViewModal.create({ id: ids[0], entity: entity[0] })
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
        />,
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

$(document).ready(() => {
  window.RbListCommon && window.RbListCommon.init(wpc)

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
      let left = 135
      if (wpc.type === 'RecordSearchList') left = 29 // v4.2

      $content.height($(window).height() - left)
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
    />,
  )
}
