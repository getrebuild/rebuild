// ~ 动态列表
// eslint-disable-next-line no-unused-vars
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
          <li className="nav-item"><a className="nav-link text-bold">动态</a></li>
          <li className="nav-item"><a className="nav-link text-bold">跟进</a></li>
          <a className="search-btn fixed-icon"><i className="zmdi zmdi-search"></i>筛选</a>
        </ul>
      </div>
      <div className="feeds-list">
        {(this.state.list || []).map((item) => {
          return <div key={`feeds-${item[0]}`}>
            <div className="feeds">
              <div className="user">
                <a className="user-show">
                  <div className="avatar"><img alt="Avatar" src={`${rb.baseUrl}/account/user-avatar/${item[1]}`} /></div>
                </a>
              </div>
              <div className="content">
                <div className="meta">
                  <span className="float-right badge">{item[7]}</span>
                  <a>{item[2]}</a>
                  <p className="text-muted fs-12 m-0">{item[3]} - {item[6]}</p>
                </div>
                <div className="rich">{item[5]}</div>
              </div>
            </div>
            <div className="actions">
              <ul className="list-unstyled m-0">
                <li className="list-inline-item"><a href="#mores" className="fixed-icon" title="更多"><i className="zmdi zmdi-more"></i>&nbsp;</a></li>
                <li className="list-inline-item"><a href="#thumbup" className="fixed-icon"><i className="zmdi zmdi-thumb-up"></i>赞<span></span></a></li>
                <li className="list-inline-item"><a href="#comment" className="fixed-icon"><i className="zmdi zmdi-comment-outline"></i>回复<span></span></a></li>
              </ul>
            </div>
          </div>
        })}
      </div>
    </div>)
  }

  componentDidMount() {
    this._fetchData()
  }

  _fetchData(filter) {
    filter = filter || this._lastFilter
    if (!filter) filter = { entity: 'Feeds', items: [] }
    this._lastFilter = filter

    $.post(`${rb.baseUrl}/feeds/data-list?page=${this.state.page || 1}`, JSON.stringify(filter), (res) => {
      this.setState({ list: res.data })
    })
  }

  _a = () => {
  }

}