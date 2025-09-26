/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

const _RbList_renderAfter = RbList.renderAfter
RbList.renderAfter = function () {
  typeof _RbList_renderAfter === 'function' && _RbList_renderAfter()
  parent && parent.referenceSearch__dlg && parent.referenceSearch__dlg.resize()
}

// v4.1 附加过滤条件变量
const _RbList_queryBefore = RbList.queryBefore
RbList.queryBefore = function (query) {
  if (typeof _RbList_queryBefore === 'function') {
    query = _RbList_queryBefore(query)
  }

  const formComp412 = parent.referenceSearch__form
  const viewComp = parent.RbViewPage ? parent.RbViewPage._RbViewForm : null
  if (wpc.protocolFilter && parent.referenceSearch__dlg && (formComp412 || viewComp)) {
    let varRecord = formComp412 ? formComp412.getFormData() : viewComp ? viewComp.__ViewData : null
    if (varRecord) {
      // FIXME 太长的值过滤
      for (let k in varRecord) {
        if (varRecord[k] && (varRecord[k] + '').length > 100) {
          console.log('Ignore large value of field :', k, varRecord[k])
          delete varRecord[k]
        }
      }
      query.protocolFilter__varRecord = { 'metadata.entity': (formComp412 || viewComp).props.entity, ...varRecord }
    }
  }
  return query
}

$(document).ready(() => {
  // 新建后自动刷新
  window.addEventListener('storage', (e) => {
    if (e.key === 'referenceSearch__reload') {
      localStorage.removeItem('referenceSearch__reload')
      RbListPage.reload()
    }
  })

  $('.J_select').on('click', () => {
    const ids = RbListPage._RbList.getSelectedIds()
    if (ids.length > 0 && parent && parent.referenceSearch__call) parent.referenceSearch__call(ids)
  })

  // v4.1 高级查询
  const $customAdvWrap = $('#dropdown-menu-advfilter')
  $(document.body).on('click', (e) => {
    if (!e.target) return
    const $target = $(e.target)
    if (
      $target.hasClass('J_filterbtn') ||
      $target.parent().hasClass('J_filterbtn') ||
      $target.hasClass('dropdown-menu-advfilter') ||
      $target.parents('.dropdown-menu-advfilter')[0] ||
      $target.hasClass('modal') ||
      $target.parents('.modal')[0] ||
      $target.parents('.select2-container')[0] ||
      $target.hasClass('select2-selection__choice__remove')
    ) {
      return
    }
    if (!$customAdvWrap.hasClass('hide')) $customAdvWrap.addClass('hide')
  })

  let _AdvFilter
  $('.J_filterbtn').on('click', () => {
    if (_AdvFilter) {
      $customAdvWrap.toggleClass('hide')
    } else {
      const props = {
        entity: wpc.entity[0],
        noSave: true,
        onConfirm: function () {
          setTimeout(() => $customAdvWrap.addClass('hide'), 200)
        },
      }
      // eslint-disable-next-line react/jsx-no-undef
      renderRbcomp(<ListAdvFilter {...props} />, $customAdvWrap, function () {
        _AdvFilter = this
      })
    }
  })

  // v4.2 已有常用查询
  $.get(`/app/${wpc.entity[0]}/advfilter/list`, function (res) {
    const $menu = $('.adv-search .dropdown-menu')
    res.data.forEach((item) => {
      const $item = $(`<div class="dropdown-item J_custom" data-id="${item.id}"><a class="text-truncate"></a></div>`).appendTo($menu)
      $item.find('>a').text(item.name)
    })

    $menu.find('.dropdown-item').on('click', function () {
      const $item = $(this)
      $('.adv-search .J_name').text($item.text())
      RbListPage._RbList.setAdvFilter($item.data('id'))
    })
  })
})
