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
    let downloadUrl = this.__buildAbsoluteUrl(currentUrl, 'attname=' + $encode(fileName))

    let previewContent = null
    if (this.__isImg(fileName)) previewContent = this.renderImgs()
    else if (this.__isDoc(fileName)) previewContent = this.renderDoc()
    else if (this.__isAudio(fileName)) previewContent = this.renderAudio()
    else if (this.__isVideo(fileName)) previewContent = this.renderVideo()

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
            <a onClick={this.share}><i className="zmdi zmdi-share fs-17"></i></a>
            <a target="_blank" rel="noopener noreferrer" href={downloadUrl}><i className="zmdi zmdi-download"></i></a>
            {!this.props.unclose && <a onClick={this.hide}><i className="zmdi zmdi-close"></i></a>}
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
          <img alt="图片" src={this.__buildAbsoluteUrl(null, 'imageView2/2/w/1000/interlace/1/q/100')} />
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
    return (<div className="container">
      <div className="audio must-center" onClick={this.__stopEvent}>
        <audio src={this.__buildAbsoluteUrl()} controls>
          您的浏览器不支持此功能
        </audio>
      </div>
    </div>)
  }

  renderVideo() {
    return (<div className="container">
      <div className="video must-center" onClick={this.__stopEvent}>
        <video src={this.__buildAbsoluteUrl()} height="500" controls>
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
    let fileName = $fileCutName(currentUrl)
    if (this.__isDoc(fileName)) {
      let that = this
      var setPreviewUrl = function (url) {
        let previewUrl = `https://view.officeapps.live.com/op/embed.aspx?src=${$encode(url)}`
        if (fileName.toLowerCase().endsWith('.pdf')) previewUrl = url
        that.setState({ previewUrl: previewUrl, errorMsg: null })
      }

      if (currentUrl.startsWith('http://') || currentUrl.startsWith('https://')) {
        setPreviewUrl(currentUrl)
      } else {
        $.get(`${rb.baseUrl}/filex/make-url?url=${currentUrl}`, (res) => {
          if (res.error_code > 0) this.setState({ errorMsg: res.error_msg })
          else setPreviewUrl(res.data.publicUrl)
        })
      }
    }

    let that = this
    $(document).unbind('keyup').keyup(function (event) { if (event.keyCode === 27) that.hide() })
  }

  componentWillUnmount() {
    if (!this.__modalOpen) $(document.body).removeClass('modal-open')
  }

  __buildAbsoluteUrl(url, params) {
    if (!url) url = this.props.urls[this.state.currentIndex]
    if (!(url.startsWith('http://') || url.startsWith('https://'))) {
      url = `${rb.baseUrl}/filex/download/${url}`
    }
    if (params) {
      url += (url.contains('?') ? '&' : '?')
      url += params
    }
    return url
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
    if (!this.props.unclose) $unmount($(this._dlg).parent(), 1)
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
const EXPIRES_TIME = [[5, '5分钟'], [30, '半小时'], [60, '1小时'], [360, '6小时'], [720, '12小时'], [1440, '1天']]
class FileShare extends RbModalHandler {
  constructor(props) {
    super(props)
  }
  render() {
    return <RbModal ref={(c) => this._dlg = c} title="分享文件" disposeOnHide="true">
      <div className="file-share">
        <label>分享链接</label>
        <div className="input-group input-group-sm">
          <input className="form-control" value={this.state.shareUrl || ''} readOnly onClick={(e) => $(e.target).select()} />
          <span className="input-group-append">
            <button className="btn btn-secondary" ref={(c) => this._btn = c}>复制</button>
          </span>
        </div>
        <div className="expires mt-2">
          <ul className="list-unstyled">
            {EXPIRES_TIME.map((item) => {
              return <li key={`time-${item[0]}`} className={`list-inline-item ${this.state.time === item[0] && 'active'}`}>
                <a onClick={this.changTime} data-time={item[0]}>{item[1]}</a>
              </li>
            })}
          </ul>
        </div>
      </div>
    </RbModal>
  }

  componentDidMount() {
    $(this._dlg._rbmodal).css({ zIndex: 1099 })
    this.changTime()

    let that = this
    let initCopy = function () {
      // eslint-disable-next-line no-undef
      new ClipboardJS(that._btn, {
        text: function () { return that.state.shareUrl }
      }).on('success', function () {
        RbHighbar.success('分享链接已复制')
      })
    }
    if (!window.ClipboardJS) {
      $.getScript(`${rb.baseUrl}/assets/lib/clipboard.min.js`, initCopy)
    } else {
      initCopy()
    }
  }

  changTime = (e) => {
    let t = e ? ~~e.target.dataset.time : 5
    if (this.state.time === t) return
    this.setState({ time: t }, () => {
      $.get(`${rb.baseUrl}/filex/make-share?url=${$encode(this.props.file)}&time=${t}`, (res) => {
        this.setState({ shareUrl: res.data.shareUrl })
      })
    })
  }
}