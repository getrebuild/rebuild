//~~ 视图
class RbViewForm extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props }
    }
    render() {
        let that = this
        return (<div className="rbview-form">
             {this.state.formComponent}
        </div>)
    }
    componentDidMount() {
        let that = this
        $.get(rb.baseUrl + '/app/' + this.props.entity + '/view-modal?id=' + this.props.id, function(res){
            let elements = res.data.elements
            const FORM = <div class="row">{elements.map((item) => {
                return __detectViewElement(item)
            })}</div>
            that.setState({ formComponent: FORM }, function(){
                $('.invisible').removeClass('invisible')
                if (parent && parent.rbViewModal) {
                    parent.rbViewModal.hideLoading(true)
                }
            })
        });
    }
}

const __detectViewElement = function(item){
    if (item.field == '$LINE$'){
        return <div className="col-12"><div className="card-header-divider">{item.label}</div></div>
    } else if (item.type == 'REFERENCE'){
        return <RbViewFormReference {...item} />
    } else if (item.type == 'URL' || item.type == 'EMAIL'){
        return <RbViewFormLink {...item} />
    } else if (item.type == 'IMAGE'){
        return <RbViewFormImage {...item} />
    } else if (item.type == 'FILE'){
        return <RbViewFormFile {...item} />
    } else {
        return <RbViewFormElement {...item} />
    }
}

// 表单元素父级
class RbViewFormElement extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props }
    }
    render() {
        const isFull = this.props.isFull == true
        return (
        <div className={'col-12 col-sm-' + (isFull ? 12 : 6)}>
        <div className="form-group row">
            <label className={'col-form-label text-sm-right col-sm-' + (isFull ? 2 : 4)}>{this.props.label}</label>
            <div className={'col-sm-' + (isFull ? 10 : 8)}>
                {this.state.value ? this.renderElement() : (<div className="form-control-plaintext text-muted">无</div>)}
            </div>
        </div></div>)
    }
    componentDidMount(e) {
    }
    renderElement() {
        return (<div className="form-control-plaintext">{this.state.value}</div>)
    }
}

class RbViewFormReference extends RbViewFormElement {
    constructor(props) {
        super(props)
    }
    renderElement() {
        return (<div className="form-control-plaintext"><a href="javascript:;" onClick={()=>this.clickView()}>{this.state.value[1]}</a></div>)
    }
    clickView() {
        console.log(this.state.value)
    }
}

class RbViewFormLink extends RbViewFormElement {
    constructor(props) {
        super(props)
    }
    renderElement() {
        let link = this.state.value;
        if (this.props.type == 'EMAIL') link = 'mailto:' + link
        else link = rb.baseUrl + '/common/url-safe?url=' + encodeURIComponent(link)
        return (<div className="form-control-plaintext"><a href={link} className="link" target={this.props.type == 'EMAIL' ? '_self' : '_blank'}>{this.state.value}</a></div>)
    }
    clickView() {
        console.log(this.state.value)
    }
}

class RbViewFormImage extends RbViewFormElement {
    constructor(props) {
        super(props)
    }
    renderElement() {
        let imgs = JSON.parse(this.state.value || '[]')
        return (<div className="img-field">
            {imgs.map((item)=>{
                return <span><a onClick={this.previewImage.bind(this, item)} className="img-thumbnail img-upload zoom-in" href={rb.storageUrl + item} target="_blank"><img src={rb.storageUrl + item + '?imageView2/2/w/100/interlace/1/q/100'} /></a></span>
            })}
        </div>)
    }
    previewImage(item) {
        console.log(item + ' > ' + this.state.value)
    }
}

class RbViewFormFile extends RbViewFormElement {
    constructor(props) {
        super(props)
    }
    renderElement() {
        let imgs = JSON.parse(this.state.value || '[]')
        return (<div className="file-field">
            {imgs.map((item)=>{
                let fname = __fileCutName(item)
                return <a onClick={this.previewFile.bind(this, item)} className="img-thumbnail" href={rb.storageUrl + item + '?attname=' + fname} target="_blank"><i className={'type ' + __fileDetectingIcon(fname)}></i><span>{fname}</span></a>
            })}
        </div>)
    }
    previewFile(item) {
        console.log(item + ' > ' + this.state.value)
    }
}
