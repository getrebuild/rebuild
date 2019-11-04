// ~ 动态发布
// eslint-disable-next-line no-unused-vars
class FeedsPost extends React.Component {

  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    return (<div className="feeds-post">
      <ul className="list-unstyled list-inline">
        <li className="list-inline-item"><a href="#activities">动态</a></li>
        <li className="list-inline-item"><a href="#followup">跟进</a></li>
      </ul>
      <div>
        <textarea className="form-control form-control-sm" value={this.state.content} name="content" onInput={this._changeValue} placeholder="输入内容"></textarea>
      </div>
      <div className="mt-3">
        <div className="float-right">
          <button className="btn btn-primary" onClick={this._submit}>发布</button>
        </div>
        <div className="float-right mr-4">
          <div className="btn-group" style={{ border: '0 none' }}>
            <button className="btn btn-scope fixed-icon" data-toggle="dropdown" ref={(c) => this._scopeBtn = c}><i className="zmdi zmdi-chart-donut"></i>公开</button>
            <div className="dropdown-menu dropdown-menu-right">
              <a className="dropdown-item" onClick={this._selectScope} data-scope="ALL"><i className="icon zmdi zmdi-chart-donut"></i>公开</a>
              <a className="dropdown-item" onClick={this._selectScope} data-scope="SELF"><i className="icon zmdi zmdi-lock"></i>私有</a>
              <a className="dropdown-item" onClick={this._selectScope} data-scope="GROUP"><i className="icon zmdi zmdi-accounts"></i>群组</a>
            </div>
          </div>
        </div>
        <div className="clearfix"></div>
      </div>
    </div>)
  }

  componentDidMount() {
  }

  _changeValue = (e) => {
    let target = e.target
    let s = {}
    s[target.name] = target.value
    this.setState(s)
  }

  _selectScope = (e) => {
    let target = e.target
    this.setState({ scope: target.dataset.scope }, () => {
      $(this._scopeBtn).html($(target).html())
    })
  }

  _submit = () => {
    let data = { content: this.state.content, type: 1 }
    if (!data.content) { RbHighbar.create('请输入发布内容'); return }
    data.metadata = { entity: 'Feeds' }

    $.post(`${rb.baseUrl}/feeds/post/publish`, JSON.stringify(data), (res) => {
      // eslint-disable-next-line react/prop-types
      typeof this.props.call === 'function' && this.props.call(res.data)
    })
  }
}

// FeedsPost.propTypes = {
//   call: React.PropTypes.func
// }