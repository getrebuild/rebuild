/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~~ 图片/文档预览

const TYPE_DOCS = ['.doc', '.docx', '.rtf', '.xls', '.xlsx', '.ppt', '.pptx', '.pdf']
const TYPE_TEXTS = ['.txt', '.xml', '.json', '.md', '.yml', '.css', '.js', '.htm', '.html', '.log', '.sql', '.conf', '.sh', '.bat']
const TYPE_IMGS = ['.jpg', '.jpeg', '.gif', '.png', '.bmp', '.jfif', '.svg', '.webp']
const TYPE_AUDIOS = ['.mp3', '.wav', '.ogg', '.acc']
const TYPE_VIDEOS = ['.mp4', '.webm']

// 点击遮罩关闭预览
const HIDE_ONCLICK = false

// eslint-disable-next-line no-unused-vars
class RbPreview extends React.Component {
  constructor(props) {
    super(props)
    this.state = { currentIndex: props.currentIndex || 0, inLoad: true }
  }

  render() {
    const currentUrl = this.props.urls[this.state.currentIndex]
    const fileName = $fileCutName(currentUrl)
    const downloadUrl = this._buildAbsoluteUrl(currentUrl, 'attname=' + $encode(fileName))

    let previewContent = null
    if (this._isImage(fileName)) previewContent = this.renderImage()
    else if (this._isDoc(fileName)) previewContent = this.renderDoc()
    else if (this._isText(fileName)) previewContent = this.renderText()
    else if (this._isAudio(fileName)) previewContent = this.renderAudio()
    else if (this._isVideo(fileName)) previewContent = this.renderVideo()

    // Has error
    if (this.state.errorMsg || !previewContent) {
      previewContent = (
        <div className="unsupports shadow-lg rounded bg-light">
          <h4 className="mt-0">{this.state.errorMsg || $L('暂不支持此类型文件的预览')}</h4>
          <a className="link" target="_blank" rel="noopener noreferrer" href={downloadUrl}>
            <i className="zmdi zmdi-download icon mr-1" />
            {$L('下载文件')}
          </a>
        </div>
      )
    }

    return (
      <React.Fragment>
        <div className={`preview-modal ${this.state.inLoad ? 'hide' : ''}`} ref={(c) => (this._dlg = c)}>
          <div className="preview-header">
            <div className="float-left">
              <h5 className="text-bold">{fileName}</h5>
            </div>
            <div className="float-right">
              {rb.fileSharable && (
                <a onClick={this.share} title={$L('分享')}>
                  <i className="zmdi zmdi-share fs-17" />
                </a>
              )}
              <a title={$L('下载')} target="_blank" rel="noopener noreferrer" href={downloadUrl}>
                <i className="zmdi zmdi-download" />
              </a>
              {!this.props.unclose && (
                <a title={`${$L('关闭')} (ESC)`} onClick={this.hide}>
                  <i className="zmdi zmdi-close" />
                </a>
              )}
            </div>
            <div className="clearfix" />
          </div>
          <div className="preview-body" onClick={HIDE_ONCLICK ? this.hide : () => {}} ref={(c) => (this._previewBody = c)}>
            {previewContent}
          </div>
        </div>
      </React.Fragment>
    )
  }

  renderImage() {
    return (
      <React.Fragment>
        <div className="img-zoom fp-content">
          {!this.state.imgRendered && (
            <div className="must-center">
              <RbSpinner fully />
            </div>
          )}
          <img
            className={!this.state.imgRendered ? 'hide' : ''}
            src={this._buildAbsoluteUrl(null, 'imageView2/2/w/1000/interlace/1/q/100')}
            alt="Loading"
            onLoad={() => this.setState({ imgRendered: true })}
            onError={() => {
              RbHighbar.error($L('无法读取图片'))
              // this.hide()
              // Qiniu: {"error":"xxx is not within the limit, area is out of range [1, 24999999]"}
            }}
          />
        </div>
        {this.props.urls.length > 1 && (
          <div className="oper-box">
            <a className="arrow float-left" onClick={this._prevImage}>
              <i className="zmdi zmdi-chevron-left" />
            </a>
            <span>
              {this.state.currentIndex + 1} / {this.props.urls.length}
            </span>
            <a className="arrow float-right" onClick={this._nextImage}>
              <i className="zmdi zmdi-chevron-right" />
            </a>
          </div>
        )}
      </React.Fragment>
    )
  }

  renderDoc() {
    return (
      <div className="container fp-content">
        <div className="iframe">
          {!this.state.docRendered && (
            <div className="must-center">
              <RbSpinner fully={true} />
            </div>
          )}
          <iframe className={!this.state.docRendered ? 'hide' : ''} src={this.state.previewUrl || ''} onLoad={() => this.setState({ docRendered: true })} frameBorder="0" scrolling="no" />
        </div>
      </div>
    )
  }

  renderText() {
    return (
      <div className="container fp-content">
        <div className="iframe text">
          {this.state.previewText || this.state.previewText === '' ? (
            <pre>{this.state.previewText || <i className="text-muted">{$L('无')}</i>}</pre>
          ) : (
            <div className="must-center">
              <RbSpinner fully={true} />
            </div>
          )}
        </div>
      </div>
    )
  }

  renderAudio() {
    return (
      <div className="container fp-content">
        <div className="audio must-center">
          <audio src={this._buildAbsoluteUrl()} controls>
            {$L('你的浏览器不支持此功能')}
          </audio>
        </div>
      </div>
    )
  }

  renderVideo() {
    return (
      <div className="container fp-content">
        <div className="video must-center">
          <video src={this._buildAbsoluteUrl()} height="500" controls>
            {$L('你的浏览器不支持此功能')}
          </video>
        </div>
      </div>
    )
  }

  componentDidMount() {
    this.__modalOpen = $(document.body).hasClass('modal-open')
    if (!this.__modalOpen) $(document.body).addClass('modal-open')
    this.setState({ inLoad: false })

    const that = this

    const currentUrl = this.props.urls[this.state.currentIndex]
    const fileName = $fileCutName(currentUrl)
    if (this._isDoc(fileName)) {
      const ispdf = fileName.toLowerCase().endsWith('.pdf')
      const setPreviewUrl = function (url) {
        const previewUrl = ispdf ? url : (rb._officePreviewUrl || 'https://view.officeapps.live.com/op/embed.aspx?src=') + $encode(url)
        that.setState({ previewUrl: previewUrl, errorMsg: null })
      }

      if ($isFullUrl(currentUrl)) {
        setPreviewUrl(currentUrl)
      } else {
        $.get(`/filex/make-url?url=${currentUrl}`, (res) => {
          if (res.error_code > 0) this.setState({ errorMsg: res.error_msg })
          else setPreviewUrl(res.data.publicUrl)
        })
      }
    } else if (this._isText(fileName)) {
      $.ajax({
        url: `/filex/read-raw?url=${$encode(currentUrl)}`,
        type: 'GET',
        dataType: 'text',
        success: function (raw) {
          that.setState({ previewText: raw })
        },
        error: function (res) {
          if (res.status > 0) RbHighbar.error(`${$L('无法读取文件')} (${res.status})`)
          that.hide()
        },
      })
    }

    $(document)
      .off('keyup')
      .on('keyup', function (e) {
        // ESC
        if (e.keyCode === 27) that.hide()
      })
    $(that._previewBody)
      .find('>div.fp-content')
      .height($(window).height() - 60)
    $addResizeHandler(function () {
      $(that._previewBody)
        .find('>div.fp-content')
        .height($(window).height() - 60)
    })

    // in `/s/`
    setTimeout(() => $('.sharebox.must-center').remove(), 400)
  }

  componentWillUnmount() {
    if (!this.__modalOpen) $(document.body).removeClass('modal-open')
  }

  _buildAbsoluteUrl(url, params) {
    if (!url) url = this.props.urls[this.state.currentIndex]
    url = $isFullUrl(url) ? url : `${rb.baseUrl}/filex/${(params || '').includes('imageView2') ? 'img' : 'download'}/${url}`

    if (params) {
      url += url.contains('?') ? '&' : '?'
      url += params
    }
    return url
  }

  _isImage(url) {
    return this._isSpecType(url, TYPE_IMGS)
  }

  _isDoc(url) {
    return this._isSpecType(url, TYPE_DOCS)
  }

  _isAudio(url) {
    return this._isSpecType(url, TYPE_AUDIOS)
  }

  _isVideo(url) {
    return this._isSpecType(url, TYPE_VIDEOS)
  }

  _isText(url) {
    return this._isSpecType(url, TYPE_TEXTS)
  }

  _isSpecType(url, types) {
    url = url.toLowerCase()
    for (let i = 0; i < types.length; i++) {
      if (url.endsWith(types[i])) return true
    }
    return false
  }

  _prevImage = () => {
    let ci = this.state.currentIndex
    if (ci <= 0) ci = this.props.urls.length
    this.setState({ currentIndex: ci - 1, imgRendered: false })
  }
  _nextImage = () => {
    let ci = this.state.currentIndex
    if (ci + 1 >= this.props.urls.length) ci = -1
    this.setState({ currentIndex: ci + 1, imgRendered: false })
  }

  hide = () => {
    if (!this.props.unclose) $unmount($(this._dlg).parent(), 1)
  }

  share = () => {
    const currentUrl = this.props.urls[this.state.currentIndex]
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
const EXPIRES_TIME = [
  [5, 5 + $L('分钟')],
  [30, 30 + $L('分钟')],
  [60, 1 + $L('小时')],
  [360, 6 + $L('小时')],
  [720, 12 + $L('小时')],
  [1440, 1 + $L('天')],
  [4320, 3 + $L('天')],
]

class FileShare extends RbModalHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$L('分享文件')} disposeOnHide="true">
        <div className="file-share">
          <label>{$L('分享链接')}</label>
          <div className="input-group input-group-sm">
            <input className="form-control" value={this.state.shareUrl || ''} readOnly onClick={(e) => $(e.target).select()} />
            <span className="input-group-append">
              <button className="btn btn-secondary" ref={(c) => (this._$copy = c)} title={$L('复制')}>
                <i className="icon zmdi zmdi-copy" />
              </button>
            </span>
          </div>
          <div className="expires mt-2">
            <ul className="list-unstyled">
              {EXPIRES_TIME.map((item) => {
                return (
                  <li key={`time-${item[0]}`} className={`list-inline-item ${this.state.time === item[0] && 'active'}`}>
                    <a onClick={this._changeTime} data-time={item[0]} _title={$L('有效')}>
                      {item[1]}&nbsp;
                    </a>
                  </li>
                )
              })}
            </ul>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    $(this._dlg._rbmodal).css({ zIndex: 1099 })
    this._changeTime()

    const that = this
    const initCopy = function () {
      // eslint-disable-next-line no-undef
      new ClipboardJS(that._$copy, {
        text: function () {
          return that.state.shareUrl
        },
      }).on('success', () => $(that._$copy).addClass('copied-check'))
      $(that._$copy).on('mouseenter', () => $(that._$copy).removeClass('copied-check'))
    }
    if (window.ClipboardJS) {
      initCopy()
    } else {
      // eslint-disable-next-line no-undef
      $getScript('/assets/lib/clipboard.min.js', initCopy)
    }
  }

  _changeTime = (e) => {
    const t = e ? ~~e.target.dataset.time : 5
    if (this.state.time === t) return
    this.setState({ time: t }, () => {
      $.get(`/filex/make-share?url=${$encode(this.props.file)}&time=${t}`, (res) => {
        this.setState({ shareUrl: res.data.shareUrl })
      })
    })
  }
}
