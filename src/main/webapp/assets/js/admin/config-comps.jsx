/* eslint-disable no-unused-vars */
// 表单 DLG
class ConfigFormDlg extends RbFormHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <RbModal title={(this.props.id ? '修改' : '添加') + (this.subtitle || '')} ref={(c) => this._dlg = c} disposeOnHide={true}>
        <div className="form">
          {this.renderFrom()}
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => this._btns = c}>
              <button className="btn btn-primary" type="button" onClick={this.confirm}>确定</button>
              <a className="btn btn-link" onClick={this.hide}>取消</a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    if (this._entity) {
      $.get(`${rb.baseUrl}/commons/metadata/entities`, (res) => {
        this.setState({ entities: res.data }, () => {
          this.__select2 = $(this._entity).select2({
            placeholder: '选择实体',
            allowClear: false
          })
        })
      })
    }
  }

  renderFrom() {
  }
  confirm = () => {
  }
}

// 列表 TABLE
class ConfigList extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  componentDidMount() {
    this.loadData()
    // 搜索
    const btn = $('.input-search .btn').click(() => { this.loadData() })
    $('.input-search .form-control').keydown((e) => { if (e.which === 13) btn.trigger('click') })
  }

  // 加载数据
  loadData(entity) {
    if (!this.requestUrl) {
      throw new Error('No `requestUrl` defined')
    }

    entity = entity || this.__entity
    this.__entity = entity
    let q = $('.input-search input').val()

    $.get(`${this.requestUrl}?entity=${entity || ''}&q=${$encode(q)}`, (res) => {
      this.setState({ data: res.data || [] }, () => {
        $('.rb-loading-active').removeClass('rb-loading-active')
        $('.dataTables_info').text(`共 ${this.state.data.length} 项`)

        if (this.state.data.length === 0) $('.list-nodata').removeClass('hide')
        else $('.list-nodata').addClass('hide')

        this.renderEntityTree()
      })
    })
  }

  // 渲染实体树
  renderEntityTree() {
    if (this.__treeRendered) return
    this.__treeRendered = true

    const ues = {}
    $(this.state.data).each(function () {
      ues[this[1]] = this[2]
    })
    const dest = $('.dept-tree ul')
    for (let k in ues) {
      $(`<li data-entity="${k}"><a class="text-truncate">${ues[k]}</a></li>`).appendTo(dest)
    }

    let that = this
    dest.find('li').click(function () {
      dest.find('li').removeClass('active')
      $(this).addClass('active')
      that.loadData($(this).data('entity') || '$ALL$')
    })
  }

  // 删除数据
  handleDelete(id) {
    $.post(`${rb.baseUrl}/app/entity/record-delete?id=${id}`, (res) => {
      if (res.error_code === 0) {
        RbHighbar.success('删除成功')
        setTimeout(() => location.reload(), 500)
      } else RbHighbar.error(res.error_msg)
    })
  }
}