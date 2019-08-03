
$(document).ready(function () {
  $('.J_add').click(() => { renderRbcomp(<DlgEdit />) })
  renderRbcomp(<GridList />, 'list')
})

const WHENS = { 1: '新建', 4: '更新', 2: '删除', 16: '分派', 32: '共享', 64: '取消共享' }
const formatWhen = function (maskVal) {
  let as = []
  for (let k in WHENS) {
    // eslint-disable-next-line eqeqeq
    if ((maskVal & k) != 0) as.push(WHENS[k])
  }
  return as.join('/')
}

class GridList extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }
  render() {
    return <div className="card-list row">
      {(this.state.list || []).map((item) => {
        return (<div key={'item-' + item[0]} className="col-xl-3 col-lg-4 col-md-6">
          <div className="card">
            <div className="card-body">
              <a className="text-truncate" href={'trigger/' + item[0]}>{item[5] || (item[2] + ' · ' + item[4])}</a>
              <p className="text-muted text-truncate">{item[1] > 0 ? ('当' + formatWhen(item[1]) + '时') : <span className="text-warning">(无触发动作)</span>}</p>
            </div>
            <div className="card-footer card-footer-contrast">
              <div className="float-left">
                <a onClick={() => renderRbcomp(<DlgEdit id={item[0]} name={item[5]} isDisabled={item[6]} />)}><i className="zmdi zmdi-edit"></i></a>
                <a onClick={() => this.delete(item[0])}><i className="zmdi zmdi-delete"></i></a>
              </div>
              {item[6] && <div className="badge badge-warning">已禁用</div>}
              <div className="clearfix"></div>
            </div>
          </div>
        </div>)
      })}
      {(!this.state.list || this.state.list.length === 0) && <div className="text-muted">尚未配置任何触发器</div>}
    </div>
  }
  componentDidMount() {
    this.loadData()
  }

  loadData(entity) {
    $.get(`${rb.baseUrl}/admin/robot/trigger/list?entity=${$encode(entity)}`, (res) => {
      this.setState({ list: res.data })
      if (!this.__treeRendered) this.renderEntityTree()
    })
  }
  renderEntityTree() {
    this.__treeRendered = true
    const dest = $('.dept-tree ul')
    const ues = []
    $(this.state.list).each(function () {
      if (!ues.contains(this[3])) $('<li data-entity="' + this[3] + '"><a class="text-truncate">' + this[4] + '</a></li>').appendTo(dest)
      ues.push(this[3])
    })
    let that = this
    dest.find('li').click(function () {
      dest.find('li').removeClass('active')
      $(this).addClass('active')
      that.loadData($(this).data('entity'))
    })
  }

  delete(configId) {
    RbAlert.create('确认删除此触发器吗？', {
      type: 'danger',
      confirmText: '删除',
      confirm: function () {
        this.disabled(true)
        $.post(`${rb.baseUrl}/app/entity/record-delete?id=${configId}`, (res) => {
          if (res.error_code === 0) {
            RbHighbar.success('触发器已删除')
            setTimeout(() => { location.reload() }, 500)
          } else RbHighbar.error(res.error_msg)
        })
      }
    })
  }
}

class DlgEdit extends RbFormHandler {
  constructor(props) {
    super(props)
  }
  render() {
    return (<RbModal title={(this.props.id ? '编辑' : '添加') + '触发器'} ref={(c) => this._dlg = c} disposeOnHide={true}>
      <div className="form">
        {!this.props.id && <React.Fragment>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">触发器</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref={(c) => this._actionType = c}>
                {(this.state.actions || []).map((item) => {
                  return <option key={'o-' + item[0]} value={item[0]}>{item[1]}</option>
                })}
              </select>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">触发源实体</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref={(c) => this._sourceEntity = c}>
                {(this.state.sourceEntities || []).map((item) => {
                  return <option key={'e-' + item[0]} value={item[0]}>{item[1]}</option>
                })}
              </select>
            </div>
          </div>
        </React.Fragment>}
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">名称 (可选)</label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} />
          </div>
        </div>
        {this.props.id &&
          <div className="form-group row">
            <div className="col-sm-7 offset-sm-3">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
                <span className="custom-control-label">是否禁用</span>
              </label>
            </div>
          </div>}
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3" ref={(c) => this._btns = c}>
            <button className="btn btn-primary" type="button" onClick={this.save}>确定</button>
          </div>
        </div>
      </div>
    </RbModal>)
  }
  componentDidMount() {
    if (this.props.id) return
    this.__select2 = []
    // #1
    $.get(`${rb.baseUrl}/admin/robot/trigger/available-actions`, (res) => {
      let s2ot = null
      this.setState({ actions: res.data }, () => {
        s2ot = $(this._actionType).select2({
          placeholder: '选择触发类型',
          allowClear: false
        }).on('change', () => {
          this.__getEntitiesByAction(s2ot.val())
        })
        this.__select2.push(s2ot)

        // #2
        let s2se = $(this._sourceEntity).select2({
          placeholder: '选择源实体',
          allowClear: false
        })
        this.__select2.push(s2se)

        s2ot.trigger('change')
      })
    })
  }
  __getEntitiesByAction(type) {
    $.get(`${rb.baseUrl}/admin/robot/trigger/available-entities?action=${type}`, (res) => {
      this.setState({ sourceEntities: res.data })
    })
  }

  save = () => {
    let _data = { name: this.state['name'] || '', isDisabled: this.state.isDisabled === true }
    if (!this.props.id) {
      _data = { actionType: this.__select2[0].val(), belongEntity: this.__select2[1].val() }
      if (!_data.actionType || !_data.belongEntity) {
        RbHighbar.create('请选择源触发实体')
        return
      }
    }
    _data.metadata = { entity: 'RobotTriggerConfig', id: this.props.id || null }
    
    this.disabled(true)
        $.post(rb.baseUrl + '/app/entity/record-save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) location.reload()
        else location.href = 'trigger/' + res.data.id
      } else RbHighbar.error(res.error_msg)
      this.disabled()
    })
  }
}