// ~~ Modal 兼容子元素和 iFrame
class RbModal extends React.Component {  // eslint-disable-line
  constructor(props) {
    super(props)
    this.state = { ...props }
  }
  render() {
    let inFrame = !this.props.children
    return (<div className="modal rbmodal colored-header colored-header-primary" ref="rbmodal">
      <div className="modal-dialog" style={{ maxWidth: (this.props.width || 680) + 'px' }}>
        <div className="modal-content">
          <div className="modal-header modal-header-colored">
            <h3 className="modal-title">{this.props.title || '无标题'}</h3>
            <button className="close" type="button" onClick={() => this.hide()}><span className="zmdi zmdi-close" /></button>
          </div>
          <div className={'modal-body' + (inFrame ? ' iframe rb-loading' : '') + (inFrame && this.state.frameLoad !== false ? ' rb-loading-active' : '')}>
            {this.props.children || <iframe src={this.props.url} frameBorder="0" scrolling="no" onLoad={() => this.resize()} />}
            {inFrame && <RbSpinner />}
          </div>
        </div>
      </div>
    </div>)
  }
  componentDidMount() {
    this.show()
  }
  show() {
    let root = $(this.refs['rbmodal'])
    root.modal({ show: true, backdrop: 'static' })
    typeof this.props.onShow === 'function' && this.props.onShow(this)
  }
  hide() {
    let root = $(this.refs['rbmodal'])
    root.modal('hide')
    if (this.props.disposeOnHide === true) {
      root.modal('dispose')
      let container = root.parent()
      ReactDOM.unmountComponentAtNode(container[0])
      setTimeout(() => { container.remove() }, 200)
    }
    typeof this.props.onHide === 'function' && this.props.onHide(this)
  }
  resize() {
    if (this.props.children) return
    let root = $(this.refs['rbmodal'])
    let that = this
    $setTimeout(function () {
      let iframe = root.find('iframe')
      let height = iframe.contents().find('.main-content').height()
      if (height === 0) height = iframe.contents().find('body').height()
      else height += 45 // .main-content's padding
      root.find('.modal-body').height(height)
      that.setState({ frameLoad: false })
    }, 100, 'RbModal-resize')
  }
}

// ~~ Modal 处理器
class RbModalHandler extends React.Component {  // eslint-disable-line
  constructor(props) {
    super(props)
    this.state = { ...props }
    this.show = this.show.bind(this)
    this.hide = this.hide.bind(this)
  }
  show(state, call) {
    let callback = () => {
      if (this.refs['dlg']) this.refs['dlg'].show()
      typeof call === 'function' && call(this)
    }
    if (state && $.type(state) == 'object') this.setState(state, callback)
    else callback()
  }
  hide() {
    if (this.refs['dlg']) this.refs['dlg'].hide()
  }
}

// ~~ Form 处理器
class RbFormHandler extends RbModalHandler {  // eslint-disable-line
  constructor(props) {
    super(props)
    this.handleChange = this.handleChange.bind(this)
  }
  handleChange(e) {
    let target = e.target
    let id = target.dataset.id
    let val = target.type === 'checkbox' ? target.checked : target.value
    let s = {}
    s[id] = val
    this.setState(s)
  }
}

// ~~ 提示框
class RbAlert extends React.Component {  // eslint-disable-line
  constructor(props) {
    super(props)
  }
  render() {
    let icon = this.props.type === 'danger' ? 'alert-triangle' : 'info-outline'
    icon = this.props.type == 'warning' ? 'alert-circle-o' : icon
    let type = this.props.type || 'primary'
    let content = this.props.htmlMessage ? <div className="mt-3" style={{ lineHeight: 1.8 }} dangerouslySetInnerHTML={{ __html: this.props.htmlMessage }} /> : <p>{this.props.message || '提示内容'}</p>
    let confirm = (this.props.confirm || this.hide).bind(this)
    return (
      <div className="modal rbalert" ref="rbalert" tabIndex="-1">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()}><span className="zmdi zmdi-close" /></button>
            </div>
            <div className="modal-body">
              <div className="text-center ml-6 mr-6">
                <div className={'text-' + type}><span className={'modal-main-icon zmdi zmdi-' + icon} /></div>
                {this.props.title && <h4 className="mb-2 mt-3">{this.props.title}</h4>}
                <div className={this.props.title ? '' : 'mt-3'}>{content}</div>
                <div className="mt-4 mb-3" ref="btns">
                  <button className="btn btn-space btn-secondary" type="button" onClick={() => this.hide()}>取消</button>
                  <button className={'btn btn-space btn-' + type} type="button" onClick={confirm}>{this.props.confirmText || '确定'}</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }
  componentDidMount() {
    $(this.refs['rbalert']).modal({ show: true, keyboard: true })
  }
  hide() {
    let root = $(this.refs['rbalert'])
    root.modal('hide')
    setTimeout(function () {
      root.modal('dispose')
      root.parent().remove()
    }, 1000)
  }
}

// ~~ 顶部提示条
class RbHighbar extends React.Component {  // eslint-disable-line
  constructor(props) {
    super(props)
    this.state = { animatedClass: 'slideInDown' }
  }
  render() {
    let icon = this.props.type === 'success' ? 'check' : 'info-outline'
    icon = this.props.type == 'danger' ? 'close-circle-o' : icon
    let content = this.props.htmlMessage ? <div className="message" dangerouslySetInnerHTML={{ __html: this.props.htmlMessage }} /> : <div className="message">{this.props.message}</div>
    return (<div ref="rbhighbar" className={'rbhighbar animated faster ' + this.state.animatedClass}>
      <div className={'alert alert-dismissible alert-' + (this.props.type || 'warning')}>
        <button className="close" type="button" onClick={() => this.close()}><span className="zmdi zmdi-close" /></button>
        <div className="icon"><span className={'zmdi zmdi-' + icon} /></div>
        {content}
      </div>
    </div>)
  }
  componentDidMount() {
    setTimeout(() => { this.close() }, this.props.timeout || 3000)
  }
  close() {
    this.setState({ animatedClass: 'fadeOut' }, () => {
      setTimeout(() => {
        $(this.refs['rbhighbar']).parent().remove()
      }, 1000)
    })
  }
}

// ~~ 加载条
function RbSpinner(props) {  // eslint-disable-line
  return <div className="rb-spinner">
    <svg width="40px" height="40px" viewBox="0 0 66 66" xmlns="http://www.w3.org/2000/svg">
      <circle fill="none" strokeWidth="4" strokeLinecap="round" cx="33" cy="33" r="30" className="circle" />
    </svg>
  </div>
}

let renderRbcomp__counter = new Date().getTime()
// @jsx
// @target id or Element
const renderRbcomp = function (jsx, target) {
  target = target || ('react-comps-' + renderRbcomp__counter++)
  if ($.type(target) === 'string') { // element id
    let container = document.getElementById(target)
    if (!container) target = $('<div id="' + target + '"></div>').appendTo(document.body)[0]
    else target = container
  } else {
    // Element object
  }
  return ReactDOM.render(jsx, target)
}

// -- Usage

var rb = rb || {}

rb.__currentModal
rb.__currentModalCache = {}
// @url - URL in iframe
// @title
// @ext - more props
rb.modal = function (url, title, ext) {
  ext = ext || {}
  ext.disposeOnHide = ext.disposeOnHide === true // default false
  if (ext.disposeOnHide === false && !!rb.__currentModalCache[url]) {
    rb.__currentModal = rb.__currentModalCache[url]
    rb.__currentModal.show()
  } else {
    rb.__currentModal = renderRbcomp(<RbModal url={url} title={title} width={ext.width} disposeOnHide={ext.disposeOnHide} />)
    if (ext.disposeOnHide === false) { //  No cache
      rb.__currentModalCache[url] = rb.__currentModal
    }
  }
  return rb.__currentModal
}
rb.modalHide = function (url) {
  if (url) {
    let c = rb.__currentModalCache[url]
    if (c) c.hide()
  } else if (rb.__currentModal) {
    rb.__currentModal.hide()
  }
}
rb.modalResize = function (url) {
  if (url) {
    let c = rb.__currentModalCache[url]
    if (c) c.resize()
  } else if (rb.__currentModal) {
    rb.__currentModal.resize()
  }
}

// @message
// @titleExt - title or ext
// @ext - more props
rb.alert = (message, titleExt, ext) => {
  let title = titleExt
  if ($.type(titleExt) === 'object') {
    title = null
    ext = titleExt
  }
  ext = ext || {}
  if (ext.html === true) return renderRbcomp(<RbAlert htmlMessage={message} title={title} type={ext.type} confirmText={ext.confirmText} confirm={ext.confirm} />)
  else return renderRbcomp(<RbAlert message={message} title={title} type={ext.type} confirmText={ext.confirmText} confirm={ext.confirm} />)
}

// @message
// @type - danger, warning or null
// @ext - more props
rb.highbar = (message, type, ext) => {
  if (top !== self && parent.rb && parent.rb.highbar) {
    parent.rb.highbar(message, type, ext)
    return
  }
  ext = ext || {}
  if (ext.html === true) return renderRbcomp(<RbHighbar htmlMessage={message} type={type} timeout={ext.timeout} />)
  else return renderRbcomp(<RbHighbar message={message} type={type} timeout={ext.timeout} />)
}
rb.hberror = (message) => {
  rb.highbar(message || '系统繁忙，请稍后重试', 'danger', { timeout: 6000 })
}
rb.hbsuccess = (message) => {
  rb.highbar(message || '操作成功', 'success')
}
