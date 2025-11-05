/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global AdvFilters */

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

  // v4.1, 4.2 高级查询
  AdvFilters.init('.adv-search', window.__PageConfig.entity[0])
})
