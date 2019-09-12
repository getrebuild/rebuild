
$(document).ready(function () {
  $('.J_add').click(() => { renderRbcomp(<TriggerEdit />) })
  renderRbcomp(<TriggerList />, 'dataList')
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

class TriggerList extends ConfigList {
  constructor(props) {
    super(props)
    this.requestUrl = `${rb.baseUrl}/admin/robot/trigger/list`
  }
  render() {
    return <React.Fragment>
      {(this.state.data || []).map((item) => {
        return <tr key={'k-' + item[0]}>
          <td><a href={`trigger/${item[0]}`}>{item[3] || (item[2] + ' · ' + item[7])}</a></td>
          <td>{item[2] || item[1]}</td>
          <td>{item[6] > 0 ? ('当' + formatWhen(item[6]) + '时') : <span className="text-warning">(无触发动作)</span>}</td>
          <td>{item[4] ? <span className="badge badge-warning font-weight-light">否</span> : <span className="badge badge-success font-weight-light">是</span>}</td>
          <td>{item[5]}</td>
          <td className="actions">
            <a className="icon" title="修改" onClick={() => this.handleEdit(item)}><i className="zmdi zmdi-edit" /></a>
            <a className="icon" title="删除" onClick={() => this.handleDelete(item[0])}><i className="zmdi zmdi-delete" /></a>
          </td>
        </tr>
      })}
    </React.Fragment>
  }

  handleEdit(item) {
    renderRbcomp(<TriggerEdit id={item[0]} name={item[3]} isDisabled={item[4]} />)
  }
  handleDelete(id) {
    let handle = super.handleDelete
    RbAlert.create('确认删除此触发器吗？', {
      type: 'danger',
      confirmText: '删除',
      confirm: function () {
        this.disabled(true)
        handle(id)
      }
    })
  }
}

class TriggerEdit extends ConfigFormDlg {
  constructor(props) {
    super(props)
    this.subtitle = '触发器'
  }
  renderFrom() {
    return <React.Fragment>
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
      </React.Fragment>
      }
      <div className="form-group row">
        <label className="col-sm-3 col-form-label text-sm-right">名称 (可选)</label>
        <div className="col-sm-7">
          <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} />
        </div>
      </div>
      {this.props.id && <div className="form-group row">
        <div className="col-sm-7 offset-sm-3">
          <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
            <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
            <span className="custom-control-label">是否禁用</span>
          </label>
        </div>
      </div>
      }
    </React.Fragment>
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

  confirm = () => {
    let post = { name: this.state['name'] || '' }
    if (this.props.id) {
      post.isDisabled = this.state.isDisabled === true
    } else {
      post = { actionType: this.__select2[0].val(), belongEntity: this.__select2[1].val() }
      if (!post.actionType || !post.belongEntity) {
        RbHighbar.create('请选择源触发实体')
        return
      }
    }
    post.metadata = { entity: 'RobotTriggerConfig', id: this.props.id || null }

    this.disabled(true)
    $.post(rb.baseUrl + '/app/entity/record-save', JSON.stringify(post), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) location.reload()
        else location.href = 'trigger/' + res.data.id
      } else RbHighbar.error(res.error_msg)
      this.disabled()
    })
  }
}