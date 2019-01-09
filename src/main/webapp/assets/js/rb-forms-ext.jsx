// 布尔 是/否
class RbFormBool extends RbFormElement {
    constructor(props) {
        super(props)
        this.state.value = props.value || 'F'
        this.changeValue = this.changeValue.bind(this)
    }
    renderElement() {
        return (
            <div className="mt-1">
                <label className="custom-control custom-control-sm custom-radio custom-control-inline">
                    <input className="custom-control-input" name={'radio-' + this.props.field} type="radio" checked={this.state.value == 'T'} data-value="T" onChange={this.changeValue} />
                    <span className="custom-control-label">是</span>
                </label>
                <label className="custom-control custom-control-sm custom-radio custom-control-inline">
                    <input className="custom-control-input" name={'radio-' + this.props.field} type="radio" checked={this.state.value == 'F'} data-value="F" onChange={this.changeValue} />
                    <span className="custom-control-label">否</span>
                </label>
            </div>
        )
    }
    changeValue(e) {
        let val = e.target.dataset.value
        this.handleChange({ target:{ value: val } }, true)
    }
}

// 头像
class RbFormAvatar extends RbFormElement {
    constructor(props) {
        super(props)
    }
    renderElement() {
        let aUrl = rb.baseUrl + (this.state.value ? `/cloud/img/${this.state.value}?imageView2/2/w/100/interlace/1/q/100` :  '/assets/img/avatar.png')
        return (
            <div className="img-field avatar">
                <span title="选择头像图片">
                    <input type="file" className="inputfile" ref="upload-input" id={this.props.field + '-input'} accept="image/*" />
                    <label htmlFor={this.props.field + '-input'} className="img-thumbnail img-upload">
                        <img src={aUrl}/>
                    </label>
                </span>
            </div>
        )
    }
    renderViewElement() {
        let aUrl = rb.baseUrl + (this.state.value ? `/cloud/img/${this.state.value}?imageView2/2/w/100/interlace/1/q/100` :  '/assets/img/avatar.png')
        return (
            <div className="img-field avatar">
                <a className="img-thumbnail img-upload"><img src={aUrl}/></a>
            </div>
        )
    }
    componentDidMount() {
        super.componentDidMount()
        let that = this
        $(that.refs['upload-input']).html5Uploader({
            name: that.props.field,
            postUrl: rb.baseUrl + '/filex/upload?cloud=auto&type=image',
            onClientLoad: function(e, file){
                if (file.type.substr(0, 5) != 'image'){
                    rb.highbar('请上传图片')
                    return false
                }
            },
            onSuccess:function(d){
                d = JSON.parse(d.currentTarget.response)
                if (d.error_code == 0){
                    that.handleChange({ target:{ value: d.data } }, true)
                } else rb.hberror(d.error_msg || '上传失败，请稍后重试')
            }
        })
    }
}

detectElementExt = function(item) {
    if (item.field == 'avatarUrl') {
        return <RbFormAvatar {...item} />
    } else if (item.type == 'BOOL'){
        return <RbFormBool {...item} />
    }
    return null
}