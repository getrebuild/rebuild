/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

let focusItem
$(document).ready(() => {
  let mList = <MessageList lazy={true} />
  if (window.__PageConfig && window.__PageConfig.type === 'Approval') mList = <ApprovalList />
  renderRbcomp(mList, 'message-list', function () {
    mList = this
  })

  const $btns = $('.notification-type>a').click(function () {
    $btns.removeClass('active')
    $(this).addClass('active')
    mList.fetchList(1, $(this).data('type'))
  })

  const ntype = (location.hash || '#unread').split('=')
  focusItem = ntype[1]
  let activeNav = $('.notification-type a[href="' + ntype[0] + '"]')
  if (ntype.length === 0) activeNav = $('.notification-type a[href="#unread"]')
  activeNav.trigger('click')
})

// 消息列表
class MessageList extends React.Component {
  state = { ...this.props, page: 1, pageSize: 40 }

  render() {
    const msglist = this.state.list || []
    return (
      <div ref={(c) => (this._list = c)}>
        <div className="rb-notifications notification-list">
          <ul className="list-unstyled">
            {msglist.map((item) => {
              return this.renderItem(item)
            })}
          </ul>
          {this.state.list && msglist.length === 0 && (
            <div className="list-nodata">
              <span className="zmdi zmdi-notifications"></span>
              <p>{$L('NoSome,Message')}</p>
            </div>
          )}
        </div>
        {(this.state.page > 1 || msglist.length >= this.state.pageSize) && (
          <div className="notification-page">
            <ul className="pagination pagination-rounded mb-0">
              <li className={`page-item ${this.state.page < 2 ? 'disabled' : ''}`}>
                <a onClick={() => this.gotoPage(-1)} className="page-link">
                  <i className="icon zmdi zmdi-chevron-left" />
                </a>
              </li>
              <li className="page-no">
                <span>{this.state.page}</span>
              </li>
              <li className={`page-item ${msglist.length < this.state.pageSize ? 'disabled' : ''}`}>
                <a onClick={() => this.gotoPage(1)} className="page-link">
                  <i className="icon zmdi zmdi-chevron-right" />
                </a>
              </li>
            </ul>
          </div>
        )}
      </div>
    )
  }

  renderItem(item) {
    // const append = item[6] === 30
    const append = item[5] && ~~item[5].substr(0, 3) !== 29 // 过滤审批步骤ID
    let clazz = 'notification'
    if (item[3]) clazz += ' notification-unread'
    if (append) clazz += ' append'

    return (
      <li id={item[4]} className={`${clazz} ${item[4] === focusItem ? 'focus' : ''}`} key={item[4]} onClick={item[3] ? () => this.makeRead(item[4]) : null}>
        <span className="a">
          <div className="image">
            <img src={`${rb.baseUrl}/account/user-avatar/${item[0][0]}`} title={item[0][1]} alt="Avatar" />
          </div>
          <div className="notification-info">
            <div className="text" dangerouslySetInnerHTML={{ __html: item[1] }}></div>
            <div className="date">
              <DateShow date={item[2]} />
            </div>
          </div>
          {append && (
            <a title={$L('ClickViewReleated')} className="badge link" href={`${rb.baseUrl}/app/list-and-view?id=${item[5]}`}>
              {$L('OpenView')}
            </a>
          )}
        </span>
      </li>
    )
  }

  componentDidMount() {
    if (this.props.lazy !== true) this.fetchList()

    const that = this
    $('.read-all').click(() => {
      RbAlert.create($L('MakeReadAllConfirm'), {
        confirm: function () {
          this.hide()
          that.makeRead('ALL')
        },
      })
    })
  }

  fetchList(page, type) {
    this.setState(
      {
        page: page || this.state.page,
        type: type || this.state.type,
      },
      () => {
        $.get(`/notification/messages?type=${this.state.type}&pageNo=${this.state.page}`, (res) => {
          this.setState({ list: res.data || [] }, () => {
            if (focusItem && $('.notification.focus').length > 0) setTimeout(() => $gotoSection($('.notification.focus').offset().top - 66), 200)
            focusItem = null
          })
        })
      }
    )
  }

  gotoPage(p) {
    if (p === -1 && this.state.page === 1) return
    if (p === 1 && (this.state.list || []).length < this.state.pageSize) return
    this.fetchList(this.state.page + p, null)
  }

  makeRead(id) {
    if (!id) return
    $.post(`/notification/make-read?id=${id}`, () => {
      let list = (this.state.list || []).map((item) => {
        if (item[4] === id || id === 'ALL') item[3] = false
        return item
      })
      this.setState({ list: list })

      if (id === 'ALL') RbHighbar.success($L('MakeReadAllTips'))
    })
  }
}

// 审批列表
class ApprovalList extends MessageList {
  constructor(props) {
    super(props)
  }

  fetchList(page) {
    this.setState(
      {
        page: page || this.state.page,
      },
      () => {
        $.get(`/notification/approvals?pageNo=${this.state.page || 1}`, (res) => {
          this.setState({ list: res.data || [] })
        })
      }
    )
  }

  renderItem(item) {
    return (
      <li className="notification append" key={item[4]}>
        <span className="a">
          <div className="image">
            <img src={`${rb.baseUrl}/account/user-avatar/${item[0][0]}`} title={item[0][1]} alt="Avatar" />
          </div>
          <div className="notification-info">
            <div className="text" dangerouslySetInnerHTML={{ __html: item[1] }}></div>
            <div className="date">
              <DateShow date={item[2]} />
            </div>
            {item[3] && item[3][0] === 1 && <span className="badge badge-warning">{item[3][1]}</span>}
            {item[3] && item[3][0] === 2 && <span className="badge badge-secondary">{item[3][1]}</span>}
            {item[3] && item[3][0] === 10 && <span className="badge badge-success">{item[3][1]}</span>}
            {item[3] && item[3][0] === 11 && <span className="badge badge-danger">{item[3][1]}</span>}
          </div>
        </span>
      </li>
    )
  }
}
