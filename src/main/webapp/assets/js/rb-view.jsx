//~~ 视图
class RbViewForm extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props }
    }
    render() {
        return (<div className="rbview-form" ref="reviewForm">{this.state.formComponent}</div>)
    }
    componentDidMount() {
        let that = this
        $.get(rb.baseUrl + '/app/' + this.props.entity + '/view-model?id=' + this.props.id, function(res){
            // 包含错误
            if (res.error_code > 0 || !!res.data.error){
                let error = res.data.error || res.error_msg
                that.renderViewError(error)
                return
            }
            
            const FORM = <div className="row">{res.data.elements.map((item) => {
                return detectViewElement(item)
            })}</div>
            that.setState({ formComponent: FORM }, function(){
                that.hideLoading()
            })
        });
    }
    renderViewError(message) {
        let error = <div className="alert alert-danger alert-icon mt-5 w-75" style={{ margin:'0 auto' }}>
            <div className="icon"><i className="zmdi zmdi-alert-triangle"></i></div>
            <div className="message" dangerouslySetInnerHTML={{ __html: '<strong>抱歉!</strong> ' + message }}></div>
        </div>
        let that = this
        that.setState({ formComponent: error }, function() {
            that.hideLoading()
        })
    }
    
    hideLoading() {
        if (parent && parent.rb.RbViewModalGet(this.state.id)) parent.rb.RbViewModalGet(this.state.id).hideLoading()
        $(this.refs['reviewForm']).find('.type-NTEXT .form-control-plaintext').perfectScrollbar()
    }
}

const detectViewElement = function(item){
    item.onView = true
    item.viewMode = true
    item.key = 'col-' + item.field
    return (<div className={'col-12 col-sm-' + (item.isFull ? 12 : 6)} key={item.key}>{detectElement(item)}</div>)
}

// -- Usage

let rb = rb || {}

// props = { entity, recordId }
rb.RbViewForm = function(props, target){
    return renderRbcomp(<RbViewForm {...props}  />, target || 'tab-rbview')
}

const RbViewPage = {
    _RbViewForm:  null,
    
    init(id, entity, ep) {
        this.__id = id
        this.__entity = entity
        this._RbViewForm = rb.RbViewForm({ entity: entity[1], id: id })
        
        let that = this
        
        $('.J_delete').click(function(){
            let alertExt = { type: 'danger', confirmText: '删除' }
            alertExt.confirm = function(){
                $(this.refs['rbalert']).find('.btn').button('loading')
                let thatModal = this
                $.post(`${rb.baseUrl}/app/entity/record-delete?id=${that.__id}`, function(res){
                    if (res.error_code == 0){
                        rb.notice('删除成功', 'success')
                        that.hide(true)
                    } else {
                        rb.notice(res.error_msg || '删除失败，请稍后重试', 'danger')
                    }
                })
            }
            rb.alert('确认删除当前记录吗？', '删除确认', alertExt)
        })
        
        $('.J_edit').click(function(){
            rb.RbFormModal({ id: id, title: `编辑${entity[0]}`, entity: entity[1], icon: entity[2] })
        })
        
        $('.J_assign').click(function(){
            rb.AssignDialog({ entity: entity[1], ids: [id] })
        })
        $('.J_share').click(function(){
            rb.ShareDialog({ entity: entity[1], ids: [id] })
        })
        
        // Privileges
        if (ep) {
            if (ep.D === false) $('.J_delete').remove()
            if (ep.U === false) $('.J_edit').remove()
            if (ep.A === false) $('.J_assign').remove()
            if (ep.S === false) $('.J_share').remove()
            
            // clear
            $cleanMenu('.J_action')
            $cleanMenu('.J_new')
            $('.view-action .col-6').each(function(){ if ($(this).children().length == 0) $(this).remove() })
            if ($('.view-action').children().length == 0){
                $('.view-action').addClass('noaction')
                $('<div class="alert alert-light alert-icon alert-icon-colored min mb-2"><div class="icon"><i class="zmdi zmdi-info-outline"></i></div><div class="message">你对此记录无可操作权限</div></div>').appendTo('.view-action')
            }
        }
        
        $('.J_close').click(function(){
            if (parent && parent.rb.RbViewModalGet(id)) parent.rb.RbViewModalGet(id).hide()
        })
    },
    
    initVTabs(config) {
        let rs = []
        $(config).each(function(){
            $('<li class="nav-item"><a class="nav-link" href="#tab-' + this[0] + '" data-toggle="tab">' + this[1] + '</a></li>').appendTo('.nav-tabs')
            $('<div class="tab-pane" id="tab-' + this[0] + '"><div class="related-list rb-loading rb-loading-active"></div></div>').appendTo('.tab-content')
            rs.push(this[0])
        })
        
        let that = this
        
        $('.nav-tabs li>a').on('click', function(e) {
            e.preventDefault()
            let _this = $(this)
            _this.tab('show')
            
            let pane = $(_this.attr('href')).find('.related-list')
            if (pane.hasClass('rb-loading-active')) {
                if (~~_this.find('.badge').text() > 0) {
                    ReactDOM.render(<RbSpinner/>, pane[0])
                    that.renderRelatedList(pane, _this.attr('href').substr(5))
                } else {
                    ReactDOM.render(<div className="list-nodata"><span className="zmdi zmdi-info-outline"/><p>没有相关数据</p></div>, pane[0])
                    pane.removeClass('rb-loading-active')
                }
            }
        })
        
        if (rs.length > 0) {
            $.get(rb.baseUrl + '/app/entity/related-counts?master=' + this.__id + '&relates=' + rs.join(','), function(res){
                for (let k in res.data) {
                    if (~~res.data[k] > 0) {
                        let nav = $('.nav-tabs a[href="#tab-' + k + '"]')
                        $('<span class="badge badge-pill badge-primary">' + res.data[k] + '</span>').appendTo(nav)
                    }
                }
            })
        }
        
        $('.vtab-settings').click(function(){
            rb.modal(`${rb.baseUrl}/page/admin/entity/viewtab-config?entity=${that.__entity[1]}`, '配置显示项')
        })
    },
    
    renderRelatedList(el, related, page) {
        page = page || 1
        $.get(rb.baseUrl + '/app/entity/related-list?master=' + this.__id + '&related=' + related + '&pageNo=' + page, function(res){
            el.removeClass('rb-loading-active')
            $(res.data.data).each(function(){
                let h = '#!/View/' + related + '/' + this[0]
                $('<div class="card"><div class="float-left"><a href="' + h + '" onclick="RbViewPage.clickView(this)">' + this[1] + '</a></div><div class="float-right" title="修改时间">' + this[2] + '</div><div class="clearfix"></div></div>').appendTo(el)
            })
        })
    },
    
    initRecordMeta() {
        $.get(`${rb.baseUrl}/app/entity/record-meta?id=${this.__id}`, function(res){
            if (res.error_code == 0) {
                let _data = res.data
                for (let k in _data) {
                    if (k == 'owningUser'){
                        $('<a href="#!/View/User/' + _data[k][0] + '" onclick="RbViewPage.clickView(this)">' + _data[k][1] + '</a>').appendTo('.J_' + k)
                    } else if (k == 'shareTo'){
                        $('<a>' + (_data[k] == 0 ? '无' : ('已共享给' + _data[k] + '位用户')) + '</a>').appendTo('.J_' + k)
                    } else {
                        $('<span>' + _data[k] + '</span>').appendTo('.J_' + k)
                    }
                }
            }
        })
    },
    
    clickView(el) {
        let viewUrl = $(el).attr('href')
        console.log(viewUrl)
        viewUrl = viewUrl.split('/')
        parent.rb.RbViewModal({ entity: viewUrl[2], id: viewUrl[3] }, true)
    },
    
    // 隐藏划出的 View
    hide(reload) {
        if (parent.rb.__currentRbViewModal) parent.rb.__currentRbViewModal.hide()
        if (reload == true) {
            if (parent.RbListPage) parent.RbListPage._RbList.reload()
            else setTimeout(function() { parent.location.reload() }, 1000)
        }
    }
}