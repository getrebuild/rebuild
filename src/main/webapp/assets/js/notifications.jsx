$(document).ready(() => {
  let mList
  renderRbcomp(<MessageList type="1" />, 'message-list', function () {
    mList = this
  })

  let btns = $('.notification-type>a').click(function () {
    btns.removeClass('active')
    $(this).addClass('active')
    mList.fetchList($(this).data('type'), 1)
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
        {list.length === 0 &&
          <div className="list-nodata"><span className="zmdi zmdi-notifications"></span><p>暂无通知</p></div>}
      </div>
      {list.length > 0 &&
        <div className="notification-page">
          <ul className="pagination pagination-rounded mb-0">
            <li className="page-item"><a onClick={() => this.gotoPage(-1)} className="page-link"><i className="icon zmdi zmdi-chevron-left" /></a></li>
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

  fetchList(type, page) {
    this.setState({
      type: type || this.state.type,
      page: page || this.state.page
    }, () => {
      $.get(`${rb.baseUrl}/notification/messages?type=${this.state.type}&page=${this.state.page || 1}`, (res) => {
        this.setState({ list: res.data })
      })
    })
  }

  gotoPage(p) {
    if (p === -1 && this.state.page === 1) return
    if (p === 1 && (this.state.list || []).length < 10) return
    this.fetchList(null, this.state.page + p)
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