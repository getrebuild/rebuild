let _entities = {}
$(document).ready(() => {
  $.get(`${rb.baseUrl}/commons//metadata/entities?slave=true`, (res) => {
    $(res.data).each(function () {
      $(`<option value="${this.name}">${this.label}</option>`).appendTo('#belongEntity')
      _entities[this.name] = this.label
    })

    renderRbcomp(<DataList />, 'react-list')
  })
})

// 列表配置
const ListConfig = {
  entity: 'RevisionHistory',
  fields: [
    { field: 'belongEntity', label: '所属实体', unsort: true },
    { field: 'revisionType', label: '变更类型' },
    { field: 'revisionOn', label: '变更时间', type: 'DATETIME' },
    { field: 'revisionBy.fullName', label: '操作人' },
    { field: 'channelWith', label: '操作方式', unsort: true },
    { field: 'recordId', label: '记录ID', unsort: true }
  ]
}

// 操作类型
const RevTypes = {
  1: '新建',
  2: '删除',
  4: '更新',
  16: '分派',
  32: '共享',
  64: '取消共享'
}

class DataList extends React.Component {
  constructor(props) {
    super(props)
  }
  render() {
    return <RbList ref={(c) => this._List = c} config={ListConfig} uncheckbox={true}></RbList>
  }

  componentDidMount() {
    let select2 = $('#belongEntity').select2({
      placeholder: '选择实体',
      width: 220,
      allowClear: false
    }).val('$ALL$').trigger('change')
    select2.on('change', () => this.queryList())

    let btn = $('.input-search .btn'),
      input = $('.input-search input')
    btn.click(() => this.queryList())
    input.keydown((event) => { if (event.which === 13) btn.trigger('click') })

    this._belongEntity = select2
    this._recordName = input
  }

  queryList() {
    let e = this._belongEntity.val(),
      n = this._recordName.val()
    if (e === '$ALL$') e = null

    let qs = []
    if (e) qs.push({ field: 'belongEntity', op: 'EQ', value: e })
    if (n && $regex.isId(n)) qs.push({ field: 'recordId', op: 'EQ', value: n })
    let q = {
      entity: 'RevisionHistory',
      items: qs
    }
    this._List.search(JSON.stringify(q), true)
  }
}

// eslint-disable-next-line react/display-name
CellRenders.renderSimple = function (v, s, k) {
  if (k.endsWith('.channelWith')) v = v ? (<React.Fragment>关联操作 <span className="badge ml-1" title="关联主记录ID">{v.toUpperCase()}</span></React.Fragment>) : '直接操作'
  else if (k.endsWith('.recordId')) v = <span className="badge">{v.toUpperCase()}</span>
  else if (k.endsWith('.belongEntity')) v = _entities[v] || `[${v.toUpperCase()}]`
  else if (k.endsWith('.revisionType')) v = RevTypes[v] || '未知'
  return <td key={k}><div style={s}>{v || ''}</div></td>
}
