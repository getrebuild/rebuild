/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig || {}

const COLUMN_MIN_WIDTH = 30
const COLUMN_MAX_WIDTH = 800
const COLUMN_DEF_WIDTH = 130

// IE/Edge 不支持首/列固定
const supportFixedColumns = !($.browser.msie || $.browser.msedge) && $(window).width() > 767

// ~~ 数据列表

class RbList extends React.Component {
  constructor(props) {
    super(props)

    this.__defaultFilterKey = `AdvFilter-${props.config.entity}`
    this.__sortFieldKey = `SortField-${props.config.entity}`
    this.__columnWidthKey = `ColumnWidth-${props.config.entity}.`

    const sort = ($storage.get(this.__sortFieldKey) || props.config.sort || ':').split(':')
    const fields = props.config.fields || []
    for (let i = 0; i < fields.length; i++) {
      const cw = $storage.get(this.__columnWidthKey + fields[i].field)
      if (!!cw && ~~cw >= COLUMN_MIN_WIDTH) fields[i].width = ~~cw

      if (sort[0] === fields[i].field) fields[i].sort = sort[1]
      if (['SIGN', 'N2NREFERENCE', 'MULTISELECT', 'FILE', 'IMAGE', 'AVATAR'].includes(fields[i].type)) fields[i].unsort = true
    }

    delete props.config.fields
    this.state = { ...props, fields: fields, rowsData: [], pageNo: 1, pageSize: 20, inLoad: true }

    this.__defaultColumnWidth = $('#react-list').width() / 10
    if (this.__defaultColumnWidth < COLUMN_DEF_WIDTH) this.__defaultColumnWidth = COLUMN_DEF_WIDTH

    this.pageNo = 1
    this.pageSize = $storage.get('ListPageSize') || 20
    this.advFilterId = wpc.advFilter !== true ? null : $storage.get(this.__defaultFilterKey) // 无高级查询
    this.fixedColumns = supportFixedColumns && this.props.uncheckbox !== true
  }

  render() {
    const that = this
    const lastIndex = this.state.fields.length

    return (
      <React.Fragment>
        <div className="row rb-datatable-body">
          <div className="col-sm-12">
            <div className="rb-scroller" ref={(c) => (this._rblistScroller = c)}>
              <table className="table table-hover table-striped">
                <thead>
                  <tr>
                    {this.props.uncheckbox !== true && (
                      <th className={`column-checkbox ${supportFixedColumns ? 'column-fixed' : ''}`}>
                        <div>
                          <label className="custom-control custom-control-sm custom-checkbox">
                            <input className="custom-control-input" type="checkbox" onChange={(e) => this._toggleRows(e)} ref={(c) => (this._checkAll = c)} />
                            <i className="custom-control-label" />
                          </label>
                        </div>
                      </th>
                    )}
                    {this.state.fields.map((item, idx) => {
                      const cWidth = item.width || that.__defaultColumnWidth
                      const styles = { width: cWidth }
                      const clazz = `unselect sortable ${idx === 0 && this.fixedColumns ? 'column-fixed column-fixed-2nd' : ''}`
                      return (
                        <th key={`column-${item.field}`} style={styles} className={clazz} data-field={item.field} onClick={(e) => !item.unsort && this._sortField(item.field, e)}>
                          <div style={styles}>
                            <span style={{ width: cWidth - 8 }}>{item.label}</span>
                            <i className={`zmdi ${item.sort || ''}`} />
                            <i className="dividing" />
                          </div>
                        </th>
                      )
                    })}
                    <th className="column-empty" />
                  </tr>
                </thead>
                <tbody ref={(c) => (this._rblistBody = c)}>
                  {this.state.rowsData.map((item) => {
                    const lastPrimary = item[lastIndex]
                    const rowKey = `row-${lastPrimary.id}`
                    return (
                      <tr key={rowKey} data-id={lastPrimary.id} onClick={(e) => this._clickRow(e, true)}>
                        {this.props.uncheckbox !== true && (
                          <td key={`${rowKey}-checkbox`} className={`column-checkbox ${supportFixedColumns ? 'column-fixed' : ''}`}>
                            <div>
                              <label className="custom-control custom-control-sm custom-checkbox">
                                <input className="custom-control-input" type="checkbox" onChange={(e) => this._clickRow(e)} />
                                <i className="custom-control-label" />
                              </label>
                            </div>
                          </td>
                        )}
                        {item.map((cell, index) => {
                          return that.renderCell(cell, index, lastPrimary)
                        })}
                        <td className="column-empty" />
                      </tr>
                    )
                  })}
                </tbody>
              </table>
              {this.state.inLoad === false && this.state.rowsData.length === 0 && (
                <div className="list-nodata">
                  <span className="zmdi zmdi-info-outline" />
                  <p>{$L('暂无数据')}</p>
                </div>
              )}
            </div>
          </div>
        </div>
        {this.state.rowsData.length > 0 && <RbListPagination ref={(c) => (this._Pagination = c)} pageSize={this.pageSize} $$$parent={this} />}
        {this.state.inLoad === true && <RbSpinner />}
      </React.Fragment>
    )
  }

  componentDidMount() {
    const $scroller = $(this._rblistScroller)
    $scroller.perfectScrollbar({
      wheelSpeed: 2,
    })

    // enable pin
    if ($(window).height() > 666 && $(window).width() >= 1280) {
      $('.main-content').addClass('pb-0')
      // $('.main-content .rb-datatable-header').addClass('header-fixed')
      if (supportFixedColumns) $scroller.find('.table').addClass('table-header-fixed')

      $addResizeHandler(() => {
        let mh = $(window).height() - 208
        if ($('.main-content>.nav-tabs-classic').length > 0) mh -= 40 // Has tab
        if ($('.main-content .quick-filter-pane').length > 0) mh -= 84 // Has query-pane
        $scroller.css({ maxHeight: mh })
        $scroller.perfectScrollbar('update')
      })()
    } else {
      $('.main-content .rb-datatable-header').addClass('header-fixed')
    }

    if (supportFixedColumns) {
      let slLast = 0
      $scroller.on('ps-scroll-x', () => {
        const sl = $scroller[0].scrollLeft
        if (sl === slLast) return
        slLast = sl
        if (sl > 0) $scroller.addClass('column-fixed-pin')
        else $scroller.removeClass('column-fixed-pin')
      })
    }

    const that = this
    $scroller.find('th .dividing').draggable({
      containment: '.rb-datatable-body',
      axis: 'x',
      helper: 'clone',
      start: function () {
        that.__columnResizing = true
      },
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
        setTimeout(() => (that.__columnResizing = false), 100)
      },
    })

    // 首次由 AdvFilter 加载
    if (wpc.advFilter !== true) this.fetchList(this.__buildQuick())

    $(document).on('keydown', (e) => this._keyEvent(e))
  }

  fetchList(filter) {
    const fields = []
    let fieldSort = null
    this.state.fields.forEach(function (item) {
      fields.push(item.field)
      if (item.sort) fieldSort = `${item.field}:${item.sort.replace('sort-', '')}`
    })
    this.lastFilter = filter || this.lastFilter

    const entity = this.props.config.entity
    const reload = this._forceReload || this.pageNo === 1
    this._forceReload = false

    const query = {
      entity: entity,
      fields: fields,
      pageNo: this.pageNo,
      pageSize: this.pageSize,
      filter: this.lastFilter,
      advFilter: this.advFilterId,
      protocolFilter: wpc.protocolFilter,
      sort: fieldSort,
      reload: reload,
      statsField: wpc.statsField === true && rb.commercial > 0,
    }
    this.__lastQueryEntry = query

    const loadingTimer = setTimeout(() => {
      this.setState({ inLoad: true }, () => $('#react-list').addClass('rb-loading-active'))
    }, 400)

    $.post(`/app/${entity}/data-list`, JSON.stringify(query), (res) => {
      if (res.error_code === 0) {
        this.setState({ rowsData: res.data.data || [], inLoad: false }, () => {
          RbList.renderAfter()
          this._clearSelected()
          $(this._rblistScroller).scrollTop(0)
        })

        if (reload && this._Pagination) {
          this._Pagination.setState({ rowsTotal: res.data.total, rowsStats: res.data.stats, pageNo: this.pageNo })
        }
      } else {
        RbHighbar.error(res.error_msg)
      }

      clearTimeout(loadingTimer)
      $('#react-list').removeClass('rb-loading-active')
    })
  }

  // 渲染表格及相关事件处理

  renderCell(cellVal, index, lastPrimary) {
    if (this.state.fields.length === index) return null
    const field = this.state.fields[index]
    if (!field) return null

    const cellKey = `row-${lastPrimary.id}-${index}`
    const width = this.state.fields[index].width || this.__defaultColumnWidth
    let type = field.type
    if (field.field === this.props.config.nameField) {
      cellVal = lastPrimary
      type = '$NAME$'
    } else if (cellVal === '$NOPRIVILEGES$') {
      type = cellVal
    }

    const c = CellRenders.render(cellVal, type, width, `${cellKey}.${field.field}`)
    if (index === 0 && this.fixedColumns) {
      return React.cloneElement(c, { className: `${c.props.className || ''} column-fixed column-fixed-2nd` })
    }
    return c
  }

  // 全选
  _toggleRows(e, uncheck) {
    const $body = $(this._rblistBody)
    if (e.target.checked) {
      $body.find('>tr').addClass('active').find('.custom-control-input').prop('checked', true)
    } else {
      $body.find('>tr').removeClass('active').find('.custom-control-input').prop('checked', false)
    }
    if (!uncheck) this._checkSelected()
  }

  // 单选
  _clickRow(e, unhold) {
    const $target = $(e.target)
    if ($target.hasClass('custom-control-label')) return
    if ($target.hasClass('custom-control-input') && unhold) return

    const $tr = $target.parents('tr')
    if (unhold) {
      this._toggleRows({ target: { checked: false } }, true)
      $tr.addClass('active').find('.custom-control-input').prop('checked', true)
    } else {
      if (e.target.checked) $tr.addClass('active')
      else $tr.removeClass('active')
    }

    this._checkSelected()
  }

  _checkSelected() {
    const chkSelected = $(this._rblistBody).find('>tr .custom-control-input:checked').length

    // 全选/半选/全清
    const chkAll = this.state.rowsData.length
    if (chkSelected === 0) {
      $(this._checkAll).prop('checked', false).parent().removeClass('indeterminate')
    } else if (chkSelected !== chkAll) {
      $(this._checkAll).prop('checked', false).parent().addClass('indeterminate')
    }

    if (chkSelected > 0 && chkSelected === chkAll) {
      $(this._checkAll).prop('checked', true).parent().removeClass('indeterminate')
    }

    // 操作按钮状态
    const $oper = $('.dataTables_oper')
    $oper.find('.J_delete, .J_view, .J_edit, .J_assign, .J_share, .J_unshare').attr('disabled', true)
    if (chkSelected > 0) {
      $oper.find('.J_delete, .J_assign, .J_share, .J_unshare').attr('disabled', false)
      if (chkSelected === 1) $oper.find('.J_view, .J_edit').attr('disabled', false)
    }

    // 分页组件
    this._Pagination && this._Pagination.setState({ selectedTotal: chkSelected })
  }

  _clearSelected() {
    $(this._checkAll).prop('checked', false)
    this._toggleRows({ target: { checked: false } })
  }

  // 排序
  _sortField(field, e) {
    if (this.__columnResizing) return // fix: firefox
    const fields = this.state.fields
    for (let i = 0; i < fields.length; i++) {
      if (fields[i].field === field) {
        if (fields[i].sort === 'sort-asc') fields[i].sort = 'sort-desc'
        else if (fields[i].sort === 'sort-desc') fields[i].sort = null
        else fields[i].sort = 'sort-asc'

        if (fields[i].sort) $storage.set(this.__sortFieldKey, `${field}:${fields[i].sort}`)
        else $storage.remove(this.__sortFieldKey)
      } else {
        fields[i].sort = null
      }
    }
    this.setState({ fields: fields }, () => this.fetchList())

    $stopEvent(e)
    return false
  }

  _tryActive($el) {
    if ($el.length === 1) {
      this._clickRow({ target: $el.find('.custom-checkbox') }, true)
    }
  }

  _keyEvent(e) {
    if (!$(e.target).is('body')) return
    if (!(e.keyCode === 40 || e.keyCode === 38 || e.keyCode === 13)) return

    const $chk = $(this._rblistBody).find('>tr .custom-control-input:checked').last()
    if ($chk.length === 0) return

    const $tr = $chk.eq(0).parents('tr')
    if (e.keyCode === 40) {
      this._tryActive($tr.next())
    } else if (e.keyCode === 38) {
      this._tryActive($tr.prev())
    } else {
      $('.J_view').trigger('click')
    }
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
    this.advFilterId = id
    this.pageNo = 1
    this.fetchList(this.__buildQuick())
    if (id) $storage.set(this.__defaultFilterKey, id)
    else $storage.remove(this.__defaultFilterKey)
  }

  /**
   * 重新加载
   */
  reload() {
    this._forceReload = true
    this.fetchList()
  }

  /**
   * 搜索
   */
  search(filter, fromAdv) {
    // 选择的过滤条件与当前的排他
    if (fromAdv === true) this.advFilterId = null
    this.pageNo = 1
    this.fetchList(filter)

    if (fromAdv === true) {
      $('.J_advfilter .indicator-primary').remove()
      if (filter.items.length > 0) $('<i class="indicator-primary bg-warning"></i>').appendTo('.J_advfilter')
    }
  }

  // @el - search element
  searchQuick = (el) => this.search(this.__buildQuick(el))

  __buildQuick(el) {
    el = $(el || '.input-search>input')
    const q = el.val()
    if (!q && !this.lastFilter) return null

    return {
      entity: this.props.config.entity,
      type: 'QUICK',
      values: { 1: q },
      quickFields: el.data('quickfields') || null,
    }
  }

  /**
   * 获取选中 ID[]
   */
  getSelectedIds(noWarn) {
    const selected = []
    $(this._rblistBody)
      .find('>tr .custom-control-input:checked')
      .each(function () {
        selected.push($(this).parents('tr').data('id'))
      })

    if (selected.length === 0 && noWarn !== true) RbHighbar.create($L('未选中任何记录'))
    return selected
  }

  /**
   * 获取最后查询记录总数
   */
  getLastQueryTotal() {
    return this._Pagination ? this._Pagination.state.rowsTotal : 0
  }

  /**
   * 获取最后查询条件
   */
  getLastQueryEntry() {
    return JSON.parse(JSON.stringify(this.__lastQueryEntry)) // Use clone
  }

  // 渲染完成后回调
  static renderAfter() {}
}

function _isFullUrl(urlKey) {
  return urlKey.startsWith('http://') || urlKey.startsWith('https://')
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
    const style = { width: width || COLUMN_MIN_WIDTH }
    if (!value) return this.renderSimple(value, style, key)
    else return (this.__renders[type] || this.renderSimple)(value, style, key)
  },

  /**
   * @param {*} v 值
   * @param {*} s 样式
   * @param {*} k key of React (contains fieldName)
   */
  renderSimple(v, s, k) {
    if (typeof v === 'string' && v.length > 300) v = v.substr(0, 300)
    else if (k.endsWith('.approvalId') && !v) v = $L('未提交')
    else if (k.endsWith('.approvalState') && !v) v = $L('草稿')

    return (
      <td key={k}>
        <div style={s} title={typeof v === 'string' ? v : null}>
          {v || ''}
        </div>
      </td>
    )
  },
}

// 名称字段
CellRenders.addRender('$NAME$', function (v, s, k) {
  return (
    <td key={k}>
      <div style={s} title={v.text}>
        <a href={`#!/View/${v.entity}/${v.id}`} onClick={(e) => CellRenders.clickView(v, e)} className="column-main">
          {v.text}
        </a>
      </div>
    </td>
  )
})

// 无权访问字段
CellRenders.addRender('$NOPRIVILEGES$', function (v, s, k) {
  return (
    <td key={k}>
      <div style={s} className="column-nopriv">
        [{$L('无权限')}]
      </div>
    </td>
  )
})

CellRenders.addRender('IMAGE', function (v, s, k) {
  v = v || []
  const vLen = v.length
  return (
    <td key={k} className="td-sm">
      <div className="column-imgs" style={s} title={$L('共 %d 项', vLen)}>
        {v.map((item, idx) => {
          if (idx > 2) return null
          const imgName = $fileCutName(item)
          const imgUrl = _isFullUrl(item) ? item : `${rb.baseUrl}/filex/img/${item}`
          return (
            <a key={item} title={imgName} onClick={(e) => CellRenders.clickPreview(v, idx, e)}>
              <img alt="IMG" src={`${imgUrl}?imageView2/2/w/100/interlace/1/q/100`} />
            </a>
          )
        })}
      </div>
    </td>
  )
})

CellRenders.addRender('FILE', function (v, s, k) {
  v = v || []
  const vLen = v.length
  return (
    <td key={k} className="td-sm">
      <div style={s} className="column-files">
        <ul className="list-unstyled" title={$L('共 %d 项', vLen)}>
          {v.map((item, idx) => {
            if (idx > 0) return null
            const fileName = $fileCutName(item)
            return (
              <li key={item} className="text-truncate">
                <a onClick={(e) => CellRenders.clickPreview(item, null, e)}>
                  {fileName}
                  {vLen > 1 ? ` ...[${vLen}]` : null}
                </a>
              </li>
            )
          })}
        </ul>
      </div>
    </td>
  )
})

CellRenders.addRender('REFERENCE', function (v, s, k) {
  return (
    <td key={k}>
      <div style={s} title={v.text}>
        <a href={`#!/View/${v.entity}/${v.id}`} onClick={(e) => CellRenders.clickView(v, e)}>
          {v.text}
        </a>
      </div>
    </td>
  )
})

CellRenders.addRender('N2NREFERENCE', function (v, s, k) {
  v = v || []
  const vLen = v.length
  return (
    <td key={k}>
      <div style={s} title={$L('共 %d 项', vLen)}>
        {v.map((item, idx) => {
          if (idx > 0) return null
          return (
            <a key={item.id} href={`#!/View/${item.entity}/${item.id}`} onClick={(e) => CellRenders.clickView(item, e)}>
              {item.text}
              {vLen > 1 ? ` ...[${vLen}]` : null}
            </a>
          )
        })}
      </div>
    </td>
  )
})

CellRenders.addRender('URL', function (v, s, k) {
  return (
    <td key={k}>
      <div style={s} title={v}>
        <a href={`${rb.baseUrl}/commons/url-safe?url=${$encode(v)}`} className="column-url" target="_blank" rel="noopener noreferrer" onClick={(e) => $stopEvent(e)}>
          {v}
        </a>
      </div>
    </td>
  )
})

CellRenders.addRender('EMAIL', function (v, s, k) {
  return (
    <td key={k}>
      <div style={s} title={v}>
        <a href={`mailto:${v}`} className="column-url" onClick={(e) => $stopEvent(e)}>
          {v}
        </a>
      </div>
    </td>
  )
})

CellRenders.addRender('PHONE', function (v, s, k) {
  return (
    <td key={k}>
      <div style={s} title={v}>
        <a href={`tel:${v}`} className="column-url" onClick={(e) => $stopEvent(e)}>
          {v}
        </a>
      </div>
    </td>
  )
})

const APPROVAL_STATE_CLAZZs = {
  [$L('审批中')]: 'warning',
  [$L('驳回')]: 'danger',
  [$L('通过')]: 'success',
}
CellRenders.addRender('STATE', function (v, s, k) {
  if (k.endsWith('.approvalState')) {
    const badge = APPROVAL_STATE_CLAZZs[v]
    return (
      <td key={k} className="td-sm column-state">
        <div style={s} title={v}>
          <span className={badge ? `badge badge-${badge}` : ''}>{v}</span>
        </div>
      </td>
    )
  } else {
    return CellRenders.renderSimple(v, s, k)
  }
})

CellRenders.addRender('DECIMAL', function (v, s, k) {
  if ((v + '').substr(0, 1) === '-') {
    return (
      <td key={k}>
        <div className="text-danger" style={s} title={v}>
          {v}
        </div>
      </td>
    )
  } else {
    return CellRenders.renderSimple(v, s, k)
  }
})

CellRenders.addRender('MULTISELECT', function (v, s, k) {
  return (
    <td key={k} className="td-sm column-multi">
      <div style={s}>
        {(v.text || []).map((item) => {
          return (
            <span key={item} className="badge" title={item}>
              {item}
            </span>
          )
        })}
      </div>
    </td>
  )
})

CellRenders.addRender('AVATAR', function (v, s, k) {
  const imgUrl = _isFullUrl(v) ? v : `${rb.baseUrl}/filex/img/${v}?imageView2/2/w/100/interlace/1/q/100`
  return (
    <td key={k} className="user-avatar">
      <img src={imgUrl} alt="Avatar" />
    </td>
  )
})

CellRenders.addRender('LOCATION', function (v, s, k) {
  return (
    <td key={k}>
      <div style={s} title={v.text}>
        <a
          href={`#!/Map:${v.lng || ''},${v.lat || ''}`}
          onClick={(e) => {
            $stopEvent(e, true)
            BaiduMapModal.view(v)
          }}>
          {v.text}
        </a>
      </div>
    </td>
  )
})

CellRenders.addRender('SIGN', function (v, s, k) {
  return (
    <td key={k} className="user-avatar sign">
      <img alt="SIGN" src={v} />
    </td>
  )
})

// ~ 分页组件

class RbListPagination extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, pageNo: props.pageNo || 1, pageSize: props.pageSize || 20, rowsTotal: props.rowsTotal || 0 }
    // via List
    this._entity = this.props.$$$parent.props.config.entity
  }

  render() {
    this.__pageTotal = Math.ceil(this.state.rowsTotal / this.state.pageSize)
    if (this.__pageTotal <= 0) this.__pageTotal = 1
    const pages = this.__pageTotal <= 1 ? [1] : $pages(this.__pageTotal, this.state.pageNo)

    return (
      <div className="row rb-datatable-footer">
        <div className="col-12 col-lg-6 col-xl-7">
          <div className="dataTables_info" key="page-rowsTotal">
            {this._renderStats()}
          </div>
        </div>
        <div className="col-12 col-lg-6 col-xl-5">
          <div className="float-right paging_sizes">
            <select className="form-control form-control-sm" title={$L('每页显示')} onChange={this.setPageSize} value={this.state.pageSize || 20}>
              {rb.env === 'dev' && <option value="5">5</option>}
              <option value="20">20</option>
              <option value="40">40</option>
              <option value="80">80</option>
              <option value="100">100</option>
              <option value="200">200</option>
              <option value="300">300</option>
              <option value="400">400</option>
              <option value="500">500</option>
            </select>
          </div>
          <div className="float-right dataTables_paginate paging_simple_numbers">
            <ul className="pagination mb-0">
              {this.state.pageNo > 1 && (
                <li className="paginate_button page-item">
                  <a className="page-link" onClick={() => this.prev()}>
                    <span className="icon zmdi zmdi-chevron-left" />
                  </a>
                </li>
              )}
              {pages.map((item, idx) => {
                if (item === '.')
                  return (
                    <li key={`pnx-${idx}`} className="paginate_button page-item disabled">
                      <a className="page-link">...</a>
                    </li>
                  )
                else
                  return (
                    <li key={`pn-${item}`} className={`paginate_button page-item ${this.state.pageNo === item && 'active'}`}>
                      <a className="page-link" onClick={this.goto.bind(this, item)}>
                        {item}
                      </a>
                    </li>
                  )
              })}
              {this.state.pageNo !== this.__pageTotal && (
                <li className="paginate_button page-item">
                  <a className="page-link" onClick={() => this.next()}>
                    <span className="icon zmdi zmdi-chevron-right" />
                  </a>
                </li>
              )}
            </ul>
          </div>
          <div className="clearfix" />
        </div>
      </div>
    )
  }

  _renderStats() {
    return (
      <div>
        {this.state.selectedTotal > 0 && <span className="mr-1">{$L('已选中 %d 条', this.state.selectedTotal)}.</span>}
        {this.state.rowsTotal > 0 && <span>{$L('共 %d 条数据', this.state.rowsTotal)}</span>}
        {(this.state.rowsStats || []).map((item, idx) => {
          return (
            <span key={idx} className="stat-item">
              {item.label} <strong className="text-warning">{item.value} </strong>
            </span>
          )
        })}
        {rb.isAdminUser && wpc.statsField && (
          <a
            className="list-stats-settings"
            onClick={() =>
              RbModal.create(
                `/p/admin/metadata/list-stats?entity=${this._entity}`,
                <React.Fragment>
                  {$L('配置统计字段')}
                  <sup className="rbv" title={$L('增值功能')} />
                </React.Fragment>
              )
            }>
            <i className="icon zmdi zmdi-settings" title={$L('配置统计字段')} />
          </a>
        )}
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

// ~~ 列表操作

const RbListPage = {
  _RbList: null,

  /**
   * @param {JSON} config DataList config
   * @param {Object} entity [Name, Label, Icon]
   * @param {JSON} ep Privileges of the entity
   */
  init: function (config, entity, ep) {
    renderRbcomp(<RbList config={config} uncheckbox={config.uncheckbox} />, 'react-list', function () {
      RbListPage._RbList = this
      if (window.FrontJS) {
        window.FrontJS.DataList._trigger('open', [])
      }
    })

    const that = this

    $('.J_edit').click(() => {
      const ids = this._RbList.getSelectedIds()
      if (ids.length >= 1) {
        RbFormModal.create({ id: ids[0], title: $L('编辑%s', entity[1]), entity: entity[0], icon: entity[2] })
      }
    })
    $('.J_delete').click(() => {
      if ($('.J_delete').attr('disabled')) return
      const ids = this._RbList.getSelectedIds()
      if (ids.length < 1) return
      const deleteAfter = function () {
        that._RbList.reload()
      }
      const needEntity = wpc.type === 'DetailList' || wpc.type === 'DetailView' ? null : entity[0]
      renderRbcomp(<DeleteConfirm ids={ids} entity={needEntity} deleteAfter={deleteAfter} />)
    })
    $('.J_view').click(() => {
      const ids = this._RbList.getSelectedIds()
      if (ids.length >= 1) {
        location.hash = `!/View/${entity[0]}/${ids[0]}`
        RbViewModal.create({ id: ids[0], entity: entity[0] })
      }
    })

    $('.J_columns').click(() => RbModal.create(`/p/general/show-fields?entity=${entity[0]}`, $L('设置列显示')))

    // 权限实体才有
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

    // Privileges
    if (ep) {
      if (ep.C === false) $('.J_new').remove()
      if (ep.D === false) $('.J_delete').remove()
      if (ep.U === false) $('.J_edit, .J_batch').remove()
      if (ep.A !== true) $('.J_assign').remove()
      if (ep.S !== true) $('.J_share, .J_unshare').remove()
      $cleanMenu('.J_action')
    }

    // Filter Pane
    if ($('.quick-filter-pane').length > 0) {
      // eslint-disable-next-line react/jsx-no-undef
      renderRbcomp(<AdvFilterPane entity={entity[0]} />, $('.quick-filter-pane')[0])
    }

    typeof window.startTour === 'function' && window.startTour(1000)
  },

  reload() {
    this._RbList.reload()
  },
}

// ~~ 视图

class RbViewModal extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, inLoad: true, isHide: true, destroy: false }
    this.mcWidth = this.props.subView === true ? 1344 : 1404
    if ($(window).width() < 1464) this.mcWidth -= 184
  }

  render() {
    if (this.state.destroy) return null

    return (
      <div className="modal-wrapper">
        <div className="modal rbview" ref={(c) => (this._rbview = c)}>
          <div className="modal-dialog">
            <div className="modal-content" style={{ width: this.mcWidth }}>
              <div className={`modal-body iframe rb-loading ${this.state.inLoad === true && 'rb-loading-active'}`}>
                <iframe ref={(c) => (this._iframe = c)} className={this.state.isHide ? 'invisible' : ''} src={this.state.showAfterUrl || 'about:blank'} frameBorder="0" scrolling="no" />
                <RbSpinner />
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const $root = $(this._rbview)
    const rootWrap = $root.parent().parent()
    const mc = $root.find('.modal-content')
    const that = this
    $root
      .on('hidden.bs.modal', function () {
        mc.css({ 'margin-right': -1500 })
        that.setState({ inLoad: true, isHide: true })
        if (!$keepModalOpen()) location.hash = '!/View/'

        // SubView 子视图不保持
        if (that.state.disposeOnHide === true) {
          $root.modal('dispose')
          that.setState({ destroy: true }, () => {
            RbViewModal.holder(that.state.id, 'DISPOSE')
            $unmount(rootWrap)
          })
        }
      })
      .on('shown.bs.modal', function () {
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
    this.__HOLDERsStack = this.__HOLDERsStack || []
    const that = this
    const viewUrl = `${rb.baseUrl}/app/${props.entity}/view/${props.id}`

    if (subView) {
      renderRbcomp(<RbViewModal url={viewUrl} id={props.id} disposeOnHide={true} subView={true} />, null, function () {
        that.__HOLDERs[props.id] = this
        that.__HOLDERsStack.push(this)
      })
    } else {
      if (this.__HOLDER) {
        this.__HOLDER.show(viewUrl)
        this.__HOLDERs[props.id] = this.__HOLDER
      } else {
        renderRbcomp(<RbViewModal url={viewUrl} id={props.id} />, null, function () {
          that.__HOLDERs[props.id] = this
          that.__HOLDERsStack.push(this)
          that.__HOLDER = this
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
    if (action === 'DISPOSE') {
      delete this.__HOLDERs[id]
      this.__HOLDERsStack.pop() // 销毁后替换
      this.__HOLDERsStack.forEach((x) => {
        if (x.props.id === id) this.__HOLDERs[id] = x
      })
    } else if (action === 'HIDE') {
      this.__HOLDERs[id] && this.__HOLDERs[id].hide()
    } else if (action === 'LOADING') {
      this.__HOLDERs[id] && this.__HOLDERs[id].showLoading()
    } else {
      return this.__HOLDERs[id]
    }
  }

  /**
   * 当前激活主视图
   */
  static currentHolder(reload) {
    if (reload && this.__HOLDER) {
      this.__HOLDER.showLoading()
      this.__HOLDER._iframe.contentWindow.location.reload()
    }
    return this.__HOLDER
  }
}

// 复写
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
    ECHART_BASE.grid = { left: 40, right: 20, top: 30, bottom: 20 }

    $('.J_load-charts').on('click', () => {
      this.chartLoaded !== true && this.loadWidget()
    })
    $('.J_add-chart').on('click', () => this.showChartSelect())

    $('.charts-wrap')
      .sortable({
        handle: '.chart-title',
        axis: 'y',
        update: () => ChartsWidget.saveWidget(),
      })
      .disableSelection()
  },

  showChartSelect: function () {
    if (this.__chartSelect) {
      this.__chartSelect.show()
      this.__chartSelect.setState({ appended: ChartsWidget.__currentCharts() })
      return
    }
    // eslint-disable-next-line react/jsx-no-undef
    renderRbcomp(<ChartSelect select={(c) => this.renderChart(c, true)} entity={wpc.entity[0]} />, null, function () {
      ChartsWidget.__chartSelect = this
      this.setState({ appended: ChartsWidget.__currentCharts() })
    })
  },

  renderChart: function (chart, append) {
    const $w = $(`<div id="chart-${chart.chart}"></div>`).appendTo('.charts-wrap')
    // eslint-disable-next-line no-undef
    renderRbcomp(detectChart({ ...chart, editable: true }, chart.chart), $w, function () {
      if (append) ChartsWidget.saveWidget()
    })
  },

  loadWidget: function () {
    $.get(`/app/${wpc.entity[0]}/widget-charts`, (res) => {
      this.chartLoaded = true
      this.__config = res.data || {}
      res.data && $(res.data.config).each((idx, chart) => this.renderChart(chart))
    })
  },

  saveWidget: function () {
    const charts = this.__currentCharts(true)
    $.post(`/app/${wpc.entity[0]}/widget-charts?id=${this.__config.id || ''}`, JSON.stringify(charts), (res) => {
      ChartsWidget.__config.id = res.data
      $('.page-aside .tab-content').perfectScrollbar('update')
    })
  },

  __currentCharts: function (o) {
    const charts = []
    $('.charts-wrap>div').each(function () {
      const id = $(this).attr('id').substr(6)
      if (o) charts.push({ chart: id })
      else charts.push(id)
    })
    return charts
  },
}

$(document).ready(() => {
  window.RbListCommon && window.RbListCommon.init(wpc)

  const viewHash = (location.hash || '').split('/')
  if ((wpc.type === 'RecordList' || wpc.type === 'DetailList') && viewHash.length === 4 && viewHash[1] === 'View' && viewHash[3].length === 20) {
    setTimeout(() => RbViewModal.create({ entity: viewHash[2], id: viewHash[3] }), 500)
  }

  // ASIDE
  if ($('#asideFilters, #asideWidgets').length > 0) {
    $('.side-toggle').on('click', () => {
      const $el = $('.rb-aside').toggleClass('rb-aside-collapsed')
      $.cookie('rb.asideCollapsed', $el.hasClass('rb-aside-collapsed'), { expires: 180 })
    })

    const $content = $('.page-aside .tab-content')
    $addResizeHandler(() => {
      $content.height($(window).height() - 147)
      $content.perfectScrollbar('update')
    })()
    ChartsWidget.init()
  }

  const $wtab = $('.page-aside.widgets .nav a:eq(0)')
  if ($wtab.length > 0) {
    $('.page-aside.widgets .ph-item.rb').remove()
    $wtab.trigger('click')
  }
})
