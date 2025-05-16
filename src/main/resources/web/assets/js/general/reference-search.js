/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

RbList.renderAfter = function () {
  parent && parent.referenceSearch__dlg && parent.referenceSearch__dlg.resize()
}

$(document).ready(() => {
  $('.J_select').on('click', () => {
    const ids = RbListPage._RbList.getSelectedIds()
    if (ids.length > 0 && parent && parent.referenceSearch__call) parent.referenceSearch__call(ids)
  })

  // 新建后自动刷新
  window.addEventListener('storage', (e) => {
    if (e.key === 'referenceSearch__reload') {
      localStorage.removeItem('referenceSearch__reload')
      RbListPage.reload()
    }
  })

  // v4.1 高级查询
  const $wrap = $('#dropdown-menu-advfilter')
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
    $wrap.addClass('hide')
  })

  let _AdvFilter
  $('.J_filterbtn').on('click', () => {
    if (_AdvFilter) {
      $wrap.toggleClass('hide')
    } else {
      const props = {
        entity: window.__PageConfig.entity[0],
        noSave: true,
        onConfirm: function () {
          setTimeout(() => $wrap.addClass('hide'), 200)
        },
      }
      // eslint-disable-next-line react/jsx-no-undef
      renderRbcomp(<ListAdvFilter {...props} />, $wrap, function () {
        _AdvFilter = this
      })
    }
  })
})
