// ~~!v1.0 表单 Dialog
class RbFormModal extends React.Component {
    constructor(props) {
        super(props);
        this.state = { ...props, inLoad: true };
    }
    render() {
        return (
            <div className="modal rbmodal colored-header colored-header-primary" ref="rbmodal">
                <div className="modal-dialog">
                    <div className="modal-content">
                        <div className="modal-header modal-header-colored">
                            <h3 className="modal-title">{this.state.title || ''}</h3>
                            <a className="close md-close" type="button" href={rb.baseUrl + '/admin/entity/' + this.props.entity + '/base'} title="实体配置"><span className="zmdi zmdi-settings"></span></a>
                            <button className="close md-close" type="button" onClick={()=>this.hide()}><span className="zmdi zmdi-close"></span></button>
                        </div>
                        <div className={'modal-body rb-loading' + (this.state.inLoad ? ' rb-loading-active' : '')}>
                            {this.state.formComponent}
                            <RbSpinner />
                        </div>
                    </div>
                </div>
            </div>
        )
    }
    componentDidMount() {
        this.show();
        
        // 渲染表单
        let that = this;
        let entity = this.props.entity;
        $.get(rb.baseUrl + '/entity/form-config?entity=' + entity, function(res){
            let elements = res.data.elements;
            const FORM = <RbForm entity={entity} $$$parent={that}>
                {elements.map((item) => {
                    return __detectElement(item)
                })}
                </RbForm>;
            that.setState({ formComponent:FORM }, function() {
                that.setState({ inLoad:false })
            })
        })
    }
    show() {
        $(this.refs['rbmodal']).modal({ show: true, backdrop: 'static' });
    }
    hide() {
        $(this.refs['rbmodal']).modal('hide');
    }
}
const __detectElement = function(item){
    if (item.type == 'TEXT'){
        return <RbFormText {...item} />
    } else if (item.type == 'URL'){
        return <RbFormUrl {...item} />
    } else if (item.type == 'IMAGE'){
        return <RbFormImage {...item} />
    } else if (item.type == 'FILE'){
        return <RbFormFile {...item} />
    } else if (item.type == 'DATETIME'){
        return <RbFormDateTime {...item} />
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
        this.__props = { entity: props.entity }
        this.state = { };
        
        this.setFieldValue = this.setFieldValue.bind(this);
        this.formData = {};
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
                <div className="form-group row footer">
                    <div className="col-12 col-sm-8 offset-sm-3">
                        <button className="btn btn-primary btn-space" type="button" onClick={()=>this.post()}>保存</button>
                        &nbsp;
                        <button className="btn btn-secondary btn-space" type="button" onClick={()=>this.props.$$$parent.hide()}>取消</button>
                    </div>
                </div>
            </form>
            </div>
        )
    }
    componentDidMount() {
        if (parent && parent.rbModal) parent.rbModal.loaded()
        $('.rb-loading-active').removeClass('rb-loading-active')
    }
    setFieldValue(field, value) {
        this.formData[field] = value;
        console.log('Set FromData ... ' + JSON.stringify(this.formData));
    }
    post() {
        console.log('Post FromData ... ' + JSON.stringify(this.formData));
        
        // TODO 验证数据???
        
        let _data = this.formData;
        _data.metadata = { entity: this.props.entity, id: this.props.id };
        //$.post(rb.baseUrl + '/entity/record-save', JSON.stringify(_data), function(res){
        //});
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
                <label className="col-12 col-sm-3 col-form-label text-sm-right">{this.props.label}{this.props.empty == false && <em/>}</label>
                <div className="col-12 col-sm-8">
                    {this.renderElement()}
                </div>
            </div>
        )
    }
    componentDidUpdate(e) {
        if (parent && parent.rbModal) parent.rbModal.loaded()
    }
    renderElement() {
        return ('请复写此方法');
    }
    handleChange(event) {
        let val = event.target.value;
        this.setState({ value: val });
        this.props.$$$parent.setFieldValue(this.props.field, val);
    }
    checkError(event) {
        let val = this.state.value;
        let el = $(event.target);
        if (!!!val && el.data('empty') == 'false') {
            el.addClass('is-invalid');
            return false;
        }
        el.removeClass('is-invalid');
        return true;
    }
}

// 文本
class RbFormText extends RbFormElement {
    constructor(props) {
        super(props);
    }
    renderElement() {
        return (
            <input ref="field-value" className="form-control form-control-sm" type="text" value={this.state.value} data-empty={this.props.empty} onChange={this.handleChange} onBlur={this.checkError} />
        )
    }
}

// 多行文本
class RbFormTextarea extends RbFormElement {
    constructor(props) {
        super(props);
    }
    renderElement() {
        return (
            <textarea ref="field-value" class="form-control form-control-sm row2x" value={this.state.value} data-empty={this.props.empty} onChange={this.handleChange} onBlur={this.checkError} />
        )
    }
}

// URL
class RbFormUrl extends RbFormText {
    constructor(props) {
        super(props);
    }
}

// 邮箱
class RbFormEMail extends RbFormText {
    constructor(props) {
        super(props);
    }
}

// 电话/手机
class RbFormPhone extends RbFormText {
    constructor(props) {
        super(props);
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
                {this.state.value.map((item, index) => {
                    return (<span><a class="img-thumbnail img-upload"><img src={rb.storageUrl + item}/><b title="移除" onClick={()=>this.removeItem(item)}><span className="zmdi zmdi-delete"></span></b></a></span>)
                })}
                <span title="选择图片">
                    <input type="file" className="inputfile" ref="upload-input" id={this.props.field + '-input'} />
                    <label for={this.props.field + '-input'} className="img-thumbnail img-upload"><span className="zmdi zmdi-image-alt"></span></label>
                </span>
                <input ref="field-value" type="hidden" value={this.state.value} />
            </div>
        )
    }
    componentDidMount() {
        let that = this;
        let mprogress;
        $(that.refs['upload-input']).html5Uploader({
            name: that.props.field,
            postUrl: __baseUrl + '/filex/upload?cloud=auto',
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
                    that.setState({ value:path })
                    that.handleChange({ target:{value:path } });
                }
                else alert(d.error_msg || '上传失败');
            }
        });
    }
    removeItem(item) {
        let path = this.state.value;
        path.remove(item);
        this.setState({ value:path })
        this.handleChange({ target:{value:path } });
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
                {this.state.value.map((item, index) => {
                    let fileName = item.split('/');
                    if (fileName.length > 1){
                        fileName = fileName[fileName.length - 1];
                        fileName = fileName.substr(15);
                    }
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
        let that = this;
        let mprogress;
        $(that.refs['upload-input']).html5Uploader({
            name: that.props.field,
            postUrl: __baseUrl + '/filex/upload?cloud=auto',
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
                    that.setState({ value:path })
                    that.handleChange({ target:{value:path } });
                }
                else alert(d.error_msg || '上传失败');
            }
        });
    }
    removeItem(item) {
        let path = this.state.value;
        path.remove(item);
        this.setState({ value:path })
        this.handleChange({ target:{value:path } });
    }
}

// 日期-时间
class RbFormDateTime extends RbFormElement {
    constructor(props) {
        super(props)
    }
    renderElement() {
        return (
            <div className="input-group datetime-field">
                <input ref="field-value" className="form-control form-control-sm" type="text" value={this.state.value} />
                <div className="input-group-append">
                    <button className="btn btn-primary" type="button" ref="field-value-icon"><i className="icon zmdi zmdi-calendar"></i></button>
                </div>
            </div>
        )
    }
    componentDidMount() {
        let that = this;
        let dtp = $(this.refs['field-value']).datetimepicker({
            componentIcon:'zmdi zmdi-calendar',
            navIcons: { rightIcon:'zmdi zmdi-chevron-right', leftIcon:'zmdi zmdi-chevron-left'},
            format: 'yyyy-mm-dd hh:ii:ss',
            weekStart: 1,
            autoclose: true,
            language: 'zh',
            todayHighlight: true,
            showMeridian: false,
            keyboardNavigation: false,
        }).on('changeDate', function(event){
            let val = $(this).val();
            that.setState({ value:val })
            that.handleChange({ target:{value:val } });
        })
        $(this.refs['field-value-icon']).click(()=>{
            dtp.datetimepicker('show');
        })
    }
}

// 引用
class RbFormReference extends RbFormElement {
    constructor(props) {
        super(props)
    }
    renderElement() {
        return (
            <select ref="field-value" className="form-control form-control-sm" value={this.state.value} onChange={this.handleChange} />
        )
    }
    componentDidMount() {
        let that = this;
        $(this.refs['field-value']).select2({
            language: 'zh-CN',
            placeholder: '选择' + that.props.label,
            allowClear: true,
            minimumInputLength: 1,
            ajax: {
                url: rb.baseUrl + '/entity/common-search',
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
            that.setState({ value:value })
            that.handleChange({ target:{value:value } });
        });
        
        //.val('').trigger('change')
    }
}