$(document).ready(function () {
  let sbtn = $('.btn-primary').click(function () {
    let entityLabel = $val('#entityLabel'),
      comments = $val('#comments')
    if (!entityLabel) {
      rb.highbar('请输入实体名称')
      return
    }
    let _data = { label: entityLabel, comments: comments }
    if ($val('#isSlave') === 'true') {
      _data.masterEntity = $val('#masterEntity')
      if (!_data.masterEntity) {
        rb.highbar('请选择选择主实体')
        return
      }
    }
    _data = JSON.stringify(_data)

    sbtn.button('loading')
    $.post(rb.baseUrl + '/admin/entity/entity-new?nameField=' + $val('#nameField'), _data, function (res) {
      if (res.error_code === 0) parent.location.href = rb.baseUrl + '/admin/entity/' + res.data + '/base'
      else rb.hberror(res.error_msg)
      sbtn.button('reset')
    })
  })

  let entitiesLoaded = false
  $('#isSlave').click(function () {
    $('.J_masterEntity').toggleClass('hide')
    parent.rb.modalResize()
    if (entitiesLoaded === false) {
      entitiesLoaded = true
      $.get(rb.baseUrl + '/commons/metadata/entities', function (res) {
        $(res.data).each(function () {
          $('<option value="' + this.name + '">' + this.label + '</option>').appendTo('#masterEntity')
        })
      })
    }
  })

  $('.nav-tabs a').click(() => { parent.rb.modalResize() })

  let indexLoaded = false
  $('.J_imports').click(() => {
    if (indexLoaded) return
    renderRbcomp(<MetaschemaList />, 'metaschemas')
    indexLoaded = true
  })
})

class MetaschemaList extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }
  render() {
    return <div>
      {this.state.indexes ? <div className="rbs-indexes">{this.state.indexes.map((item) => {
        return (<div key={'data-' + item.file}>
          <div className="float-left">
            <h5>{item.name}</h5>
            <div className="text-muted">
              数据来源 <a target="_blank" rel="noopener noreferrer" href={item.source}>{item.author || item.source}</a>
              {item.updated && (' · ' + item.updated)}
            </div>
          </div>
          <div className="float-right pt-1">
            <button disabled={this.state.inProgress === true} className="btn btn-sm btn-primary" data-file={item.file} data-name={item.name} onClick={this.imports}>导入</button>
          </div>
          <div className="clearfix"></div>
        </div>)
      })}</div>
        : <RbSpinner fully={true} />}
    </div>
  }
  componentDidMount() {
    $.get(`${rb.baseUrl}/admin/rbstore/load-index?type=metaschemas`, (res) => {
      if (res.error_code === 0) this.setState({ indexes: res.data }, () => { parent.rb.modalResize() })
      else rb.hberror(res.error_msg)
    })
  }

  imports = (e) => {
    let file = e.currentTarget.dataset.file
    let name = e.currentTarget.dataset.name
    let url = `${rb.baseUrl}/admin/metaschema/imports?file=${$encode(file)}`
    let that = this
    rb.alert(`<strong>${name}</strong><br>你可在导入后进行适当调整。立即开始导入吗？`, {
      html: true,
      confirm: function () {
        this.hide()
        that.setState({ inProgress: true })
        let mpro = new Mprogress({ template: 3, start: true })
        $.post(url, (res) => {
          mpro.end()
          if (res.error_code === 0) {
            rb.hbsuccess('导入完成')
            setTimeout(() => { parent.location.href = `entity/${res.data}/base` }, 1500)
          } else rb.hberror(res.error_msg || '导入失败')
        })
      }
    })
  }
}