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
    return <RbList ref={(c) => this._List = c} config={ListConfig}></RbList>
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

    $('.J_details').click(() => this.showDetails())
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

  showDetails() {
    let ids = this._List.getSelectedIds()
    if (!ids || ids.length === 0) return
    renderRbcomp(<DlgDetails id={ids[0]} width="681" />)
  }
}

// eslint-disable-next-line react/display-name
CellRenders.renderSimple = function (v, s, k) {
  if (k.endsWith('.channelWith')) v = v ? (<React.Fragment>关联操作 <span className="badge text-id ml-1" title="关联主记录ID">{v.toUpperCase()}</span></React.Fragment>) : '直接操作'
  else if (k.endsWith('.recordId')) v = <span className="badge text-id">{v.toUpperCase()}</span>
  else if (k.endsWith('.belongEntity')) v = _entities[v] || `[${v.toUpperCase()}]`
  else if (k.endsWith('.revisionType')) v = RevTypes[v] || '未知'
  return <td key={k}><div style={s}>{v || ''}</div></td>
}

class DlgDetails extends RbAlert {
  constructor(props) {
    super(props)
  }
  renderContent() {
    if (!this.state.data || this.state.data.length === 0) return <div>无变更详情</div>
    return <table className="table table-fixed">
      <thead>
        <tr>
          <th width="22%">字段</th>
          <th>变更前</th>
          <th>变更后</th>
        </tr>
      </thead>
      <tbody>
        {this.state.data.map((item) => {
          return <tr key={`fk-${item.field}`}>
            <td>{item.field}</td>
            <td><div>{item.before || <span className="text-muted">空值</span>}</div></td>
            <td><div>{item.after || <span className="text-muted">空值</span>}</div></td>
          </tr>
        })}
      </tbody>
    </table>
  }
  componentDidMount() {
    $.get(`${rb.baseUrl}/admin/audit/revision-history/details?id=${this.props.id}`, (res) => {
      if (res.data.length === 0) {
        RbHighbar.create('选中纪录无变更详情')
        this.hide()
      } else {
        super.componentDidMount()
        this.setState({ data: res.data })
      }
    })
  }
}