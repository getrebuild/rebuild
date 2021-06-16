/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 部门树

var __AsideTree
// eslint-disable-next-line no-unused-vars
var loadDeptTree = function () {
  $.get('/admin/bizuser/dept-tree', function (res) {
    if (__AsideTree) {
      ReactDOM.unmountComponentAtNode(document.getElementById('dept-tree'))
    }

    const activeItem = __AsideTree ? __AsideTree.state.activeItem || '$ALL$' : '$ALL$'
    const data = [{ id: '$ALL$', name: $L('全部部门') }, ...res.data]
    renderRbcomp(
      <AsideTree
        data={data}
        activeItem={activeItem}
        onItemClick={(item) => {
          const depts = item.id === '$ALL$' ? [] : AsideTree.findAllChildIds(item)
          const exp = { items: [], values: {} }
          exp.items.push({ op: 'in', field: 'deptId', value: '{2}' })
          exp.values['2'] = depts
          RbListPage._RbList.search(depts.length === 0 ? {} : exp)
        }}
      />,
      'dept-tree',
      function () {
        __AsideTree = this
      }
    )
  })
}

$(document).ready(() => loadDeptTree())
