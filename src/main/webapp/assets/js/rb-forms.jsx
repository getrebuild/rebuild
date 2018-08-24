// ~~!v1.0 表单
class RbForm extends React.Component {
    constructor(props) {
        super(props);
        this.__props = { entity:props.entity }
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
                    child.props.setFieldValue = that.setFieldValue;
                    child.props.parentProps = that.__props;
                    return child;
                })}
                <div className="form-group row footer">
                    <div className="col-12 col-sm-8 col-lg-4 offset-sm-3">
                        <button className="btn btn-primary btn-space" type="button" onClick={()=>this.post()}>保存</button>
                        &nbsp;
                        <button className="btn btn-secondary btn-space" type="button" onClick={()=>location.reload(true)}>重置</button>
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
    }
    post() {
        console.log('Post FromData ... ' + JSON.stringify(this.formData));
        
        // TODO 验证数据
        
        let _data = this.formData;
        _data.metadata = { entity: this.props.entity, id: this.props.id };
        $.post(rb.baseUrl + '/entity/record-save', JSON.stringify(_data), function(res){
        });
    }
}

class RbFormElement extends React.Component {
    constructor(props) {
        super(props);
        this.state = { ...props };
        
        this.handleChange = this.handleChange.bind(this);
    }
    render() {
        return (
            <div className={'form-group row type-' + this.props.type}>
                <label className="col-12 col-sm-3 col-form-label text-sm-right">{this.props.label}</label>
                <div className="col-12 col-sm-8 col-lg-4">
                    {this.renderElement()}
                </div>
            </div>
        )
    }
    componentDidUpdate(e) {
        this.props.setFieldValue(this.props.field, this.state.value);
        if (parent && parent.rbModal) parent.rbModal.loaded()
    }
    renderElement() {
        return ('请复写此方法');
    }
    handleChange(event) {
        this.setState({ value: event.target.value });
    }
    valid(v) {
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
            <input ref="field.value" className="form-control form-control-sm" type="text" id={this.props.field} value={this.state.value} maxlength={this.props.maxLength || 200} onChange={this.handleChange} />
        )
    }
}

// 文本域
class RbFormTextarea extends RbFormElement {
    constructor(props) {
        super(props);
    }
    renderElement() {
        return (
            <textarea ref="field.value" class="form-control form-control-sm row2" id={this.props.field} value={this.state.value} maxlength={this.props.maxLength || 200} onChange={this.handleChange} />
        )
    }
}

// 图片
class RbFormImage extends RbFormElement {
    constructor(props) {
        super(props);
        this.state.imgsPath = [];
    }
    renderElement() {
        return (
            <div className="img-field">
                {this.state.imgsPath.map((item, index) => {
                    return (<span><a class="img-thumbnail img-upload"><img src={rb.storageUrl + item}/><b title="删除" onClick={()=>this.removeItem()}><span className="zmdi zmdi-delete"></span></b></a></span>)
                })}
                <span>
                    <input type="file" className="inputfile" ref="upload.input" id={this.props.field + '-input'} />
                    <label for={this.props.field + '-input'} className="img-thumbnail img-upload"><span className="zmdi zmdi-image-alt"></span></label>
                </span>
            </div>
        )
    }
    componentDidMount() {
        let that = this;
        $(that.refs['upload.input']).html5Uploader({
            name: that.props.field,
            postUrl: __baseUrl + '/filex/upload?cloud=auto',
            onClientLoad: function(e, file){
            },
            onSuccess:function(d){
                d = JSON.parse(d.currentTarget.response);
                if (d.error_code == 0){
                    let imgsPath = that.state.imgsPath;
                    imgsPath.push(d.data);
                    that.setState({ imgsPath:imgsPath })
                }
                else alert(d.error_msg || '上传失败');
            }
        });
    }
    getValue() {
        return this.state.imgsPath.join(',');
    }
    removeItem(e) {
    }
}

// 文件
class RbFormFile extends RbFormElement {
    constructor(props) {
        super(props);
        this.state.filesPath = [];
    }
    renderElement() {
        return (
            <div className="file-field">
                {this.state.filesPath.map((item, index) => {
                    return (<div className="img-thumbnail"><i className="zmdi zmdi-file"></i><span>{item}</span><b title="删除" onClick={()=>this.removeItem()}><span className="zmdi zmdi-delete"></span></b></div>)
                })}
                <div className="file-select">
                    <input type="file" className="inputfile" ref="upload.input" id={this.props.field + '-input'} />
                    <label for={this.props.field + '-input'} className="btn-secondary"><i className="zmdi zmdi-upload"></i><span>选择文件</span></label>
                </div>
            </div>
        )
    }
    componentDidMount() {
        let that = this;
        $(that.refs['upload.input']).html5Uploader({
            name: that.props.field,
            postUrl: __baseUrl + '/filex/upload?cloud=auto',
            onClientLoad: function(e, file){
            },
            onSuccess:function(d){
                d = JSON.parse(d.currentTarget.response);
                if (d.error_code == 0){
                    let filesPath = that.state.filesPath;
                    filesPath.push(d.data);
                    that.setState({ filesPath:filesPath })
                }
                else alert(d.error_msg || '上传失败');
            }
        });
    }
    getValue() {
        return this.state.filesPath.join(',');
    }
    removeItem(e) {
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
                <input ref="field.value" className="form-control form-control-sm" type="text" id={this.props.field} value={this.props.defaultValue} />
                <div className="input-group-append">
                    <button className="btn btn-primary" type="button"><i className="icon zmdi zmdi-calendar"></i></button>
                </div>
            </div>
        )
    }
    componentDidMount() {
        $(this.refs['field.value']).datetimepicker({
            componentIcon:'zmdi zmdi-calendar',
            navIcons: { rightIcon:'zmdi zmdi-chevron-right', leftIcon:'zmdi zmdi-chevron-left'},
            format: 'yyyy-mm-dd hh:ii:ss',
            weekStart: 1,
            autoclose: true,
            language: 'zh',
            todayHighlight: true,
            showMeridian: false
        })
    }
    getValue() {
    }
    removeItem(e) {
    }
}

// 引用
class RbFormReference extends RbFormElement {
    constructor(props) {
        super(props)
    }
    renderElement() {
        return (
            <select ref="field.value" className="form-control form-control-sm" id={this.props.field} value={this.state.value} />
        )
    }
    componentDidMount() {
        let that = this;
        $(this.refs['field.value']).select2({
            language: 'zh-CN',
            placeholder: '选择' + this.props.label,
            allowClear: true,
            minimumInputLength: 1,
            ajax: {
                url: rb.baseUrl + '/entity/common-search',
                delay: 300,
                data: function(params) {
                    let query = {
                        search: params.term,
                        entity: that.props.parentProps.entity,
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
        }).val('').trigger('change')
    }
    getValue() {
    }
    removeItem(e) {
    }
}

const __detectElement = function(item){
    if (item.type == 'TEXT'){
        return <RbFormText {...item} />
    } else if (item.type == 'URL'){
        item.regexp = '';
        return <RbFormText {...item} />
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
const renderRbform = function(config) {
    const form = <RbForm entity={config.entity}>
        {config.elements.map((item, index) => {
            return (__detectElement(item))
        })}
        </RbForm>;
    ReactDOM.render(form, document.getElementById('form-container'));
    return form;
};