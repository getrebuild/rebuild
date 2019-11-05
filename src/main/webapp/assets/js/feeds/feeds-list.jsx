/* eslint-disable react/jsx-no-undef */
/* eslint-disable react/prop-types */
/* eslint-disable no-unused-vars */

const FeedsSortTypes = { newer: '最近发布', older: '较早发布', modified: '最近修改' }

// ~ 动态列表
class FeedsList extends React.Component {

  constructor(props) {
    super(props)
    this.state = { ...props, page: 1 }

    this.state.sort = $storage.get('Feeds-sort')
    this._lastFilter = { entity: 'Feeds', items: [] }
  }

  render() {
    return (<div>
      <div className="search-bar">
        <ul className="nav nav-tabs">
          <li className="nav-item"><a className="nav-link text-bold active">全部</a></li>
          <li className="nav-item"><a className="nav-link text-bold">@我的</a></li>
          <li className="nav-item"><a className="nav-link text-bold">我发布的</a></li>
          <li className="nav-item"><a className="nav-link text-bold">我评论的</a></li>
          <li className="nav-item"><a className="nav-link text-bold">我点赞的</a></li>
          <span className="float-right">
            <div className="btn-group">
              <button type="button" className="btn btn-link pr-0 text-right" data-toggle="dropdown">{FeedsSortTypes[this.state.sort] || '默认排序'} <i className="icon zmdi zmdi-chevron-down up-1"></i></button>
              <div className="dropdown-menu dropdown-menu-right">
                <a className="dropdown-item" data-sort="newer" onClick={this._sortFeeds}>最近发布</a>
                <a className="dropdown-item" data-sort="modified" onClick={this._sortFeeds}>最近修改</a>
                <a className="dropdown-item" data-sort="older" onClick={this._sortFeeds}>较早发布</a>
              </div>
            </div>
          </span>
        </ul>
      </div>
      <div className="feeds-list">
        {(this.state.list && this.state.list.length === 0) && <div className="list-nodata pt-8 pb-8">
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
                  <a data-toggle="dropdown" href="#mores" className="fixed-icon" title="更多"><i className="zmdi zmdi-more"></i>&nbsp;</a>
                  <div className="dropdown-menu dropdown-menu-right">
                    <a className="dropdown-item"><i className="icon zmdi zmdi-edit" /> 编辑</a>
                    <a className="dropdown-item"><i className="icon zmdi zmdi-delete" />删除</a>
                  </div>
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
            <span className={`${item.showComments ? '' : 'hide'}`}>{item.showCommentsReal && <FeedsComments feeds={item.id} />}</span>
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

  componentDidMount = () => this.fetchFeeds()
  /**
   * 加载数据
   * @param {*} filter AdvFilter
   */
  fetchFeeds(filter) {
    this._lastFilter = filter = filter || this._lastFilter
    $.post(`${rb.baseUrl}/feeds/feeds-list?pageNo=${this.state.page}&sort=${this.state.sort}`, JSON.stringify(filter), (res) => {
      this.setState({ list: res.data })
    })
  }
  _sortFeeds = (e) => {
    let s = e.target.dataset.sort
    $storage.set('Feeds-sort', s)
    this.setState({ sort: s }, () => this.fetchFeeds())
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
      if (feeds === item.id) {
        item.showComments = !item.showComments
        item.showCommentsReal = true
      }
    })
    this.setState({ list: list })
  }
}

// ~ 评论
class FeedsComments extends React.Component {

  constructor(props) {
    super(props)
    this.state = { ...props, openReply: false, page: 1 }
  }

  render() {
    return (<div className="comments">
      <div className="comment-reply">
        <div onClick={() => this._replyState(true)} className={`reply-mask ${this.state.openReply ? 'hide' : ''}`}>添加评论</div>
        <span className={`${this.state.openReply ? '' : 'hide'}`}>
          <FeedsRichInput placeholder="添加评论" ref={(c) => this._input = c} />
          <div className="mt-2 text-right">
            <button onClick={() => this._replyState(false)} className="btn btn-sm btn-link">取消</button>
            <button className="btn btn-sm btn-primary" ref={(c) => this._btn = c} onClick={this._post}>评论</button>
          </div>
        </span>
      </div>
      <div className="feeds-list comment-list">
        {(this.state.comments || []).map((item) => {
          return <div key={`comment-${item.id}`}>
            <div className="feeds">
              <div className="user">
                <a className="user-show">
                  <div className="avatar"><img alt="Avatar" src={`${rb.baseUrl}/account/user-avatar/${item.createdBy[0]}`} /></div>
                </a>
              </div>
              <div className="content">
                <div className="meta">
                  <a>{item.createdBy[1]}</a>
                  <span className="float-right">
                    <span>{item.createdOn}</span>
                    <a href="#mores" data-toggle="dropdown" className="fixed-icon"><i className="zmdi zmdi-more"></i></a>
                    <div className="dropdown-menu dropdown-menu-right">
                      <a className="dropdown-item"><i className="icon zmdi zmdi-mail-reply" /> 回复</a>
                      <a className="dropdown-item"><i className="icon zmdi zmdi-edit" /> 编辑</a>
                      <a className="dropdown-item"><i className="icon zmdi zmdi-delete" />删除</a>
                    </div>
                  </span>
                </div>
                <div className="rich">{item.content}</div>
              </div>
            </div>
          </div>
        })}
      </div>
    </div>)
  }

  componentDidMount = () => this._fetchComments()
  _fetchComments() {
    $.get(`${rb.baseUrl}/feeds/comments-list?feeds=${this.props.feeds}&pageNo=${this.state.page}`, (res) => {
      this.setState({ comments: res.data })
    })
  }

  _replyState = (state) => {
    this.setState({ openReply: state }, () => {
      if (this.state.openReply) this._input.focus()
    })
  }

  _post = () => {
    let data = { content: this._input.val(), feedsId: this.props.feeds }
    if (!data.content) return
    data.metadata = { entity: 'FeedsComment' }

    let btn = $(this._btn).button('loading')
    $.post(`${rb.baseUrl}/feeds/post/publish`, JSON.stringify(data), (res) => {
      btn.button('reset')
      this._fetchComments()
    })
  }
}