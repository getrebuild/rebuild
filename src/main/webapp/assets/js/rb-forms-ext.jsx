// 布尔 是/否
class RbFormBool extends RbFormElement {
    constructor(props) {
        super(props)
        this.state.value = props.value || 'F'
        if (this.props.onView === true) {
        } else {
            if (props.value == '是' || props.value == '否') {
                this.state.value = props.value == '是' ? 'T' : 'F'
            }
        }
    }
    renderElement() {
        return (
            <div>
                <label className="custom-control custom-radio custom-control-inline">
                    <input className="custom-control-input" name={'radio-' + this.props.field} type="radio" checked={this.state.value == 'T'} onChange={this.changeValue.bind(this, 'T')} />
                    <span className="custom-control-label">是</span>
                </label>
                <label className="custom-control custom-radio custom-control-inline">
                    <input className="custom-control-input" name={'radio-' + this.props.field} type="radio" checked={this.state.value == 'F'} onChange={this.changeValue.bind(this, 'F')} />
                    <span className="custom-control-label">否</span>
                </label>
            </div>
        )
    }
    changeValue(val) {
        this.setState({ value: val })
    }
}

// 头像
class RbFormAvatar extends RbFormElement {
    constructor(props) {
        super(props)
    }
    renderElement() {
        return (
            <div className="img-field avatar">
                <span title="选择头像图片">
                    <input type="file" className="inputfile" ref="upload-input" id={this.props.field + '-input'} accept="image/*" />
                    <label htmlFor={this.props.field + '-input'} className="img-thumbnail img-upload">
                        <img src={rb.storageUrl + (this.state.value || 'rebuild/20181010/041046550__avatar.png') + '?imageView2/2/w/100/interlace/1/q/100'}/>
                    </label>
                </span>
            </div>
        )
    }
    renderViewElement() {
        let avatarUrl = rb.storageUrl + (this.props.value || 'rebuild/20181010/041046550__avatar.png') + '?imageView2/2/w/100/interlace/1/q/100'
        return (
            <div className="img-field avatar">
                <a className="img-thumbnail img-upload">
                    <img src={rb.storageUrl + (this.state.value || 'rebuild/20181010/041046550__avatar.png') + '?imageView2/2/w/100/interlace/1/q/100'}/>
                </a>
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
                    rb.notice('请上传图片')
                    return false
                }
            },
            onSuccess:function(d){
                d = JSON.parse(d.currentTarget.response)
                if (d.error_code == 0){
                    that.handleChange({ target:{ value: d.data } }, true)
                } else rb.notice(d.error_msg || '上传失败，请稍后重试', 'danger')
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