/* eslint-disable react/no-string-refs */
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

// eslint-disable-next-line no-unused-vars
var detectElementExt = function (item) {
  if (item.type === 'BOOL') {
    return <RbFormBool {...item} />
  }
  return null
}