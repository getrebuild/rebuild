/* eslint-disable no-undef */
// FrontJS 演示
// 建议将上方匹配路径设置为 `/` 以便观察效果

const demoEntityName = 'Account' // TODO 修改为你要测试的实体
const demoFieldName = 'AccountName' // TODO 修改为你要测试的实体字段

let _List, _Form, _View

FrontJS.ready(() => {
  _List = FrontJS.DataList
  _Form = FrontJS.Form
  _View = FrontJS.View

  demoAddButton()

  demoForList()

  demoForForm()

  // V35
  _Form.onOpen(() => {
    _Form.setTopAlert('表单顶部显示提示')
  })

  // 以下方法为同步请求，建议在 setTimeout 中调用以免堵塞主线程渲染页面
  setTimeout(() => {
    console.log(FrontJS.getPageToken())
    console.log(FrontJS.getRecord(rb.currentUser))
    console.log(FrontJS.checkPermission(rb.currentUser, 'D'))
  })
})

// 添加按钮
// 注意在 onOpen 中调用
function demoAddButton() {
  _List.onOpen(() => {
    _List.addButtonGroup({
      text: 'FrontJS!',
      items: [
        {
          text: '获取第一选中',
          onClick: () => {
            alert(_List.getSelectedId())
          },
        },
        {
          text: '获取全部选中',
          onClick: () => {
            alert(_List.getSelectedIds())
          },
        },
      ],
    })
  })

  _Form.onOpen(() => {
    _Form.addButton({
      text: 'FrontJS!',
      onClick: () => {
        alert(_Form.getCurrentId())
      },
    })
  })

  _View.onOpen(() => {
    _Form.addButton({
      text: 'FrontJS!',
      onClick: () => {
        alert(_Form.getCurrentId())
      },
    })
  })
}

// 列表操作
function demoForList() {
  // 指定字段加粗加红显示
  const fieldKey = `${demoEntityName}.${demoFieldName}`
  _List.regCellRender(fieldKey, function (v) {
    return <strong className="text-danger">{JSON.stringify(v)}</strong>
  })

  // 轻量级表单
  _List.onOpen(() => {
    _List.addButton({
      text: 'LiteForm',
      onClick: () => {
        const id = _List.getSelectedId()
        if (!id) {
          alert('请选择一条记录')
          return
        }

        FrontJS.openLiteForm(id, [{ field: demoFieldName, tip: '提示', readonly: true, nullable: true }])
      },
    })
  })
}

// 表单操作
function demoForForm() {
  // 监听指定字段值变化
  const lookFieldKey = `${demoEntityName}.${demoFieldName}`
  _Form.onFieldValueChange(function (fieldKey, fieldValue, recordId) {
    if (lookFieldKey === fieldKey) {
      RbHighbar.create(`记录: ${recordId || ''} 的新值 : ${fieldValue}`)
    }
  })
}
