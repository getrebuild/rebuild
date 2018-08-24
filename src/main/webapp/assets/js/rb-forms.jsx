// ~~!v1.0 表单
class RbForm extends React.Component {
    constructor(props) {
        super(props);
        this.state = { ...props };
    }
    render() {
        return (
            <div className="rb-form">
            <form>
                {this.props.children}
                <div className="form-group row footer">
                    <div className="col-12 col-sm-8 col-lg-4 offset-sm-3">
                        <button className="btn btn-primary btn-space" type="button" onClick={()=>this.post()}>保存</button>
                        <button className="btn btn-secondary btn-space" type="button" onClick={()=>location.reload()}>取消</button>
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
    post() {
        let vals = React.Children.map(this.props.children, function(child) {
            console.log(child)
            if (child) return child.getValue()
        })
        console.log(vals)
    }
}

class RbFormElement extends React.Component {
    constructor(props) {
        super(props);
        this.state = { ...props };
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
    renderElement() {
        return ('请复写此方法');
    }
    componentDidUpdate() {
        console.log('fire componentDidUpdate');
        if (parent && parent.rbModal) parent.rbModal.loaded()
    }
    getValue() {
        let v = $val(this.refs['field.value'])
        if (this.validate(v) == true) return v;
        else throw new Error('无效值:' + this.props.field);
    }
    validate(v) {
        return true;
    }
}

class RbFormText extends RbFormElement {
    constructor(props) {
        super(props);
    }
    renderElement() {
        return (
            <input ref="field.value" className="form-control form-control-sm" type="text" id={this.props.field} value={this.props.defaultValue} maxlength={this.props.maxLength || 200} />
        )
    }
}

class RbFormTextarea extends RbFormElement {
    constructor(props) {
        super(props);
    }
    renderElement() {
        return (
            <textarea ref="field.value" class="form-control form-control-sm row2" id={this.props.field} maxlength={this.props.maxLength || 200}>{this.props.defaultValue}</textarea>
        )
    }
}

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

const __detectElement = function(item){
    if (item.type == 'TEXT'){
        return <RbFormText {...item} />
    } else if (item.type == 'IMAGE'){
        return <RbFormImage {...item} />
    } else if (item.type == 'FILE'){
        return <RbFormFile {...item} />
    } else {
        console.error('Unknow element : ' + JSON.stringify(item))
    }
};
const renderRbform = function(config) {
    const form = <RbForm>
        {config.elements.map((item, index) => {
            return (__detectElement(item))
        })}
        </RbForm>;
    ReactDOM.render(form, document.getElementById('form-container'));
    return form;
};