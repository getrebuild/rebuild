/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */

let dlgActionAfter_List
const dlgActionAfter = function (dlg) {
  dlg && dlg.hide()
  dlgActionAfter_List && dlgActionAfter_List.loadData()
}

// 表单 DLG
class ConfigFormDlg extends RbFormHandler {
  constructor(props) {
    super(props)
  }

  render() {
    const title = this.title || (this.props.id ? $L('修改%s', this.subtitle || '') : $L('添加%s', this.subtitle || ''))
    return (
      <RbModal title={title} ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form">
          {this.renderFrom()}
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={this.confirm}>
                {$L('确定')}
              </button>
              <a className="btn btn-link" onClick={this.hide}>
                {$L('取消')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    if (this._entity) {
      $.get(`/commons/metadata/entities?detail=${this.hasDetail === true}`, (res) => {
        this.setState({ entities: res.data }, () => {
          this.__select2 = $(this._entity).select2({
            placeholder: $L('选择实体'),
            allowClear: false,
          })
        })
      })
    }
  }

  renderFrom() {}

  confirm = () => {}
}

// 列表 TABLE
class ConfigList extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  componentDidMount() {
    dlgActionAfter_List = this
    this.loadData()
    // 搜索
    const $btn = $('.input-search .btn').click(() => this.loadData())
    $('.input-search .form-control').keydown((e) => (e.which === 13 ? $btn.trigger('click') : true))
  }

  // 加载数据
  loadData(entity) {
    if (!this.requestUrl) throw new Error('No `requestUrl` defined')

    entity = entity || this.__entity
    this.__entity = entity
    const q = $('.input-search input').val()

    $.get(`${this.requestUrl}?entity=${entity || ''}&q=${$encode(q)}`, (res) => {
      if (res.error_code === 0) {
        this.setState({ data: res.data || [] }, () => {
          $('.rb-loading-active').removeClass('rb-loading-active')
          $('.dataTables_info').text($L('共 %d 项', this.state.data.length))

          if (this.state.data.length === 0) $('.list-nodata').removeClass('hide')
          else $('.list-nodata').addClass('hide')

          this.renderEntityTree()
        })
      } else {
        RbHighbar.error(res.error_msg)
      }
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

    const uesSorted = []
    for (let k in ues) uesSorted.push([k, ues[k]])
    uesSorted.sort((x, y) => {
      return x[1] > y[1] ? 1 : x[1] < y[1] ? -1 : 0
    })

    const $dest = $('.aside-tree ul')
    uesSorted.forEach((item) => {
      $(`<li data-entity="${item[0]}"><a class="text-truncate">${item[1]}</a></li>`).appendTo($dest)
    })

    const that = this
    $dest.find('li').click(function () {
      $dest.find('li').removeClass('active')
      $(this).addClass('active')
      that.loadData($(this).data('entity') || '$ALL$')
    })
  }

  // 删除数据
  handleDelete(id, call) {
    $.post(`/app/entity/common-delete?id=${id}`, (res) => {
      if (res.error_code === 0) {
        RbHighbar.success($L('删除成功'))
        if (typeof call === 'function') call()
        else setTimeout(() => location.reload(), 500)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

const ShowEnable = (enable) => {
  return enable ? <span className="badge badge-warning font-weight-light">{$L('否')}</span> : <span className="badge badge-success font-weight-light">{$L('是')}</span>
}
