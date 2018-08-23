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
                        <button className="btn btn-primary btn-space" type="button">保存</button>
                    </div>
                </div>
            </form>
            </div>
        )
    }
    componentDidMount() {
        $('.rb-loading-active').removeClass('rb-loading-active');
    }
}

class RbFormElement extends React.Component {
    constructor(props) {
        super(props);
        this.state = { ...props };
    }
    render() {
        console.log('Fire : ' + JSON.stringify(this.props))
        return (
            <div className="form-group row">
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
}

class RbFormText extends RbFormElement {
    constructor(props) {
        super(props);
    }
    renderElement() {
        return (
            <input className="form-control form-control-sm" type="text" id={this.props.field} value={this.props.defaultValue} maxlength={this.props.maxLength || 200} />
        )
    }
}

class RbFormTextarea extends RbFormElement {
    constructor(props) {
        super(props);
    }
    renderElement() {
        return (
            <textarea class="form-control form-control-sm row2" id={this.props.field} maxlength={this.props.maxLength || 200}>{this.props.defaultValue}</textarea>
        )
    }
}

class RbFormImage extends RbFormElement {
    constructor(props) {
        super(props);
        this.state.imgs = [];
    }
    renderElement() {
        return (
            <div className="img-field">
                {this.state.imgs.map((item, index) => {
                    return (<span><a class="img-thumbnail img-upload" ref="upload.handle"><img src={'http://rb-cdn.errorpage.cn/' + item}/></a></span>)
                })}
                <span><a class="img-thumbnail img-upload" ref="upload.handle"><span className="zmdi zmdi-image-alt"></span></a></span>
                <input type="file" className="hide" ref="upload.input" />
            </div>
        )
    }
    componentDidMount() {
        let that = this;
        $(this.refs['upload.handle']).click(function(){
            $(that.refs['upload.input'])[0].click();
        })
        $(that.refs['upload.input']).html5Uploader({
            name: that.props.field,
            postUrl: __baseUrl + '/filex/upload?cloud=true',
            onClientLoad: function(e, file){
            },
            onSuccess:function(d){
                d = JSON.parse(d.currentTarget.response);
                if (d.error_code == 0){
                    let imgs = that.state.imgs;
                    imgs.push(d.data);
                    console.log(imgs)
                    that.setState({ imgs:imgs })
                }
                else alert(d.error_msg || '上传失败');
            }
        });
    }
}

const __h5upload = function(opt){
};
const __detectElement = function(item){
    if (item.type == 'TEXT'){
        return <RbFormText {...item} />
    } else if (item.type == 'IMAGE'){
        return <RbFormImage {...item} />
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