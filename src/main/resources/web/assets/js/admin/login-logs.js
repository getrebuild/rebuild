/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(() => {
  renderRbcomp(<DataList />, 'react-list', function () {
    RbListPage._RbList = this._List
  })

  $('.J_view-online').on('click', () => renderRbcomp(<OnlineUserViewer width="681" />))
})

// 列表配置
const ListConfig = {
  entity: 'LoginLog',
  fields: [
    { field: 'user', label: $L('登录用户'), type: 'REFERENCE' },
    { field: 'loginTime', label: $L('登录时间'), type: 'DATETIME' },
    { field: 'ipAddr', label: $L('IP 地址') },
    { field: 'userAgent', label: $L('客户端'), width: 250 },
  ],
  sort: 'loginTime:desc',
}

class DataList extends React.Component {
  render() {
    return <RbList ref={(c) => (this._List = c)} config={ListConfig} />
  }
}

let _pageIps = []
const CellRenders_renderSimple = CellRenders.renderSimple

// eslint-disable-next-line react/display-name
CellRenders.renderSimple = function (v, s, k) {
  let comp = CellRenders_renderSimple(v, s, k)
  if (k.endsWith('.ipAddr')) {
    if (!_pageIps.contains(v)) _pageIps.push(v)
    comp = React.cloneElement(comp, { className: `J_ip-${v.replace(/[^0-9]/g, '')}` })
  }
  return comp
}

RbList.renderAfter = function () {
  _pageIps.forEach(function (ip) {
    $.get(`/commons/ip-location?ip=${ip}`, (res) => {
      if (res.error_code === 0 && res.data.country !== 'N') {
        let L = res.data.country === 'R' ? $L('局域网') : [res.data.region, res.data.country].join(', ')
        L = `${ip} (${L})`
        $(`.J_ip-${ip.replace(/[^0-9]/g, '')}`)
          .find('div')
          .attr('title', L)
          .text(L)
      }
    })
  })
  _pageIps = []
}

// ~ 在线用户
class OnlineUserViewer extends RbAlert {
  renderContent() {
    return (
      <table className="table table-striped table-hover">
        <thead>
          <tr>
            <th width="30%">{$L('用户')}</th>
            <th>{$L('最近活跃')}</th>
            <th width="90" />
          </tr>
        </thead>
        <tbody>
          {(this.state.users || []).map((item) => {
            return (
              <tr key={item.sid}>
                <td className="user-avatar cell-detail user-info">
                  <img src={`${rb.baseUrl}/account/user-avatar/${item.user}`} alt="Avatar" />
                  <span className="pt-1">{item.fullName}</span>
                </td>
                <td className="cell-detail">
                  <code className="text-break text-primary">{item.activeUrl || 'n/a'}</code>
                  <span className="cell-detail-description">
                    <DateShow date={item.activeTime} />
                    <span className="ml-1">{item.activeIp}</span>
                  </span>
                </td>
                <td className="actions text-right">
                  <button className="btn btn-danger btn-sm btn-outline" type="button" onClick={() => this._killSession(item.sid)}>
                    {$L('强退')}
                  </button>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    )
  }

  componentDidMount() {
    super.componentDidMount()
    this._load()
  }

  _load() {
    $.get('/admin/audit/online-users', (res) => {
      if (res.error_code === 0) this.setState({ users: res.data })
      else RbHighbar.error(res.error_msg)
    })
  }

  _killSession(user) {
    const that = this
    RbAlert.create($L('确认强制退出此用户？'), {
      confirm: function () {
        $.post(`/admin/audit/kill-session?user=${user}`, () => {
          this.hide()
          that._load()
        })
      },
    })
  }
}
