
$(document).ready(function () {
  $('.J_add').click(() => { renderRbcomp(<ReporEdit title="添加报表模板" />) })
  renderRbcomp(<ReportList />, 'dataList')
})

class ReportList extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    return <React.Fragment>
      {(this.state.list || []).map((item) => {
        return <tr key={'api-' + item[0]}>
          <td>{item[1]}</td>
          <td>{item[3] || item[2]}</td>
          <td>{item[4] ? '否' : '是'}</td>
          <td>{item[5]}</td>
          <td className="actions">
            <a className="icon" onClick={() => this.delete(item[0])}><i className="zmdi zmdi-settings" /></a>
            <a className="icon" onClick={() => this.delete(item[0])}><i className="zmdi zmdi-delete" /></a>
          </td>
        </tr>
      })}
    </React.Fragment>
  }

  componentDidMount() {
    this.loadData()
  }

  loadData(entity) {
    $.get(`${rb.baseUrl}/admin/datas/data-reports/list?entity=${entity || ''}`, (res) => {
      this.setState({ list: res.data || [] }, () => {
        $('.rb-loading-active').removeClass('rb-loading-active')
        $('.dataTables_info').text(`共 ${this.state.list.length} 项`)

        if (this.state.list.length === 0) $('.list-nodata').removeClass('hide')
        else $('.list-nodata').addClass('hide')

        if (!this.__treeRendered) this.renderEntityTree()
      })
    })
  }

  renderEntityTree() {
    this.__treeRendered = true
    const dest = $('.dept-tree ul')
    const ues = []
    $(this.state.list).each(function () {
      if (!ues.contains(this[2])) $('<li data-entity="' + this[2] + '"><a class="text-truncate">' + this[3] + '</a></li>').appendTo(dest)
      ues.push(this[2])
    })
    $('<li data-entity="$DISABLED$"><a class="text-truncate">已禁用的</a></li>').appendTo(dest)

    let that = this
    dest.find('li').click(function () {
      dest.find('li').removeClass('active')
      $(this).addClass('active')
      that.loadData($(this).data('entity'))
    })
  }
}

class ReporEdit extends LiteFormDlg {
  constructor(props) {
    super(props)
  }

  renderFrom() {
    return <React.Fragment>
      <div className="form-group row">
        <label className="col-sm-3 col-form-label text-sm-right">报表名称</label>
        <div className="col-sm-7">
          <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} />
        </div>
      </div>
      <div className="form-group row">
        <label className="col-sm-3 col-form-label text-sm-right">选择应用实体</label>
        <div className="col-sm-7">
          <select className="form-control form-control-sm" ref={(c) => this._entity = c}>
            {(this.state.entities || []).map((item) => {
              return <option key={'e-' + item.name} value={item.name}>{item.label}</option>
            })}
          </select>
        </div>
      </div>
      <div className="form-group row">
        <label className="col-sm-3 col-form-label text-sm-right">模板文件</label>
        <div className="col-sm-7">
          <div className="file-select">
            <input type="file" className="inputfile" id="upload-input" accept=".xlsx,.xls" data-maxsize="5000000" ref={(c) => this.__upload = c} />
            <label htmlFor="upload-input" className="btn-secondary"><i className="zmdi zmdi-upload"></i><span>选择文件</span></label>
          </div>
          {this.state.uploadFileName && <div className="text-bold">{this.state.uploadFileName}</div>}
        </div>
      </div>
    </React.Fragment>
  }

  componentDidMount() {
    super.componentDidMount()

    let that = this
    $(this.__upload).html5Uploader({
      postUrl: rb.baseUrl + '/filex/upload?temp=yes',
      onSelectError: function (field, error) {
        if (error === 'ErrorType') RbHighbar.create('请上传 Excel 文件')
        else if (error === 'ErrorMaxSize') RbHighbar.create('文件不能大于 5M')
      },
      onSuccess: function (d) {
        d = JSON.parse(d.currentTarget.response)
        if (d.error_code === 0) {
          that.setState({
            templateFile: d.data,
            uploadFileName: $fileCutName(d.data)
          })
        } else RbHighbar.error('上传失败，请稍后重试')
      }
    })
  }

  confirm = () => {
    let post = { name: this.state['name'] }
    if (!post.name) { RbHighbar.create('请输入报表名称'); return }
    post.belongEntity = this.__select2.val()
    if (!post.belongEntity) { RbHighbar.create('请选择应用实体'); return }
    post.templateFile = this.state.templateFile
    if (!post.templateFile) { RbHighbar.create('请上传模板文件'); return }
    post.metadata = { entity: 'DataReportConfig' }

    this.disabled(true)
    $.post(rb.baseUrl + '/app/entity/record-save', JSON.stringify(post), (res) => {
      if (res.error_code === 0) location.reload()
      else RbHighbar.error(res.error_msg)
      this.disabled()
    })
  }
}
