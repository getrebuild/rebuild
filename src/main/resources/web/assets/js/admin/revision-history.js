/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const _entities = {}
$(document).ready(() => {
  $.get('/commons/metadata/entities?detail=true', (res) => {
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
    { field: 'belongEntity', label: $lang('BelongEntity'), unsort: true },
    { field: 'revisionType', label: $lang('RevisionType') },
    { field: 'revisionOn', label: $lang('RevisionOn'), type: 'DATETIME' },
    { field: 'revisionBy.fullName', label: $lang('RevisionBy') },
    { field: 'channelWith', label: $lang('RevisionChannel'), unsort: true },
    { field: 'recordId', label: $lang('RecordId'), unsort: true },
  ],
}

// 操作类型
const RevTypes = {
  1: $lang('Create'),
  2: $lang('Delete'),
  4: $lang('Update'),
  16: $lang('Assign'),
  32: $lang('Share'),
  64: $lang('UnShare'),
}

class DataList extends React.Component {
  state = { ...this.props }

  render() {
    return <RbList ref={(c) => (this._List = c)} config={ListConfig}></RbList>
  }

  componentDidMount() {
    const select2 = $('#belongEntity')
      .select2({
        placeholder: '选择实体',
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
        {$lang('CasOperation')}
        <span className="badge text-id ml-1" title={$lang('CasMainId')}>
          {v}
        </span>
      </React.Fragment>
    ) : (
      $lang('DirectOperation')
    )
  } else if (k.endsWith('.recordId')) {
    v = <span className="badge text-id">{v}</span>
  } else if (k.endsWith('.belongEntity')) {
    v = _entities[v] || `[${v.toUpperCase()}]`
  } else if (k.endsWith('.revisionType')) {
    v = RevTypes[v] || 'N'
  }

  return (
    <td key={k}>
      <div style={s}>{v || ''}</div>
    </td>
  )
}

class DlgDetails extends RbAlert {
  constructor(props) {
    super(props)
  }

  renderContent() {
    if (!this.state.data || this.state.data.length === 0) return <div>{$lang('NoHistoryDetails')}</div>
    return (
      <table className="table table-fixed">
        <thead>
          <tr>
            <th width="22%">{$lang('Field')}</th>
            <th>{$lang('UpdateBefore')}</th>
            <th>{$lang('UpdateAfter')}</th>
          </tr>
        </thead>
        <tbody>
          {this.state.data.map((item) => {
            return (
              <tr key={`fk-${item.field}`}>
                <td>{item.field}</td>
                <td>
                  <div>{item.before || <span className="text-muted">{$lang('Empty')}</span>}</div>
                </td>
                <td>
                  <div>{item.after || <span className="text-muted">{$lang('Empty')}</span>}</div>
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
        RbHighbar.create($lang('SelectNoHistoryDetails'))
        this.hide()
      } else {
        super.componentDidMount()
        this.setState({ data: res.data })
      }
    })
  }
}
