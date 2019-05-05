/* eslint-disable react/no-string-refs */
/* eslint-disable react/prop-types */
// ~~ 数据列表
const COLUMN_MIN_WIDTH = 30
class RbList extends React.Component {
  constructor(props) {
    super(props)

    this.__defaultFilterKey = 'AdvFilter-' + this.props.config.entity
    this.__sortFieldKey = 'SortField-' + this.props.config.entity
    this.__columnWidthKey = 'ColumnWidth-' + this.props.config.entity + '.'

    let sort = ($storage.get(this.__sortFieldKey) || ':').split(':')
    let fields = props.config.fields
    for (let i = 0; i < fields.length; i++) {
      let cw = $storage.get(this.__columnWidthKey + fields[i].field)
      if (!!cw && ~~cw >= COLUMN_MIN_WIDTH) fields[i].width = ~~cw
      if (sort[0] === fields[i].field) fields[i].sort = sort[1]
    }
    props.config.fields = null
    this.state = { ...props, fields: fields, rowsData: [], pageNo: 1, pageSize: 20, inLoad: true, checkedAll: false }

    this.__defaultColumnWidth = $('#react-list').width() / 10
    if (this.__defaultColumnWidth < 130) this.__defaultColumnWidth = 130

    this.pageNo = 1
    this.pageSize = $storage.get('ListPageSize') || 20
    this.advFilter = $storage.get(this.__defaultFilterKey)

    this.toggleAllRow = this.toggleAllRow.bind(this)
  }
  render() {
    let that = this
    const lastIndex = this.state.fields.length
    return (
      <div>
        <div className="row rb-datatable-body">
          <div className="col-sm-12">
            <div className="rb-scroller" ref="rblist-scroller">
              <table className="table table-hover table-striped">
                <thead>
                  <tr>
                    <th className="column-checkbox">
                      <div><label className="custom-control custom-control-sm custom-checkbox"><input className="custom-control-input" type="checkbox" checked={this.state.checkedAll} onClick={this.toggleAllRow} /><span className="custom-control-label"></span></label></div>
                    </th>
                    {this.state.fields.map((item) => {
                      let cWidth = (item.width || that.__defaultColumnWidth)
                      let styles = { width: cWidth + 'px' }
                      let sortClazz = item.sort || ''
                      return (<th key={'column-' + item.field} style={styles} className="sortable unselect" onClick={this.sortField.bind(this, item.field)}><div style={styles}><span style={{ width: (cWidth - 8) + 'px' }}>{item.label}</span><i className={'zmdi ' + sortClazz}></i><i className="split" data-field={item.field}></i></div></th>)
                    })}
                    <th className="column-empty"></th>
                  </tr>
                </thead>
                <tbody>
                  {this.state.rowsData.map((item, index) => {
                    let lastGhost = item[lastIndex]
                    let rowKey = 'row-' + lastGhost[0]
                    return (<tr key={rowKey} className={lastGhost[3] ? 'table-active' : ''} onClick={this.clickRow.bind(this, index, false)}>
                      <td key={rowKey + '-checkbox'} className="column-checkbox">
                        <div><label className="custom-control custom-control-sm custom-checkbox"><input className="custom-control-input" type="checkbox" checked={lastGhost[3]} onClick={this.clickRow.bind(this, index, true)} /><span className="custom-control-label"></span></label></div>
                      </td>
                      {item.map((cell, index) => {
                        return that.renderCell(cell, index, lastGhost)
                      })}
                      <td className="column-empty"></td>
                    </tr>)
                  })}
                </tbody>
              </table>
              {this.state.inLoad === false && this.state.rowsData.length === 0 ? <div className="list-nodata"><span className="zmdi zmdi-info-outline" /><p>暂无数据</p></div> : null}
            </div>
          </div></div>
        {this.state.rowsData.length > 0 ? <RbListPagination ref="pagination" rowsTotal={this.state.rowsTotal} pageSize={this.pageSize} $$$parent={this} /> : null}
        {this.state.inLoad === true && <RbSpinner />}
      </div>)
  }
  componentDidMount() {
    const scroller = $(this.refs['rblist-scroller'])
    scroller.perfectScrollbar()

    let that = this
    scroller.find('th .split').draggable({
      containment: '.rb-datatable-body', axis: 'x', helper: 'clone', stop: function (event, ui) {
        let field = $(event.target).data('field')
        let left = ui.position.left - 2
        if (left < COLUMN_MIN_WIDTH) left = COLUMN_MIN_WIDTH
        let fields = that.state.fields
        for (let i = 0; i < fields.length; i++) {
          if (fields[i].field === field) {
            fields[i].width = left
            $storage.set(that.__columnWidthKey + field, left)
            break
          }
        }
        that.setState({ fields: fields })
      }
    })
    this.fetchList()
  }
  componentDidUpdate() {
    let that = this
    this.__selectedRows = []
    this.state.rowsData.forEach((item) => {
      let lastGhost = item[that.state.fields.length]
      if (lastGhost[3] === true) that.__selectedRows.push(lastGhost)
    })

    let oper = $('.dataTables_oper')
    oper.find('.J_delete, .J_view, .J_edit').attr('disabled', true)
    let len = this.__selectedRows.length
    if (len > 0) oper.find('.J_delete').attr('disabled', false)
    if (len === 1) oper.find('.J_view, .J_edit').attr('disabled', false)
  }

  fetchList(filter) {
    let fields = []
    let field_sort = null
    this.state.fields.forEach(function (item) {
      fields.push(item.field)
      if (item.sort) field_sort = item.field + ':' + item.sort.replace('sort-', '')
    })

    let entity = this.props.config.entity
    this.lastFilter = filter || this.lastFilter
    let query = {
      entity: entity,
      fields: fields,
      pageNo: this.pageNo,
      pageSize: this.pageSize,
      filter: this.lastFilter,
      advFilter: this.advFilter,
      sort: field_sort,
      reload: this.pageNo === 1
    }

    let loadingTimer = setTimeout(() => {
      this.setState({ inLoad: true })
      $('#react-list').addClass('rb-loading-active')
    }, 400)
    $.post(`${rb.baseUrl}/app/${entity}/data-list`, JSON.stringify(query), (res) => {
      if (res.error_code === 0) {
        let rowsdata = res.data.data || []
        if (rowsdata.length > 0) {
          let lastIndex = rowsdata[0].length - 1
          rowsdata = rowsdata.map((item) => {
            item[lastIndex][3] = false  // Checked?
            return item
          })
        }

        this.setState({ rowsData: rowsdata, inLoad: false })
        if (res.data.total > 0) this.refs['pagination'].setState({ rowsTotal: res.data.total })

      } else {
        rb.hberror(res.error_msg)
      }

      clearTimeout(loadingTimer)
      $('#react-list').removeClass('rb-loading-active')
    })
  }

  // 渲染表格及相关事件处理

  renderCell(cellVal, index, lastGhost) {
    if (this.state.fields.length === index) return null

    const cellKey = 'row-' + lastGhost[0] + '-' + index
    if (!cellVal) return <td key={cellKey}><div></div></td>

    const field = this.state.fields[index]
    let styles = { width: (this.state.fields[index].width || this.__defaultColumnWidth) + 'px' }
    if (field.type === 'IMAGE') {
      cellVal = JSON.parse(cellVal || '[]')
      return (<td key={cellKey} className="td-min">
        <div style={styles} className="column-imgs" title={cellVal.length + ' 个图片'}>
          {cellVal.map((item, idx) => {
            let imgUrl = rb.baseUrl + '/filex/img/' + item
            let imgName = $fileCutName(item)
            return <a key={cellKey + idx} href={'#!/Preview/' + item} title={imgName}><img src={imgUrl + '?imageView2/2/w/100/interlace/1/q/100'} /></a>
          })}</div></td>)
    } else if (field.type === 'FILE') {
      cellVal = JSON.parse(cellVal || '[]')
      return (<td key={cellKey} className="td-min"><div style={styles} className="column-files">
        <ul className="list-unstyled" title={cellVal.length + ' 个文件'}>
          {cellVal.map((item, idx) => {
            let fileName = $fileCutName(item)
            return <li key={cellKey + idx} className="text-truncate"><a href={'#!/Preview/' + item} title={fileName}>{fileName}</a></li>
          })}</ul>
      </div></td>)
    } else if (field.type === 'REFERENCE') {
      return <td key={cellKey}><div style={styles}><a href={'#!/View/' + cellVal[2][0] + '/' + cellVal[0]} onClick={() => this.clickView(cellVal)}>{cellVal[1]}</a></div></td>
    } else if (field.field === this.props.config.nameField) {
      cellVal = lastGhost
      return <td key={cellKey}><div style={styles}><a href={'#!/View/' + cellVal[2][0] + '/' + cellVal[0]} onClick={() => this.clickView(cellVal)} className="column-main">{cellVal[1]}</a></div></td>
    } else if (field.type === 'URL') {
      return <td key={cellKey}><div style={styles}><a href={rb.baseUrl + '/common/url-safe?url=' + encodeURIComponent(cellVal)} className="column-url" target="_blank" rel="noopener noreferrer">{cellVal}</a></div></td>
    } else if (field.type === 'EMAIL') {
      return <td key={cellKey}><div style={styles}><a href={'mailto:' + cellVal} className="column-url">{cellVal}</a></div></td>
    } else if (field.type === 'AVATAR') {
      let imgUrl = rb.baseUrl + '/filex/img/' + cellVal + '?imageView2/2/w/100/interlace/1/q/100'
      return <td key={cellKey} className="user-avatar"><img src={imgUrl} alt="Avatar" /></td>
    } else {
      return <td key={cellKey}><div style={styles}>{cellVal}</div></td>
    }
  }

  toggleAllRow() {
    let checked = this.state.checkedAll === false
    let rowsdata = this.state.rowsData
    rowsdata = rowsdata.map((item) => {
      item[item.length - 1][3] = checked  // Checked?
      return item
    })
    this.setState({ checkedAll: checked, rowsData: rowsdata })
    return false
  }
  clickRow(rowIndex, holdOthers, e) {
    if (e.target.tagName === 'SPAN') return false
    e.stopPropagation()
    e.nativeEvent.stopImmediatePropagation()

    let rowsdata = this.state.rowsData
    let lastIndex = rowsdata[0].length - 1
    if (holdOthers === true) {
      let item = rowsdata[rowIndex]
      item[lastIndex][3] = item[lastIndex][3] === false  // Checked?
      rowsdata[rowIndex] = item
    } else {
      rowsdata = rowsdata.map((item, index) => {
        item[lastIndex][3] = index === rowIndex
        return item
      })
    }
    this.setState({ rowsData: rowsdata })
    return false
  }

  clickView(cellVal) {
    rb.RbViewModal({ id: cellVal[0], entity: cellVal[2][0] })
    return false
  }

  sortField(field, e) {
    let fields = this.state.fields
    for (let i = 0; i < fields.length; i++) {
      if (fields[i].field === field) {
        if (fields[i].sort === 'sort-asc') fields[i].sort = 'sort-desc'
        else fields[i].sort = 'sort-asc'
        $storage.set(this.__sortFieldKey, field + ':' + fields[i].sort)
      } else {
        fields[i].sort = null
      }
    }
    let that = this
    this.setState({ fields: fields }, function () {
      that.fetchList()
    })

    e.stopPropagation()
    e.nativeEvent.stopImmediatePropagation()
    return false
  }

  // 外部接口

  setPage(pageNo, pageSize) {
    this.pageNo = pageNo || this.pageNo
    if (pageSize) {
      this.pageSize = pageSize
      $storage.set('ListPageSize', pageSize)
    }
    this.fetchList()
  }

  setAdvFilter(id) {
    this.advFilter = id
    this.fetchList()

    if (id) $storage.set(this.__defaultFilterKey, id)
    else $storage.remove(this.__defaultFilterKey)
  }

  getSelectedRows() {
    return this.__selectedRows
  }
  getSelectedIds() {
    if (!this.__selectedRows || this.__selectedRows.length < 1) { rb.highbar('未选中任何记录'); return [] }
    let ids = this.__selectedRows.map((item) => { return item[0] })
    return ids
  }

  search(filter, noHold) {
    this.fetchList(filter)
    if (noHold === true) {
      this.lastFilter = null
    }
  }
  reload() {
    this.fetchList()
  }
}

// 分页组件
class RbListPagination extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }

    this.state.pageNo = this.state.pageNo || 1
    this.state.pageSize = this.state.pageSize || 20
    this.state.rowsTotal = this.state.rowsTotal || 0
  }
  render() {
    this.__pageTotal = Math.ceil(this.state.rowsTotal / this.state.pageSize)
    if (this.__pageTotal <= 0) this.__pageTotal = 1
    let pages = this.__pageTotal <= 1 ? [1] : $pages(this.__pageTotal, this.state.pageNo)
    return (
      <div className="row rb-datatable-footer">
        <div className="col-12 col-md-4">
          <div className="dataTables_info" key="page-rowsTotal">{this.state.rowsTotal > 0 ? `共 ${this.state.rowsTotal} 条数据` : ''}</div>
        </div>
        <div className="col-12 col-md-8">
          <div className="float-right paging_sizes">
            <select className="form-control form-control-sm" title="每页显示" onChange={this.setPageSize} value={this.state.pageSize || 20}>
              {rb.env === 'dev' && <option value="5">5 条</option>}
              <option value="20">20 条</option>
              <option value="40">40 条</option>
              <option value="80">80 条</option>
              <option value="100">100 条</option>
              <option value="200">200 条</option>
            </select>
          </div>
          <div className="float-right dataTables_paginate paging_simple_numbers">
            <ul className="pagination">
              {this.state.pageNo > 1 && <li className="paginate_button page-item"><a className="page-link" onClick={() => this.prev()}><span className="icon zmdi zmdi-chevron-left"></span></a></li>}
              {pages.map((item) => {
                if (item === '.') return <li key={'page-' + item} className="paginate_button page-item disabled"><a className="page-link">...</a></li>
                else return <li key={'page-' + item} className={'paginate_button page-item ' + (this.state.pageNo === item && 'active')}><a className="page-link" onClick={this.goto.bind(this, item)}>{item}</a></li>
              })}
              {this.state.pageNo !== this.__pageTotal && <li className="paginate_button page-item"><a className="page-link" onClick={() => this.next()}><span className="icon zmdi zmdi-chevron-right"></span></a></li>}
            </ul>
          </div>
          <div className="clearfix" />
        </div>
      </div>
    )
  }
  prev() {
    if (this.state.pageNo === 1) return
    this.goto(this.state.pageNo - 1)
  }
  next() {
    if (this.state.pageNo === this.__pageTotal) return
    this.goto(this.state.pageNo + 1)
  }
  goto(pageNo) {
    this.setState({ pageNo: pageNo }, () => {
      this.props.$$$parent.setPage(this.state.pageNo)
    })
  }
  setPageSize = (e) => {
    let s = e.target.value
    this.setState({ pageSize: s }, () => {
      this.props.$$$parent.setPage(1, s)
    })
  }
}

// -- Usage

var rb = rb || {}

// @props = { config }
rb.RbList = function (props, target) {
  return renderRbcomp(<RbList {...props} />, target || 'react-list')
}

// 列表页面初始化
const RbListPage = {
  _RbList: null,

  // @config - List config
  // @entity - [Name, Label, Icon]
  // @ep - Privileges of this entity
  init: function (config, entity, ep) {
    this._RbList = renderRbcomp(<RbList config={config} />, 'react-list')

    const that = this

    $('.J_new').click(() => {
      rb.RbFormModal({ title: `新建${entity[1]}`, entity: entity[0], icon: entity[2] })
    })
    $('.J_edit').click(() => {
      let selected = this._RbList.getSelectedRows()
      if (selected.length === 1) {
        selected = selected[0]
        rb.RbFormModal({ id: selected[0], title: `编辑${entity[1]}`, entity: entity[0], icon: entity[2] })
      }
    })
    $('.J_delete').click(() => {
      let ids = this._RbList.getSelectedIds()
      if (ids.length < 1) return
      let deleteAfter = function () {
        that._RbList.reload()
      }

      const wpc = window.__PageConfig
      const needEntity = (wpc.type === 'SlaveList' || wpc.type === 'SlaveView') ? null : entity[0]
      renderRbcomp(<DeleteConfirm ids={ids} entity={needEntity} deleteAfter={deleteAfter} />)
    })
    $('.J_view').click(() => {
      let selected = this._RbList.getSelectedRows()
      if (selected.length === 1) {
        selected = selected[0]
        rb.RbViewModal({ id: selected[0], entity: entity[0] })
      }
    })
    $('.J_assign').click(() => {
      let ids = this._RbList.getSelectedIds()
      if (ids.length > 0) rb.DlgAssign({ entity: entity[0], ids: ids })
    })
    $('.J_share').click(() => {
      let ids = this._RbList.getSelectedIds()
      if (ids.length > 0) rb.DlgShare({ entity: entity[0], ids: ids })
    })
    $('.J_unshare').click(() => {
      let ids = this._RbList.getSelectedIds()
      if (ids.length > 0) rb.DlgUnshare({ entity: entity[0], ids: ids })
    })

    $('.J_columns').click(function () {
      rb.modal(`${rb.baseUrl}/p/general-entity/show-fields?entity=${entity[0]}`, '设置列显示')
    })

    // Privileges
    if (ep) {
      if (ep.C === false) $('.J_new').remove()
      if (ep.D === false) $('.J_delete').remove()
      if (ep.U === false) $('.J_edit').remove()
      if (ep.A === false) $('.J_assign').remove()
      if (ep.S === false) $('.J_share, .J_unshare').remove()

      $cleanMenu('.J_action')
    }

    this.initQuickFilter(entity[0])
  },

  initQuickFilter: function (e) {
    let btn = $('.input-search .btn'),
      input = $('.input-search input')
    btn.click(() => {
      let q = $val(input)
      let filterExp = { entity: e, type: 'QUICK', values: { 1: q }, qfields: $('.input-search').data('qfields') }
      this._RbList.search(filterExp)
    })
    input.keydown((event) => { if (event.which === 13) btn.trigger('click') })
  }
}

// 列表高级查询
const AdvFilters = {

  // @el - 控件
  // @entity - 实体
  init(el, entity) {
    this.__el = $(el)
    this.__entity = entity

    this.__el.find('.J_advfilter').click(() => { this.showAdvFilter() })
    // $ALL$
    $('.adv-search .dropdown-item:eq(0)').click(() => {
      $('.adv-search .J_name').text('全部数据')
      RbListPage._RbList.setAdvFilter(null)
    })

    this.loadFilters()
  },

  loadFilters() {
    let dfilter = $storage.get(RbListPage._RbList.__defaultFilterKey)
    let that = this
    $.get(`${rb.baseUrl}/app/${this.__entity}/advfilter/list`, function (res) {
      $('.adv-search .J_custom').each(function () { $(this).remove() })

      $(res.data).each(function () {
        let item = $('<div class="dropdown-item J_custom" data-id="' + this[0] + '"><a class="text-truncate">' + this[1] + '</a></div>').appendTo('.adv-search .dropdown-menu')
        let _data = this
        if (_data[2] === true) {
          let action = $('<div class="action"><a title="修改"><i class="zmdi zmdi-edit"></i></a><a title="删除"><i class="zmdi zmdi-delete"></i></a></div>').appendTo(item)
          action.find('a:eq(0)').click(function () {
            that.showAdvFilter(_data[0])
            $('.adv-search .btn.dropdown-toggle').dropdown('toggle')
            return false
          })
          action.find('a:eq(1)').click(function () {
            let _alert = rb.alert('确认要删除此过滤项吗？', {
              type: 'danger', confirm: () => {
                $.post(`${rb.baseUrl}/app/entity/advfilter/delete?id=${_data[0]}`, (res) => {
                  if (res.error_code === 0) {
                    _alert.hide()
                    that.loadFilters()

                    if (dfilter === _data[0]) {
                      RbListPage._RbList.setAdvFilter(null)
                      $('.adv-search .J_name').text('全部数据')
                    }

                  } else rb.hberror(res.error_msg)
                })
              }
            })
            return false
          })
        }

        item.click(function () {
          $('.adv-search .J_name').text(_data[1])
          RbListPage._RbList.setAdvFilter(_data[0])
        })

        if (dfilter === _data[0]) {
          $('.adv-search .J_name').text(_data[1])
        }
      })
    })
  },

  saveFilter(filter, name, toAll) {
    if (!filter) return
    let that = AdvFilters
    let url = `${rb.baseUrl}/app/${that.__entity}/advfilter/post?id=${that.__cfgid || ''}&toAll=${toAll}`
    if (name) url += '&name=' + $encode(name)
    $.post(url, JSON.stringify(filter), function (res) {
      if (res.error_code === 0) {
        that.loadFilters()
      } else rb.hberror(res.error_msg)
    })
  },

  showAdvFilter(id) {
    this.__cfgid = id
    let props = { entity: this.__entity, inModal: true, fromList: true, confirm: this.saveFilter }
    if (!id) {
      if (this.__showHolder) this.__showHolder.show()
      else this.__showHolder = renderRbcomp(<AdvFilter {...props} title="高级查询" />)
    } else {
      $.get(rb.baseUrl + '/app/entity/advfilter/get?id=' + id, function (res) {
        let _data = res.data
        renderRbcomp(<AdvFilter {...props} title="修改查询条件" filter={_data.filter} filterName={_data.name} shareToAll={_data.shareTo === 'ALL'} />)
      })
    }
  }
}

// Init
$(document).ready(() => {
  const wpc = window.__PageConfig
  if (!wpc) return
  RbListPage.init(wpc.listConfig, wpc.entity, wpc.privileges)
  if (!(wpc.advFilter === false)) AdvFilters.init('.adv-search', wpc.entity[0])
})