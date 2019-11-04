/* eslint-disable react/prop-types */
/* eslint-disable no-unused-vars */

// ~ 动态列表
class FeedsList extends React.Component {

  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    return (<div>
      <div className="search-bar">
        <ul className="nav nav-tabs">
          <li className="nav-item"><a className="nav-link text-bold active">全部</a></li>
          <li className="nav-item"><a className="nav-link text-bold">@我的</a></li>
          <li className="nav-item"><a className="nav-link text-bold">我评论的</a></li>
          <li className="nav-item"><a className="nav-link text-bold">私密</a></li>
          <a className="search-btn fixed-icon"><i className="zmdi zmdi-search"></i>筛选</a>
        </ul>
      </div>
      <div className="feeds-list">
        {(this.state.list || []).length === 0 &&
          <div className="list-nodata pt-8 pb-8">
            <i className="zmdi zmdi-chart-donut"></i>
            <p>暂无动态</p>
          </div>
        }
        {(this.state.list || []).map((item) => {
          return <div key={`feeds-${item.id}`}>
            <div className="feeds">
              <div className="user">
                <a className="user-show">
                  <div className="avatar"><img alt="Avatar" src={`${rb.baseUrl}/account/user-avatar/${item.createdBy[0]}`} /></div>
                </a>
              </div>
              <div className="content">
                <div className="meta">
                  <span className="float-right badge">{item.type}</span>
                  <a>{item.createdBy[1]}</a>
                  <p className="text-muted fs-12 m-0">{item.createdOn} - {item.scope}</p>
                </div>
                <div className="rich">{item.content}</div>
              </div>
            </div>
            <div className="actions">
              <ul className="list-unstyled m-0">
                <li className="list-inline-item mr-1">
                  <a href="#mores" className="fixed-icon" title="更多"><i className="zmdi zmdi-more"></i>&nbsp;</a>
                </li>
                <li className="list-inline-item mr-3">
                  <a href="#thumbup" onClick={() => this._handleLike(item.id)} className={`fixed-icon ${item.hasLike && 'text-primary'}`}>
                    <i className="zmdi zmdi-thumb-up"></i>赞 {item.numLike > 0 && <span>({item.numLike})</span>}
                  </a>
                </li>
                <li className="list-inline-item">
                  <a href="#comment" onClick={() => this._toggleComment(item.id)} className={`fixed-icon ${item.showComments && 'text-primary'}`}>
                    <i className="zmdi zmdi-comment-outline"></i>评论 {item.numComments > 0 && <span>({item.numComments})</span>}
                  </a>
                </li>
              </ul>
            </div>
            {item.showComments && <FeedsComments feeds={item.id} />}
          </div>
        })}
      </div>
      <div className="mt-2">
        <div className="float-left">
          <p className="m-0 text-muted mt-1">共 0 条数据</p>
        </div>
        <div className="float-right">
          <ul className="pagination mb-0">
            <li className="paginate_button page-item active"><a className="page-link">1</a></li>
            <li className="paginate_button page-item"><a className="page-link">2</a></li>
            <li className="paginate_button page-item"><a className="page-link">3</a></li>
          </ul>
        </div>
      </div>
    </div>)
  }

  componentDidMount = () => this._fetchFeeds()
  _fetchFeeds(filter) {
    filter = filter || this._lastFilter
    if (!filter) filter = { entity: 'Feeds', items: [] }
    this._lastFilter = filter

    $.post(`${rb.baseUrl}/feeds/data-list?page=${this.state.page || 1}`, JSON.stringify(filter), (res) => {
      this.setState({ list: res.data })
    })
  }

  _handleLike(feeds) {
    event.preventDefault()
    $.post(`${rb.baseUrl}/feeds/post/like?feeds=${feeds}`, (res) => {
      let list = this.state.list
      list.forEach((item) => {
        if (feeds === item.id) item.numLike += (res.data ? 1 : -1)
      })
      this.setState({ list: list })
    })
  }

  _toggleComment(feeds) {
    event.preventDefault()
    let list = this.state.list
    list.forEach((item) => {
      if (feeds === item.id) item.showComments = !item.showComments
    })
    this.setState({ list: list })
  }
}

// ~ 评论
class FeedsComments extends React.Component {

  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    return (<div className="comments">
      <div>
        <textarea className={`form-control form-control-sm ${this.state.badContent ? 'is-invalid' : ''}`} name="content" onInput={this._changeValue} placeholder="输入评论"></textarea>
        <div className="mt-2 text-right">
          <button className="btn btn-primary" ref={(c) => this._btn = c} onClick={this._post}>评论</button>
        </div>
      </div>
      <div className="comment-list">
        {(this.state.comments || []).map((item) => {
          return <div key={`comment-${item[0]}`}>{item}</div>
        })}
      </div>
    </div>)
  }

  componentDidMount = () => this._fetchComments()
  _fetchComments() {
  }

  _changeValue = (e) => {
    let target = e.target
    let s = {}
    s[target.name] = target.value
    this.setState(s)
  }

  _post = () => {
    let data = { content: this.state.content, feedsId: this.props.feeds }
    if (!data.content) { this.setState({ badContent: true }); return }
    else this.setState({ badContent: false })
    data.metadata = { entity: 'FeedsComment' }

    let btn = $(this._btn).button('loading')
    $.post(`${rb.baseUrl}/feeds/post/publish`, JSON.stringify(data), (res) => {
      btn.button('reset')
      this._fetchComments()
    })
  }
}