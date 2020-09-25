/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~~ 图片/文档预览

const TYPE_DOCS = ['.doc', '.docx', '.rtf', '.xls', '.xlsx', '.ppt', '.pptx', '.pdf']
const TYPE_TEXTS = ['.txt', '.xml', '.json', '.md', '.yml', '.css', '.js', '.htm', '.html', '.log', '.sql']
const TYPE_IMGS = ['.jpg', '.jpeg', '.gif', '.png', '.bmp']
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
    const downloadUrl = this.__buildAbsoluteUrl(currentUrl, 'attname=' + $encode(fileName))

    let previewContent = null
    if (this.__isImg(fileName)) previewContent = this.renderImgs()
    else if (this.__isDoc(fileName)) previewContent = this.renderDoc()
    else if (this.__isText(fileName)) previewContent = this.renderText()
    else if (this.__isAudio(fileName)) previewContent = this.renderAudio()
    else if (this.__isVideo(fileName)) previewContent = this.renderVideo()

    // Has error
    if (this.state.errorMsg || !previewContent) {
      previewContent = (
        <div className="unsupports shadow-lg rounded bg-light" onClick={this.__stopEvent}>
          <h4 className="mt-0">{this.state.errorMsg || $lang('UpsupportPreviewTips')}</h4>
          <a className="link" target="_blank" rel="noopener noreferrer" href={downloadUrl}>
            {$lang('DownloadFile')}
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
                <a onClick={this.share} title={$lang('Share0')}>
                  <i className="zmdi zmdi-share fs-17"></i>
                </a>
              )}
              <a title={$lang('Download')} target="_blank" rel="noopener noreferrer" href={downloadUrl}>
                <i className="zmdi zmdi-download"></i>
              </a>
              {!this.props.unclose && (
                <a title={$lang('Close')} onClick={this.hide}>
                  <i className="zmdi zmdi-close"></i>
                </a>
              )}
            </div>
            <div className="clearfix"></div>
          </div>
          <div className="preview-body" onClick={HIDE_ONCLICK ? this.hide : () => {}} ref={(c) => (this._previewBody = c)}>
            {previewContent}
          </div>
        </div>
      </React.Fragment>
    )
  }

  renderImgs() {
    return (
      <React.Fragment>
        <div className="img-zoom fp-content">
          {!this.state.imgRendered && (
            <div className="must-center">
              <RbSpinner fully={true} />
            </div>
          )}
          <img
            className={!this.state.imgRendered ? 'hide' : ''}
            src={this.__buildAbsoluteUrl(null, 'imageView2/2/w/1000/interlace/1/q/100')}
            alt="Loading"
            onLoad={() => this.setState({ imgRendered: true })}
            onError={() => RbHighbar.error($lang('LoadImgError'))}
          />
        </div>
        {this.props.urls.length > 1 && (
          <div className="oper-box" onClick={this.__stopEvent}>
            <a className="arrow float-left" onClick={this.__previmg}>
              <i className="zmdi zmdi-chevron-left" />
            </a>
            <span>
              {this.state.currentIndex + 1} / {this.props.urls.length}
            </span>
            <a className="arrow float-right" onClick={this.__nextimg}>
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
        <div className="iframe" onClick={this.__stopEvent}>
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
        <div className="iframe text" onClick={this.__stopEvent}>
          {this.state.previewText || this.state.previewText === '' ? (
            <pre>{this.state.previewText || <i className="text-muted">{$lang('Null')}</i>}</pre>
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
        <div className="audio must-center" onClick={this.__stopEvent}>
          <audio src={this.__buildAbsoluteUrl()} controls>
            {$lang('YourBrowserUnsupport')}
          </audio>
        </div>
      </div>
    )
  }

  renderVideo() {
    return (
      <div className="container fp-content">
        <div className="video must-center" onClick={this.__stopEvent}>
          <video src={this.__buildAbsoluteUrl()} height="500" controls>
            {$lang('YourBrowserUnsupport')}
          </video>
        </div>
      </div>
    )
  }

  componentDidMount() {
    this.__modalOpen = $(document.body).hasClass('modal-open')
    if (!this.__modalOpen) $(document.body).addClass('modal-open')
    this.setState({ inLoad: false })

    const currentUrl = this.props.urls[this.state.currentIndex]
    const fileName = $fileCutName(currentUrl)
    if (this.__isDoc(fileName)) {
      const that = this
      const setPreviewUrl = function (url) {
        const previewUrl = fileName.toLowerCase().endsWith('.pdf') ? url : `https://view.officeapps.live.com/op/embed.aspx?src=${$encode(url)}`
        that.setState({ previewUrl: previewUrl, errorMsg: null })
      }

      if (currentUrl.startsWith('http://') || currentUrl.startsWith('https://')) {
        setPreviewUrl(currentUrl)
      } else {
        $.get(`/filex/make-url?url=${currentUrl}`, (res) => {
          if (res.error_code > 0) this.setState({ errorMsg: res.error_msg })
          else setPreviewUrl(res.data.publicUrl)
        })
      }
    } else if (this.__isText(fileName)) {
      const textUrl = currentUrl.startsWith('http://') || currentUrl.startsWith('https://') ? currentUrl : `/filex/download/${currentUrl}`
      $.ajax({
        url: textUrl,
        type: 'GET',
        dataType: 'text',
        success: function (res) {
          that.setState({ previewText: res })
        },
      })
    }

    const that = this
    $(document)
      .unbind('keyup')
      .keyup(function (event) {
        if (event.keyCode === 27) that.hide()
      })
    $(that._previewBody)
      .find('>div.fp-content')
      .height($(window).height() - 60)
    $addResizeHandler(function () {
      $(that._previewBody)
        .find('>div.fp-content')
        .height($(window).height() - 60)
    })
  }

  componentWillUnmount() {
    if (!this.__modalOpen) $(document.body).removeClass('modal-open')
  }

  __buildAbsoluteUrl(url, params) {
    if (!url) url = this.props.urls[this.state.currentIndex]
    if (!(url.startsWith('http://') || url.startsWith('https://'))) {
      url = `${rb.baseUrl}/filex/${(params || '').includes('imageView2') ? 'img' : 'download'}/${url}`
    }
    if (params) {
      url += url.contains('?') ? '&' : '?'
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

  __isText(url) {
    return this.__isType(url, TYPE_TEXTS)
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
    this.setState({ currentIndex: ci - 1, imgRendered: false })
  }
  __nextimg = (e) => {
    this.__stopEvent(e)
    let ci = this.state.currentIndex
    if (ci + 1 >= this.props.urls.length) ci = -1
    this.setState({ currentIndex: ci + 1, imgRendered: false })
  }
  __stopEvent = (e) => e && e.stopPropagation()

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
  [5, 5 + $lang('Minte')],
  [30, 30 + $lang('Minte')],
  [60, 1 + $lang('Hour')],
  [360, 6 + $lang('Hour')],
  [720, 12 + $lang('Hour')],
  [1440, 1 + $lang('Day0')],
  [4320, 3 + $lang('Day0')],
]

class FileShare extends RbModalHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$lang('Share0File')} disposeOnHide="true">
        <div className="file-share">
          <label>{$lang('Share0Link')}</label>
          <div className="input-group input-group-sm">
            <input className="form-control" value={this.state.shareUrl || ''} readOnly onClick={(e) => $(e.target).select()} />
            <span className="input-group-append">
              <button className="btn btn-secondary" ref={(c) => (this._btn = c)}>
                {$lang('Copy')}
              </button>
            </span>
          </div>
          <div className="expires mt-2">
            <ul className="list-unstyled">
              {EXPIRES_TIME.map((item) => {
                return (
                  <li key={`time-${item[0]}`} className={`list-inline-item ${this.state.time === item[0] && 'active'}`}>
                    <a onClick={this.changTime} data-time={item[0]} _title={$lang('Validity')}>
                      {item[1]}
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
    this.changTime()

    const that = this
    const initCopy = function () {
      // eslint-disable-next-line no-undef
      new ClipboardJS(that._btn, {
        text: function () {
          return that.state.shareUrl
        },
      }).on('success', function () {
        RbHighbar.success($lang('Share0LinkCopyTips'))
      })
    }
    if (!window.ClipboardJS) {
      $.getScript('/assets/lib/clipboard.min.js', initCopy)
    } else {
      initCopy()
    }
  }

  changTime = (e) => {
    const t = e ? ~~e.target.dataset.time : 5
    if (this.state.time === t) return
    this.setState({ time: t }, () => {
      $.get(`/filex/make-share?url=${$encode(this.props.file)}&time=${t}`, (res) => {
        this.setState({ shareUrl: res.data.shareUrl })
      })
    })
  }
}
