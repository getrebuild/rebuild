/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const _ENTITIES = {
  'Feeds': $L('e.Feeds'),
  'ProjectTask': $L('e.ProjectTask'),
}

$(document).ready(() => {
  $.get('/commons/metadata/entities?detail=true', (res) => {
    $(res.data).each(function () {
      _ENTITIES[this.name] = this.label
    })

    for (let name in _ENTITIES) {
      $(`<option value="${name}">${_ENTITIES[name]}</option>`).appendTo('#belongEntity')
    }

    renderRbcomp(<DataList />, 'react-list')
  })
})

// 列表配置
const ListConfig = {
  entity: 'RevisionHistory',
  fields: [
    { field: 'belongEntity', label: $L('BelongEntity'), unsort: true },
    { field: 'revisionType', label: $L('RevisionType') },
    { field: 'revisionOn', label: $L('RevisionOn'), type: 'DATETIME' },
    { field: 'revisionBy.fullName', label: $L('RevisionBy') },
    { field: 'channelWith', label: $L('RevisionChannel'), unsort: true },
    { field: 'recordId', label: $L('RecordId'), unsort: true },
  ],
  sort: 'revisionOn:desc',
}

// 操作类型
const RevTypes = {
  1: $L('Create'),
  2: $L('Delete'),
  4: $L('Update'),
  16: $L('Assign'),
  32: $L('Share'),
  64: $L('UnShare'),
}

class DataList extends React.Component {
  state = { ...this.props }

  render() {
    return <RbList ref={(c) => (this._List = c)} config={ListConfig}></RbList>
  }

  componentDidMount() {
    const select2 = $('#belongEntity')
      .select2({
        placeholder: $L('SelectSome,Entity'),
        width: 220,
        allowClear: false,
      })
      .val('$ALL$')
      .trigger('change')
    select2.on('change', () => this.queryList())

    const $btn = $('.input-search .btn'),
      $input = $('.input-search input')
    $btn.click(() => this.queryList())
    $input.keydown((e) => (e.which === 13 ? $btn.trigger('click') : true))

    this._belongEntity = select2
    this._recordName = $input

    $('.J_details').click(() => this.showDetails())
  }

  queryList() {
    let e = this._belongEntity.val(),
      n = this._recordName.val()
    if (e === '$ALL$') e = null

    const qs = []
    if (e) qs.push({ field: 'belongEntity', op: 'EQ', value: e })
    if (n && $regex.isId(n)) qs.push({ field: 'recordId', op: 'EQ', value: n })
    const q = {
      entity: 'RevisionHistory',
      equation: 'AND',
      items: qs,
    }
    this._List.search(JSON.stringify(q))
  }

  showDetails() {
    const ids = this._List.getSelectedIds()
    if (!ids || ids.length === 0) return
    renderRbcomp(<DlgDetails id={ids[0]} width="681" />)
  }
}

// eslint-disable-next-line react/display-name
CellRenders.renderSimple = function (v, s, k) {
  if (k.endsWith('.channelWith')) {
    v = v ? (
      <React.Fragment>
        {$L('CasOperation')}
        <span className="badge text-id ml-1" title={$L('CasMainId')}>
          {v.id}
        </span>
      </React.Fragment>
    ) : (
      $L('DirectOperation')
    )
  } else if (k.endsWith('.recordId')) {
    v = <span className="badge text-id">{v.id}</span>
  } else if (k.endsWith('.belongEntity')) {
    v = _ENTITIES[v] || `[${v.toUpperCase()}]`
  } else if (k.endsWith('.revisionType')) {
    v = RevTypes[v] || 'N'
  }

  return (
    <td key={k}>
      <div style={s}>{v || ''}</div>
    </td>
  )
}

// ~~ 变更详情
class DlgDetails extends RbAlert {
  constructor(props) {
    super(props)
  }

  renderContent() {
    const _data = (this.state.data || []).filter(item => item.after !== item.before)
    if (_data.length === 0) return <div className="m-3 text-center text-muted">{$L('NoHistoryDetails')}</div>

    return (
      <table className="table table-fixed">
        <thead>
          <tr>
            <th width="22%">{$L('Field')}</th>
            <th>{$L('UpdateBefore')}</th>
            <th>{$L('UpdateAfter')}</th>
          </tr>
        </thead>
        <tbody>
          {_data.map((item) => {
            return (
              <tr key={item.field}>
                <td>{item.field}</td>
                <td>
                  <div>{item.before || <span className="text-muted">{$L('Empty')}</span>}</div>
                </td>
                <td>
                  <div>{item.after || <span className="text-muted">{$L('Empty')}</span>}</div>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    )
  }

  componentDidMount() {
    $.get(`/admin/audit/revision-history/details?id=${this.props.id}`, (res) => {
      if (res.data.length === 0) {
        RbHighbar.create($L('SelectNoHistoryDetails'))
        this.hide()
      } else {
        super.componentDidMount()
        this.setState({ data: res.data })
      }
    })
  }
}
