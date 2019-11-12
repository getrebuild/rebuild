/* eslint-disable react/prop-types */
/* eslint-disable no-unused-vars */

// ~ 目录导航
class NavTree extends React.Component {
  state = { ...this.props, activeItem: '$ALL$' }

  render() {
    return <div className="dept-tree p-0">
      <ul className="list-unstyled">
        {(this.state.list || []).map((item) => { return this._renderItem(item) })}
      </ul>
    </div>
  }
  _renderItem(item) {
    return <li key={`xd-${item.id}`} className={this.state.activeItem === item.id ? 'active' : ''}>
      <a onClick={() => this._clickItem(item)} href={`#!/${item.text}`}>{item.text}</a>
      {item.children && <ul className="list-unstyled">
        {item.children.map((item) => { return this._renderItem(item) })}
      </ul>}
    </li>
  }
  _clickItem(item) {
    this.setState({ activeItem: item.id }, () => {
      this.props.call && this.props.call(item)
    })
  }

  componentDidMount() {
    $.get(this.props.dataUrl, (res) => {
      let _list = res.data || []
      _list.unshift({ id: '$ALL$', text: '全部' })
      this.setState({ list: _list })
    })
  }
}

// ~ 文件列表
class FilesList extends React.Component {
  state = { ...this.props }

  render() {
    return <div className="file-list">
      {(this.state.files || []).map((item) => {
        return <div key={`file-${item.id}`} className="file-list-item">
          <span className="type"><i className="file-icon" data-type={item.fileType}></i></span>
          <span className="on">{item.uploadOn}</span>
          <span className="by">{item.uploadBy[1]}</span>
          <div className="detail">
            <a title="点击查看文件" onClick={() => this._preview(item.filePath)}>{$fileCutName(item.filePath)}</a>
            <div>
              <span>{item.uploadBy[1]}</span>
            </div>
          </div>
        </div>
      })}
    </div>
  }

  componentDidMount = () => this._loadFiles()
  _loadFiles() {
    $.get(`${rb.baseUrl}/files/list-file`, (res) => {
      this.setState({ files: res.data || [] })
    })
  }

  preview(path) {
    RbPreview.create(path)
    // TODO 检查权限
  }

  search = () => {
    this._loadFiles()
  }
}
