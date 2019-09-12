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
  entity: 'RecycleBin',
  fields: [
    { field: 'belongEntity', label: '所属实体', unsort: true },
    { field: 'recordName', label: '记录名称', width: 300 },
    { field: 'deletedOn', label: '删除时间', type: 'DATETIME' },
    { field: 'deletedBy.fullName', label: '删除人' },
    { field: 'channelWith', label: '删除方式', unsort: true },
    { field: 'recordId', label: '记录ID', unsort: true }
  ]
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

    $('.J_restore').click(() => this.restore())
  }

  queryList() {
    let e = this._belongEntity.val(),
      n = this._recordName.val()
    if (e === '$ALL$') e = null

    let qs = []
    if (e) qs.push({ field: 'belongEntity', op: 'EQ', value: e })
    if (n) {
      if ($regex.isId(n)) qs.push({ field: 'recordId', op: 'EQ', value: n })
      else qs.push({ field: 'recordName', op: 'LK', value: n })
    }
    let q = {
      entity: 'RecycleBin',
      items: qs
    }
    this._List.search(JSON.stringify(q), true)
  }

  restore() {
    let ids = this._List.getSelectedIds()
    if (!ids || ids.length === 0) return

    const that = this
    let cont = `<div class="text-bold mb-2">是否恢复选中的 ${ids.length} 条记录？</div>`
    cont += '<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-2"><input class="custom-control-input" type="checkbox"><span class="custom-control-label"> 同时恢复级联删除的记录</span></label>'
    RbAlert.create(cont, {
      html: true,
      confirm: function () {
        this.disabled(true)
        let c = $(this._dlg).find('input').prop('checked')
        $.post(`${rb.baseUrl}/admin/audit/recycle-bin/restore?cascade=${c}&ids=${ids.join(',')}`, (res) => {
          this.hide()
          this.disabled()
          if (res.error_code === 0 && res.data.restored > 0) {
            RbHighbar.success(`成功恢复 ${res.data.restored} 条记录`)
            that.queryList()
          } else RbHighbar.error(res.error_code > 0 ? res.error_msg : '无法恢复选中记录')
        })
      }
    })
  }
}

// eslint-disable-next-line react/display-name
CellRenders.renderSimple = function (v, s, k) {
  if (k.endsWith('.channelWith')) v = v ? (<React.Fragment>关联删除 <span className="badge text-id ml-1" title="关联主记录ID">{v.toUpperCase()}</span></React.Fragment>) : '直接删除'
  else if (k.endsWith('.recordId')) v = <span className="badge text-id">{v.toUpperCase()}</span>
  else if (k.endsWith('.belongEntity')) v = _entities[v] || `[${v.toUpperCase()}]`
  return <td key={k}><div style={s}>{v || ''}</div></td>
}
