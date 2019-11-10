$(document).ready(() => {
  let mList = <MessageList lazy={true} />
  if (window.__PageConfig && window.__PageConfig.type === 'Approval') mList = <ApprovalList />
  renderRbcomp(mList, 'message-list', function () { mList = this })

  const btns = $('.notification-type>a').click(function () {
    btns.removeClass('active')
    $(this).addClass('active')
    mList.fetchList(1, $(this).data('type'))
  })

  let ntype = location.hash || '#unread'
  ntype = $('.notification-type a[href="' + ntype + '"]')
  if (ntype.length === 0) ntype = $('.notification-type a[href="#unread"]')
  ntype.trigger('click')
})

// 消息列表
class MessageList extends React.Component {
  state = { ...this.props, page: 1 }

  render() {
    let list = this.state.list || []
    return (<div ref={(c) => this._list = c}>
      <div className="rb-notifications notification-list">
        <ul className="list-unstyled">
          {list.map((item) => { return this.renderItem(item) })}
        </ul>
        {this.state.list && list.length === 0 &&
          <div className="list-nodata"><span className="zmdi zmdi-notifications"></span><p>暂无消息</p></div>}
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
    let append = item[5] === 30
    return <li className={`notification ${item[3] ? 'notification-unread' : ''} ${append ? 'append' : ''}`} key={item[4]} onClick={item[3] ? () => this._makeRead(item[4]) : null}><a>
      <div className="image"><img src={`${rb.baseUrl}/account/user-avatar/${item[0][0]}`} title={item[0][1]} alt="Avatar" /></div>
      <div className="notification-info">
        <div className="text" dangerouslySetInnerHTML={{ __html: item[1] }}></div>
        <div className="date">{item[2]}</div>
        {this.__renderAppend(item)}
      </div>
    </a></li>
  }
  // 特殊消息类型渲染额外元素
  __renderAppend(item) {
    // eslint-disable-next-line react/jsx-no-target-blank
    if (item[5] === 30) return <a className="badge link" href={`${rb.baseUrl}/feeds/home#gs=${item[6]}`} target="_blank">查看</a>
    return null
  }

  componentDidMount() {
    // eslint-disable-next-line react/prop-types
    if (this.props.lazy !== true) this.fetchList()
    $('.read-all').click(() => this._makeRead('ALL'))
  }

  fetchList(page, type) {
    this.setState({
      page: page || this.state.page,
      type: type || this.state.type
    }, () => {
      $.get(`${rb.baseUrl}/notification/messages?type=${this.state.type}&pageNo=${this.state.page}`, (res) => {
        this.setState({ list: res.data || [] }, () => this.__setLink())
      })
    })
  }
  __setLink = () => $(this._list).find('.notification-info a').attr('target', '_blank').addClass('link')

  gotoPage(p) {
    if (p === -1 && this.state.page === 1) return
    if (p === 1 && (this.state.list || []).length < 40) return
    this.fetchList(this.state.page + p, null)
  }

  _makeRead(id) {
    if (!id) return
    $.post(`${rb.baseUrl}/notification/make-read?id=${id}`, () => {
      let list = (this.state.list || []).map((item) => {
        if (item[4] === id || id === 'ALL') item[3] = false
        return item
      })
      this.setState({ list: list })

      if (id === 'ALL') RbHighbar.success('全部消息已设为已读')
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
      $.get(`${rb.baseUrl}/notification/approvals?pageNo=${this.state.page || 1}`, (res) => {
        this.setState({ list: res.data || [] }, () => this.__setLink())
      })
    })
  }

  renderItem(item) {
    return <li className="notification append" key={item[4]}><a>
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