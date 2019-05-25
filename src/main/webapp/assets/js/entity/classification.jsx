$(document).ready(function () {
  $('.J_add').click(() => {
    renderRbcomp(<DlgEdit />)
  })

  $('.class-list .card').each(function () {
    let $this = $(this)
    let id = $this.data('id')

    $this.find('.J_del').click(() => {
      rb.alert('确认删除？<br>删除前请确认此分类数据未被使用。', {
        html: true,
        type: 'danger',
        confirmText: '删除',
        confirm: function () {
          this.disabled(true)
          $.post(`${rb.baseUrl}/admin/classification/delete?id=${id}`, (res) => {
            if (res.error_code === 0) {
              this.hide()
              $this.animate({ opacity: 0 }, 600, () => {
                $this.parent().remove()
              })
              rb.hbsuccess('分类数据已删除')
            } else rb.hberror(res.error_msg)
          })
        }
      })
    })

    $this.find('.J_edit').click(() => {
      renderRbcomp(<DlgEdit id={id} isDisabled={$this.data('disabled')} name={$this.find('.card-body>a').text()} />)
    })
  })
})

class DlgEdit extends RbFormHandler {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }
  render() {
    return (<RbModal title={(this.props.id ? '编辑' : '添加') + '分类'} ref={(c) => this._dlg = c}>
      <div className="form">
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">分类名称</label>
          <div className="col-sm-7">
            <input className="form-control form-control-sm" value={this.state.name || ''} data-id="name" onChange={this.handleChange} maxLength="40" />
          </div>
        </div>
        {this.props.id &&
          <div className="form-group row">
            <div className="col-sm-7 offset-sm-3">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
                <span className="custom-control-label">是否禁用 (禁用不影响已有数据)</span>
              </label>
            </div>
          </div>
        }
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3">
            <button className="btn btn-primary" type="button" onClick={this.save}>确定</button>
          </div>
        </div>
      </div>
    </RbModal>)
  }
  save = (e) => {
    e.preventDefault()
    if (!this.state.name) { rb.highbar('请输入名称'); return }
    let _data = { name: this.state.name, isDisabled: this.state.isDisabled === true }
    _data.metadata = { entity: 'Classification', id: this.props.id || null }
    $.post(rb.baseUrl + '/admin/classification/save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) location.reload()
        else location.href = 'classification/' + res.data.id
      } else rb.hberror(res.error_msg)
    })
  }
}