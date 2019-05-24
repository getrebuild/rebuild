/* eslint-disable react/prop-types */
const wpc = window.__PageConfig
$(document).ready(() => {
  let contentComp
  if (wpc.operatorType === 'COUNTSSLAVE') {
    contentComp = renderRbcomp(<ContentCountsSlave sourceEntity={wpc.sourceEntity} />, 'react-content')
  }

  let advFilter
  $('.J_whenFilter .btn').click(() => {
    if (advFilter) advFilter.show()
    else advFilter = renderRbcomp(<AdvFilter title="设置过滤条件" entity={wpc.sourceEntity} inModal={true} confirm={saveFilter} canNoFilters={true} />)
  })

  $('.J_save').click(() => {
    let when = 0
    $('.J_when input:checked').each(function () {
      when += ~~$(this).val()
    })
    alert(when)

    let content = contentComp.buildContent()
    if (content === false) return
  })
})

const saveFilter = function (res) {
  wpc.whenFilter = res
}

// 明细汇总
class ContentCountsSlave extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }
  render() {
    return <div>
      <div className="row counts-slave">
        <div className="col-5">
          <p>源字段 (明细实体)</p>
          <select className="form-control form-control-sm" ref={(c) => this._sourceField = c}>
            {(this.state.sourceFields || []).map((item) => {
              return <option key={'so-' + item[0]} value={item[0]}>{item[1]}</option>
            })}
          </select>
        </div>
        <div className="col-2 pl-0 pr-0">
          <p>计算方式</p>
          <select className="form-control form-control-sm" ref={(c) => this._calcMode = c}>
            <option value="SUM">求和</option>
            <option value="COUNT">计数</option>
            <option value="AVG">平均值</option>
            <option value="MAX">最大值</option>
            <option value="MIN">最小值</option>
          </select>
        </div>
        <div className="col-5">
          <p>目标字段 (主实体)</p>
          <select className="form-control form-control-sm" ref={(c) => this._targetField = c}>
            {(this.state.targetFields || []).map((item) => {
              return <option key={'to-' + item[0]} value={item[0]}>{item[1]}</option>
            })}
          </select>
        </div>
      </div>
    </div>
  }
  componentDidMount() {
    $.get(`${rb.baseUrl}/admin/robot/trigger/counts-slave-fields?sourceEntity=${this.props.sourceEntity}`, (res) => {
      this.setState({ sourceFields: res.data.slave, targetFields: res.data.master }, () => {
        $(this._sourceField).select2({
          placeholder: '选择源字段',
          allowClear: false
        })
        $(this._calcMode).select2({
          allowClear: false
        })
        $(this._targetField).select2({
          placeholder: '选择目标字段',
          allowClear: false
        })
      })
    })
  }
  buildContent() {
    return false
  }
}