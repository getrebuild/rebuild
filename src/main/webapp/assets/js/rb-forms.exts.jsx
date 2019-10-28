// 内部用/暂未开放

// 布尔 是/否
class RbFormBool extends RbFormElement {
  constructor(props) {
    super(props)
    if (!props.onView) {
      if (props.value === true || props.value === 'true') this.state.value = 'T'
      else if (props.value === false || props.value === 'false') this.state.value = 'F'
    }
    this.changeValue = this.changeValue.bind(this)
  }
  renderElement() {
    return (
      <div className="mt-1">
        <label className="custom-control custom-control-sm custom-radio custom-control-inline">
          <input className="custom-control-input" name={'radio-' + this.props.field} type="radio" checked={this.state.value === 'T'} data-value="T" onChange={this.changeValue} />
          <span className="custom-control-label">是</span>
        </label>
        <label className="custom-control custom-control-sm custom-radio custom-control-inline">
          <input className="custom-control-input" name={'radio-' + this.props.field} type="radio" checked={this.state.value === 'F'} data-value="F" onChange={this.changeValue} />
          <span className="custom-control-label">否</span>
        </label>
      </div>
    )
  }
  changeValue(e) {
    let val = e.target.dataset.value
    this.handleChange({ target: { value: val } }, true)
  }
}

// 头像
class RbFormAvatar extends RbFormElement {
  constructor(props) {
    super(props)
  }
  renderElement() {
    let aUrl = rb.baseUrl + (this.state.value ? `/filex/img/${this.state.value}?imageView2/2/w/100/interlace/1/q/100` : '/assets/img/avatar.png')
    return (
      <div className="img-field avatar">
        <span title="选择头像图片">
          <input type="file" className="inputfile" ref={(c) => this._uploadInput = c} id={this.props.field + '-input'} accept="image/png,image/jpeg,image/gif" />
          <label htmlFor={this.props.field + '-input'} className="img-thumbnail img-upload">
            <img src={aUrl} />
          </label>
        </span>
      </div>
    )
  }
  renderViewElement() {
    let aUrl = rb.baseUrl + (this.state.value ? `/filex/img/${this.state.value}?imageView2/2/w/100/interlace/1/q/100` : '/assets/img/avatar.png')
    return (
      <div className="img-field avatar">
        <a className="img-thumbnail img-upload"><img src={aUrl} /></a>
      </div>
    )
  }
  componentDidMount() {
    super.componentDidMount()
    let that = this
    let mp
    $createUploader(this._uploadInput, function (res) {
      if (!mp) mp = new Mprogress({ template: 1, start: true })
      mp.set(res.percent / 100)  // 0.x
    }, function (res) {
      if (mp) mp.end()
      that.handleChange({ target: { value: res.key } }, true)
    })
  }
  // Not implemented
  setValue() { }
  getValue() { }
}

// 状态
// eslint-disable-next-line no-undef
class RbFormState extends RbFormPickList {
  constructor(props) {
    super(props)
  }
}
// 审批状态
// eslint-disable-next-line no-undef
class RbApprovalState extends RbFormReadonly {
  constructor(props) {
    super(props)
  }
}

// eslint-disable-next-line no-unused-vars
var detectElementExt = function (item) {
  if (item.type === 'BOOL') {
    return <RbFormBool {...item} />
  } else if (item.type === 'AVATAR') {
    return <RbFormAvatar {...item} />
  } else if (item.type === 'STATE') {
    return item.field === 'approvalState' ? <RbApprovalState {...item} /> : <RbFormState {...item} />
  }
  return null
}

// 列表渲染

if (window.CellRenders) {
  CellRenders.addRender('AVATAR', function (v, s, k) {
    let imgUrl = rb.baseUrl + '/filex/img/' + v + '?imageView2/2/w/100/interlace/1/q/100'
    return <td key={k} className="user-avatar"><img src={imgUrl} alt="Avatar" /></td>
  })
}