/* eslint-disable react/prop-types */
// ~ 动态发布
// eslint-disable-next-line no-unused-vars
class FeedsPost extends React.Component {

  constructor(props) {
    super(props)
    this.state = { ...props, type: 1 }
  }

  render() {
    return (<div className="feeds-post">
      <ul className="list-unstyled list-inline mb-1 pl-1">
        <li className="list-inline-item">
          <a onClick={() => this.setState({ type: 1 })} className={`${this.state.type === 1 && 'text-primary'}`}>动态</a>
        </li>
        <li className="list-inline-item">
          <a onClick={() => this.setState({ type: 2 })} className={`${this.state.type === 2 && 'text-primary'}`}>跟进</a>
        </li>
      </ul>
      <div className={`arrow_box ${this.state.type === 2 && 'index2'}`}></div>
      <div>
        <textarea className={`form-control form-control-sm ${this.state.badContent ? 'is-invalid' : ''}`} value={this.state.content} name="content" onInput={this._changeValue} placeholder="输入内容"></textarea>
      </div>
      <div className="mt-3">
        <div className="float-right">
          <button className="btn btn-primary" ref={(c) => this._btn = c} onClick={this._submit}>发布</button>
        </div>
        <div className="float-right mr-4">
          <div className="btn-group" style={{ border: '0 none' }}>
            <button className="btn btn-scope fixed-icon" data-toggle="dropdown" ref={(c) => this._scopeBtn = c}><i className="zmdi zmdi-chart-donut"></i>公开</button>
            <div className="dropdown-menu dropdown-menu-right">
              <a className="dropdown-item" onClick={this._selectScope} data-scope="ALL" title="全部可见"><i className="icon zmdi zmdi-chart-donut"></i>公开</a>
              <a className="dropdown-item" onClick={this._selectScope} data-scope="SELF" title="仅自己可见"><i className="icon zmdi zmdi-lock"></i>私密</a>
              <a className="dropdown-item" onClick={this._selectScope} data-scope="GROUP" title="群组内可见"><i className="icon zmdi zmdi-accounts"></i>群组</a>
            </div>
          </div>
        </div>
        <div className="clearfix"></div>
      </div>
    </div>)
  }

  componentDidMount() {
    $('.feeds-container .rb-loading').remove()
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
    if (!data.content) { this.setState({ badContent: true }); return }
    else this.setState({ badContent: false })
    data.metadata = { entity: 'Feeds' }

    let btn = $(this._btn).button('loading')
    $.post(`${rb.baseUrl}/feeds/post/publish`, JSON.stringify(data), (res) => {
      btn.button('reset')
      typeof this.props.call === 'function' && this.props.call(res.data)
    })
  }
}
