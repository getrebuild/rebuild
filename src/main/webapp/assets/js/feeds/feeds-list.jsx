/* eslint-disable no-undef */
/* eslint-disable react/jsx-no-undef */
/* eslint-disable react/prop-types */
/* eslint-disable no-unused-vars */

const FeedsSortTypes = { newer: '最近发布', older: '最早发布', modified: '最近修改' }

// ~ 动态列表
class FeedsList extends React.Component {

  constructor(props) {
    super(props)
    this.state = { ...props, tabType: 0, pageNo: 1 }

    this.state.sort = $storage.get('Feeds-sort')
    this._lastFilter = { entity: 'Feeds', items: [] }
  }

  render() {
    return (<div>
      <div className="search-bar">
        <ul className="nav nav-tabs">
          <li className="nav-item"><a onClick={() => this._switchTab(0)} className={`nav-link ${this.state.tabType === 0 && 'active'}`}>全部</a></li>
          <li className="nav-item"><a onClick={() => this._switchTab(1)} className={`nav-link ${this.state.tabType === 1 && 'active'}`}>@我的</a></li>
          <li className="nav-item"><a onClick={() => this._switchTab(10)} className={`nav-link ${this.state.tabType === 10 && 'active'}`}>我发布的</a></li>
          <li className="nav-item"><a onClick={() => this._switchTab(2)} className={`nav-link ${this.state.tabType === 2 && 'active'}`}>我评论的</a></li>
          <li className="nav-item"><a onClick={() => this._switchTab(3)} className={`nav-link ${this.state.tabType === 3 && 'active'}`}>我点赞的</a></li>
          <li className="nav-item"><a onClick={() => this._switchTab(11)} className={`nav-link ${this.state.tabType === 11 && 'active'}`}>私密</a></li>
          <span className="float-right">
            <div className="btn-group">
              <button type="button" className="btn btn-link pr-0 text-right" data-toggle="dropdown">{FeedsSortTypes[this.state.sort] || '默认排序'} <i className="icon zmdi zmdi-chevron-down up-1"></i></button>
              <div className="dropdown-menu dropdown-menu-right">
                <a className="dropdown-item" data-sort="newer" onClick={this._sortFeeds}>最近发布</a>
                <a className="dropdown-item" data-sort="modified" onClick={this._sortFeeds}>最近修改</a>
                <a className="dropdown-item" data-sort="older" onClick={this._sortFeeds}>最早发布</a>
              </div>
            </div>
          </span>
        </ul>
      </div>
      <div className="feeds-list">
        {(this.state.data && this.state.data.length === 0) && <div className="list-nodata pt-8 pb-8">
          <i className="zmdi zmdi-chart-donut"></i>
          <p>暂无相关动态</p>
        </div>
        }
        {(this.state.data || []).map((item) => {
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
                  <p className="text-muted fs-12 m-0"><span title={item.createdOn}>{item.createdOnFN}</span> - {item.scope}</p>
                </div>
                <div className="rich" dangerouslySetInnerHTML={{ __html: converEmoji(item.content) }} />
              </div>
            </div>
            <div className="actions">
              <ul className="list-unstyled m-0">
                {item.self && <li className="list-inline-item mr-3">
                  <a href="#delete" onClick={() => this._handleDelete(item.id)} className="hover-show fixed-icon">
                    <i className="zmdi zmdi-delete"></i>删除
                  </a>
                </li>
                }
                <li className="list-inline-item mr-3">
                  <a href="#thumbup" onClick={() => this._handleLike(item.id)} className={`fixed-icon ${item.hasLike && 'text-danger'}`}>
                    <i className="zmdi zmdi-thumb-up"></i>赞 {item.numLike > 0 && <span>({item.numLike})</span>}
                  </a>
                </li>
                <li className="list-inline-item">
                  <a href="#comments" onClick={() => this._toggleComment(item.id)} className={`fixed-icon ${item.shownComments && 'text-primary'}`}>
                    <i className="zmdi zmdi-comment-outline"></i>评论 {item.numComments > 0 && <span>({item.numComments})</span>}
                  </a>
                </li>
              </ul>
            </div>
            <span className={`${!item.shownComments && 'hide'}`}>{item.shownCommentsReal && <FeedsComments feeds={item.id} />}</span>
          </div>
        })}
      </div>
      <Pagination ref={(c) => this._pagination = c} call={this.gotoPage} pageSize={40} />
    </div >)
  }

  componentDidMount = () => this.fetchFeeds()
  /**
   * 加载数据
   * @param {*} filter AdvFilter
   */
  fetchFeeds(filter) {
    this._lastFilter = filter = filter || this._lastFilter
    $.post(`${rb.baseUrl}/feeds/feeds-list?pageNo=${this.state.pageNo}&sort=${this.state.sort}&type=${this.state.tabType}`, JSON.stringify(filter), (res) => {
      let _data = res.data || { data: [], total: 0 }
      this.state.pageNo === 1 && this._pagination.setState({ rowsTotal: _data.total, pageNo: 1 })
      this.setState({ data: _data.data })
    })
  }

  _switchTab(t) {
    this.setState({ tabType: t, pageNo: 1 }, () => this.fetchFeeds())
  }
  _sortFeeds = (e) => {
    let s = e.target.dataset.sort
    $storage.set('Feeds-sort', s)
    this.setState({ sort: s, pageNo: 1 }, () => this.fetchFeeds())
  }

  _toggleComment(feeds) {
    event.preventDefault()
    let _data = this.state.data
    _data.forEach((item) => {
      if (feeds === item.id) {
        item.shownComments = !item.shownComments
        item.shownCommentsReal = true
      }
    })
    this.setState({ data: _data })
  }

  _handleLike(id) {
    event.preventDefault()
    $.post(`${rb.baseUrl}/feeds/post/like?id=${id}`, (res) => {
      let _data = this.state.data
      _data.forEach((item) => {
        if (id === item.id) item.numLike += (res.data ? 1 : -1)
      })
      this.setState({ data: _data })
    })
  }

  _handleEdit(id) {
    // NOOP
  }

  _handleDelete(id) {
    event.preventDefault()
    let that = this
    RbAlert.create('确认删除该动态？', {
      type: 'danger',
      confirmText: '删除',
      confirm: function () {
        this.disabled(true)
        $.post(`${rb.baseUrl}/feeds/post/delete?id=${id}`, () => {
          this.hide()
          that.fetchFeeds()
        })
      }
    })
  }

  gotoPage = (pageNo) => {
    this.setState({ pageNo: pageNo }, () => this.fetchFeeds())
  }
}

// ~ 评论
class FeedsComments extends React.Component {

  constructor(props) {
    super(props)
    this.state = { ...props, pageNo: 1 }
  }

  render() {
    return (<div className="comments">
      <div className="comment-reply">
        <div onClick={() => this._commentState(true)} className={`reply-mask ${this.state.openComment && 'hide'}`}>添加评论</div>
        <span className={`${!this.state.openComment && 'hide'}`}>
          <FeedsRichInput placeholder="添加评论" ref={(c) => this._input = c} />
          <div className="mt-2 text-right">
            <button onClick={() => this._commentState(false)} className="btn btn-sm btn-link">取消</button>
            <button className="btn btn-sm btn-primary" ref={(c) => this._btn = c} onClick={() => this._post()}>评论</button>
          </div>
        </span>
      </div>
      <div className="feeds-list comment-list">
        {(this.state.data || []).map((item) => {
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
                </div>
                <div className="rich" dangerouslySetInnerHTML={{ __html: converEmoji(item.content) }} />
                <div className="actions">
                  <div className="float-left text-muted fs-12 time">
                    <span title={item.createdOn}>{item.createdOnFN}</span>
                  </div>
                  <ul className="list-unstyled m-0">
                    {item.self && <li className="list-inline-item mr-3">
                      <a href="#delete" onClick={() => this._handleDelete(item.id)} className="fixed-icon">
                        <i className="zmdi zmdi-delete"></i>删除
                      </a>
                    </li>
                    }
                    <li className="list-inline-item mr-3">
                      <a href="#thumbup" onClick={() => this._handleLike(item.id)} className={`fixed-icon ${item.hasLike && 'text-primary'}`}>
                        <i className="zmdi zmdi-thumb-up"></i>赞 {item.numLike > 0 && <span>({item.numLike})</span>}
                      </a>
                    </li>
                    <li className="list-inline-item">
                      <a href="#reply" onClick={() => this._toggleReply(item.id)} className={`fixed-icon ${item.shownReply && 'text-primary'}`}>
                        <i className="zmdi zmdi-mail-reply"></i>回复
                      </a>
                    </li>
                  </ul>
                </div>
                <div className={`comment-reply ${!item.shownReply && 'hide'}`}>
                  {item.shownReplyReal && <FeedsRichInput placeholder="添加回复" initValue={`回复 @${item.createdBy[1]} : `} ref={(c) => item._input = c} />}
                  <div className="mt-2 text-right">
                    <button onClick={() => this._toggleReply(item.id, false)} className="btn btn-sm btn-link">取消</button>
                    <button className="btn btn-sm btn-primary" ref={(c) => this._btn = c} onClick={() => this._post(item._input)}>回复</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        })}
      </div>
      <Pagination ref={(c) => this._pagination = c} call={this.gotoPage} pageSize={20} comment={true} />
    </div>)
  }

  componentDidMount = () => this._fetchComments()
  _fetchComments() {
    $.get(`${rb.baseUrl}/feeds/comments-list?feeds=${this.props.feeds}&pageNo=${this.state.pageNo}`, (res) => {
      let _data = res.data || {}
      this.state.pageNo === 1 && this._pagination.setState({ rowsTotal: _data.total, pageNo: 1 })
      this.setState({ data: _data.data })
    })
  }

  _post = (whichInput) => {
    if (!whichInput) whichInput = this._input
    let data = { content: whichInput.val(), feedsId: this.props.feeds }
    if (!data.content) return
    data.metadata = { entity: 'FeedsComment' }

    let btn = $(this._btn).button('loading')
    $.post(`${rb.baseUrl}/feeds/post/publish`, JSON.stringify(data), (res) => {
      btn.button('reset')
      this._input.reset()
      this._fetchComments()
    })
  }

  _commentState = (state) => {
    this.setState({ openComment: state }, () => {
      if (this.state.openComment) this._input.focus()
    })
  }

  _toggleReply = (id, state) => {
    event.preventDefault()
    let _data = this.state.data
    let itemState
    _data.forEach((item) => {
      if (id === item.id) {
        if (state !== undefined) item.shownReply = state
        else item.shownReply = !item.shownReply
        item.shownReplyReal = true
        if (item.shownReply) setTimeout(() => item._input.focus(), 200)
      }
    })
    this.setState({ data: _data })
  }

  _handleLike = (id) => {
    event.preventDefault()
    $.post(`${rb.baseUrl}/feeds/post/like?id=${id}`, (res) => {
      let _data = this.state.data
      _data.forEach((item) => {
        if (id === item.id) item.numLike += (res.data ? 1 : -1)
      })
      this.setState({ data: _data })
    })
  }

  _handleDelete = (id) => {
    event.preventDefault()
    let that = this
    RbAlert.create('确认删除该评论？', {
      type: 'danger',
      confirmText: '删除',
      confirm: function () {
        this.disabled(true)
        $.post(`${rb.baseUrl}/feeds/post/delete?id=${id}`, () => {
          this.hide()
          that._fetchComments()
        })
      }
    })
  }

  gotoPage = (pageNo) => {
    this.setState({ pageNo: pageNo }, () => this._fetchComments())
  }
}

// ~ 分页
class Pagination extends React.Component {

  constructor(props) {
    super(props)
    this.state = { ...props, pageSize: props.pageSize || 5, pageNo: props.pageNo || 1 }
  }

  render() {
    if (!this.state.rowsTotal) return null

    this.__pageTotal = Math.ceil(this.state.rowsTotal / this.state.pageSize)
    if (this.__pageTotal <= 0) this.__pageTotal = 1
    let pages = this.__pageTotal <= 1 ? [1] : $pages(this.__pageTotal, this.state.pageNo)

    return <div className="feeds-pages">
      <div className="float-left">
        <p className="text-muted">共 {this.state.rowsTotal} 条动态</p>
      </div>
      <div className="float-right">
        <ul className={`pagination ${this.props.comment && 'pagination-sm'}`}>
          {this.state.pageNo > 1
            && <li className="paginate_button page-item"><a className="page-link" onClick={this._prev}><span className="icon zmdi zmdi-chevron-left"></span></a></li>}
          {pages.map((item, idx) => {
            if (item === '.') return <li key={`pnx-${idx}`} className="paginate_button page-item disabled"><a className="page-link">...</a></li>
            else return <li key={`pn-${item}`} className={'paginate_button page-item ' + (this.state.pageNo === item && 'active')}><a className="page-link" onClick={() => this._goto(item)}>{item}</a></li>
          })}
          {this.state.pageNo !== this.__pageTotal
            && <li className="paginate_button page-item"><a className="page-link" onClick={this._next}><span className="icon zmdi zmdi-chevron-right"></span></a></li>}
        </ul>
      </div>
      <div className="clearfix"></div>
    </div>
  }

  _prev = () => {
    if (this.state.pageNo === 1) return
    this._goto(this.state.pageNo - 1)
  }
  _next = () => {
    if (this.state.pageNo === this.__pageTotal) return
    this._goto(this.state.pageNo + 1)
  }
  _goto = (pageNo) => {
    this.setState({ pageNo: pageNo }, () => {
      typeof this.props.call === 'function' && this.props.call(pageNo)
    })
  }
}