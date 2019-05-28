
$(document).ready(function () {
  $('.J_add').click(() => { renderRbcomp(<DlgEdit />) })
  renderRbcomp(<GridList />, 'list')
})

const WHENS = { 1: '创建', 4: '更新', 2: '删除', 16: '分派', 32: '共享', 64: '取消共享' }
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
    this.state = { list: [] }
  }
  render() {
    return <div className="card-list row">
      {this.state.list.map((item) => {
        return (<div key={'item-' + item[0]} className="col-xl-3 col-lg-4 col-md-6">
          <div className="card">
            <div className="card-body">
              <a className="text-truncate" href={'trigger/' + item[0]}>{item[3] + ' · ' + item[1]}</a>
              <p className="text-muted text-truncate">{item[2] > 0 ? ('当' + formatWhen(item[2]) + '时') : <span className="text-warning">未生效</span>}</p>
            </div>
            <div className="card-footer card-footer-contrast">
              <div className="float-left">
                <a onClick={() => this.delete(item[0])}><i className="zmdi zmdi-delete"></i></a>
              </div>
              <div className="clearfix"></div>
            </div>
          </div>
        </div>)
      })}
    </div>
  }
  componentDidMount() {
    this.loadData()
  }

  loadData(entity) {
    $.get(`${rb.baseUrl}/admin/robot/trigger/list?entity=${$encode(entity)}`, (res) => {
      this.setState({ list: res.data })
      if (!this.__entityLoaded) this.renderEntityTree()
    })
  }
  renderEntityTree() {
    const ues = []
    $(this.state.list).each(function () {
      ues.push(this[4])
    })
    let that = this
    let dest = $('.dept-tree ul')
    $.get(`${rb.baseUrl}/commons/metadata/entities?slave=true`, (res) => {
      this.__entityLoaded = true
      $(res.data).each(function () {
        if (ues.contains(this.name)) {
          $('<li data-entity="' + this.name + '"><a class="text-truncate">' + this.label + '</a></li>').appendTo(dest)
        }
      })
      dest.find('li').click(function () {
        dest.find('li').removeClass('active')
        $(this).addClass('active')
        that.loadData($(this).data('entity'))
      })
    })
  }

  delete(configId) {
    rb.alert('确认要删除此触发器？', {
      type: 'danger',
      confirm: function () {
        $.post('./trigger/delete?id=' + configId, (res) => {
          if (res.error_code === 0) location.reload()
          else rb.hberror(res.error_msg)
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
    return (<RbModal title="添加触发器" ref={(c) => this._dlg = c}>
      <div className="form">
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">触发类型</label>
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
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3" ref={(c) => this._btns = c}>
            <button className="btn btn-primary" type="button" onClick={this.save}>确定</button>
          </div>
        </div>
      </div>
    </RbModal>)
  }
  componentDidMount() {
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

  save = (e) => {
    e.preventDefault()
    let _data = { actionType: this.__select2[0].val(), belongEntity: this.__select2[1].val() }
    if (!_data.actionType || !_data.belongEntity) {
      rb.hignbar('请选择源触发实体')
      return
    }
    _data.metadata = { entity: 'RobotTriggerConfig', id: this.props.id || null }

    let _btns = $(this._btns).find('.btn').button('loading')
    $.post(rb.baseUrl + '/admin/robot/trigger/save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) location.reload()
        else location.href = 'trigger/' + res.data.id
      } else rb.hberror(res.error_msg)
      _btns.button('reset')
    })
  }
}