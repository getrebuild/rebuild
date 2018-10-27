// ~~ 表单 Dialog
class RbFormModal extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props, inLoad: true }
    }
    render() {
        return (this.state.isDestroy == true ? null :
            <div className="modal-warpper">
            <div className="modal rbmodal colored-header colored-header-primary" ref="rbmodal">
                <div className="modal-dialog">
                    <div className="modal-content">
                        <div className="modal-header modal-header-colored">
                            {this.state.icon ? (<span className={'icon zmdi zmdi-' + this.state.icon}></span>) : '' }
                            <h3 className="modal-title">{this.state.title || '新建'}</h3>
                            {rb.isAdminUser ? <a className="close s" href={rb.baseUrl + '/admin/entity/' + this.state.entity + '/form-design'} title="配置布局" target="_blank"><span className="zmdi zmdi-settings"></span></a> : null}
                            <button className="close md-close" type="button" onClick={()=>this.hide()}><span className="zmdi zmdi-close"></span></button>
                        </div>
                        <div className={'modal-body rb-loading' + (this.state.inLoad ? ' rb-loading-active' : '')}>
                            {this.state.formComponent}
                            {this.state.inLoad && <RbSpinner />}
                        </div>
                    </div>
                </div>
            </div>
            </div>
        )
    }
    componentDidMount() {
        this.show()
        if (!!!this.state.id) this.getFormModel()
    }
    
    // 渲染表单
    getFormModel() {
        let that = this
        const entity = this.state.entity
        const id = this.state.id || ''
        $.get(rb.baseUrl + '/app/' + entity + '/form-model?entity=' + entity + '&id=' + id, function(res){
            let elements = res.data.elements
            const FORM = <RbForm entity={entity} id={id} $$$parent={that}>
                {elements.map((item) => {
                    return detectElement(item)
                })}
                </RbForm>
            that.setState({ formComponent: FORM }, function() {
                that.setState({ inLoad: false })
            })
        })
    }
    
    show(state) {
        state = state || {}
        let that = this
        if (state.id != this.state.id || state.entity != this.state.entity) {
            state = { ...state, isDestroy: true, formComponent: null, inLoad: true }
            this.setState(state, function(){
                that.showAfter({ ...state, isDestroy: false }, true)
            })
        } else {
            this.showAfter({ ...state, isDestroy: false })
        }
    }
    showAfter(state, modelChanged) {
        let that = this
        this.setState(state, function(){
            $(that.refs['rbmodal']).modal({ show: true, backdrop: 'static' })
            if (modelChanged == true) {
                that.getFormModel()
            }
        })
    }
    
    hide(destroy) {
        $(this.refs['rbmodal']).modal('hide')
        let state = { isDestroy: false }
        if (destroy === true) state = { ...state, isDestroy: true, id: null }
        this.setState(state)
    }
}

// ~~ 表单
class RbForm extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props }
        
        this.__FormData = {}
        this.setFieldValue = this.setFieldValue.bind(this)
    }
    render() {
        let that = this
        return (
            <div className="rbform">
            <form>
                {this.props.children.map((child) => {
                    return React.cloneElement(child, { $$$parent: that })
                    // strict mode have error
//                    child.$$$parent = that
//                    return child
                })}
                {this.props.children.length == 0 ? this.renderFormError() : this.renderFormAction()}
            </form>
            </div>
        )
    }
        
    renderFormAction() {
        return (
            <div className="form-group row footer">
                <div className="col-12 col-sm-8 offset-sm-3" ref="rbform-action">
                    <button className="btn btn-primary btn-space" type="button" onClick={()=>this.post()}>保存</button>
                    &nbsp;
                    <button className="btn btn-secondary btn-space" type="button" onClick={()=>this.props.$$$parent.hide()}>取消</button>
                </div>
            </div>
        )
    }
    
    renderFormError(message) {
        let adminUrl = rb.baseUrl + '/admin/entity/' + this.props.entity + '/form-design'
        message = message || `布局尚未配置，请 <a href="${adminUrl}">配置</a> 后使用`
        message = { __html: '<strong>错误! </strong> ' + message }
        return <div className="alert alert-contrast alert-warning">
            <div className="icon"><span className="zmdi zmdi-alert-triangle"></span></div>
            <div className="message" dangerouslySetInnerHTML={message}></div>
        </div>
    }
    
    setFieldValue(field, value, error) {
        this.__FormData[field] = { value: value, error: error }
        console.log('Sets field-value ... ' + JSON.stringify(this.__FormData))
    }
    setFieldUnchanged(field) {
        delete this.__FormData[field]
        console.log('Unchanged field-value ... ' + JSON.stringify(this.__FormData))
    }
    post() {
        let _data = {}
        for (let k in this.__FormData) {
            let err = this.__FormData[k].error
            if (err){ rb.notice(err); return }
            else _data[k] = this.__FormData[k].value
        }
        
        _data.metadata = { entity: this.state.entity, id: this.state.id }
        if (RbForm.postBefore(_data) == false) {
            return
        }
        
        let btns = $(this.refs['rbform-action']).find('.btn').button('loading')
        let that = this
        $.post(rb.baseUrl + '/app/entity/record-save', JSON.stringify(_data), function(res){
            btns.button('reset')
            if (res.error_code == 0){
                rb.notice('保存成功', 'success')
                that.props.$$$parent.hide(true)
                RbForm.postAfter(res.data)
            }else{
                rb.notice(res.error_msg || '保存失败，请稍后重试', 'danger')
            }
        })
    }
    
    // 保存前调用
    // @return false 则不继续保存
    static postBefore(data) {
        return true
    }
    // 保存后调用
    static postAfter(data) {
        if (window.RbListPage) window.RbListPage._RbList.reload()
        else if (parent.RbListPage) parent.RbListPage._RbList.reload()
        if (window.rbFromView) location.reload()
    }
}

// 表单元素
class RbFormElement extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props }
        
        this.handleChange = this.handleChange.bind(this)
        this.checkError = this.checkError.bind(this)
    }
    
    render() {
        let colWidths = [3, 8]
        if (this.props.onView) {
            colWidths[0] = 4
            if (this.props.isFull == true) colWidths = [2, 10]
        }
        return (
            <div className={'form-group row type-' + this.props.type}>
                <label className={'col-12 col-form-label text-sm-right col-sm-' + colWidths[0]} title={this.props.nullable ? '' : '必填项'}>{this.props.label}{!this.props.nullable && <em/>}</label>
                <div className={'col-12 col-sm-' + colWidths[1]}>
                    {this.state.viewMode === true ? this.renderViewElement() : this.renderElement()}
                </div>
            </div>
        )
    }
    renderViewElement() {
        return <div className="form-control-plaintext">{this.state.value || (<span className="text-muted">无</span>)}</div>
    }
    renderElement() {
        return '子类复写此方法'
    }
    componentDidMount() {
        let props = this.props
        // 必填字段
        if (props.nullable == false && props.readonly == false && props.onView != true) {
            if (!props.value) {
                props.$$$parent.setFieldValue(props.field, null, props.label + '不能为空')
            }
        }
    }
    
    // 表单组件（字段）值变化应调用此方法
    handleChange(event, checkError) {
        let val = event.target.value
        let that = this
        this.setState({ value: val }, function(){ checkError == true && that.checkError() } )
    }
    checkError() {
        // Unchanged Uncheck
        if (this.state.value && this.props.value == this.state.value) {
            if (this.__lastValue != this.props.value) {
                this.props.$$$parent.setFieldUnchanged(this.props.field)
                this.__lastValue = this.props.value
            }
            return
        }
        if (this.__lastValue && this.__lastValue == this.state.value) {
            return
        }
        this.__lastValue = this.state.value
        
        let err = this.checkHasError()
        this.setState({ hasError: err })
        let errMsg = !!err ? (this.props.label + err) : null
        this.props.$$$parent.setFieldValue(this.props.field, this.state.value, errMsg)
    }
    checkHasError(){
        if (this.props.nullable == false) {
            return !!!this.state.value ? '不能为空' : null
        }
        return null
    }
}

// 只读字段
class RbFormReadonly extends RbFormElement {
    constructor(props) {
        super(props)
    }
    renderElement() {
        let text = this.props.value
        if (this.props.type == 'REFERENCE' && text) text = text[1]
        return <input className="form-control form-control-sm" type="text" readOnly="true" value={text} />
    }
}

// 文本
class RbFormText extends RbFormElement {
    constructor(props) {
        super(props)
    }
    renderElement() {
        return (
            <input ref="field-value" className={'form-control form-control-sm ' + (this.state.hasError ? 'is-invalid' : '')} title={this.state.hasError} type="text" value={this.state.value} onChange={this.handleChange} onBlur={this.checkError} />
        )
    }
}

// 链接
class RbFormUrl extends RbFormText {
    constructor(props) {
        super(props)
    }
    renderViewElement() {
        if (!!!this.state.value) return super.renderViewElement()
        let clickUrl = rb.baseUrl + '/commons/url-safe?url=' + encodeURIComponent(this.state.value)
        return (<div className="form-control-plaintext"><a href={clickUrl} className="link" target="_blank">{this.state.value}</a></div>)
    }
    checkHasError() {
        let err = super.checkHasError()
        if (err) return err
        return (!!this.state.value && $regex.isUrl(this.state.value) == false) ? '链接格式不正确' : null
    }
}

// 邮箱
class RbFormEMail extends RbFormText {
    constructor(props) {
        super(props)
    }
    renderViewElement() {
        if (!!!this.state.value) return super.renderViewElement()
        return (<div className="form-control-plaintext"><a href={'mailto:' + this.state.value} className="link">{this.state.value}</a></div>)
    }
    checkHasError() {
        let err = super.checkHasError()
        if (err) return err
        return (!!this.state.value && $regex.isMail(this.state.value) == false) ? '邮箱格式不正确' : null
    }
}

// 电话/手机
class RbFormPhone extends RbFormText {
    constructor(props) {
        super(props)
    }
    checkHasError() {
        let err = super.checkHasError()
        if (err) return err
        return (!!this.state.value && $regex.isTel(this.state.value) == false) ? '电话/手机格式不正确' : null
    }
}

// 整数
class RbFormNumber extends RbFormText {
    constructor(props) {
        super(props)
    }
    checkHasError() {
        let err = super.checkHasError()
        if (err) return err
        return (!!this.state.value && $regex.isNumber(this.state.value) == false) ? '整数格式不正确' : null
    }
}

// 货币
class RbFormDecimal extends RbFormText {
    constructor(props) {
        super(props)
    }
    checkHasError() {
        let err = super.checkHasError()
        if (err) return err
        return (!!this.state.value && $regex.isDecimal(this.state.value) == false) ? '货币格式不正确' : null
    }
}

// 多行文本
class RbFormTextarea extends RbFormElement {
    constructor(props) {
        super(props)
    }
    renderElement() {
        return (
            <textarea ref="field-value" className={'form-control form-control-sm row3x ' + (this.state.hasError ? 'is-invalid' : '')} title={this.state.hasError} value={this.state.value} onChange={this.handleChange} onBlur={this.checkError} />
        )
    }
}

// 日期-时间
class RbFormDateTime extends RbFormElement {
    constructor(props) {
        super(props)
        this.cleanValue = this.cleanValue.bind(this)
    }
    renderElement() {
        return (
            <div className="input-group datetime-field">
                <input ref="field-value" className={'form-control form-control-sm ' + (this.state.hasError ? 'is-invalid' : '')} title={this.state.hasError} type="text" value={this.state.value}/>
                <span className={'zmdi zmdi-close clean ' + (this.state.value ? '' : 'hide')} onClick={this.cleanValue}></span>
                <div className="input-group-append">
                    <button className="btn btn-primary" type="button" ref="field-value-icon"><i className="icon zmdi zmdi-calendar"></i></button>
                </div>
            </div>
        )
    }
    componentDidMount() {
        super.componentDidMount()
        let format = (this.props.datetimeFormat || this.props.dateFormat).replace('mm', 'ii').toLowerCase()
        let minView = 0
        switch (format.length) {
            case 7:
                minView = 'year'
                break
        	case 10:
        	    minView = 'month'
        		break
        	default:
        		break
        }
        
        let that = this
        let dtp = $(this.refs['field-value']).datetimepicker({
            componentIcon:'zmdi zmdi-calendar',
            navIcons: { rightIcon:'zmdi zmdi-chevron-right', leftIcon:'zmdi zmdi-chevron-left'},
            format: format || 'yyyy-mm-dd hh:ii:ss',
            minView: minView,
            startView: minView == 'year' ? 'year' : 'month',
            weekStart: 1,
            autoclose: true,
            language: 'zh',
            todayHighlight: true,
            showMeridian: false,
            keyboardNavigation: false,
        }).on('changeDate', function(){
            let val = $(this).val()
            that.handleChange({ target: { value: val } }, true)
        })
        $(this.refs['field-value-icon']).click(()=>{
            dtp.datetimepicker('show')
        })
    }
    cleanValue() {
        this.handleChange({ target: { value: '' } }, true)
    }
}

// 图片
class RbFormImage extends RbFormElement {
    constructor(props) {
        super(props)
        this.state.value = JSON.parse(props.value || '[]')
    }
    renderElement() {
        return (
            <div className="img-field">
                {this.state.value.map((item) => {
                    return (<span><a className="img-thumbnail img-upload"><img src={rb.storageUrl + item + '?imageView2/2/w/100/interlace/1/q/100'}/><b title="移除" onClick={()=>this.removeItem(item)}><span className="zmdi zmdi-delete"></span></b></a></span>)
                })}
                <span title="选择图片">
                    <input type="file" className="inputfile" ref="upload-input" id={this.props.field + '-input'} accept="image/*" />
                    <label htmlFor={this.props.field + '-input'} className="img-thumbnail img-upload"><span className="zmdi zmdi-image-alt"></span></label>
                </span>
                <input ref="field-value" type="hidden" value={this.state.value} />
            </div>
        )
    }
    renderViewElement() {
        return (<div className="img-field">
            {this.state.value.map((item)=>{
                return <span><a onClick={this.clickPreview.bind(this, item)} className="img-thumbnail img-upload zoom-in" href={rb.storageUrl + item} target="_blank"><img src={rb.storageUrl + item + '?imageView2/2/w/100/interlace/1/q/100'} /></a></span>
            })}
        </div>)
    }
    componentDidMount() {
        super.componentDidMount()
        let that = this
        let mprogress
        $(that.refs['upload-input']).html5Uploader({
            name: that.props.field,
            postUrl: rb.baseUrl + '/filex/upload?cloud=auto&type=image',
            onClientLoad: function(e, file){
                if (file.type.substr(0, 5) != 'image'){
                    rb.notice('请上传图片')
                    return false
                }
                mprogress = new Mprogress({ template:3 })
                mprogress.start()
            },
            onSuccess:function(d){
                if (mprogress == null) return false
                mprogress.end()
                d = JSON.parse(d.currentTarget.response)
                if (d.error_code == 0){
                    let paths = that.state.value
                    paths.push(d.data)
                    that.handleChange({ target:{ value: paths } }, true)
                } else rb.notice(d.error_msg || '上传失败，请稍后重试', 'danger')
            }
        })
    }
    removeItem(item) {
        let paths = this.state.value
        paths.remove(item)
        this.handleChange({ target:{ value: paths } }, true)
    }
    clickPreview() {
    }
}

// 文件
class RbFormFile extends RbFormElement {
    constructor(props) {
        super(props)
        this.state.value = JSON.parse(props.value || '[]')
    }
    renderElement() {
        return (
            <div className="file-field">
                {this.state.value.map((item) => {
                    let fileName = __fileCutName(item)
                    return (<div className="img-thumbnail"><i className={'ftype ' + __fileDetectingIcon(fileName)}/><span>{fileName}</span><b title="移除" onClick={()=>this.removeItem(item)}><span className="zmdi zmdi-delete"></span></b></div>)
                })}
                <div className="file-select">
                    <input type="file" className="inputfile" ref="upload-input" id={this.props.field + '-input'} />
                    <label htmlFor={this.props.field + '-input'} className="btn-secondary"><i className="zmdi zmdi-upload"></i><span>选择文件</span></label>
                </div>
                <input ref="field-value" type="hidden" value={this.state.value} />
            </div>
        )
    }
    renderViewElement() {
        return (<div className="file-field">
            {this.state.value.map((item)=>{
                let fileName = __fileCutName(item)
                return <a onClick={this.clickPreview.bind(this, item)} className="img-thumbnail" href={rb.storageUrl + item + '?attname=' + fileName} target="_blank"><i className={'ftype ' + __fileDetectingIcon(fileName)}/><span>{fileName}</span></a>
            })}
        </div>)
    }
    componentDidMount() {
        super.componentDidMount()
        let that = this
        let mprogress
        $(that.refs['upload-input']).html5Uploader({
            name: that.props.field,
            postUrl: rb.baseUrl + '/filex/upload?cloud=auto',
            onClientLoad: function(e, file){
                mprogress = new Mprogress({ template:3 })
                mprogress.start()
            },
            onSuccess:function(d){
                mprogress.end()
                d = JSON.parse(d.currentTarget.response)
                if (d.error_code == 0){
                    let paths = that.state.value
                    paths.push(d.data)
                    that.handleChange({ target:{ value: paths } }, true)
                } else rb.notice(d.error_msg || '上传失败，请稍后重试', 'danger')
            }
        })
    }
    removeItem(item) {
        let paths = this.state.value
        paths.remove(item)
        this.handleChange({ target:{ value: paths } }, true)
    }
    clickPreview() {
    }
}

// 列表
class RbFormPickList extends RbFormElement {
    constructor(props) {
        super(props)
    }
    renderElement() {
        return (
            <select ref="field-value" className="form-control form-control-sm" value={this.state.value} onChange={this.handleChange}>
            {this.props.options.map((item) => {
                return (<option value={item.id}>{item.text}</option>)
            })}
            </select>
        )
    }
    componentDidMount() {
        super.componentDidMount()
        let that = this
        let select2 = $(this.refs['field-value']).select2({
            language: 'zh-CN',
            placeholder: '选择' + that.props.label,
            allowClear: true,
        }).on('change.select2', function(e){
            let opt = e.target.value
            that.handleChange({ target:{ value: opt } }, true)
        })
        
        let val = this.state.value
        $setTimeout(function() {
            if (!!val) {
            }
            select2.trigger("change")
        }, 100)
    }
    componentWillUnmount() {
        $(this.refs['field-value']).select2('destroy')
    }
}

// 引用
class RbFormReference extends RbFormElement {
    constructor(props) {
        super(props)
    }
    renderElement() {
        return (
            <select ref="field-value" className="form-control form-control-sm" onChange={this.handleChange} multiple="multiple" />
        )
    }
    renderViewElement() {
        if (!!!this.state.value) return super.renderViewElement()
        let val = this.state.value
        return <div className="form-control-plaintext"><a ref="field-text" href={`#!/View/${val[2]}/${val[0]}`} onClick={()=>this.clickView()}>{val[1]}</a></div>
    }
    componentDidMount() {
        super.componentDidMount()
        let that = this
        let select2 = $(this.refs['field-value']).select2({
            language: 'zh-CN',
            placeholder: '选择' + that.props.label,
            width: '100%',
            allowClear: true,
            minimumInputLength: 1,
            maximumSelectionLength: 1,
            ajax: {
                url: rb.baseUrl + '/app/entity/ref-search',
                delay: 300,
                data: function(params) {
                    let query = {
                        entity: that.props.$$$parent.props.entity,
                        field: that.props.field,
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
        
        let val = this.state.value
        $setTimeout(function() {
            if (val) {
                let option = new Option(val[1], val[0], true, true)
                select2.append(option)
            }
            select2.trigger('change')
            
            select2.on('change.select2', function(e){
                that.handleChange({ target:{ value: e.target.value } }, true)
            })
        }, 100)
    }
    componentWillUnmount() {
        $(this.refs['field-value']).select2('destroy')
    }
    clickView() {
        RbViewPage.clickView($(this.refs['field-text']))
    }
}

// 分割线
class RbFormDivider extends React.Component {
    constructor(props) {
        super(props)
    }
    render() {
        let label = this.props.label || ''
        if (label == '分栏') label = null
        if (this.props.onView == true) return <div className="form-line"><fieldset>{label ? (<legend>{label}</legend>) : null}</fieldset></div>
        else return <div />  // TODO 编辑页暂无分割线
    }
}

// 确定元素类型
const detectElement = function(item){
    let isExtElement = detectElementExt(item)
    if (isExtElement != null) {
        return isExtElement
    }
    
    if (item.onView === true) {
    } else if (item.readonly == true) {
        return <RbFormReadonly {...item} />
    }
    
    if (item.type == 'TEXT'){
        return <RbFormText {...item} />
    } else if (item.type == 'NTEXT'){
        return <RbFormTextarea {...item} />
    } else if (item.type == 'URL'){
        return <RbFormUrl {...item} />
    } else if (item.type == 'EMAIL'){
        return <RbFormEMail {...item} />
    } else if (item.type == 'PHONE'){
        return <RbFormPhone {...item} />
    } else if (item.type == 'NUMBER'){
        return <RbFormNumber {...item} />
    } else if (item.type == 'DECIMAL'){
        return <RbFormDecimal {...item} />
    } else if (item.type == 'IMAGE'){
        return <RbFormImage {...item} />
    } else if (item.type == 'FILE'){
        return <RbFormFile {...item} />
    } else if (item.type == 'DATETIME' || item.type == 'DATE'){
        return <RbFormDateTime {...item} />
    } else if (item.type == 'PICKLIST'){
        return <RbFormPickList {...item} />
    } else if (item.type == 'REFERENCE'){
        return <RbFormReference {...item} />
    } else if (item.field == '$LINE$' || item.field == '$DIVIDER$'){
        return <RbFormDivider {...item} />
    } else {
        throw new Error('Unknow element : ' + JSON.stringify(item))
    }
}
var detectElementExt = function(item){
    return null
}

const __fileCutName = function(file) {
    file = file.split('/')
    file = file[file.length - 1]
    return file.substr(file.indexOf('__') + 2)
}
const __fileDetectingIcon = function(file){
    if (file.endsWith('.png') || file.endsWith('.gif') || file.endsWith('.jpg') || file.endsWith('.jpeg') || file.endsWith('.bmp')) return 'png';
    else if (file.endsWith('.doc') || file.endsWith('.docx')) return 'word';
    else if (file.endsWith('.ppt') || file.endsWith('.pptx')) return 'ppt';
    else if (file.endsWith('.xls') || file.endsWith('.xlsx')) return 'excel';
    else if (file.endsWith('.pdf')) return 'pdf';
    else if (file.endsWith('.mp4') || file.endsWith('.rmvb') || file.endsWith('.rm') || file.endsWith('.avi') || file.endsWith('.flv')) return 'mp4';
    return ''
}

// -- for View

//~~ 右侧滑出视图窗口
class RbViewModal extends React.Component {
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
                            {rb.isAdminUser ? <a className="close s" href={rb.baseUrl + '/admin/entity/' + this.state.entity + '/form-design'} title="配置布局" target="_blank"><span className="zmdi zmdi-settings"></span></a> : null}
                            <button className="close" type="button" onClick={()=>this.hide()}><span className="zmdi zmdi-close"></span></button>
                        </div>
                        <div className={'modal-body iframe rb-loading ' + (this.state.inLoad == true && 'rb-loading-active')}>
                            <iframe src={this.state.showAfterUrl || 'about:blank'} frameBorder="0" scrolling="no"></iframe>
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
            $setTimeout(function(){
                that.resizeModal()
                that.resize() 
            }, 200, 'rbview-resizeModal')
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
        $setTimeout(function(){
            root.find('.modal-body').height($(window).height())
        }, 40, 'RbView-resize')
    }
    resizeModal() {
        let root = $(this.refs['rbview'])
        root.find('.modal-content').css('min-height', $(window).height())
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

// -- Usage

let rb = rb || {}

let RbFormModal_Comp
// props = { id, entity, title, icon }
rb.RbFormModal = function(props) {
    if (RbFormModal_Comp) RbFormModal_Comp.show(props)
    else RbFormModal_Comp = renderRbcomp(<RbFormModal {...props} />)
    return RbFormModal_Comp
}

let RbViewModal_Comp
// props = { id, entity }
rb.RbViewModal = function(props) {
    let viewUrl = `${rb.baseUrl}/app/${props.entity}/view/${props.id}`
    if (RbViewModal_Comp) RbViewModal_Comp.show(viewUrl, { entity: props.entity })
    else RbViewModal_Comp = renderRbcomp(<RbViewModal url={viewUrl} entity={props.entity} />)
    return RbViewModal_Comp
}
