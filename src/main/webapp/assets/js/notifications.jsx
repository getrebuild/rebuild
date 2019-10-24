$(document).ready(() => {
  let mList = <MessageList type="1" />
  if (window.__PageConfig && window.__PageConfig.type === 'Approval') mList = <ApprovalList />
  renderRbcomp(mList, 'message-list', function () {
    mList = this
  })

  let btns = $('.notification-type>a').click(function () {
    btns.removeClass('active')
    $(this).addClass('active')
    mList.fetchList(1, $(this).data('type'))
  })
})

// 消息列表
class MessageList extends React.Component {
  constructor(props) {
    super(props)
    this.state = { page: 1, ...props }
  }

  render() {
    let list = this.state.list || []
    return (<div>
      <div className="rb-notifications notification-list">
        <ul className="list-unstyled">
          {list.map((item) => {
            return this.renderItem(item)
          })}
        </ul>
        {this.state.list && list.length === 0 &&
          <div className="list-nodata"><span className="zmdi zmdi-notifications"></span><p>暂无通知</p></div>}
      </div>
      {(this.state.page > 1 || list.length >= 40) &&
        <div className="notification-page">
          <ul className="pagination pagination-rounded mb-0">
            <li className="page-item"><a onClick={() => this.gotoPage(-1)} className="page-link"><i className="icon zmdi zmdi-chevron-left" /></a></li>
            <li className="page-no"><span>{this.state.page}</span></li>
            <li className="page-item"><a onClick={() => this.gotoPage(1)} className="page-link"><i className="icon zmdi zmdi-chevron-right" /></a></li>
          </ul>
        </div>}
    </div>)
  }

  renderItem(item) {
    return <li className={`notification ${item[3] ? 'notification-unread' : ''}`} key={item[4]} onClick={() => this.makeRead(item[4])}><a>
      <div className="image"><img src={`${rb.baseUrl}/account/user-avatar/${item[0][0]}`} title={item[0][1]} alt="Avatar" /></div>
      <div className="notification-info">
        <div className="text" dangerouslySetInnerHTML={{ __html: item[1] }}></div>
        <div className="date">{item[2]}</div>
      </div>
    </a></li>
  }

  componentDidMount() {
    this.fetchList()
    $('.read-all').click(() => this.makeRead('ALL'))
  }

  fetchList(page, type) {
    this.setState({
      page: page || this.state.page,
      type: type || this.state.type
    }, () => {
      $.get(`${rb.baseUrl}/notification/messages?type=${this.state.type}&page=${this.state.page}`, (res) => {
        this.setState({ list: res.data || [] })
      })
    })
  }

  gotoPage(p) {
    if (p === -1 && this.state.page === 1) return
    if (p === 1 && (this.state.list || []).length < 40) return
    this.fetchList(this.state.page + p, null)
  }

  makeRead(id) {
    $.post(`${rb.baseUrl}/notification/make-read?id=${id || 'ALL'}`, () => {
      let list = (this.state.list || []).map((item) => {
        if (item[4] === id || id === 'ALL') item[3] = false
        return item
      })
      this.setState({ list: list })

      if (id === 'ALL') RbHighbar.success('全部通知已设为已读')
    })
  }
}

// 审批列表
class ApprovalList extends MessageList {
  constructor(props) {
    super(props)
  }

  fetchList(page) {
    this.setState({
      page: page || this.state.page
    }, () => {
      $.get(`${rb.baseUrl}/notification/approvals?page=${this.state.page || 1}`, (res) => {
        this.setState({ list: res.data || [] })
      })
    })
  }

  renderItem(item) {
    return <li className="notification approval" key={item[4]}><a>
      <div className="image"><img src={`${rb.baseUrl}/account/user-avatar/${item[0][0]}`} title={item[0][1]} alt="Avatar" /></div>
      <div className="notification-info">
        <div className="text" dangerouslySetInnerHTML={{ __html: item[1] }}></div>
        <div className="date">{item[2]}</div>
        {(item[3] && item[3][0] === 1) && <span className="badge badge-warning">{item[3][1]}</span>}
        {(item[3] && item[3][0] === 2) && <span className="badge badge-secondary">{item[3][1]}</span>}
        {(item[3] && item[3][0] === 10) && <span className="badge badge-success">{item[3][1]}</span>}
        {(item[3] && item[3][0] === 11) && <span className="badge badge-danger">{item[3][1]}</span>}
      </div>
    </a></li>
  }
}