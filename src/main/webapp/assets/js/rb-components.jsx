// ~~ 弹出窗口
class RbModal extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props, inLoad: true, isDestroy: false }
    }
	render() {
		return (this.state.isDestroy == true ? null :
			<div className="modal rbmodal colored-header colored-header-primary" ref="rbmodal">
		        <div className="modal-dialog" style={{ maxWidth:(this.props.width || 680) + 'px' }}>
    		        <div className="modal-content">
        		        <div className="modal-header modal-header-colored">
            		        <h3 className="modal-title">{this.state.title || 'RbModal'}</h3>
            		        <button className="close" type="button" onClick={()=>this.hide()}><span className="zmdi zmdi-close"></span></button>
            		    </div>
            		    <div className={'modal-body rb-loading ' + (this.state.inLoad == true && ' rb-loading-active') + ' ' + (this.state.url && ' iframe')}>
            		        {this.props.children || <iframe src={this.state.url || 'about:blank'} frameborder="0" scrolling="no" onLoad={()=>this.resize()} onResize={()=>this.resize()}></iframe>}
                            <RbSpinner />
                        </div>
    		        </div>
		        </div>
			</div>
		)
	}
	componentDidMount() {
	    if (this.props.children) this.setState({ inLoad: false })
	    this.show()
    }
	resize() {
        if (!!!this.state.url) return
        let root = $(this.refs['rbmodal'])
        let that = this
        $setTimeout(function(){
            let iframe = root.find('iframe')
            let height = iframe.contents().find('.main-content').height()
            if (height == 0) height = iframe.contents().find('body').height()
            else height += 45;  // .main-content's padding
            root.find('.modal-body').height(height)
            that.setState({ inLoad: false })
        }, 200, 'RbModal-resize')
    }
	show(callback) {
        let that = this
	    this.setState({ isDestroy: false }, function(){
	        $(that.refs['rbmodal']).modal({ show: true, backdrop: 'static' })
            typeof callback == 'function' && callback(that)
        })
    }
    hide(){
        $(this.refs['rbmodal']).modal('hide')
        let d = true
        if (this.props.destroyOnHide == false) d = false
        this.setState({ isDestroy: d, inLoad: d  })
    }
}

// ~~ 视图窗口
class RbView extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props, inLoad: true }
    }
    render() {
        return (
            <div className="modal-warpper">
            <div className="modal rbview" ref="rbview">
                <div className="modal-dialog">
                    <div className="modal-content">
                        <div className="modal-header">
                            {this.state.icon ? (<span className={'icon zmdi zmdi-' + this.state.icon}></span>) : '' }
                            <h3 className="modal-title">{this.state.title || '视图'}</h3>
                            <a className="close admin-settings" href={rb.baseUrl + '/admin/entity/' + this.state.entity + '/view-design'} title="配置布局" target="_blank"><span className="zmdi zmdi-settings"></span></a>
                            <button className="close" type="button" onClick={()=>this.hide()}><span className="zmdi zmdi-close"></span></button>
                        </div>
                        <div className={'modal-body iframe rb-loading ' + (this.state.inLoad == true && 'rb-loading-active')}>
                            <iframe src={this.state.showAfterUrl || 'about:blank'} frameborder="0" scrolling="no"></iframe>
                            <RbSpinner />
                        </div>
                    </div>
                </div>
            </div>
            </div>
        )
    }
    componentDidMount() {
        this.resizeModal()
        let that = this
        $(window).resize(function(){
            that.resizeModal()
            that.resize() 
        })
        
        let root = $(this.refs['rbview'])
        let mc = root.find('.modal-content')
        root.on('hidden.bs.modal', function(){
            mc.css({ 'margin-right': -1280 })
            that.setState({ inLoad: true })
        }).on('shown.bs.modal', function(){
            mc.animate({ 'margin-right': 0 }, 400)
        })
        this.show()
    }
    hideLoading(resize) {
        this.setState({ inLoad: false })
        if (resize == true) this.resize()
    }
    resize() {
        let root = $(this.refs['rbview'])
        let that = this
        $setTimeout(function(){
            let iframe = root.find('iframe')
            let height = iframe.contents().find('.main-content').height()
            root.find('.modal-body').height(height + 40)
            console.log('fire loaded ... ' + height)
        }, 40, 'RbView-resize')
    }
    resizeModal() {
        $(this.refs['rbview']).find('.modal-content').css('min-height', $(window).height())
    }
    show(url, ext) {
        let urlChanged = true
        if (url && url == this.state.url) urlChanged = false
        ext = ext || {}
        url = url || this.state.url
        let root = $(this.refs['rbview'])
        let that = this
        this.setState({ ...ext, url: url, inLoad: urlChanged }, function(){
            root.modal({ show: true, backdrop: true, keyboard: true, focus: true })
            $setTimeout(function(){
                that.setState({ showAfterUrl: that.state.url })
            }, 400)
        })
    }
    hide() {
        let root = $(this.refs['rbview'])
        root.modal('hide')
    }
}

// ~~ 提示框
class RbAlter extends React.Component {
    constructor(props) {
       super(props)
    }
    render() {
        let icon = this.props.type == 'danger' ? 'alert-triangle' : 'info-outline'
        let type = this.props.type || 'primary'
        let content = !!this.props.htmlMessage ? <div className="mt-3" dangerouslySetInnerHTML={{ __html : this.props.htmlMessage }}></div> : <p>{this.props.message || '提示内容'}</p>
        let confirm = (this.props.confirm || this.hide).bind(this)
        return (
            <div className="modal fade rbalter" ref="rbalter">
                <div className="modal-dialog">
                    <div className="modal-content">
                        <div className="modal-header">
                            <button className="close" type="button" onClick={()=>this.hide()}><span className="zmdi zmdi-close"></span></button>
                        </div>
                        <div className="modal-body">
                            <div className="text-center">
                                <div className={'text-' + type}><span className={'modal-main-icon zmdi zmdi-' + icon}></span></div>
                                <h4>{this.props.title || '提示'}</h4>
                                {content}
                                <div className="mt-6 mb-4">
                                    <button class="btn btn-space btn-secondary" type="button" onClick={()=>this.hide()}>取消</button>
                                    <button className={'btn btn-space btn-' + type} type="button" onClick={confirm}>确定</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        )
    }
    componentDidMount() {
        $(this.refs['rbalter']).modal({ show: true, keyboard: true })
    }
    hide() {
        let root = $(this.refs['rbalter'])
        root.modal('hide')
        setTimeout(function(){ root.parent().remove() }, 1000)
    }
}

// ~~ 提示条
class RbNotice extends React.Component {
    constructor(props) {
       super(props)
       this.state = { animatedClass: 'slideInDown' }
    }
    render() {
        let icon = this.props.type == 'success' ? 'check' : 'info-outline'
        icon = this.props.type == 'danger' ? 'close-circle-o' : icon
        let content = !!this.props.htmlMessage ? <div className="message" dangerouslySetInnerHTML={{ __html : this.props.htmlMessage }}></div> : <div className="message">{this.props.message}</div>
        return (
        <div ref="rbnotice" className={'rbnotice animated faster ' + this.state.animatedClass}>
            <div className={'alert alert-dismissible alert-' + (this.props.type || 'warning')}>
                <button className="close" type="button" onClick={()=>this.close()}><span className="zmdi zmdi-close"></span></button>
                <div className="icon"><span className={'zmdi zmdi-' + icon}></span></div>
                {content}
            </div>
        </div>
        )
    }
    componentDidMount() {
        if (this.props.closeAuto == false) return
        let that = this
        setTimeout(function(){ that.close() }, that.props.timeout || 3000)
    }
    close() {
        let that = this
        this.setState({ animatedClass: 'fadeOut' }, function(){
            setTimeout(function(){
                $(that.refs['rbnotice']).parent().remove();
            }, 1000)
        })
    }
}

function RbSpinner(props) {
    return <div className="rb-spinner">
        <svg width="40px" height="40px" viewBox="0 0 66 66" xmlns="http://-www.w3.org/2000/svg">
            <circle fill="none" stroke-width="4" stroke-linecap="round" cx="33" cy="33" r="30" class="circle"></circle>
        </svg>
    </div>
}

var renderRbcompTimes = 1;
const renderRbcomp = function(jsx, target) {
    target = target || ('react-comps-' + new Date().getTime()) + '-' + renderRbcompTimes++;
    let container = $('#' + target);
    if (container.length == 0) container = $('<div id="' + target + '"></div>').appendTo(document.body);
    return ReactDOM.render(jsx, container[0]);
}

var rb = rb || {}
rb.notice = function(message, type, ext){
    if (top != self && parent.rb && parent.rb.notice){
        parent.rb.notice(message, type, ext)
        return;
    }
    ext = ext || {}
    if (ext.html == true) return renderRbcomp(<RbNotice htmlMessage={message} type={type} timeout={ext.timeout} />)
    else return renderRbcomp(<RbNotice message={message} type={type} timeout={ext.timeout} />)
}
rb.alter = function(message, title, ext){
    ext = ext || {}
    if (ext.html == true) return renderRbcomp(<RbAlter htmlMessage={message} title={title} type={ext.type} confirm={ext.confirm} />)
    else return renderRbcomp(<RbAlter message={message} title={title} type={ext.type} confirm={ext.confirm} />)
}
rb.modal = function(url, title, ext) {
    ext = ext || {}
    return renderRbcomp(<RbModal url={url} title={title} width={ext.width} destroyOnHide={ext.destroyOnHide === false ? false : true } />)
}

// 打开视图
var viewModal
rb.recordView = function(id, title, entity, icon) {
    let viewUrl = rb.baseUrl + '/app/' + entity + '/view/' + id
//    renderRbcomp(<RbModal url={viewUrl} title={title} />)
    if (viewModal) viewModal.show(viewUrl, { entity: entity, title: title, icon: icon })
    else viewModal = renderRbcomp(<RbView url={viewUrl} entity={entity} title={title} icon={icon} />)
}