/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(function () {
  renderRbcomp(<DataList />, 'react-list')
  $('.J_view-online').click(() => renderRbcomp(<OnlineUserViewer />))
})

// 列表配置
const ListConfig = {
  entity: 'LoginLog',
  fields: [
    { field: 'user', label: $L('登录用户'), type: 'REFERENCE' },
    { field: 'loginTime', label: $L('登录时间'), type: 'DATETIME' },
    { field: 'ipAddr', label: $L('IP 地址') },
    { field: 'userAgent', label: $L('客户端') },
  ],
  sort: 'loginTime:desc',
}

class DataList extends React.Component {
  constructor(props) {
    super(props)
  }

  render() {
    return <RbList ref={(c) => (this._List = c)} config={ListConfig} uncheckbox={true}></RbList>
  }

  componentDidMount() {
    const $btn = $('.input-search .btn'),
      $input = $('.input-search input')
    $btn.click(() => this._List.searchQuick())
    $input.keydown((e) => (e.which === 13 ? $btn.trigger('click') : true))
  }
}

let pageIps = []
const CellRenders_renderSimple = CellRenders.renderSimple
// eslint-disable-next-line react/display-name
CellRenders.renderSimple = function (v, s, k) {
  let comp = CellRenders_renderSimple(v, s, k)
  if (k.endsWith('.ipAddr')) {
    if (!pageIps.contains(v)) pageIps.push(v)
    comp = React.cloneElement(comp, { className: `J_ip-${v.replace(/\./g, '-')}` })
  }
  return comp
}

RbList.renderAfter = function () {
  pageIps.forEach(function (ip) {
    $.get(`/commons/ip-location?ip=${ip}`, (res) => {
      if (res.error_code === 0 && res.data.country !== 'N') {
        let L = res.data.country === 'R' ? $L('局域网') : [res.data.region, res.data.country].join(', ')
        L = `${ip} (${L})`
        $(`.J_ip-${ip.replace(/\./g, '-')}`)
          .attr('title', L)
          .find('div')
          .text(L)
      }
    })
  })
}

// ~ 在线用户
class OnlineUserViewer extends RbModalHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$L('查看在线用户')} disposeOnHide={true}>
        <table className="table table-striped table-hover table-sm dialog-table">
          <thead>
            <tr>
              <th style={{ minWidth: 150 }}>{$L('用户')}</th>
              <th style={{ minWidth: 150 }}>{$L('最近活跃')}</th>
              <th width="90" />
            </tr>
          </thead>
          <tbody>
            {(this.state.users || []).map((item) => {
              return (
                <tr key={`user-${item.user}`}>
                  <td className="user-avatar cell-detail user-info">
                    <img src={`${rb.baseUrl}/account/user-avatar/${item.user}`} />
                    <span className="pt-1">{item.fullName}</span>
                  </td>
                  <td className="cell-detail">
                    <code className="text-break text-primary">{item.activeUrl || 'n/a'}</code>
                    <span className="cell-detail-description">
                      <DateShow date={item.activeTime} />
                    </span>
                  </td>
                  <td className="actions text-right">
                    <button className="btn btn-danger btn-sm btn-outline" type="button" onClick={() => this._killSession(item.user)}>
                      {$L('强退')}
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </RbModal>
    )
  }

  componentDidMount = () => this._load()
  _load() {
    $.get('/admin/audit/online-users', (res) => {
      if (res.error_code === 0) this.setState({ users: res.data })
      else RbHighbar.error(res.error_msg)
    })
  }

  _killSession(user) {
    const that = this
    RbAlert.create($L('确认强制退出该用户？'), {
      confirm: function () {
        $.post(`/admin/audit/kill-session?user=${user}`, () => {
          this.hide()
          that._load()
        })
      },
    })
  }
}
