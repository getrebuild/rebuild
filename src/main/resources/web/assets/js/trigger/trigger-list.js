/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(function () {
  $('.J_add').click(() => renderRbcomp(<TriggerEdit />))
  renderRbcomp(<TriggerList />, 'dataList')
})

const WHENS = {
  1: $lang('Create'),
  2: $lang('Delete'),
  4: $lang('Update'),
  16: $lang('Assign'),
  32: $lang('Share'),
  64: $lang('UnShare'),
  128: $lang('Approved'),
  256: $lang('Revoked'),
  512: `(${$lang('JobExecution')})`,
}

const formatWhen = function (maskVal) {
  const as = []
  for (let k in WHENS) {
    // eslint-disable-next-line eqeqeq
    if ((maskVal & k) !== 0) as.push(WHENS[k])
  }
  return as.join('/')
}

class TriggerList extends ConfigList {
  constructor(props) {
    super(props)
    this.requestUrl = '/admin/robot/trigger/list'
  }

  render() {
    return (
      <React.Fragment>
        {(this.state.data || []).map((item) => {
          return (
            <tr key={'k-' + item[0]}>
              <td>
                <a href={`trigger/${item[0]}`}>{item[3] || item[2] + ' · ' + item[7]}</a>
              </td>
              <td>{item[2] || item[1]}</td>
              <td>{item[6] > 0 ? $lang('WhenXTime').replace('%s', formatWhen(item[6])) : <span className="text-warning">({$lang('NoTriggerAction')})</span>}</td>
              <td>{item[4] ? <span className="badge badge-warning font-weight-light">{$lang('False')}</span> : <span className="badge badge-success font-weight-light">{$lang('True')}</span>}</td>
              <td>{item[5]}</td>
              <td className="actions">
                <a className="icon" title={$lang('Modify')} onClick={() => this.handleEdit(item)}>
                  <i className="zmdi zmdi-edit" />
                </a>
                <a className="icon danger-hover" title={$lang('Delete')} onClick={() => this.handleDelete(item[0])}>
                  <i className="zmdi zmdi-delete" />
                </a>
              </td>
            </tr>
          )
        })}
      </React.Fragment>
    )
  }

  handleEdit(item) {
    renderRbcomp(<TriggerEdit id={item[0]} name={item[3]} isDisabled={item[4]} />)
  }

  handleDelete(id) {
    const handle = super.handleDelete
    RbAlert.create($lang('DeleteSomeConfirm,Trigger'), {
      type: 'danger',
      confirmText: $lang('Delete'),
      confirm: function () {
        this.disabled(true)
        handle(id)
      },
    })
  }
}

class TriggerEdit extends ConfigFormDlg {
  constructor(props) {
    super(props)
    this.subtitle = $lang('Trigger')
  }

  renderFrom() {
    return (
      <React.Fragment>
        {!this.props.id && (
          <React.Fragment>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$lang('SelectSome,Trigger')}</label>
              <div className="col-sm-7">
                <select className="form-control form-control-sm" ref={(c) => (this._actionType = c)}>
                  {(this.state.actions || []).map((item) => {
                    return (
                      <option key={'o-' + item[0]} value={item[0]}>
                        {item[1]}
                      </option>
                    )
                  })}
                </select>
              </div>
            </div>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$lang('SourceEntity')}</label>
              <div className="col-sm-7">
                <select className="form-control form-control-sm" ref={(c) => (this._sourceEntity = c)}>
                  {(this.state.sourceEntities || []).map((item) => {
                    return (
                      <option key={'e-' + item[0]} value={item[0]}>
                        {item[1]}
                      </option>
                    )
                  })}
                </select>
              </div>
            </div>
          </React.Fragment>
        )}
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">
            {$lang('Name')} ({$lang('Optional')})
          </label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} placeholder={$lang('Unname')} />
          </div>
        </div>
        {this.props.id && (
          <div className="form-group row">
            <div className="col-sm-7 offset-sm-3">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
                <span className="custom-control-label">{$lang('IsDisable')}</span>
              </label>
            </div>
          </div>
        )}
      </React.Fragment>
    )
  }

  componentDidMount() {
    if (this.props.id) return

    this.__select2 = []
    // #1
    $.get('/admin/robot/trigger/available-actions', (res) => {
      this.setState({ actions: res.data }, () => {
        const s2ot = $(this._actionType)
          .select2({
            placeholder: $lang('SelectSome,TriggerType'),
            allowClear: false,
          })
          .on('change', () => {
            this.__getEntitiesByAction(s2ot.val())
          })
        this.__select2.push(s2ot)

        // #2
        const s2se = $(this._sourceEntity).select2({
          placeholder: $lang('SelectSome,SourceEntity'),
          allowClear: false,
        })
        this.__select2.push(s2se)

        s2ot.trigger('change')
      })
    })
  }

  __getEntitiesByAction(type) {
    $.get(`/admin/robot/trigger/available-entities?action=${type}`, (res) => {
      this.setState({ sourceEntities: res.data })
    })
  }

  confirm = () => {
    let data = { name: this.state['name'] || '' }
    if (this.props.id) {
      data.isDisabled = this.state.isDisabled === true
    } else {
      data = { ...data, actionType: this.__select2[0].val(), belongEntity: this.__select2[1].val() }
      if (!data.actionType || !data.belongEntity) {
        RbHighbar.create($lang('PlsSelectSome,SourceEntity'))
        return
      }
    }
    data.metadata = {
      entity: 'RobotTriggerConfig',
      id: this.props.id || null,
    }

    this.disabled(true)
    $.post('/app/entity/record-save', JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) location.reload()
        else location.href = 'trigger/' + res.data.id
      } else RbHighbar.error(res.error_msg)
      this.disabled()
    })
  }
}
