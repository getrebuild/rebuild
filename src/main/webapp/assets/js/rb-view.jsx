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
        $('.view-operating .view-action').empty()
    }
    
    hideLoading() {
        if (parent && parent.rb.RbViewModalGet(this.state.id)) parent.rb.RbViewModalGet(this.state.id).hideLoading()
        $(this.refs['reviewForm']).find('.type-NTEXT .form-control-plaintext').perfectScrollbar()
    }
}

const detectViewElement = function(item){
    item.onView = true
    item.viewMode = true
    item.key = 'col-' + (item.field == '$DIVIDER$' ? $random() : item.field)
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
    
    // @id - Record ID
    // @entity - [Name, Label, Icon]
    // @ep - Privileges of this entity
    init(id, entity, ep) {
        this.__id = id
        this.__entity = entity
        this.__ep = ep
        this._RbViewForm = rb.RbViewForm({ entity: entity[0], id: id })
        
        let that = this
        
        $('.J_delete').click(function(){
            let alertExt = { type: 'danger', confirmText: '删除' }
            alertExt.confirm = function(){
                $(this.refs['rbalert']).find('.btn').button('loading')
                let thatModal = this
                $.post(`${rb.baseUrl}/app/entity/record-delete?id=${that.__id}`, function(res){
                    if (res.error_code == 0){
                        rb.highbar('删除成功', 'success')
                        that.hide(true)
                    } else {
                        rb.hberror(res.error_msg)
                    }
                })
            }
            rb.alert('确认删除当前记录吗？', alertExt)
        })
        
        $('.J_edit').click(function(){
            rb.RbFormModal({ id: id, title: `编辑${entity[1]}`, entity: entity[0], icon: entity[2] })
        })
        $('.J_assign').click(function(){
            rb.DlgAssign({ entity: entity[0], ids: [id] })
        })
        $('.J_share').click(function(){
            rb.DlgShare({ entity: entity[0], ids: [id] })
        })
        $('.J_add-slave').click(function(){
            let iv = { '$MASTER$': id }
            let _this = $(this)
            rb.RbFormModal({ title: '添加明细', entity: _this.data('entity'), icon: _this.data('icon'), initialValue: iv })
        })
        
        // Privileges
        if (ep) {
            if (ep.D === false) $('.J_delete').remove()
            if (ep.U === false) $('.J_edit, .J_add-slave').remove()
            if (ep.A === false) $('.J_assign').remove()
            if (ep.S === false) $('.J_share').remove()
            
            that.__cleanButton()
        }
        
        $('.J_close').click(function(){
            if (parent && parent.rb.RbViewModalGet(id)) parent.rb.RbViewModalGet(id).hide()
        })
        $('.J_reload').click(function(){
            location.reload()
        })
    },
    
    initRecordMeta() {
        let that = this
        $.get(`${rb.baseUrl}/app/entity/record-meta?id=${this.__id}`, function(res){
            if (res.error_code != 0) return
            for (let k in res.data) {
                let v = res.data[k]
                if (!v || v == undefined) return
                if (k == 'owningUser'){
                    renderRbcomp(<UserShow id={v[0]} name={v[1]} avatarUrl={v[2]} showName={true} onClick={()=>{ that.clickViewUser(v[0]) }} />, $('.J_owningUser')[0])
                } else if (k == 'sharingList'){
                    let list = $('<ul class="list-unstyled list-inline mb-0"></ul>').appendTo('.J_sharingList')
                    $(v).each(function(){
                        let _this = this
                        let item = $('<li class="list-inline-item"></li>').appendTo(list)
                        renderRbcomp(<UserShow id={_this[0]} name={_this[1]} avatarUrl={_this[2]} onClick={()=>{ that.clickViewUser(_this[0]) }} />, item[0])
                    })
                    
                    if (that.__ep && that.__ep.S === true) {
                        let item_op = $('<li class="list-inline-item"></li>').appendTo(list)[0]
                        if (v.length == 0) renderRbcomp(<UserShow name="添加共享" icon="zmdi zmdi-plus" onClick={()=>{ $('.J_share').trigger('click') }} />, item_op)
                        else renderRbcomp(<UserShow name="管理共享用户" icon="zmdi zmdi-more" onClick={()=>{ rb.DlgUnShare(that.__id) }} />, item_op)
                    } else if (v.length > 0){
                        let item_op = $('<li class="list-inline-item"></li>').appendTo(list)[0]
                        renderRbcomp(<UserShow name="查看共享用户" icon="zmdi zmdi-more" onClick={()=>{ rb.DlgUnShare(that.__id, false) }} />, item_op)
                    } else {
                        $('.J_sharingList').parent().remove()
                    }

                } else {
                    $('<span>' + v + '</span>').appendTo('.J_' + k)
                }
            }
        })
    },
    
    initVTabs(config) {
        let that = this
        let rs = []
        $(config).each(function(){
            let entity = this[0]
            $('<li class="nav-item"><a class="nav-link" href="#tab-' + entity + '">' + this[1] + '</a></li>').appendTo('.nav-tabs')
            let rl = $('<div class="tab-pane" id="tab-' + entity + '"><div class="related-list rb-loading rb-loading-active"></div></div>').appendTo('.tab-content')
            rs.push(this[0])
            
            let mores = $('<div class="text-center J_mores mt-4 hide"><button type="button" class="btn btn-secondary load-mores">加载更多 ...</button></div>').appendTo(rl)
            rl = rl.find('.related-list')
            mores.find('.btn').on('click', function(){
                let pno = ~~($(this).attr('data-pno') || 1) + 1
                $(this).attr('data-pno', pno)
                that.renderRelatedGrid(rl, entity, pno)
            })
        })
        this.__vtab_es = rs
        
        $('.nav-tabs li>a').on('click', function(e) {
            e.preventDefault()
            let _this = $(this)
            _this.tab('show')
            
            let pane = $(_this.attr('href')).find('.related-list')
            if (pane.hasClass('rb-loading-active')) {
                if (~~_this.find('.badge').text() > 0) {
                    ReactDOM.render(<RbSpinner/>, pane[0])
                    that.renderRelatedGrid(pane, _this.attr('href').substr(5))
                } else {
                    ReactDOM.render(<div className="list-nodata"><span className="zmdi zmdi-info-outline"/><p>暂无数据</p></div>, pane[0])
                    pane.removeClass('rb-loading-active')
                }
            }
        })
        
        $('.J_view-addons').click(function(){
            let type = $(this).data('type')
            rb.modal(`${rb.baseUrl}/p/admin/entity/view-addons?entity=${that.__entity[0]}&type=${type}`, '配置' + (type == 'TAB' ? '显示项' : '新建项'))
        })
        
        this.updateVTabs()
    },
    
    updateVTabs(es) {
        es = es || this.__vtab_es
        if (!!!es || es.length == 0) return
        $.get(rb.baseUrl + '/app/entity/related-counts?masterId=' + this.__id + '&relateds=' + es.join(','), function(res){
            for (let k in res.data) {
                if (~~res.data[k] > 0) {
                    let tab = $('.nav-tabs a[href="#tab-' + k + '"]')
                    if (tab.find('.badge').length > 0) tab.find('.badge').text(res.data[k])
                    else $('<span class="badge badge-pill badge-primary">' + res.data[k] + '</span>').appendTo(tab)
                }
            }
        })
    },
    
    renderRelatedGrid(el, related, page) {
        page = page || 1
        let psize = 20
        $.get(rb.baseUrl + '/app/entity/related-list?masterId=' + this.__id + '&related=' + related + '&pageNo=' + page + '&pageSize=' + psize, function(res){
            el.removeClass('rb-loading-active')
            let _data = res.data.data
            $(_data).each(function(){
                let h = '#!/View/' + related + '/' + this[0]
                $('<div class="card"><div class="float-left"><a href="' + h + '" onclick="RbViewPage.clickView(this)">' + this[1] + '</a></div><div class="float-right" title="修改时间">' + this[2] + '</div><div class="clearfix"></div></div>').appendTo(el)
            })
            
            let mores = $(el).next('.J_mores')
            if (_data.length >= psize) mores.removeClass('hide')
            else mores.find('.btn').attr({ disabled: true }).text('已加载全部')
        })
    },
    
    initVAdds(config) {
        let that = this
        $(config).each(function(){
            let entity = this
            let item = $('<a class="dropdown-item"><i class="icon zmdi zmdi-' + entity[2] + '"></i>新建' + entity[1] + '</a>')
            item.click(function(){
                let iv = {}
                iv['&' + that.__entity[0]] = that.__id
                rb.RbFormModal({ title: `新建${entity[1]}`, entity: entity[0], icon: entity[2], initialValue: iv })
            })
            $('.J_adds .dropdown-divider').before(item)
        })
        this.__cleanButton()
    },
    
    clickView(el) {
        let viewUrl = $(el).attr('href')
        viewUrl = viewUrl.split('/')
        parent.rb.RbViewModal({ entity: viewUrl[2], id: viewUrl[3] }, true)
    },
    clickViewUser(id) {
        parent.rb.RbViewModal({ entity: 'User', id: id }, true)
    },
    
    __cleanButton() {
        $setTimeout(() => {
            $cleanMenu('.view-action .J_mores')
            $cleanMenu('.view-action .J_adds')
            $('.view-action .col-6').each(function(){ if ($(this).children().length == 0) $(this).remove() })
            if ($('.view-action').children().length == 0){
                $('.view-action').addClass('empty').empty()
            }
        }, 100, '__cleanButton')
    },
    
    // 隐藏划出的 View
    hide(reload) {
        if (parent && parent.rb.RbViewModalGet(this.__id)) parent.rb.RbViewModalGet(this.__id).hide()
        if (reload == true) {
            if (parent.RbListPage) parent.RbListPage._RbList.reload()
            else setTimeout(function() { parent.location.reload() }, 1000)
        }
    }
}

// Init
$(document).ready(function(){
    let wpc = window.__PageConfig
    if (!wpc) return
    RbViewPage.init(wpc.recordId, wpc.entity, wpc.privileges)
    RbViewPage.initRecordMeta()
    if (!!wpc.viewTabs) RbViewPage.initVTabs(wpc.viewTabs)
    if (!!wpc.viewAdds) RbViewPage.initVAdds(wpc.viewAdds)
})