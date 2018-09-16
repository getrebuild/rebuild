// ~~!v1.0 表单 Dialog
class RbFormModal extends React.Component {
    constructor(props) {
        super(props);
        this.state = { ...props, inLoad: true };
        
        this.showNotice = this.showNotice.bind(this)
        this.destroy = this.destroy.bind(this)
    }
    render() {
        return (
            <div className="modal-warpper" ref="rbmodal-warpper">
            <div className={'modal rbmodal colored-header colored-header-primary'} ref="rbmodal">
                <div className="modal-dialog">
                    <div className="modal-content">
                        <div className="modal-header modal-header-colored">
                            <h3 className="modal-title">{this.state.title || 'TITLE'}</h3>
                            <a className="close md-close admin-settings admin-visibility" href={rb.baseUrl + '/admin/entity/' + this.props.entity + '/form-design'} title="配置布局" target="_blank"><span className="zmdi zmdi-settings"></span></a>
                            <button className="close md-close" type="button" onClick={()=>this.hide()}><span className="zmdi zmdi-close"></span></button>
                        </div>
                        <div className={'modal-body rb-loading' + (this.state.inLoad ? ' rb-loading-active' : '')}>
                            {this.state.formComponent}
                            {this.state.inLoad && <RbSpinner />}
                        </div>
                    </div>
                </div>
            </div>
            {this.state.noticeMessage && <RbNotice autoClose="false" message={this.state.noticeMessage} type={this.state.noticeType} />}
            </div>
        )
    }
    componentDidMount() {
        this.show();
        
        // 渲染表单
        let that = this;
        let entity = this.props.entity;
        $.get(rb.baseUrl + '/app/entity/form-config?entity=' + entity, function(res){
            let elements = res.data.elements;
            const FORM = <RbForm entity={entity} $$$parent={that}>
                {elements.map((item) => {
                    return __detectElement(item)
                })}
                </RbForm>;
            that.setState({ formComponent: FORM }, function() {
                that.setState({ inLoad:false })
            })
        })
    }
    show() {
        $(this.refs['rbmodal']).modal({ show: true, backdrop: 'static' });
    }
    hide() {
        $(this.refs['rbmodal']).modal('hide')
        this.setState({ noticeMessage: null })
    }
    destroy() {
        this.hide()
        $(this.refs['rbmodal-warpper']).remove()
    }
    showNotice(message, type) {
        let that = this;
        that.setState({ noticeMessage: message, noticeType: type || 'warning'  }, function(){
            $setTimeout(function(){
                that.setState({ noticeMessage: null })
            }, 3000, 'rbform-notice');
        })
    }
}
const __detectElement = function(item){
    if (item.creatable == false){
        return <RbFormReadonly {...item} />
    } else if (item.type == 'TEXT'){
        return <RbFormText {...item} />
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
    } else if (item.type == 'DATETIME'){
        return <RbFormDateTime {...item} />
    } else if (item.type == 'PICKLIST'){
        return <RbFormPickList {...item} />
    } else if (item.type == 'REFERENCE'){
        return <RbFormReference {...item} />
    } else {
        console.error('Unknow element : ' + JSON.stringify(item))
    }
};

// ~~!v1.0 表单
class RbForm extends React.Component {
    constructor(props) {
        super(props);
        this.state = { ...props };
        
        this.__FormData = {};
        this.setFieldValue = this.setFieldValue.bind(this);
        this.showNotice = this.showNotice.bind(this);
    }
    render() {
        let that = this;
        return (
            <div className="rb-form">
            <form>
                {this.props.children.map((child) => {
                    child.props.$$$parent = that;
                    return child;
                })}
                {this.props.children.length == 0 ? this.__renderFormError() :
                    (<div className="form-group row footer">
                        <div className="col-12 col-sm-8 offset-sm-3" ref="rbform-action">
                            <button className="btn btn-primary btn-space" type="button" onClick={()=>this.post()}>保存</button>
                            &nbsp;
                            <button className="btn btn-secondary btn-space" type="button" onClick={()=>this.props.$$$parent.hide()}>取消</button>
                        </div>
                    </div>)
                }
            </form>
            </div>
        )
    }
    __renderFormError(message) {
        let fdUrl = rb.baseUrl + '/admin/entity/' + this.props.entity + '/form-design';
        message = message || `表单未配置，请 <a href="${fdUrl}">配置</a> 后使用`;
        message = { __html: '<strong>错误!</strong> ' + message };
        return <div class="alert alert-contrast alert-warning">
            <div class="icon"><span class="zmdi zmdi-alert-triangle"></span></div>
            <div class="message" dangerouslySetInnerHTML={message}></div>
        </div>;
    }
    componentDidMount() {
    }
    setFieldValue(field, value, error) {
        this.__FormData[field] = { value:value, error:error };
        console.log('Set data ... ' + JSON.stringify(this.__FormData));
    }
    showNotice(message, type) {
        this.props.$$$parent.showNotice(message, type);
    }
    post() {
        console.log('Post data ... ' + JSON.stringify(this.__FormData));
        // Check Error
        let _data = {};
        for (let k in this.__FormData) {
            let err = this.__FormData[k].error;
            if (err){ this.showNotice(err); return; }
            else _data[k] = this.__FormData[k].value;
        }
        
        _data.metadata = { entity: this.props.entity, id: this.props.id };
        
        let actions = $(this.refs['rbform-action']).find('.btn').button('loading');
        let that = this;
        $.post(rb.baseUrl + '/app/entity/record-save', JSON.stringify(_data), function(res){
            actions.button('reset');
            if (res.error_code == 0){
                rb.notice('保存成功，<a>点击查看</a>');
                that.props.$$$parent.destroy()
            }else{
                that.showNotice(res.error_msg || '保存失败，请稍后重试');
            }
        });
    }
}

// 表单元素父级
class RbFormElement extends React.Component {
    constructor(props) {
        super(props);
        this.state = { ...props };
        
        this.handleChange = this.handleChange.bind(this);
        this.checkError = this.checkError.bind(this);
    }
    render() {
        return (
            <div className={'form-group row type-' + this.props.type}>
                <label className="col-12 col-sm-3 col-form-label text-sm-right" title={this.props.nullable ? '' : '必填项'}>{this.props.label}{!this.props.nullable && <em/>}</label>
                <div className="col-12 col-sm-8">
                    {this.renderElement()}
                </div>
            </div>
        )
    }
    componentDidMount(e) {
        if (this.props.nullable == false && this.props.creatable == true) {
            this.props.$$$parent.setFieldValue(this.props.field, null, this.props.label + '不能为空');
        }
    }
    renderElement() {
        return '子类复写此方法';
    }
    handleChange(event, fireCheckError) {
        let val = event.target.value;
        let that = this;
        this.setState({ value: val }, fireCheckError && function(){ that.checkError() });
    }
    checkError() {
        let err = this.checkHasError();
        this.setState({ hasError: err });
        let errTips = !!err ? (this.props.label + err) : null;
        this.props.$$$parent.setFieldValue(this.props.field, this.state.value, errTips);
    }
    checkHasError(){
        if (this.props.nullable == false) {
            return !!!this.state.value ? '不能为空' : null;
        }
        return null;
    }
}

// 只读字段
class RbFormReadonly extends RbFormElement {
    constructor(props) {
        super(props);
    }
    renderElement() {
        return (
            <input className="form-control form-control-sm" type="text" readonly="true" value={this.props.value} />
        )
    }
}

// 文本
class RbFormText extends RbFormElement {
    constructor(props) {
        super(props);
    }
    renderElement() {
        return (
            <input ref="field-value" className={'form-control form-control-sm ' + (this.state.hasError ? 'is-invalid' : '')} title={this.state.hasError} type="text" value={this.state.value} data-nullable={this.props.nullable} onChange={this.handleChange} onBlur={this.checkError} />
        )
    }
}

// 链接
class RbFormUrl extends RbFormText {
    constructor(props) {
        super(props);
    }
    checkHasError() {
        let err = super.checkHasError();
        if (err) return err;
        let val = this.state.value;
        return (!!val && $regex.isUrl(val) == false) ? '链接格式不正确' : null;
    }
}

// 邮箱
class RbFormEMail extends RbFormText {
    constructor(props) {
        super(props);
    }
    checkHasError() {
        let err = super.checkHasError();
        if (err) return err;
        let val = this.state.value;
        return (!!val && $regex.isMail(val) == false) ? '邮箱格式不正确' : null;
    }
}

// 电话/手机
class RbFormPhone extends RbFormText {
    constructor(props) {
        super(props);
    }
    checkHasError() {
        let err = super.checkHasError();
        if (err) return err;
        let val = this.state.value;
        return (!!val && $regex.isTel(val) == false) ? '电话/手机格式不正确' : null;
    }
}

// 整数
class RbFormNumber extends RbFormText {
    constructor(props) {
        super(props);
    }
    checkHasError() {
        let err = super.checkHasError();
        if (err) return err;
        let val = this.state.value;
        return (!!val && $regex.isNumber(val) == false) ? '整数格式不正确' : null;
    }
}

// 货币
class RbFormDecimal extends RbFormText {
    constructor(props) {
        super(props);
    }
    checkHasError() {
        let err = super.checkHasError();
        if (err) return err;
        let val = this.state.value;
        return (!!val && $regex.isDecimal(val) == false) ? '货币格式不正确' : null;
    }
}

// 多行文本
class RbFormTextarea extends RbFormElement {
    constructor(props) {
        super(props);
    }
    renderElement() {
        return (
            <textarea ref="field-value" className={'form-control form-control-sm row2x ' + (this.state.hasError ? 'is-invalid' : '')} title={this.state.hasError} value={this.state.value} data-nullable={this.props.nullable} onChange={this.handleChange} onBlur={this.checkError} />
        )
    }
}

// 日期-时间
class RbFormDateTime extends RbFormElement {
    constructor(props) {
        super(props)
        this.cleanValue = this.cleanValue.bind(this);
    }
    renderElement() {
        return (
            <div className="input-group datetime-field">
                <input ref="field-value" className={'form-control form-control-sm ' + (this.state.hasError ? 'is-invalid' : '')} title={this.state.hasError} type="text" value={this.state.value} data-nullable={this.props.nullable} />
                <span className={'zmdi zmdi-close clean ' + (this.state.value ? '' : 'hide')} onClick={this.cleanValue}></span>
                <div className="input-group-append">
                    <button className="btn btn-primary" type="button" ref="field-value-icon"><i className="icon zmdi zmdi-calendar"></i></button>
                </div>
            </div>
        )
    }
    componentDidMount() {
        super.componentDidMount()
        let format = (this.props.dateFormat || 'yyyy-MM-dd HH:mm:ss').replace('mm', 'ii').toLowerCase();
        let minView = 0;
        switch (format.length) {
            case 7:
                minView = 'year';
                break;
        	case 10:
        	    minView = 'month';
        		break;
        	default:
        		break;
        }
        
        let that = this;
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
        }).on('changeDate', function(event){
            let val = $(this).val();
            that.handleChange({ target: { value:val } }, true);
        });
        $(this.refs['field-value-icon']).click(()=>{
            dtp.datetimepicker('show');
        })
    }
    cleanValue() {
        this.handleChange({ target: { value:'' } }, true);
    }
}

// 图片
class RbFormImage extends RbFormElement {
    constructor(props) {
        super(props);
        this.state.value = [];
    }
    renderElement() {
        return (
            <div className="img-field">
                {this.state.value.map((item) => {
                    return (<span><a class="img-thumbnail img-upload"><img src={rb.storageUrl + item}/><b title="移除" onClick={()=>this.removeItem(item)}><span className="zmdi zmdi-delete"></span></b></a></span>)
                })}
                <span title="选择图片">
                    <input type="file" className="inputfile" ref="upload-input" id={this.props.field + '-input'} accept="image/*" />
                    <label for={this.props.field + '-input'} className="img-thumbnail img-upload"><span className="zmdi zmdi-image-alt"></span></label>
                </span>
                <input ref="field-value" type="hidden" value={this.state.value} />
            </div>
        )
    }
    componentDidMount() {
        super.componentDidMount()
        let that = this;
        let mprogress;
        $(that.refs['upload-input']).html5Uploader({
            name: that.props.field,
            postUrl: rb.baseUrl + '/filex/upload?cloud=auto&type=image',
            onClientLoad: function(e, file){
                if (file.type.substr(0, 5) != 'image'){
                    that.props.$$$parent.showNotice('请上传图片');
                    return false;
                }
                mprogress = new Mprogress({ template:3 });
                mprogress.start()
            },
            onSuccess:function(d){
                if (mprogress == null) return false;
                mprogress.end();
                d = JSON.parse(d.currentTarget.response);
                if (d.error_code == 0){
                    let path = that.state.value;
                    path.push(d.data);
                    that.handleChange({ target:{ value:path } }, true);
                } else that.showNitice(d.error_msg || '上传失败，请稍后重试');
            }
        });
    }
    removeItem(item) {
        let path = this.state.value;
        path.remove(item);
        this.handleChange({ target:{ value:path } }, true);
    }
}

// 文件
class RbFormFile extends RbFormElement {
    constructor(props) {
        super(props);
        this.state.value = [];
    }
    renderElement() {
        return (
            <div className="file-field">
                {this.state.value.map((item) => {
                    let fileName = item.split('/');
                    if (fileName.length > 1) fileName = fileName[fileName.length - 1];
                    fileName = fileName.substr(15);
                    return (<div className="img-thumbnail"><i className="zmdi zmdi-file"></i><span>{fileName}</span><b title="移除" onClick={()=>this.removeItem(item)}><span className="zmdi zmdi-delete"></span></b></div>)
                })}
                <div className="file-select">
                    <input type="file" className="inputfile" ref="upload-input" id={this.props.field + '-input'} />
                    <label for={this.props.field + '-input'} className="btn-secondary"><i className="zmdi zmdi-upload"></i><span>选择文件</span></label>
                </div>
                <input ref="field-value" type="hidden" value={this.state.value} />
            </div>
        )
    }
    componentDidMount() {
        super.componentDidMount()
        let that = this;
        let mprogress;
        $(that.refs['upload-input']).html5Uploader({
            name: that.props.field,
            postUrl: rb.baseUrl + '/filex/upload?cloud=auto',
            onClientLoad: function(e, file){
                mprogress = new Mprogress({ template:3 });
                mprogress.start()
            },
            onSuccess:function(d){
                mprogress.end();
                d = JSON.parse(d.currentTarget.response);
                if (d.error_code == 0){
                    let path = that.state.value;
                    path.push(d.data);
                    that.handleChange({ target:{ value:path } }, true);
                } else that.showNitice(d.error_msg || '上传失败，请稍后重试');
            }
        });
    }
    removeItem(item) {
        let path = this.state.value;
        path.remove(item);
        this.handleChange({ target:{ value:path } }, true);
    }
}

// 列表
class RbFormPickList extends RbFormElement {
    constructor(props) {
        super(props)
        
        let options = this.props.options
        for (let i = 0; i < options.length; i++){
            if (options[i]['default'] === true) {
                this.state.value = options[i]['id'];
                break;
            }
        }
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
        let that = this;
        $(this.refs['field-value']).select2({
            language: 'zh-CN',
            placeholder: '选择' + that.props.label,
            allowClear: true,
        }).on('change.select2', function(e){
            let value = e.target.value;
            that.handleChange({ target:{ value:value } }, true);
        });
    }
}

// 引用
class RbFormReference extends RbFormElement {
    constructor(props) {
        super(props)
    }
    renderElement() {
        return (
            <select ref="field-value" className="form-control form-control-sm" value={this.state.value} onChange={this.handleChange} multiple="multiple" />
        )
    }
    componentDidMount() {
        super.componentDidMount()
        let that = this;
        $(this.refs['field-value']).select2({
            language: 'zh-CN',
            placeholder: '选择' + that.props.label,
            allowClear: true,
            minimumInputLength: 1,
            ajax: {
                url: rb.baseUrl + '/app/common/search',
                delay: 300,
                data: function(params) {
                    let query = {
                        search: params.term,
                        entity: that.props.$$$parent.props.entity,
                    };
                    return query;
                },
                processResults: function(data){
                    let rs = data.data.map((item) => {
                        return item;
                    });
                    return {
                        results: rs
                    };
                }
            }
        }).on('change.select2', function(e){
            let value = e.target.value;
            that.handleChange({ target:{ value:value } }, true);
        });
    }
}