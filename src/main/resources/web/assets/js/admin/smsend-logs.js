/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(() => {
  renderRbcomp(<DataList />, 'react-list', function () {
    RbListPage._RbList = this._List
  })
})

// 列表配置
const ListConfig = {
  entity: 'SmsendLog',
  fields: [
    { field: 'to', label: $L('接收人') },
    { field: 'sendTime', label: $L('发送时间'), type: 'DATETIME' },
    { field: 'sendResult', label: $L('发送结果') },
    { field: 'type', label: $L('发送类型') },
    { field: 'content', label: $L('发送内容'), width: 300 },
    { field: 'fromSource', label: $L('发送源'), type: 'ANYREFERENCE' },
  ],
  sort: 'sendTime:desc',
}

class DataList extends React.Component {
  render() {
    return <RbList ref={(c) => (this._List = c)} config={ListConfig} hideCheckbox showLineNo />
  }
}

const CellRenders_renderSimple = CellRenders.renderSimple
CellRenders.renderSimple = function (v, s, k) {
  if (k.endsWith('.type')) {
    return (
      <td key={k}>
        <div style={s}>{~~v === 1 ? '短信' : '邮件'}</div>
      </td>
    )
  }

  let comp = CellRenders_renderSimple(v, s, k)
  if (k.endsWith('.sendResult') && v && v.startsWith('ERR:')) {
    comp = React.cloneElement(comp, { className: 'text-danger' })
  }
  return comp
}
