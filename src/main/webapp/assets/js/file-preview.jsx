/* eslint-disable react/prop-types */
// ~~ 图片/文档预览

const TYPE_DOCS = ['.doc', '.docx', '.rtf', '.xls', '.xlsx', '.ppt', '.pptx', '.pdf']
const TYPE_IMGS = ['.jpg', '.jpeg', '.gif', '.png', '.bmp']
const TYPE_AUDIOS = ['.mp3', '.wav', '.ogg', '.acc']
const TYPE_VIDEOS = ['.mp4', '.webm']

// eslint-disable-next-line no-unused-vars
class RbPreview extends React.Component {
  constructor(props) {
    super(props)
    this.state = { currentIndex: props.currentIndex || 0, inLoad: true }
  }

  render() {
    let currentUrl = this.props.urls[this.state.currentIndex]
    let fileName = $fileCutName(currentUrl)
    let downloadUrl = `${rb.baseUrl}/filex/download/${currentUrl}?attname=${fileName}`

    let previewContent = null
    if (this.__isImg(currentUrl)) previewContent = this.renderImgs()
    else if (this.__isDoc(currentUrl)) previewContent = this.renderDoc()
    else if (this.__isAudio(currentUrl)) previewContent = this.renderAudio()
    else if (this.__isVideo(currentUrl)) previewContent = this.renderVideo()

    // Has error
    if (this.state.errorMsg || !previewContent) {
      previewContent = <div className="unsupports shadow-lg rounded bg-light" onClick={this.__stopEvent}>
        <h4 className="mt-0">{this.state.errorMsg || '暂不支持此类型文件的预览'}</h4>
        <a className="link" target="_blank" rel="noopener noreferrer" href={downloadUrl}>下载文件</a>
      </div>
    }

    return <React.Fragment>
      <div className={`preview-modal ${this.state.inLoad ? 'hide' : ''}`} ref={(c) => this._dlg = c}>
        <div className="preview-header">
          <div className="float-left"><h5>{fileName}</h5></div>
          <div className="float-right">
            <a onClick={this.share}><i className="zmdi zmdi-share fs-16"></i></a>
            <a target="_blank" rel="noopener noreferrer" href={downloadUrl}><i className="zmdi zmdi-download"></i></a>
            <a onClick={this.hide}><i className="zmdi zmdi-close"></i></a>
          </div>
          <div className="clearfix"></div>
        </div>
        <div className="preview-body" onClick={this.hide}>
          {previewContent}
        </div>
      </div>
    </React.Fragment>
  }

  renderImgs() {
    return (<React.Fragment>
      <div className="img-zoom">
        <div className="must-center" onClick={this.__stopEvent}>
          <img alt="图片" src={`${rb.baseUrl}/filex/img/${this.props.urls[this.state.currentIndex]}?imageView2/2/w/1000/interlace/1/q/100`} />
        </div>
      </div>
      {this.props.urls.length > 1 && <div className="op-box" onClick={this.__stopEvent}>
        <a className="arrow float-left" onClick={this.__previmg}><i className="zmdi zmdi-chevron-left" /></a>
        <span>{this.state.currentIndex + 1} / {this.props.urls.length}</span>
        <a className="arrow float-right" onClick={this.__nextimg}><i className="zmdi zmdi-chevron-right" /></a>
      </div>
      }
    </React.Fragment>)
  }

  renderDoc() {
    return (<div className="container">
      <div className="iframe" onClick={this.__stopEvent}>
        <iframe frameBorder="0" scrolling="no" src={this.state.previewUrl || ''}></iframe>
      </div>
    </div>)
  }

  renderAudio() {
    let url = this.props.urls[this.state.currentIndex]
    url = `${rb.baseUrl}/filex/download/${url}`
    return (<div className="container">
      <div className="audio must-center" onClick={this.__stopEvent}>
        <audio src={url} controls>
          您的浏览器不支持此功能
        </audio>
      </div>
    </div>)
  }

  renderVideo() {
    let url = this.props.urls[this.state.currentIndex]
    url = `${rb.baseUrl}/filex/download/${url}`
    return (<div className="container">
      <div className="video must-center" onClick={this.__stopEvent}>
        <video src={url} height="500" controls>
          您的浏览器不支持此功能
        </video >
      </div>
    </div>)
  }

  componentDidMount() {
    this.__modalOpen = $(document.body).hasClass('modal-open')
    if (!this.__modalOpen) $(document.body).addClass('modal-open')
    this.setState({ inLoad: false })

    let currentUrl = this.props.urls[this.state.currentIndex]
    if (this.__isDoc(currentUrl)) {
      $.get(`${rb.baseUrl}/filex/make-url?url=${currentUrl}`, (res) => {
        if (res.error_code > 0) {
          this.setState({ errorMsg: res.error_msg })
        } else {
          // view.aspx
          let previewUrl = `https://view.officeapps.live.com/op/embed.aspx?src=${$encode(res.data.private_url)}`
          // PDF
          if (currentUrl.toLowerCase().endsWith('.pdf')) previewUrl = res.data.private_url
          this.setState({ previewUrl: previewUrl, errorMsg: null })
        }
      })
    }

    let that = this
    $(document).unbind('keyup').keyup(function (event) { if (event.keyCode === 27) that.hide() })
  }

  componentWillUnmount() {
    if (!this.__modalOpen) $(document.body).removeClass('modal-open')
  }

  __isImg(url) {
    return this.__isType(url, TYPE_IMGS)
  }
  __isDoc(url) {
    return this.__isType(url, TYPE_DOCS)
  }
  __isAudio(url) {
    return this.__isType(url, TYPE_AUDIOS)
  }
  __isVideo(url) {
    return this.__isType(url, TYPE_VIDEOS)
  }
  __isType(url, types) {
    url = url.toLowerCase()
    for (let i = 0; i < types.length; i++) {
      if (url.endsWith(types[i])) return true
    }
    return false
  }

  __previmg = (e) => {
    this.__stopEvent(e)
    let ci = this.state.currentIndex
    if (ci <= 0) ci = this.props.urls.length
    this.setState({ currentIndex: ci - 1 })
  }
  __nextimg = (e) => {
    this.__stopEvent(e)
    let ci = this.state.currentIndex
    if (ci + 1 >= this.props.urls.length) ci = -1
    this.setState({ currentIndex: ci + 1 })
  }
  __stopEvent = (e) => {
    e.stopPropagation()
  }

  hide = () => {
    $unmount($(this._dlg).parent(), 1)
  }

  share = () => {
    let currentUrl = this.props.urls[this.state.currentIndex]
    renderRbcomp(<FileShare file={currentUrl} />)
  }

  /**
   * @param {*} urls string or array of URL
   * @param {*} index 
   */
  static create(urls, index) {
    if (!urls) return
    if (typeof urls === 'string') urls = [urls]
    renderRbcomp(<RbPreview urls={urls} currentIndex={index || 0} />)
  }
}

// ~ 共享
class FileShare extends RbModalHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return <RbModal ref={(c) => this._dlg = c} title="共享文件" disposeOnHide="true">
      <div className="input-group input-group-sm">
        <input className="form-control" />
        <span className="input-group-append">
          <button className="btn btn-secondary">复制</button>
        </span>
      </div>
    </RbModal>
  }
}