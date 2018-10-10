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
                <label className="custom-control custom-radio custom-control-inline" onClick={this.changeValue.bind(this, 'T')}>
                    <input className="custom-control-input" name="radio-inline" type="radio" value="F" checked={this.state.value == 'T'}/><span className="custom-control-label">是</span>
                </label>
                <label className="custom-control custom-radio custom-control-inline" onClick={this.changeValue.bind(this, 'F')}>
                    <input className="custom-control-input" name="radio-inline" type="radio" value="T" checked={this.state.value == 'F'}/><span className="custom-control-label">否</span>
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
        let avatarUrl = rb.storageUrl + (this.state.value || 'rebuild/20181010/041046550__avatar.png') + '?imageView2/2/w/100/interlace/1/q/100'
        return (
            <div className="img-field avatar">
                <span title="选择头像图片">
                    <input type="file" className="inputfile" ref="upload-input" id={this.props.field + '-input'} accept="image/*" />
                    <label for={this.props.field + '-input'} class="img-thumbnail img-upload"><img src={avatarUrl}/></label>
                </span>
            </div>)
    }
    componentDidMount() {
        super.componentDidMount()
        let that = this
        $(that.refs['upload-input']).html5Uploader({
            name: that.props.field,
            postUrl: rb.baseUrl + '/filex/upload?cloud=auto&type=image',
            onClientLoad: function(e, file){
                if (file.type.substr(0, 5) != 'image'){
                    that.props.$$$parent.showNotice('请上传图片')
                    return false
                }
            },
            onSuccess:function(d){
                d = JSON.parse(d.currentTarget.response)
                if (d.error_code == 0){
                    that.handleChange({ target:{ value: d.data } }, false)
                } else that.showNotice(d.error_msg || '上传失败，请稍后重试')
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