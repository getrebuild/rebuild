/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global FieldValueSet */
// 列表公共操作

const _RbList = function () {
  return RbListPage._RbList || {}
}

// ~~ 高级查询操作类

const AdvFilters = {
  /**
   * @param {Element} el 控件
   * @param {String} entity 实体
   */
  init(el, entity) {
    this.__el = $(el)
    this.__entity = entity

    this.__el.find('.J_advfilter').on('click', () => {
      // this.showAdvFilter(null, this.current)  // 2.9.4 取消 useCopyId，可能引起误解
      this.showAdvFilter()
      this.current = null
    })
    const $all = $('.adv-search .dropdown-item:eq(0)')
    $all.on('click', () => this._effectFilter($all, 'aside'))

    this.loadFilters()

    this.__savedCached = []
  },

  loadFilters() {
    const lastFilter = $storage.get(_RbList().__defaultFilterKey)

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
        $item.on('click', () => that._effectFilter($item, 'aside'))

        if (lastFilter === item.id) $defaultFilter = $item

        // 可修改
        if (item.editable) {
          const $action = $(
            `<div class="action"><a title="${$L('修改')}"><i class="zmdi zmdi-edit"></i></a><a title="${$L('删除')}" class="danger-hover"><i class="zmdi zmdi-delete"></i></a></div>`
          ).appendTo($item)

          $action.find('a:eq(0)').on('click', function () {
            that.showAdvFilter(item.id)
            $('.adv-search .btn.dropdown-toggle').dropdown('toggle')
            return false
          })

          $action.find('a:eq(1)').on('click', function () {
            RbAlert.create($L('确认删除此高级查询？'), {
              type: 'danger',
              confirmText: $L('删除'),
              confirm: function () {
                this.disabled(true)
                $.post(`/app/entity/common-delete?id=${item.id}`, (res) => {
                  if (res.error_code === 0) {
                    this.hide()
                    that.loadFilters()
                    if (lastFilter === item.id) {
                      _RbList().setAdvFilter(null)
                      $('.adv-search .J_name').text($L('全部数据'))
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
        $ghost.find('.dropdown-item').on('click', function () {
          $ghost.find('.dropdown-item').removeClass('active')
          $(this).addClass('active')
          that._effectFilter($(this), 'aside')
        })
        $ghost.appendTo($('#asideFilters').empty())

        if ($ghost.find('.dropdown-item').length < 2 && typeof window.startTour === 'function') {
          const $hub = $('<div class="mt-3"></div>').appendTo($('#asideFilters'))
          renderRbcomp(
            <RbAlertBox
              type="info"
              message={
                <RF>
                  <a className="mr-1" href="#" onClick={(e) => AdvFilters._showAddCommonQuery(e)}>
                    {$L('添加')}
                  </a>
                  {$L('常用查询方便以后使用')}
                </RF>
              }
            />,
            $hub
          )
        }
      }

      if (!$defaultFilter) $defaultFilter = $('.adv-search .dropdown-item:eq(0)')
      $defaultFilter.trigger('click')
    })
  },

  _effectFilter(item, rel) {
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
    _RbList().setAdvFilter(this.current)
  },

  _showAddCommonQuery(e) {
    $stopEvent(e, true)
    if (this._AddCommonQuery) {
      this._AddCommonQuery.show()
    } else {
      const that = this
      renderRbcomp(<AddCommonQuery entity={this.__entity} />, null, function () {
        that._AddCommonQuery = this
      })
    }
  },

  showAdvFilter(id, useCopyId) {
    const props = {
      entity: this.__entity,
      inModal: true,
      fromList: true,
      confirm: this.saveFilter,
    }

    if (!id) {
      if (this.__customAdv) {
        this.__customAdv.show()
      } else {
        const that = this
        if (useCopyId) {
          this._getFilter(useCopyId, (res) => {
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
      if (this.__savedCached[id]) {
        const res = this.__savedCached[id]
        renderRbcomp(<AdvFilter {...props} title={$L('修改高级查询')} filter={res.filter} filterName={res.name} shareTo={res.shareTo} />)
      } else {
        this._getFilter(id, (res) => {
          this.__savedCached[id] = res
          renderRbcomp(<AdvFilter {...props} title={$L('修改高级查询')} filter={res.filter} filterName={res.name} shareTo={res.shareTo} />)
        })
      }
    }
  },

  saveFilter(filter, name, shareTo) {
    if (!filter) return
    const that = AdvFilters
    let url = `/app/${that.__entity}/advfilter/post?id=${that.current || ''}`
    if (name) url += `&name=${$encode(name)}`
    if (shareTo) url += `&shareTo=${$encode(shareTo)}`

    $.post(url, JSON.stringify(filter), (res) => {
      if (res.error_code === 0) {
        $storage.set(_RbList().__defaultFilterKey, res.data.id)
        that.loadFilters()
        if (that.current) that.__savedCached[that.current] = null
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  },

  _getFilter(id, call) {
    $.get(`/app/entity/advfilter/get?id=${id}`, (res) => call(res.data))
  },
}

class AddCommonQuery extends RbFormHandler {
  render() {
    const defs = [
      [$L('今天新增的'), 'DAY_NEW'],
      [$L('本周新增的'), 'WEEK_NEW'],
      [$L('本月新增的'), 'MONTH_NEW'],
      [],
      [$L('属于我的'), 'MY_OWN'],
      [$L('我创建的'), 'MY_CREATE'],
      [$L('我修改的'), 'MY_MODIFY'],
    ]

    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$L('添加常用查询')}>
        <RbAlertBox type="info" message={$L('可在添加后修改这些查询，以便更适合自己的使用需要')} />

        <div ref={(c) => (this._$chks = c)}>
          {defs.map((item, idx) => {
            if (item.length === 0) return <br key={idx} />
            return (
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline w-25" key={item[1]}>
                <input className="custom-control-input" type="checkbox" value={item[1]} />
                <span className="custom-control-label"> {item[0]}</span>
              </label>
            )
          })}
        </div>
        <div className="dialog-footer" ref={(c) => (this._btns = c)}>
          <button className="btn btn-primary" type="button" onClick={() => this.saveAdd()}>
            {$L('确定')}
          </button>
          <button className="btn btn-secondary" onClick={() => this.hide()} type="button">
            {$L('取消')}
          </button>
        </div>
      </RbModal>
    )
  }

  saveAdd() {
    const adds = []
    $(this._$chks)
      .find('input')
      .each(function () {
        const $this = $(this)
        if ($this.prop('checked')) adds.push($this.val())
      })

    if (adds.length === 0) return RbHighbar.create($L('请选择要添加的常用查询'))

    this.disabled(true)
    $.post(`/app/${this.props.entity}/advfilter/add-commons?adds=${adds.join(',')}`, (res) => {
      this.disabled()
      if (res.error_code !== 0) return RbHighbar.error(res.error_msg)

      AdvFilters.loadFilters()
      this.hide()
    })
  }
}

// ~~ 列表记录批量操作

class BatchOperator extends RbFormHandler {
  constructor(props) {
    super(props)
    this.state.dataRange = 2
  }

  render() {
    const _listRef = this.props.listRef
    const selectedRows = _listRef.getSelectedIds(true).length
    const pageRows = _listRef.state.rowsData.length
    const queryRows = _listRef.getLastQueryTotal()

    return (
      <RbModal title={this._title} disposeOnHide={true} ref={(c) => (this._dlg = c)}>
        <div className="form batch-form">
          <div className="form-group">
            <label className="text-bold">{$L('选择数据范围')}</label>
            <div>
              {selectedRows > 0 && (
                <label className="custom-control custom-control-sm custom-radio mb-2">
                  <input className="custom-control-input" name="dataRange" type="radio" checked={~~this.state.dataRange === 1} value="1" onChange={this.handleChange} />
                  <span className="custom-control-label">
                    {$L('选中的数据')} ({$L('共 %d 条', selectedRows)})
                  </span>
                </label>
              )}
              <label className="custom-control custom-control-sm custom-radio mb-2">
                <input className="custom-control-input" name="dataRange" type="radio" checked={~~this.state.dataRange === 2} value="2" onChange={this.handleChange} />
                <span className="custom-control-label">
                  {$L('当前页的数据')} ({$L('共 %d 条', pageRows)})
                </span>
              </label>
              <label className="custom-control custom-control-sm custom-radio mb-2">
                <input className="custom-control-input" name="dataRange" type="radio" checked={~~this.state.dataRange === 3} value="3" onChange={this.handleChange} />
                <span className="custom-control-label">
                  {$L('查询后的数据')} ({$L('共 %d 条', queryRows)})
                </span>
              </label>
              <label className="custom-control custom-control-sm custom-radio mb-1">
                <input className="custom-control-input" name="dataRange" type="radio" checked={~~this.state.dataRange === 10} value="10" onChange={this.handleChange} />
                <span className="custom-control-label">{$L('全部数据')}</span>
              </label>
            </div>
          </div>

          {this.renderOperator()}
        </div>

        <div className="dialog-footer" ref={(c) => (this._btns = c)}>
          <a className="btn btn-link btn-space" onClick={this.hide}>
            {$L('取消')}
          </a>
          <button className="btn btn-primary btn-space" type="button" onClick={this.confirm}>
            {$L('确定')}
          </button>
        </div>
      </RbModal>
    )
  }

  getQueryData() {
    const qd = this.props.listRef.getLastQueryEntry()
    if (~~this.state.dataRange === 1) qd._selected = this.props.listRef.getSelectedIds(true).join('|')
    return qd
  }

  // 子类复写

  renderOperator() {}

  confirm = () => {}
}

// ~ 数据导出

// eslint-disable-next-line no-unused-vars
class DataExport extends BatchOperator {
  constructor(props) {
    super(props)
    this._title = $L('数据导出')
  }

  confirm = () => {
    const useReport = $(this._$report).val()
    if (useReport !== '0' && rb.commercial < 1) {
      RbHighbar.error(WrapHtml($L('免费版不支持使用报表模板 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return false
    }

    this.disabled(true)
    $.post(`/app/${this.props.entity}/export/submit?dr=${this.state.dataRange}&report=${useReport}`, JSON.stringify(this.getQueryData()), (res) => {
      if (res.error_code === 0) {
        this.hide()
        window.open(`${rb.baseUrl}/filex/download/${res.data.fileKey}?temp=yes&attname=${$encode(res.data.fileName)}`)
      } else {
        this.disabled(false)
        RbHighbar.error(res.error_msg)
      }
    })
  }

  renderOperator() {
    return (
      <div className="form-group">
        <label className="text-bold">
          {$L('使用报表模板')} <sup className="rbv" title={$L('增值功能')} />
        </label>
        <select className="form-control form-control-sm w-50" ref={(c) => (this._$report = c)}>
          <option value="0">{$L('不使用')}</option>
          {(this.state.reports || []).map((item) => {
            return (
              <option value={item.id} key={item.id}>
                {item.name}
              </option>
            )
          })}
        </select>

        {this.state.reports && this.state.reports.length === 0 && (
          <p className="text-muted mt-1 mb-0">
            {$L('暂无报表模板')}
            {rb.isAdminUser && (
              <a className="icon-link ml-2" target="_blank" href={`${rb.baseUrl}/admin/data/report-templates`}>
                <i className="zmdi zmdi-settings" /> {$L('点击配置')}
              </a>
            )}
          </p>
        )}
      </div>
    )
  }

  componentDidMount() {
    $.get(`/app/${this.props.entity}/report/available?type=2`, (res) => this.setState({ reports: res.data }))
  }
}

// ~ 批量修改

// eslint-disable-next-line no-unused-vars
class BatchUpdate extends BatchOperator {
  constructor(props) {
    super(props)
    this._title = $L('批量修改')
  }

  componentDidMount() {
    $.get(`/app/${this.props.entity}/batch-update/fields`, (res) => this.setState({ fields: res.data }))
  }

  renderOperator() {
    return (
      <div className="form-group">
        <label className="text-bold">{$L('修改内容')}</label>
        <div>
          <div className="batch-contents">
            {(this.state.updateContents || []).map((item) => {
              const field = this.state.fields.find((x) => item.field === x.name)
              return (
                <div key={item.field}>
                  <div className="row">
                    <div className="col-4">
                      <a className="del" onClick={() => this.delItem(item.field)} title={$L('移除')}>
                        <i className="zmdi zmdi-close" />
                      </a>
                      <span className="badge badge-light">{field.label}</span>
                    </div>
                    <div className="col-2 pl-0 pr-0">
                      <span className="badge badge-light">{BUE_OPTYPES[item.op]}</span>
                    </div>
                    <div className="col-6">{item.op !== 'NULL' && <span className="badge badge-light text-break">{FieldValueSet.formatFieldText(item.value, field)}</span>}</div>
                  </div>
                </div>
              )
            })}
          </div>
          <div className="mt-2">
            {this.state.fields && <BatchUpdateEditor ref={(c) => (this._editor = c)} fields={this.state.fields} entity={this.props.entity} />}
            <div className="mt-1">
              <button className="btn btn-primary btn-sm btn-outline" onClick={this.addItem} type="button">
                + {$L('添加')}
              </button>
            </div>
          </div>
        </div>
      </div>
    )
  }

  addItem = () => {
    const item = this._editor.buildItem()
    if (!item) return

    const contents = this.state.updateContents || []
    const exists = contents.find((x) => item.field === x.field)
    if (exists) {
      RbHighbar.create($L('修改字段已经存在'))
      return
    }

    contents.push(item)
    this.setState({ updateContents: contents })
  }

  delItem(fieldName) {
    const contents = []
    this.state.updateContents.forEach((item) => {
      if (fieldName !== item.field) contents.push(item)
    })
    this.setState({ updateContents: contents })
  }

  confirm = () => {
    if (!this.state.updateContents || this.state.updateContents.length === 0) {
      RbHighbar.create($L('请添加修改内容'))
      return
    }

    const _data = {
      queryData: this.getQueryData(),
      updateContents: this.state.updateContents,
    }
    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log(JSON.stringify(_data))

    const that = this
    RbAlert.create($L('请再次确认修改数据范围和修改内容。开始修改吗？'), {
      confirm: function () {
        this.hide()
        that.disabled(true)
        $.post(`/app/${that.props.entity}/batch-update/submit?dr=${that.state.dataRange}`, JSON.stringify(_data), (res) => {
          if (res.error_code === 0) {
            const mp_parent = $(that._dlg._element).find('.modal-body').attr('id')
            const mp = new Mprogress({ template: 2, start: true, parent: `#${mp_parent}` })

            that.__checkState(res.data, mp)
          } else {
            that.disabled(false)
            RbHighbar.error(res.error_msg)
          }
        })
      },
    })
  }

  __checkState(taskid, mp) {
    $.get(`/commons/task/state?taskid=${taskid}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.hasError) {
          mp && mp.end()
          RbHighbar.error(res.data.hasError)
          return
        }

        const cp = res.data.progress
        if (cp >= 1) {
          mp && mp.end()
          $(this._btns).find('.btn-primary').text($L('已完成'))
          RbHighbar.success($L('成功修改 %d 条记录', res.data.succeeded))
          setTimeout(() => {
            this.hide()
            window.RbListPage && window.RbListPage.reload()
          }, 500)
        } else {
          mp && mp.set(cp)
          setTimeout(() => this.__checkState(taskid, mp), 1000)
        }
      }
    })
  }
}

const BUE_OPTYPES = {
  SET: $L('修改为'),
  NULL: $L('置空'),
  // TODO 支持更多修改模式
  // PREFIX: $L('前添加'),
  // SUFFIX: $L('后添加'),
  // PLUS: $L('加上'),
  // MINUS: $L('减去'),
}

// ~ 批量修改编辑器
class BatchUpdateEditor extends React.Component {
  state = { ...this.props, selectOp: 'SET' }

  componentDidMount() {
    const $field2s = $(this._$field)
      .select2({
        allowClear: false,
      })
      .on('change', () => {
        this.setState({ selectField: $field2s.val() }, () => this._renderFieldValueSet())
      })
    const $op2s = $(this._$op)
      .select2({
        allowClear: false,
      })
      .on('change', () => {
        this.setState({ selectOp: $op2s.val() })
      })

    $field2s.trigger('change')
    this.__select2 = [$field2s, $op2s]
  }

  componentWillUnmount() {
    this.__select2.forEach((item) => item.select2('destroy'))
    this.__select2 = null
  }

  render() {
    if (this.props.fields.length === 0) {
      return <div className="text-danger">{$L('没有可修改字段')}</div>
    }

    return (
      <div className="row">
        <div className="col-4">
          <select className="form-control form-control-sm" ref={(c) => (this._$field = c)}>
            {this.props.fields.map((item) => {
              return (
                <option value={item.name} key={item.name}>
                  {item.label}
                </option>
              )
            })}
          </select>
        </div>
        <div className="col-2 pl-0 pr-0">
          <select className="form-control form-control-sm" ref={(c) => (this._$op = c)}>
            <option value="SET">{BUE_OPTYPES['SET']}</option>
            <option value="NULL">{BUE_OPTYPES['NULL']}</option>
          </select>
        </div>
        <div className="col-6">
          <div className={`${this.state.selectOp === 'NULL' ? 'hide' : ''}`}>
            {this.state.selectFieldObj && <FieldValueSet entity={this.props.entity} field={this.state.selectFieldObj} placeholder={$L('新值')} ref={(c) => (this._FieldValue = c)} />}
          </div>
        </div>
      </div>
    )
  }

  _renderFieldValueSet() {
    if (this.state.selectOp === 'NULL') return null // set Null
    const field = this.props.fields.find((x) => this.state.selectField === x.name)
    this.setState({ selectFieldObj: null }, () => this.setState({ selectFieldObj: field }))
  }

  buildItem() {
    const d = {
      field: this.state.selectField,
      op: this.state.selectOp,
    }

    const field = this.props.fields.find((x) => this.state.selectField === x.name)
    if (d.op === 'NULL') {
      if (!field.nullable) {
        RbHighbar.create($L('%s 不能为空', field.label))
        return null
      } else {
        return d
      }
    }

    d.value = this._FieldValue.val()
    if (!d.value) return null
    else return d
  }
}

// eslint-disable-next-line no-unused-vars
const RbListCommon = {
  inUrlViewId: null,

  init: function (wpc) {
    // 全局搜索
    const gs = $urlp('gs', location.hash)
    if (gs) $('.search-input-gs, .input-search>input').val($decode(gs))

    // 快速查询
    const $btn = $('.input-search .input-group-btn .btn'),
      $input = $('.input-search input')
    $btn.on('click', () => _RbList().searchQuick())
    $input.on('keydown', (e) => {
      e.which === 13 && $btn.trigger('click')
    })
    $('.input-search .btn-input-clear').on('click', () => {
      $input.val('')
      $btn.trigger('click')
    })

    // Via 过滤
    const via = $urlp('via') || $urlp('via', location.hash)
    if (via) {
      wpc.protocolFilter = `via:${via}`
      const $cleanVia = $(`<div class="badge filter-badge J_via-filter">${$L('当前数据已过滤')}<a class="close" title="${$L('查看全部数据')}">&times;</a></div>`).appendTo('.dataTables_filter')
      $cleanVia.find('a').on('click', () => {
        wpc.protocolFilter = null
        RbListPage.reload()
        $cleanVia.remove()
      })
    }

    const entity = wpc.entity

    RbListPage.init(wpc.listConfig, entity, wpc.privileges)
    if (wpc.advFilter !== false) AdvFilters.init('.adv-search', entity[0])

    // 新建
    $('.J_new').on('click', () => RbFormModal.create({ title: $L('新建%s', entity[1]), entity: entity[0], icon: entity[2] }))
    // 导出
    $('.J_export').on('click', () => renderRbcomp(<DataExport listRef={_RbList()} entity={entity[0]} />))
    // 批量修改
    $('.J_batch').on('click', () => renderRbcomp(<BatchUpdate listRef={_RbList()} entity={entity[0]} />))

    // 自动打开新建
    if (location.hash === '#!/New') {
      setTimeout(() => $('.J_new').trigger('click'), 200)
    }
  },
}
