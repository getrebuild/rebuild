/* eslint-disable eqeqeq */
/* eslint-disable react/jsx-no-undef */
// ~~ 自动分派
// eslint-disable-next-line
class ContentAutoAssign extends ActionContentSpec {
  constructor(props) {
    super(props)
    this.state.assignRule = 1
  }

  render() {
    return <div className="auto-assign">
      <form className="simple">
        <div className="form-group row pt-1">
          <label className="col-12 col-lg-3 col-form-label text-lg-right">分派给谁</label>
          <div className="col-12 col-lg-8">
            <UserSelectorExt ref={(c) => this._assignTo = c} />
          </div>
        </div>
        <div className="form-group row pb-1">
          <label className="col-12 col-lg-3 col-form-label text-lg-right">(多人) 分派规则</label>
          <div className="col-12 col-lg-8 pt-1">
            <label className="custom-control custom-control-sm custom-radio custom-control-inline">
              <input className="custom-control-input" name="assignRule" type="radio" checked={this.state.assignRule == 1} value="1" onChange={this.changeValue} />
              <span className="custom-control-label">依次平均分派</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio custom-control-inline">
              <input className="custom-control-input" name="assignRule" type="radio" checked={this.state.assignRule == 2} value="2" onChange={this.changeValue} />
              <span className="custom-control-label">随机分派</span>
            </label>
          </div>
        </div>
        <div className="form-group row pb-1">
          <label className="col-12 col-lg-3 col-form-label text-lg-right">同时分派关联记录</label>
          <div className="col-12 col-lg-8">
            <div className="entity-select">
              <select className="form-control form-control-sm" ref={(c) => this._cascades = c}>
                {(this.state.cascadesEntity || []).map((item) => {
                  return <option key={'option-' + item[0]} value={item[0]}>{item[1]}</option>
                })}
              </select>
            </div>
          </div>
        </div>
      </form>
    </div>
  }

  componentDidMount() {
    $('.J_when').find('.custom-control-input').each(function () {
      let v = ~~$(this).val()
      if (!(v == 1 || v == 4)) $(this).attr('disabled', true)
      // if (!(v == 1 || v == 4)) $(this).parent().remove()
    })

    if (this.props.content && this.props.content.assignTo) {
      $.post(`${rb.baseUrl}/commons/search/user-selector?entity=${this.props.sourceEntity}`, JSON.stringify(this.props.content.assignTo), (res) => {
        if (res.error_code === 0 && res.data.length > 0) this._assignTo.setState({ selected: res.data })
      })
    }

    if (this.props.content && this.props.content.assignRule == 2) this.setState({ assignRule: 2 })

    let cascades = this.props.content && this.props.content.cascades ? this.props.content.cascades.split(',') : []
    $.get(rb.baseUrl + '/commons/metadata/references?entity=' + this.props.sourceEntity, (res) => {
      this.setState({ cascadesEntity: res.data }, () => {
        this.__select2 = $(this._cascades).select2({
          multiple: true,
          placeholder: '选择关联实体 (可选)'
        }).val(cascades.length === 0 ? null : cascades).trigger('change')
      })
    })
  }

  changeValue = (e) => {
    let s = {}
    s[e.target.name] = e.target.value
    this.setState(s)
  }

  buildContent() {
    let _data = { assignTo: this._assignTo.getSelected(), assignRule: ~~this.state.assignRule, cascades: this.__select2.val().join(',') }
    if (!_data.assignTo || _data.assignTo.length === 0) { RbHighbar.create('请选择分派给谁'); return false }
    return _data
  }
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  // eslint-disable-next-line no-undef
  renderRbcomp(<ContentAutoAssign {...props} />, 'react-content', function () { contentComp = this })
}