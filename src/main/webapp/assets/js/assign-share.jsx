class TheBothDialog extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props, cascadesSpec: false }
        this.opType = props.type == 'assign' ? '分派' : '共享'
    }
    render() {
        return (
            <div className="modal-warpper">
            <div className="modal rbmodal colored-header colored-header-primary" ref="rbmodal">
                <div className="modal-dialog">
                    <div className="modal-content">
                        <div className="modal-header modal-header-colored">
                            <h3 className="modal-title">{this.opType}</h3>
                            <button className="close md-close" type="button" onClick={()=>this.hide()}><span className="zmdi zmdi-close"></span></button>
                        </div>
                        <div className='modal-body'>
                            <form>
                                <div className="form-group row">
                                    <label className="col-sm-3 col-form-label text-sm-right">{this.opType + '哪些记录'}</label>
                                    <div className="col-sm-7">
                                        <div className="form-control-plaintext" id="records">{'选中的记录 (' + this.state.ids.length + '条)'}</div>
                                    </div>
                                </div>
                                <div className="form-group row">
                                    <label className="col-sm-3 col-form-label text-sm-right">{this.opType + '给谁'}</label>
                                    <div className="col-sm-7">
                                        <select className="form-control form-control-sm" ref="toUser" />
                                    </div>
                                </div>
                                <div className={'form-group row ' + (this.state.cascadesSpec == false ? '' : 'hide')}>
                                    <div className="col-sm-7 offset-sm-3"><a href="javascript:;" onClick={()=>this.showCascades()}>{'同时' + this.opType + '关联记录'}</a></div>
                                </div>
                                <div className={'form-group row ' + (this.state.cascadesSpec == false ? 'hide' : '')}>
                                    <label className="col-sm-3 col-form-label text-sm-right">{'同时' + this.opType + '关联记录'}</label>
                                    <div className="col-sm-7">
                                        <select className="form-control form-control-sm" ref="cascades" multiple="multiple">
                                        {(this.state.cascadesEntity || []).map((item) => {
                                            return <option value={item[0]}>{item[1]}</option>
                                        })}
                                        </select>
                                    </div>
                                 </div>
                                 <div className="form-group row footer">
                                    <div className="col-sm-7 offset-sm-3" ref="actions">
                                        <button className="btn btn-primary" type="button" data-loading-text="请稍后" onClick={()=>this.post()}>确定</button>
                                        <a className="btn btn-link" onClick={()=>this.hide()}>取消</a>
                                    </div>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
            </div>
        )
    }
    componentDidMount() {
        let that = this
        let select2 = $(this.refs['toUser']).select2({
            language: 'zh-CN',
            placeholder: '选择用户',
            width: '100%',
            allowClear: true,
            ajax: {
                url: rb.baseUrl + '/commons/search',
                delay: 300,
                data: function(params) {
                    let query = {
                        entity: 'User',
                        fields: 'loginName,fullName,email',
                        q: params.term,
                    }
                    return query
                },
                processResults: function(data){
                    let rs = data.data.map((item) => { return item })
                    return { results: rs }
                }
            }
        })
        this.show()
    }
    componentWillUnmount() {
        $(this.refs['toUser']).select2('destroy')
        $(this.refs['cascades']).select2('destroy')
    }
    
    showCascades() {
        let that = this
        $.get(rb.baseUrl + '/commons/metadata/references?entity=' + this.props.entity, function(res){
            that.setState({ cascadesSpec: true, cascadesEntity: res.data }, function(){
                $(that.refs['cascades']).select2({
                     language: 'zh-CN',
                     placeholder: '选择关联实体 (可选)',
                })
            })
        })
    }
    
    show(callback) {
        $(this.refs['rbmodal']).modal({ show: true, backdrop: 'static' })
        typeof callback == 'function' && callback(this)
    }
    hide() {
        $(this.refs['rbmodal']).modal('hide')
    }
    
    post() {
        let to = $(this.refs['toUser']).val()
        if (!!!to) { rb.notice('请选择' + this.opType + '给谁'); return }
        let cas = $(this.refs['cascades']).val() || []
        
        let that = this
        let btns = $(this.refs['actions']).find('.btn-primary').button('loading')
        $.post(`${rb.baseUrl}/app/entity/record-${this.state.type}?id=${this.state.ids.join(',')}&cascades=${cas.join(',')}&to=${to}`, function(res){
            if (res.error_code == 0){
                that.hide()
                rb.notice('已成功' + that.opType + ' ' + (res.data.assigned || res.data.shared) + ' 条记录', 'success')
                
                if (window.RbListPage) RbListPage._RbList.reload()
                if (window.RbViewPage) location.reload()
                
            } else {
                rb.notice(res.error_msg || ('操作失败，请稍后重试'), 'danger')
            }
            btns.button('reset')
        })
    }
}

var rb = rb || {}

// props = { entity, ids }
rb.AssignDialog = function(props){
    props = { ...props, type: 'assign' }
    return renderRbcomp(<TheBothDialog { ...props} />)
}
// props = { entity, ids }
rb.ShareDialog = function(props){
    props = { ...props, type: 'share' }
    return renderRbcomp(<TheBothDialog { ...props} />)
}