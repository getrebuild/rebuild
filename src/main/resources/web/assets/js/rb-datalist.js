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
    this.advFilterId = wpc.advFilter === false ? null : $storage.get(this.__defaultFilterKey)
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
                            <span className="custom-control-label"></span>
                          </label>
                        </div>
                      </th>
                    )}
                    {this.state.fields.map((item, idx) => {
                      const cWidth = item.width || that.__defaultColumnWidth
                      const styles = { width: cWidth + 'px' }
                      const clazz = `unselect ${item.unsort ? '' : 'sortable'} ${idx === 0 && this.fixedColumns ? 'column-fixed column-fixed-2nd' : ''}`
                      return (
                        <th key={'column-' + item.field} style={styles} className={clazz} data-field={item.field} onClick={(e) => !item.unsort && this._sortField(item.field, e)}>
                          <div style={styles}>
                            <span style={{ width: cWidth - 8 + 'px' }}>{item.label}</span>
                            <i className={'zmdi ' + (item.sort || '')} />
                            <i className="dividing" />
                          </div>
                        </th>
                      )
                    })}
                    <th className="column-empty"></th>
                  </tr>
                </thead>
                <tbody ref={(c) => (this._rblistBody = c)}>
                  {this.state.rowsData.map((item) => {
                    const lastPrimary = item[lastIndex]
                    const rowKey = 'row-' + lastPrimary.id
                    return (
                      <tr key={rowKey} data-id={lastPrimary.id} onClick={(e) => this._clickRow(e, true)}>
                        {this.props.uncheckbox !== true && (
                          <td key={rowKey + '-checkbox'} className={`column-checkbox ${supportFixedColumns ? 'column-fixed' : ''}`}>
                            <div>
                              <label className="custom-control custom-control-sm custom-checkbox">
                                <input className="custom-control-input" type="checkbox" onChange={(e) => this._clickRow(e)} />
                                <span className="custom-control-label"></span>
                              </label>
                            </div>
                          </td>
                        )}
                        {item.map((cell, index) => {
                          return that.renderCell(cell, index, lastPrimary)
                        })}
                        <td className="column-empty"></td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
              {this.state.inLoad === false && this.state.rowsData.length === 0 && (
                <div className="list-nodata">
                  <span className="zmdi zmdi-info-outline" />
                  <p>{$L('NoData')}</p>
                </div>
              )}
            </div>
          </div>
        </div>
        {this.state.rowsData.length > 0 && <RbListPagination ref={(c) => (this._pagination = c)} rowsTotal={this.state.rowsTotal} pageSize={this.pageSize} $$$parent={this} />}
        {this.state.inLoad === true && <RbSpinner />}
      </React.Fragment>
    )
  }

  componentDidMount() {
    const $scroller = $(this._rblistScroller)
    $scroller.perfectScrollbar({
      wheelSpeed: 2,
    })

    // enable pins
    if ($(window).height() > 666 && $(window).width() >= 1280) {
      $('.main-content').addClass('pb-0')
      $('.main-content .rb-datatable-header').addClass('header-fixed')
      if (supportFixedColumns) $scroller.find('.table').addClass('table-header-fixed')

      $addResizeHandler(() => {
        let mh = $(window).height() - 215
        if ($('.main-content>.nav-tabs-classic').length > 0) mh -= 44 // Has tab
        $scroller.css({ maxHeight: mh })
        $scroller.perfectScrollbar('update')
      })()
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
      if (item.sort) fieldSort = item.field + ':' + item.sort.replace('sort-', '')
    })

    const entity = this.props.config.entity
    this.lastFilter = filter || this.lastFilter
    const query = {
      entity: entity,
      fields: fields,
      pageNo: this.pageNo,
      pageSize: this.pageSize,
      filter: this.lastFilter,
      advFilter: this.advFilterId,
      protocolFilter: wpc.protocolFilter,
      sort: fieldSort,
      reload: this.pageNo === 1,
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

        if (res.data.total > 0) {
          this._pagination.setState({ rowsTotal: res.data.total, pageNo: this.pageNo })
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

    const cellKey = 'row-' + lastPrimary.id + '-' + index
    const width = this.state.fields[index].width || this.__defaultColumnWidth
    let type = field.type
    if (field.field === this.props.config.nameField) {
      cellVal = lastPrimary
      type = '$NAME$'
    } else if (cellVal === '$NOPRIVILEGES$') {
      type = cellVal
    }

    const c = CellRenders.render(cellVal, type, width, cellKey + '.' + field.field)
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
    this._pagination && this._pagination.setState({ selectedTotal: chkSelected })
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

        if (fields[i].sort) $storage.set(this.__sortFieldKey, field + ':' + fields[i].sort)
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

  // Alias `fetchList`
  reload = () => this.fetchList()

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
    if (selected.length === 0 && noWarn !== true) RbHighbar.create($L('UnselectAnySome,Record'))
    return selected
  }

  /**
   * 获取最后查询记录总数
   */
  getLastQueryTotal() {
    return this._pagination ? this._pagination.state.rowsTotal : 0
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
    else if (k.endsWith('.approvalId') && !v) v = $L('UnSubmit')
    else if (k.endsWith('.approvalState') && !v) v = $L('s.ApprovalState.DRAFT')

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
        [{$L('NoPrivileges')}]
      </div>
    </td>
  )
})

CellRenders.addRender('IMAGE', function (v, s, k) {
  v = v || []
  const vLen = v.length
  return (
    <td key={k} className="td-sm">
      <div className="column-imgs" style={s} title={$L('EtcXItems').replace('%d', vLen)}>
        {v.map((item, idx) => {
          if (idx > 2) return null
          const imgUrl = `${rb.baseUrl}/filex/img/${item}`
          const imgName = $fileCutName(item)
          return (
            <a key={'k-' + item} title={imgName} onClick={(e) => CellRenders.clickPreview(v, idx, e)}>
              <img alt={$L('t.IMAGE')} src={`${imgUrl}?imageView2/2/w/100/interlace/1/q/100`} />
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
        <ul className="list-unstyled" title={$L('EtcXItems').replace('%d', vLen)}>
          {v.map((item, idx) => {
            if (idx > 0) return null
            const fileName = $fileCutName(item)
            return (
              <li key={'k-' + item} className="text-truncate">
                <a title={fileName} onClick={(e) => CellRenders.clickPreview(item, null, e)}>
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
      <div style={s} title={$L('EtcXItems').replace('%d', vLen)}>
        {v.map((item, idx) => {
          if (idx > 0) return null
          return (
            <a key={`o-${item.id}`} href={`#!/View/${item.entity}/${item.id}`} onClick={(e) => CellRenders.clickView(item, e)}>
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
        <a href={'mailto:' + v} className="column-url" onClick={(e) => $stopEvent(e)}>
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
        <a href={'tel:' + v} className="column-url" onClick={(e) => $stopEvent(e)}>
          {v}
        </a>
      </div>
    </td>
  )
})

const APPROVAL_STATE_CLAZZs = {
  [$L('s.ApprovalState.PROCESSING')]: 'warning',
  [$L('s.ApprovalState.REJECTED')]: 'danger',
  [$L('s.ApprovalState.APPROVED')]: 'success',
}
CellRenders.addRender('STATE', function (v, s, k) {
  if (k.endsWith('.approvalState')) {
    const badge = APPROVAL_STATE_CLAZZs[v]
    return (
      <td key={k} className="td-sm column-state">
        <div style={s} title={v}>
          <span className={badge ? 'badge badge-' + badge : ''}>{v}</span>
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
            <span key={'opt-' + item} className="badge" title={item}>
              {item}
            </span>
          )
        })}
      </div>
    </td>
  )
})

CellRenders.addRender('AVATAR', function (v, s, k) {
  const imgUrl = `${rb.baseUrl}/filex/img/${v}?imageView2/2/w/100/interlace/1/q/100`
  return (
    <td key={k} className="user-avatar">
      <img src={imgUrl} alt="Avatar" />
    </td>
  )
})

// ~ 分页组件
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
          <div className="dataTables_info" key="page-rowsTotal">
            {this.state.selectedTotal > 0 && <span className="mr-2">{$L('SelectedXRecords').replace('%d', this.state.selectedTotal)}.</span>}
            {this.state.rowsTotal > 0 && <span>{$L('CountXRecords').replace('%d', this.state.rowsTotal)}</span>}
          </div>
        </div>
        <div className="col-12 col-md-8">
          <div className="float-right paging_sizes">
            <select className="form-control form-control-sm" title={$L('PerPageShow')} onChange={this.setPageSize} value={this.state.pageSize || 20}>
              {rb.env === 'dev' && <option value="5">5</option>}
              <option value="20">{$L('XItem').replace('%d', 20)}</option>
              <option value="40">{$L('XItem').replace('%d', 40)}</option>
              <option value="80">{$L('XItem').replace('%d', 80)}</option>
              <option value="100">{$L('XItem').replace('%d', 100)}</option>
              <option value="200">{$L('XItem').replace('%d', 200)}</option>
            </select>
          </div>
          <div className="float-right dataTables_paginate paging_simple_numbers">
            <ul className="pagination">
              {this.state.pageNo > 1 && (
                <li className="paginate_button page-item">
                  <a className="page-link" onClick={() => this.prev()}>
                    <span className="icon zmdi zmdi-chevron-left"></span>
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
                    <li key={`pn-${item}`} className={'paginate_button page-item ' + (this.state.pageNo === item && 'active')}>
                      <a className="page-link" onClick={this.goto.bind(this, item)}>
                        {item}
                      </a>
                    </li>
                  )
              })}
              {this.state.pageNo !== this.__pageTotal && (
                <li className="paginate_button page-item">
                  <a className="page-link" onClick={() => this.next()}>
                    <span className="icon zmdi zmdi-chevron-right"></span>
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
   * @param {JSON} config DataList config
   * @param {Object} entity [Name, Label, Icon]
   * @param {JSON} ep Privileges of the entity
   */
  init: function (config, entity, ep) {
    renderRbcomp(<RbList config={config} uncheckbox={config.uncheckbox} />, 'react-list', function () {
      RbListPage._RbList = this
    })

    const that = this

    $('.J_new').click(() => RbFormModal.create({ title: $L('NewSome').replace('{0}', entity[1]), entity: entity[0], icon: entity[2] }))
    $('.J_edit').click(() => {
      const ids = this._RbList.getSelectedIds()
      if (ids.length >= 1) {
        RbFormModal.create({ id: ids[0], title: $L('EditSome').replace('{0}', entity[1]), entity: entity[0], icon: entity[2] })
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
        location.hash = '!/View/' + entity[0] + '/' + ids[0]
        RbViewModal.create({ id: ids[0], entity: entity[0] })
      }
    })
    $('.J_columns').click(() => RbModal.create(`/p/general/show-fields?entity=${entity[0]}`, $L('SetSome,FieldShow')))

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

    // in `rb-datalist.append.js`
    // eslint-disable-next-line react/jsx-no-undef
    $('.J_export').click(() => renderRbcomp(<DataExport listRef={RbListPage._RbList} entity={entity[0]} />))
    // eslint-disable-next-line react/jsx-no-undef
    $('.J_batch').click(() => renderRbcomp(<BatchUpdate listRef={RbListPage._RbList} entity={entity[0]} />))

    // Privileges
    if (ep) {
      if (ep.C === false) $('.J_new').remove()
      if (ep.D === false) $('.J_delete').remove()
      if (ep.U === false) $('.J_edit, .J_batch').remove()
      if (ep.A !== true) $('.J_assign').remove()
      if (ep.S !== true) $('.J_share, .J_unshare').remove()
      $cleanMenu('.J_action')
    }

    // Quick search
    const $btn = $('.input-search .btn'),
      $input = $('.input-search input')
    $btn.click(() => this._RbList.searchQuick())
    $input.keydown((e) => (e.which === 13 ? $btn.trigger('click') : true))
  },

  reload() {
    this._RbList.reload()
  },
}

// 高级查询操作类
const AdvFilters = {
  /**
   * @param {Element} el 控件
   * @param {String} entity 实体
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
    const lastFilter = $storage.get(RbListPage._RbList.__defaultFilterKey)

    const that = this
    let $defaultFilter

    $.get(`/app/${this.__entity}/advfilter/list`, function (res) {
      $('.adv-search .J_custom').each(function () {
        $(this).remove()
      })

      const $menu = $('.adv-search .dropdown-menu')
      $(res.data).each(function () {
        const item = this
        const $item = $(`<div class="dropdown-item J_custom" data-id="${item.id}"><a class="text-truncate">${item.name}</a></div>`).appendTo($menu)
        $item.click(() => that.__effectFilter($item, 'aside'))

        if (lastFilter === item.id) $defaultFilter = $item

        // 可修改
        if (item.editable) {
          const $action = $(`<div class="action"><a title="${$L('Modify')}"><i class="zmdi zmdi-edit"></i></a><a title="${$L('Delete')}"><i class="zmdi zmdi-delete"></i></a></div>`).appendTo($item)

          $action.find('a:eq(0)').click(function () {
            that.showAdvFilter(item.id)
            $('.adv-search .btn.dropdown-toggle').dropdown('toggle')
            return false
          })

          $action.find('a:eq(1)').click(function () {
            RbAlert.create($L('DeleteSomeConfirm,AdvFilter'), {
              type: 'danger',
              confirmText: $L('Delete'),
              confirm: function () {
                this.disabled(true)
                $.post(`/app/entity/record-delete?id=${item.id}`, (res) => {
                  if (res.error_code === 0) {
                    this.hide()
                    that.loadFilters()
                    if (lastFilter === item.id) {
                      RbListPage._RbList.setAdvFilter(null)
                      $('.adv-search .J_name').text($L('AllDatas'))
                    }
                  } else {
                    RbHighbar.error(res.error_msg)
                  }
                })
              },
            })
            return false
          })
        }
      })

      // ASIDE
      if ($('#asideFilters').length > 0) {
        const $ghost = $('.adv-search .dropdown-menu').clone()
        $ghost.removeAttr('class')
        $ghost.removeAttr('style')
        $ghost.removeAttr('data-ps-id')
        $ghost.find('.ps-scrollbar-x-rail, .ps-scrollbar-y-rail').remove()
        $ghost.find('.dropdown-item').click(function () {
          $ghost.find('.dropdown-item').removeClass('active')
          $(this).addClass('active')
          that.__effectFilter($(this), 'aside')
        })
        $ghost.appendTo($('#asideFilters').empty())
      }

      if (!$defaultFilter) $defaultFilter = $('.adv-search .dropdown-item:eq(0)')
      $defaultFilter.trigger('click')
    })
  },

  __effectFilter(item, rel) {
    this.current = item.data('id')
    $('.adv-search .J_name').text(item.find('>a').text())
    if (rel === 'aside') {
      const current_id = this.current
      $('#asideFilters .dropdown-item')
        .removeClass('active')
        .each(function () {
          if ($(this).data('id') === current_id) {
            $(this).addClass('active')
            return false
          }
        })
    }

    if (this.current === '$ALL$') this.current = null
    RbListPage._RbList.setAdvFilter(this.current)
  },

  showAdvFilter(id, useCopyId) {
    const props = { entity: this.__entity, inModal: true, fromList: true, confirm: this.saveFilter }
    if (!id) {
      if (this.__customAdv) {
        this.__customAdv.show()
      } else {
        const that = this
        if (useCopyId) {
          this.__getFilter(useCopyId, (res) => {
            renderRbcomp(<AdvFilter {...props} filter={res.filter} />, null, function () {
              that.__customAdv = this
            })
          })
        } else {
          renderRbcomp(<AdvFilter {...props} />, null, function () {
            that.__customAdv = this
          })
        }
      }
    } else {
      this.current = id
      this.__getFilter(id, (res) => {
        renderRbcomp(<AdvFilter {...props} title={$L('ModifyFilterItem')} filter={res.filter} filterName={res.name} shareTo={res.shareTo} />)
      })
    }
  },

  saveFilter(filter, name, shareTo) {
    if (!filter) return
    const that = AdvFilters
    let url = `/app/${that.__entity}/advfilter/post?id=${that.current || ''}`
    if (name) url += '&name=' + $encode(name)
    if (shareTo) url += '&shareTo=' + $encode(shareTo)

    $.post(url, JSON.stringify(filter), (res) => {
      if (res.error_code === 0) {
        $storage.set(RbListPage._RbList.__defaultFilterKey, res.data.id)
        that.loadFilters()
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  },

  __getFilter(id, call) {
    $.get(`/app/entity/advfilter/get?id=${id}`, (res) => call(res.data))
  },
}

// init: DataList
$(document).ready(() => {
  const via = $urlp('via', location.hash)
  if (via) {
    wpc.protocolFilter = `via:${via}`
    const $cleanVia = $(`<div class="badge filter-badge">${$L('DatasFiltered')}<a class="close" title="${$L('ViewAllDatas')}">&times;</a></div>`).appendTo('.dataTables_filter')
    $cleanVia.find('a').click(() => {
      wpc.protocolFilter = null
      RbListPage.reload()
      $cleanVia.remove()
    })
  }

  const gs = $urlp('gs', location.hash)
  if (gs) $('.search-input-gs, .input-search>input').val($decode(gs))
  if (wpc.entity) {
    RbListPage.init(wpc.listConfig, wpc.entity, wpc.privileges)
    if (wpc.advFilter !== false) AdvFilters.init('.adv-search', wpc.entity[0])
  }
})

// -- for View
// ~~视图窗口（右侧滑出）
class RbViewModal extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, inLoad: true, isHide: true, isDestroy: false }
    this.mcWidth = this.props.subView === true ? 1344 : 1404
    if ($(window).width() < 1464) this.mcWidth -= 184
  }

  render() {
    return (
      !this.state.isDestroy && (
        <div className="modal-wrapper">
          <div className="modal rbview" ref={(c) => (this._rbview = c)}>
            <div className="modal-dialog">
              <div className="modal-content" style={{ width: this.mcWidth + 'px' }}>
                <div className={'modal-body iframe rb-loading ' + (this.state.inLoad === true && 'rb-loading-active')}>
                  <iframe ref={(c) => (this._iframe = c)} className={this.state.isHide ? 'invisible' : ''} src={this.state.showAfterUrl || 'about:blank'} frameBorder="0" scrolling="no"></iframe>
                  <RbSpinner />
                </div>
              </div>
            </div>
          </div>
        </div>
      )
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
          that.setState({ isDestroy: true }, () => {
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
   * 当前激活视图
   */
  static currentHolder(reload) {
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
    ECHART_BASE.grid = { left: 40, right: 20, top: 30, bottom: 20 }

    $('.J_load-chart').click(() => {
      if (this.chartLoaded !== true) this.loadWidget()
    })
    $('.J_add-chart').click(() => this.showChartSelect())

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
    renderRbcomp(detectChart({ ...chart, isManageable: true }, chart.chart), $w, function () {
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
  // 自动打开 View
  let viewHash = location.hash
  if (viewHash && viewHash.startsWith('#!/View/') && (wpc.type === 'RecordList' || wpc.type === 'DetailList')) {
    viewHash = viewHash.split('/')
    if (viewHash.length === 4 && viewHash[3].length === 20) {
      setTimeout(() => {
        RbViewModal.create({ entity: viewHash[2], id: viewHash[3] })
      }, 500)
    }
  } else if (viewHash === '#!/New') {
    $('.J_new').trigger('click')
  }

  // ASIDE
  if ($('#asideFilters, #asideWidgets').length > 0) {
    $('.side-toggle').click(() => {
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
})
