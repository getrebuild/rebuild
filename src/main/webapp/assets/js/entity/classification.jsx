$(document).ready(function () {
  $('.J_add').click(() => {
    renderRbcomp(<DlgAddOne />)
  })
})

class DlgAddOne extends RbFormHandler {
  constructor(props) {
    super(props)
  }
  render() {
    return (<RbModal title="添加分类数据" ref="dlg">
      <form onSubmit={this.save}>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">名称</label>
          <div className="col-sm-7">
            <input className="form-control form-control-sm" value={this.state.name || ''} data-id="name" onChange={this.handleChange} maxLength="40" />
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3">
            <button className="btn btn-primary" type="submit">确定</button>
          </div>
        </div>
      </form>
    </RbModal>)
  }
  save = (e) => {
    e.preventDefault()
    if (!this.state.name) { rb.highbar('请输入名称'); return }
    let _data = { name: this.state.name }
    _data.metadata = { entity: 'Classification' }
    $.post(rb.baseUrl + '/app/entity/record-save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        location.href = 'classification/' + res.data.id
      } else rb.hberror(res.error_msg)
    })
  }
}