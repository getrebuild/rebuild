const wcp = window.__PageConfig
$(document).ready(() => {
  $('.J_add-rule').click(() => { renderRbcomp(<DlgRuleEdit />) })
})

class DlgRuleEdit extends RbFormHandler {
  constructor(props) {
    super(props)
    this.state = { ...this.state, whenCreate: true, updateIfnull: true }
  }

  render() {
    return (<RbModal title="回填规则" ref={(c) => this._dlg = c}>
      <div className="form">
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">源字段</label>
          <div className="col-sm-7">
            <select className="form-control form-control-sm" ref={(c) => this._sourceField = c}></select>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">目标字段</label>
          <div className="col-sm-7">
            <select className="form-control form-control-sm" ref={(c) => this._targetField = c}></select>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right pt-1">何时回填</label>
          <div className="col-sm-7">
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" checked={this.state.whenCreate === true} data-id="whenCreate" onChange={this.handleChange} />
              <span className="custom-control-label">新建时</span>
            </label>
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" checked={this.state.whenUpdate === true} data-id="whenUpdate" onChange={this.handleChange} />
              <span className="custom-control-label">更新时</span>
            </label>
          </div>
        </div>
        <div className="form-group row pt-1">
          <label className="col-sm-3 col-form-label text-sm-right pt-1">当目标字段值非空</label>
          <div className="col-sm-7">
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" checked={this.state.updateIfnull === true} data-id="updateIfnull" onChange={this.handleChange} />
              <span className="custom-control-label">保留原值</span>
            </label>
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" checked={this.state.updateForce === true} data-id="updateForce" onChange={this.handleChange} />
              <span className="custom-control-label">仍旧回填</span>
            </label>
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3">
            <button className="btn btn-primary" type="button" onClick={this.save}>确定</button>
          </div>
        </div>
      </div>
    </RbModal>)
  }

}