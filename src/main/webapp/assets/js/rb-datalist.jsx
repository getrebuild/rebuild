/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable react/no-string-refs */

const wpc = window.__PageConfig || {}

const COLUMN_MIN_WIDTH = 30
const COLUMN_MAX_WIDTH = 800
const COLUMN_DEF_WIDTH = 130
// 启用试图页编辑功能
const FIXED_FOOTER = true

// ~~ 数据列表
class RbList extends React.Component {

  constructor(props) {
    super(props)

    this.__defaultFilterKey = 'AdvFilter-' + this.props.config.entity
    this.__sortFieldKey = 'SortField-' + this.props.config.entity
    this.__columnWidthKey = 'ColumnWidth-' + this.props.config.entity + '.'

    const sort = ($storage.get(this.__sortFieldKey) || ':').split(':')
    const fields = props.config.fields
    for (let i = 0; i < fields.length; i++) {
      const cw = $storage.get(this.__columnWidthKey + fields[i].field)
      if (!!cw && ~~cw >= COLUMN_MIN_WIDTH) fields[i].width = ~~cw
      if (sort[0] === fields[i].field) fields[i].sort = sort[1]
    }
    props.config.fields = null
    this.state = { ...props, fields: fields, rowsData: [], pageNo: 1, pageSize: 20, inLoad: true }

    this.__defaultColumnWidth = $('#react-list').width() / 10
    if (this.__defaultColumnWidth < COLUMN_DEF_WIDTH) this.__defaultColumnWidth = COLUMN_DEF_WIDTH

    this.pageNo = 1
    this.pageSize = $storage.get('ListPageSize') || 20
    this.advFilter = $storage.get(this.__defaultFilterKey)
  }

  render() {
    const that = this
    const lastIndex = this.state.fields.length
    return <React.Fragment>
      <div className="row rb-datatable-body">
        <div className="col-sm-12">
          <div className="rb-scroller" ref={(c) => this._rblistScroller = c}>
            <table className="table table-hover table-striped">
              <thead ref={(c) => this._rblistHead = c}>
                <tr>
                  {this.props.uncheckbox !== true && <th className="column-checkbox">
                    <div>
                      <label className="custom-control custom-control-sm custom-checkbox">
                        <input className="custom-control-input" type="checkbox" onChange={(e) => this.toggleRows(e)} />
                        <span className="custom-control-label"></span>
                      </label>
                    </div>
                  </th>}
                  {this.state.fields.map((item) => {
                    const cWidth = item.width || that.__defaultColumnWidth
                    const styles = { width: cWidth + 'px' }
                    return <th key={'column-' + item.field} style={styles} className={`unselect ${item.unsort ? '' : 'sortable'}`} data-field={item.field}
                      onClick={item.unsort ? null : this.sortField.bind(this, item.field)}>
                      <div style={styles}>
                        <span style={{ width: (cWidth - 8) + 'px' }}>{item.label}</span>
                        <i className={'zmdi ' + (item.sort || '')} />
                        <i className="split" />
                      </div>
                    </th>
                  })}
                  <th className="column-empty"></th>
                </tr>
              </thead>
              <tbody ref={(c) => this._rblistBody = c}>
                {this.state.rowsData.map((item) => {
                  const lastPrimary = item[lastIndex]
                  const rowKey = 'row-' + lastPrimary.id
                  return <tr key={rowKey} data-id={lastPrimary.id} onClick={(e) => this.clickRow(e, true)}>
                    {this.props.uncheckbox !== true && <td key={rowKey + '-checkbox'} className="column-checkbox">
                      <div>
                        <label className="custom-control custom-control-sm custom-checkbox">
                          <input className="custom-control-input" type="checkbox" onChange={(e) => this.clickRow(e)} />
                          <span className="custom-control-label"></span>
                        </label>
                      </div>
                    </td>
                    }
                    {item.map((cell, index) => { return that.renderCell(cell, index, lastPrimary) })}
                    <td className="column-empty"></td>
                  </tr>
                })}
              </tbody>
            </table>
            {this.state.inLoad === false && this.state.rowsData.length === 0 &&
              <div className="list-nodata"><span className="zmdi zmdi-info-outline" /><p>暂无数据</p></div>}
          </div>
        </div>
      </div>
      {this.state.rowsData.length > 0
        && <RbListPagination ref={(c) => this._pagination = c} rowsTotal={this.state.rowsTotal} pageSize={this.pageSize} $$$parent={this} />}
      {this.state.inLoad === true && <RbSpinner />}
    </React.Fragment>
  }

  componentDidMount() {
    const $scroller = $(this._rblistScroller)
    $scroller.perfectScrollbar()

    if (FIXED_FOOTER && $('.main-content').width() > 998) {
      $('.main-content').addClass('pb-0')
      $addResizeHandler(() => {
        let mh = $(window).height() - 215
        if ($('.main-content>.nav-tabs-classic').length > 0) mh -= 44  // Has tab
        $scroller.css({ maxHeight: mh })
        $scroller.perfectScrollbar('update')
      })()
    }

    const that = this
    $scroller.find('th .split').draggable({
      containment: '.rb-datatable-body',
      axis: 'x',
      helper: 'clone',
      stop: function (event, ui) {
        const field = $(event.target).parents('th').data('field')
        let left = ui.position.left - 0
        if (left < COLUMN_MIN_WIDTH) left = COLUMN_MIN_WIDTH
        else if (left > COLUMN_MAX_WIDTH) left = COLUMN_MAX_WIDTH
        const fields = that.state.fields
        for (let i = 0; i < fields.length; i++) {
          if (fields[i].field === field) {
            fields[i].width = left
            $storage.set(that.__columnWidthKey + field, left)
            break
          }
        }
        that.setState({ fields: fields }, () => $scroller.perfectScrollbar('update'))
      }
    })

    // 首次由 AdvFilter 加载
    if (wpc.advFilter !== true) this.fetchList(this.__buildQuick())
  }

  componentDidUpdate() {
    // 按钮状态
    const $oper = $('.dataTables_oper')
    $oper.find('.J_delete, .J_view, .J_edit, .J_assign, .J_share, .J_unshare').attr('disabled', true)
    const selected = this.getSelectedIds(true).length
    if (selected > 0) $oper.find('.J_delete, .J_assign, .J_share, .J_unshare').attr('disabled', false)
    else $(this._rblistHead).find('.custom-control-input').prop('checked', false)
    if (selected === 1) $oper.find('.J_view, .J_edit').attr('disabled', false)
  }

  fetchList(filter) {
    const fields = []
    let field_sort = null
    this.state.fields.forEach(function (item) {
      fields.push(item.field)
      if (item.sort) field_sort = item.field + ':' + item.sort.replace('sort-', '')
    })

    const entity = this.props.config.entity
    this.lastFilter = filter || this.lastFilter
    const query = {
      entity: entity,
      fields: fields,
      pageNo: this.pageNo,
      pageSize: this.pageSize,
      filter: this.lastFilter,
      advFilter: this.advFilter,
      sort: field_sort,
      reload: this.pageNo === 1
    }
    this.__lastQueryEntry = query

    let loadingTimer = setTimeout(() => {
      this.setState({ inLoad: true })
      $('#react-list').addClass('rb-loading-active')
    }, 400)
    $.post(`${rb.baseUrl}/app/${entity}/data-list`, JSON.stringify(query), (res) => {
      if (res.error_code === 0) {
        this.setState({ rowsData: res.data.data || [], inLoad: false }, () => RbList.renderAfter())
        if (res.data.total > 0) this._pagination.setState({ rowsTotal: res.data.total, pageNo: this.pageNo })
      } else {
        RbHighbar.error(res.error_msg)
      }

      clearTimeout(loadingTimer)
      loadingTimer = null
      $('#react-list').removeClass('rb-loading-active')
    })
  }

  // 渲染表格及相关事件处理

  renderCell(cellVal, index, lastPrimary) {
    if (this.state.fields.length === index) return null
    const field = this.state.fields[index]
    if (!field) return null

    const cellKey = 'row-' + lastPrimary.id + '-' + index
    if (cellVal === '$NOPRIVILEGES$') {
      return <td key={cellKey}><div className="column-nopriv" title="你无权读取此项数据">[无权限]</div></td>
    } else {
      const width = this.state.fields[index].width || this.__defaultColumnWidth
      let type = field.type
      if (field.field === this.props.config.nameField) {
        cellVal = lastPrimary
        type = '$NAME$'
      }
      return CellRenders.render(cellVal, type, width, cellKey + '.' + field.field)
    }
  }

  // 全选
  toggleRows(e, noUpdate) {
    const $body = $(this._rblistBody)
    if (e.target.checked) $body.find('>tr').addClass('active').find('.custom-control-input').prop('checked', true)
    else $body.find('>tr').removeClass('active').find('.custom-control-input').prop('checked', false)
    // this.setState({ checkedChanged: true })
    if (!noUpdate) this.componentDidUpdate()  // perform
  }

  // 单选
  clickRow(e, unhold) {
    const $target = $(e.target)
    if ($target.hasClass('custom-control-label')) return
    if ($target.hasClass('custom-control-input') && unhold) return

    const $tr = $target.parents('tr')
    if (unhold) {
      this.toggleRows({ target: { checked: false } }, true)
      $tr.addClass('active').find('.custom-control-input').prop('checked', true)
    } else {
      if (e.target.checked) $tr.addClass('active')
      else $tr.removeClass('active')
    }
    // this.setState({ checkedChanged: true })
    this.componentDidUpdate()  // for perform
  }

  sortField(field, e) {
    const fields = this.state.fields
    for (let i = 0; i < fields.length; i++) {
      if (fields[i].field === field) {
        if (fields[i].sort === 'sort-asc') fields[i].sort = 'sort-desc'
        else fields[i].sort = 'sort-asc'
        $storage.set(this.__sortFieldKey, field + ':' + fields[i].sort)
      } else {
        fields[i].sort = null
      }
    }
    this.setState({ fields: fields }, () => this.fetchList())

    $stopEvent(e)
    return false
  }

  // 外部接口

  /**
   * 分页设置
   */
  setPage(pageNo, pageSize) {
    this.pageNo = pageNo || this.pageNo
    if (pageSize) {
      this.pageSize = pageSize
      $storage.set('ListPageSize', pageSize)
    }
    this.fetchList()
  }

  /**
   * 设置高级过滤器ID
   */
  setAdvFilter(id) {
    this.advFilter = id
    this.pageNo = 1
    this.fetchList(this.__buildQuick())
    if (id) $storage.set(this.__defaultFilterKey, id)
    else $storage.remove(this.__defaultFilterKey)
  }

  /**
   * 搜索
   */
  search(filter, fromAdv) {
    const afHold = this.advFilter
    if (fromAdv === true) this.advFilter = null
    this.pageNo = 1
    this.fetchList(filter)

    // No keep last filter
    if (fromAdv === true) {
      this.advFilter = afHold
      this.lastFilter = null
    }
  }

  // Alias `fetchList`
  reload = () => this.fetchList()

  // @el - search element
  searchQuick = (el) => this.search(this.__buildQuick(el))
  __buildQuick(el) {
    el = $(el || '.input-search>input')
    const q = el.val()
    if (!q && !this.lastFilter) return null
    return { entity: this.props.config.entity, type: 'QUICK', values: { 1: q }, qfields: el.data('fields') }
  }

  /**
   * 获取选中 ID[]
   */
  getSelectedIds(noWarn) {
    const selected = []
    $(this._rblistBody).find('>tr .custom-control-input').each(function () {
      const $this = $(this)
      if ($this.prop('checked')) selected.push($this.parents('tr').data('id'))
    })
    if (selected.length === 0 && noWarn !== true) RbHighbar.create('未选中任何记录')
    return selected
  }

  /**
   * 获取最后查询记录总数
   */
  getLastQueryTotal() {
    return this._pagination ? this._pagination.state.rowsTotal : 0
  }

  /**
   * 获取最后查询过滤数据
   */
  getLastQueryData() {
    return JSON.parse(JSON.stringify(this.__lastQueryEntry))  // Use clone
  }

  // 渲染完成后回调
  static renderAfter() {
  }
}

// 列表（单元格）渲染
const CellRenders = {
  __renders: {},

  addRender(type, func) {
    this.__renders[type] = func
  },

  clickView(v, e) {
    RbViewModal.create({ id: v.id, entity: v.entity })
    e && $stopEvent(e)
    return false
  },

  clickPreview(v, idx, e) {
    RbPreview.create(v, idx)
    e && $stopEvent(e)
    return false
  },

  render(value, type, width, key) {
    const style = { width: (width || COLUMN_MIN_WIDTH) + 'px' }
    if (!value) return this.renderSimple(value, style, key)
    else return (this.__renders[type] || this.renderSimple)(value, style, key)
  },

  /**
   * @param {*} v 值
   * @param {*} s 样式
   * @param {*} k key of React (contains fieldName)
   */
  renderSimple(v, s, k) {
    if (typeof v === 'string' && v.length > 300) v = v.sub(0, 300)
    else if (k.endsWith('.approvalId') && !v) v = '未提交'
    else if (k.endsWith('.approvalState') && !v) v = '草稿'
    return <td key={k}><div style={s}>{v || ''}</div></td>
  }
}

CellRenders.addRender('$NAME$', function (v, s, k) {
  return <td key={k}><div style={s}>
    <a href={'#!/View/' + v.entity + '/' + v.id} onClick={(e) => CellRenders.clickView(v, e)} className="column-main">{v.text}</a>
  </div></td>
})

CellRenders.addRender('IMAGE', function (v, s, k) {
  v = v || []
  return <td key={k} className="td-min">
    <div style={s} className="column-imgs" title={'共 ' + v.length + ' 个图片'}>
      {v.map((item, idx) => {
        if (idx > 2) return null
        const imgUrl = rb.baseUrl + '/filex/img/' + item
        const imgName = $fileCutName(item)
        return <a key={'k-' + item} title={imgName} onClick={(e) => CellRenders.clickPreview(v, idx, e)}><img alt="图片" src={imgUrl + '?imageView2/2/w/100/interlace/1/q/100'} /></a>
      })}</div></td>
})

CellRenders.addRender('FILE', function (v, s, k) {
  v = v || []
  return <td key={k} className="td-min">
    <div style={s} className="column-files">
      <ul className="list-unstyled" title={'共 ' + v.length + ' 个文件'}>
        {v.map((item, idx) => {
          if (idx > 0) return null
          const fileName = $fileCutName(item)
          return <li key={'k-' + item} className="text-truncate"><a title={fileName} onClick={(e) => CellRenders.clickPreview(item, null, e)}>{fileName}</a></li>
        })}
      </ul>
    </div></td>
})

CellRenders.addRender('REFERENCE', function (v, s, k) {
  return <td key={k}><div style={s}>
    <a href={'#!/View/' + v.entity + '/' + v.id} onClick={(e) => CellRenders.clickView(v, e)}>{v.text}</a>
  </div></td>
})

CellRenders.addRender('URL', function (v, s, k) {
  return <td key={k}><div style={s}>
    <a href={rb.baseUrl + '/commons/url-safe?url=' + $encode(v)} className="column-url" target="_blank" rel="noopener noreferrer" onClick={(e) => $stopEvent(e)}>{v}</a>
  </div></td>
})

CellRenders.addRender('EMAIL', function (v, s, k) {
  return <td key={k}><div style={s}><a href={'mailto:' + v} className="column-url" onClick={(e) => $stopEvent(e)}>{v}</a></div></td>
})

CellRenders.addRender('PHONE', function (v, s, k) {
  return <td key={k}><div style={s}><a href={'tel:' + v} className="column-url" onClick={(e) => $stopEvent(e)}>{v}</a></div></td>
})

const APPROVAL_STATE_CLAZZs = { '审批中': 'warning', '驳回': 'danger', '通过': 'success' }
CellRenders.addRender('STATE', function (v, s, k) {
  if (k.endsWith('.approvalState')) {
    const badge = APPROVAL_STATE_CLAZZs[v]
    return <td key={k} className="td-min column-state"><div style={s}><span className={badge ? 'badge badge-' + badge : ''}>{v}</span></div></td>
  } else return CellRenders.renderSimple(v, s, k)
})

CellRenders.addRender('DECIMAL', function (v, s, k) {
  if ((v + '').substr(0, 1) === '-') return <td key={k}><div style={s} className="text-danger">{v}</div></td>
  else return CellRenders.renderSimple(v, s, k)
})

CellRenders.addRender('MULTISELECT', function (v, s, k) {
  return <td key={k} className="td-min column-multi"><div style={s}>
    {v.split(' / ').map((item) => {
      return <span key={'opt-' + item} className="badge">{item}</span>
    })}
  </div></td>
})

// ～分页组件
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
    const pages = this.__pageTotal <= 1 ? [1] : $pages(this.__pageTotal, this.state.pageNo)

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
              {pages.map((item, idx) => {
                if (item === '.') return <li key={`pnx-${idx}`} className="paginate_button page-item disabled"><a className="page-link">...</a></li>
                else return <li key={`pn-${item}`} className={'paginate_button page-item ' + (this.state.pageNo === item && 'active')}><a className="page-link" onClick={this.goto.bind(this, item)}>{item}</a></li>
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
    const s = e.target.value
    this.setState({ pageSize: s, pageNo: 1 }, () => {
      this.props.$$$parent.setPage(1, s)
    })
  }
}

// 列表页操作类
const RbListPage = {
  _RbList: null,

  /**
   * @param {*} config DataList config
   * @param {*} entity [Name, Label, Icon]
   * @param {*} ep Privileges of this entity
   */
  init: function (config, entity, ep) {
    renderRbcomp(<RbList config={config} />, 'react-list', function () { RbListPage._RbList = this })

    const that = this

    $('.J_new').click(() => RbFormModal.create({ title: `新建${entity[1]}`, entity: entity[0], icon: entity[2] }))
    $('.J_edit').click(() => {
      const ids = this._RbList.getSelectedIds()
      if (ids.length >= 1) {
        RbFormModal.create({ id: ids[0], title: `编辑${entity[1]}`, entity: entity[0], icon: entity[2] })
      }
    })
    $('.J_delete').click(() => {
      if ($('.J_delete').attr('disabled')) return
      const ids = this._RbList.getSelectedIds()
      if (ids.length < 1) return
      const deleteAfter = function () {
        that._RbList.reload()
      }
      const needEntity = (wpc.type === $pgt.SlaveList || wpc.type === $pgt.SlaveView) ? null : entity[0]
      renderRbcomp(<DeleteConfirm ids={ids} entity={needEntity} deleteAfter={deleteAfter} />)
    })
    $('.J_view').click(() => {
      const ids = this._RbList.getSelectedIds()
      if (ids.length >= 1) {
        location.hash = '!/View/' + entity[0] + '/' + ids[0]
        RbViewModal.create({ id: ids[0], entity: entity[0] })
      }
    })
    $('.J_columns').click(() => RbModal.create(`${rb.baseUrl}/p/general-entity/show-fields?entity=${entity[0]}`, '设置列显示'))

    // 一般权限实体才有
    $('.J_assign').click(() => {
      if ($('.J_assign').attr('disabled')) return
      const ids = this._RbList.getSelectedIds()
      ids.length > 0 && DlgAssign.create({ entity: entity[0], ids: ids })
    })
    $('.J_share').click(() => {
      if ($('.J_share').attr('disabled')) return
      const ids = this._RbList.getSelectedIds()
      ids.length > 0 && DlgShare.create({ entity: entity[0], ids: ids })
    })
    $('.J_unshare').click(() => {
      if ($('.J_unshare').attr('disabled')) return
      const ids = this._RbList.getSelectedIds()
      ids.length > 0 && DlgUnshare.create({ entity: entity[0], ids: ids })
    })
    // In append
    // eslint-disable-next-line react/jsx-no-undef
    $('.J_export').click(() => renderRbcomp(<DataExport listRef={RbListPage._RbList} entity={entity[0]} />))
    // eslint-disable-next-line react/jsx-no-undef
    $('.J_batch').click(() => renderRbcomp(<BatchUpdate listRef={RbListPage._RbList} entity={entity[0]} />))

    // Privileges
    if (ep) {
      if (ep.C === false) $('.J_new').remove()
      if (ep.D === false) $('.J_delete').remove()
      if (ep.U === false) $('.J_edit, .J_batch').remove()
      if (ep.A === false) $('.J_assign').remove()
      if (ep.S === false) $('.J_share, .J_unshare').remove()
      $cleanMenu('.J_action')
    }

    // Quick search
    const $btn = $('.input-search .btn'),
      $input = $('.input-search input')
    $btn.click(() => this._RbList.searchQuick())
    $input.keydown((event) => { if (event.which === 13) $btn.trigger('click') })
  },

  reload() { this._RbList.reload() }
}

// 高级查询操作类
const AdvFilters = {

  /**
   * @param {*} el 控件
   * @param {*} entity 实体
   */
  init(el, entity) {
    this.__el = $(el)
    this.__entity = entity

    this.__el.find('.J_advfilter').click(() => {
      this.showAdvFilter(null, this.current)
      this.current = null
    })
    const $all = $('.adv-search .dropdown-item:eq(0)')
    $all.click(() => this.__effectFilter($all, 'aside'))

    this.loadFilters()
  },

  loadFilters() {
    const dFilter = $storage.get(RbListPage._RbList.__defaultFilterKey)
    const that = this
    let dFilterItem
    $.get(`${rb.baseUrl}/app/${this.__entity}/advfilter/list`, function (res) {
      $('.adv-search .J_custom').each(function () { $(this).remove() })

      const $menu = $('.adv-search .dropdown-menu')
      $(res.data).each(function () {
        const _data = this
        const item = $('<div class="dropdown-item J_custom" data-id="' + _data.id + '"><a class="text-truncate">' + _data.name + '</a></div>').appendTo($menu)
        item.click(() => that.__effectFilter(item, 'aside'))
        if (dFilter === _data.id) dFilterItem = item

        // 可修改
        if (_data.editable) {
          const action = $('<div class="action"><a title="修改"><i class="zmdi zmdi-edit"></i></a><a title="删除"><i class="zmdi zmdi-delete"></i></a></div>').appendTo(item)
          action.find('a:eq(0)').click(function () {
            that.showAdvFilter(_data.id)
            $('.adv-search .btn.dropdown-toggle').dropdown('toggle')
            return false
          })
          action.find('a:eq(1)').click(function () {
            RbAlert.create('确认删除此查询项吗？', {
              type: 'danger',
              confirmText: '删除',
              confirm: function () {
                this.disabled(true)
                $.post(`${rb.baseUrl}/app/entity/record-delete?id=${_data.id}`, (res) => {
                  if (res.error_code === 0) {
                    this.hide()
                    that.loadFilters()
                    if (dFilter === _data.id) {
                      RbListPage._RbList.setAdvFilter(null)
                      $('.adv-search .J_name').text('全部数据')
                    }
                  } else RbHighbar.error(res.error_msg)
                })
              }
            })
            return false
          })
        }
      })

      // ASIDE
      if ($('#asideFilters').length > 0) {
        const ghost = $('.adv-search .dropdown-menu').clone()
        ghost.removeAttr('class')
        ghost.removeAttr('style')
        ghost.removeAttr('data-ps-id')
        ghost.find('.ps-scrollbar-x-rail, .ps-scrollbar-y-rail').remove()
        ghost.find('.dropdown-item').click(function () {
          ghost.find('.dropdown-item').removeClass('active')
          $(this).addClass('active')
          that.__effectFilter($(this), 'aside')
        })
        ghost.appendTo($('#asideFilters').empty())
      }

      if (!dFilterItem) dFilterItem = $('.adv-search .dropdown-item:eq(0)')
      dFilterItem.trigger('click')
    })
  },

  __effectFilter(item, rel) {
    this.current = item.data('id')
    $('.adv-search .J_name').text(item.find('>a').text())
    if (rel === 'aside') {
      const current_id = this.current
      $('#asideFilters .dropdown-item').removeClass('active').each(function () {
        if ($(this).data('id') === current_id) {
          $(this).addClass('active')
          return false
        }
      })
    }

    if (this.current === '$ALL$') this.current = null
    RbListPage._RbList.setAdvFilter(this.current)
  },

  showAdvFilter(id, copyId) {
    const props = { entity: this.__entity, inModal: true, fromList: true, confirm: this.saveFilter }
    if (!id) {
      if (this.__customAdv) {
        this.__customAdv.show()
      } else {
        const that = this
        if (copyId) {
          this.__getFilter(copyId, (res) => {
            renderRbcomp(<AdvFilter {...props} filter={res.filter} />, null, function () { that.__customAdv = this })
          })
        } else {
          renderRbcomp(<AdvFilter {...props} />, null, function () { that.__customAdv = this })
        }
      }
    } else {
      this.current = id
      this.__getFilter(id, (res) => {
        renderRbcomp(<AdvFilter {...props} title="修改查询条件" filter={res.filter} filterName={res.name} shareTo={res.shareTo} />)
      })
    }
  },

  saveFilter(filter, name, shareTo) {
    if (!filter) return
    const that = AdvFilters
    let url = `${rb.baseUrl}/app/${that.__entity}/advfilter/post?id=${that.current || ''}`
    if (name) url += '&name=' + $encode(name)
    if (shareTo) url += '&shareTo=' + $encode(shareTo)
    $.post(url, JSON.stringify(filter), (res) => {
      if (res.error_code === 0) that.loadFilters()
      else RbHighbar.error(res.error_msg)
    })
  },

  __getFilter(id, call) {
    $.get(`${rb.baseUrl}/app/entity/advfilter/get?id=${id}`, (res) => call(res.data))
  }
}

// init
$(document).ready(() => {
  const gs = $urlp('gs', location.hash)
  if (gs) $('.search-input-gs, .input-search>input').val($decode(gs))
  if (wpc.entity) {
    RbListPage.init(wpc.listConfig, wpc.entity, wpc.privileges)
    if (!(wpc.advFilter === false)) AdvFilters.init('.adv-search', wpc.entity[0])
  }
})

// -- for View

// ~~视图窗口（右侧滑出）
class RbViewModal extends React.Component {

  constructor(props) {
    super(props)
    this.state = { ...props, inLoad: true, isHide: true, isDestroy: false }
    this.mcWidth = this.props.subView === true ? 1170 : 1220
    if ($(window).width() < 1280) this.mcWidth -= 100
  }

  render() {
    return !this.state.isDestroy && <div className="modal-wrapper">
      <div className="modal rbview" ref={(c) => this._rbview = c}>
        <div className="modal-dialog">
          <div className="modal-content" style={{ width: this.mcWidth + 'px' }}>
            <div className={'modal-body iframe rb-loading ' + (this.state.inLoad === true && 'rb-loading-active')}>
              <iframe ref={(c) => this._iframe = c} className={this.state.isHide ? 'invisible' : ''} src={this.state.showAfterUrl || 'about:blank'} frameBorder="0" scrolling="no"></iframe>
              <RbSpinner />
            </div>
          </div>
        </div>
      </div>
    </div>
  }

  componentDidMount() {
    const $root = $(this._rbview)
    const rootWrap = $root.parent().parent()
    const mc = $root.find('.modal-content')
    const that = this
    $root.on('hidden.bs.modal', function () {
      mc.css({ 'margin-right': -1500 })
      that.setState({ inLoad: true, isHide: true })
      if (!$keepModalOpen()) location.hash = '!/View/'

      // SubView 子视图不保持
      if (that.state.disposeOnHide === true) {
        $root.modal('dispose')
        that.setState({ isDestroy: true }, () => {
          RbViewModal.holder(that.state.id, 'DISPOSE')
          $unmount(rootWrap)
        })
      }

    }).on('shown.bs.modal', function () {
      mc.css('margin-right', 0)
      if (that.__urlChanged === false) {
        const cw = mc.find('iframe')[0].contentWindow
        if (cw.RbViewPage && cw.RbViewPage._RbViewForm) cw.RbViewPage._RbViewForm.showAgain(that)
        this.__urlChanged = true
      }

      const mcs = $('body>.modal-backdrop.show')
      if (mcs.length > 1) {
        mcs.addClass('o')
        mcs.eq(0).removeClass('o')
      }
    })
    this.show()
  }

  hideLoading() {
    this.setState({ inLoad: false, isHide: false })
  }
  showLoading() {
    this.setState({ inLoad: true, isHide: true })
  }
  show(url, ext) {
    let urlChanged = true
    if (url && url === this.state.url) urlChanged = false
    ext = ext || {}
    url = url || this.state.url
    this.__urlChanged = urlChanged
    this.setState({ ...ext, url: url, inLoad: urlChanged, isHide: urlChanged }, () => {
      $(this._rbview).modal({ show: true, backdrop: true, keyboard: false })
      setTimeout(() => {
        this.setState({ showAfterUrl: this.state.url })
      }, 210) // 0.2s in rb-page.css '.rbview.show .modal-content'
    })
  }
  hide() {
    $(this._rbview).modal('hide')
  }

  // -- Usage
  /**
   * @param {*} props 
   * @param {Boolean} subView 
   */
  static create(props, subView) {
    this.__HOLDERs = this.__HOLDERs || {}
    const that = this
    const viewUrl = `${rb.baseUrl}/app/${props.entity}/view/${props.id}`

    if (subView) {
      renderRbcomp(<RbViewModal url={viewUrl} disposeOnHide={true} id={props.id} subView={true} />, null, function () {
        that.__HOLDERs[props.id] = this
      })
    } else {
      if (this.__HOLDER) {
        this.__HOLDER.show(viewUrl)
        this.__HOLDERs[props.id] = this.__HOLDER
      } else {
        renderRbcomp(<RbViewModal url={viewUrl} />, null, function () {
          that.__HOLDER = this
          that.__HOLDERs[props.id] = this
        })
      }
    }
  }
  /**
   * 获取视图
   * @param {*} id 
   * @param {*} action [DISPOSE|HIDE|LOADING]
   */
  static holder(id, action) {
    if (action === 'DISPOSE') this.__HOLDERs[id] = null
    if (action === 'HIDE') this.__HOLDERs[id] && this.__HOLDERs[id].hide()
    if (action === 'LOADING') this.__HOLDERs[id] && this.__HOLDERs[id].showLoading()
    else return this.__HOLDERs[id]
  }
  /**
   * 主视图
   */
  static holderMain(reload) {
    if (reload && this.__HOLDER) {
      this.__HOLDER.showLoading()
      this.__HOLDER._iframe.contentWindow.location.reload()
    }
    return this.__HOLDER
  }
}

window.chart_remove = function (box) {
  box.parent().animate({ opacity: 0 }, function () {
    box.parent().remove()
    ChartsWidget.saveWidget()
  })
}

// 列表图表部件
const ChartsWidget = {

  init: function () {
    // eslint-disable-next-line no-undef
    ECHART_Base.grid = { left: 40, right: 20, top: 30, bottom: 20 }

    $('.J_load-chart').click(() => { if (this.chartLoaded !== true) this.loadWidget() })
    $('.J_add-chart').click(() => this.showChartSelect())

    $('.charts-wrap').sortable({
      handle: '.chart-title',
      axis: 'y',
      update: () => ChartsWidget.saveWidget()
    }).disableSelection()
  },

  showChartSelect: function () {
    if (this.__chartSelect) {
      this.__chartSelect.show()
      this.__chartSelect.setState({ appended: ChartsWidget.__currentCharts() })
      return
    }
    renderRbcomp(<ChartSelect select={(c) => this.renderChart(c, true)} entity={wpc.entity[0]} />, null, function () {
      ChartsWidget.__chartSelect = this
      this.setState({ appended: ChartsWidget.__currentCharts() })
    })
  },

  renderChart: function (chart, append) {
    const $w = $(`<div id="chart-${chart.chart}"></div>`).appendTo('.charts-wrap')
    // eslint-disable-next-line no-undef
    renderRbcomp(detectChart(chart, chart.chart), $w, function () {
      if (append) ChartsWidget.saveWidget()
    })
  },

  loadWidget: function () {
    $.get(`${rb.baseUrl}/app/${wpc.entity[0]}/widget-charts`, (res) => {
      this.chartLoaded = true
      this.__config = res.data || {}
      res.data && $(res.data.config).each((idx, chart) => this.renderChart(chart))
    })
  },

  saveWidget: function () {
    const charts = this.__currentCharts(true)
    $.post(`${rb.baseUrl}/app/${wpc.entity[0]}/widget-charts?id=${this.__config.id || ''}`, JSON.stringify(charts), (res) => {
      ChartsWidget.__config.id = res.data
      $('.page-aside .tab-content').perfectScrollbar('update')
    })
  },

  __currentCharts: function (o) {
    const charts = []
    $('.charts-wrap>div').each((function () {
      const id = $(this).attr('id').substr(6)
      if (o) charts.push({ chart: id })
      else charts.push(id)
    }))
    return charts
  }
}

$(document).ready(() => {
  // 自动打开 View
  let viewHash = location.hash
  if (viewHash && viewHash.startsWith('#!/View/') && (wpc.type === $pgt.RecordList || wpc.type === $pgt.SlaveList)) {
    viewHash = viewHash.split('/')
    if (viewHash.length === 4 && viewHash[3].length === 20) {
      setTimeout(() => {
        RbViewModal.create({ entity: viewHash[2], id: viewHash[3] })
      }, 500)
    }
  }

  // ASIDE
  if ($('#asideFilters, #asideWidgets').length > 0) {
    $('.side-toggle').click(() => {
      const el = $('.rb-aside').toggleClass('rb-aside-collapsed')
      $.cookie('rb.asideCollapsed', el.hasClass('rb-aside-collapsed'), { expires: 180 })
    })
    // 默认不展开（由后台处理 jsp，避免页面闪动）
    // if ($.cookie('rb.asideCollapsed') === 'false') $('.rb-aside').removeClass('rb-aside-collapsed')

    const $content = $('.page-aside .tab-content')
    $addResizeHandler(() => {
      $content.height($(window).height() - 147)
      $content.perfectScrollbar('update')
    })()
    ChartsWidget.init()
  }
})
