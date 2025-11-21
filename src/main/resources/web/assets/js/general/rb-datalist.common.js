/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global FieldValueSet, ListAdvFilter, LiteFormModal */
// 列表公共操作

const _RbList = function () {
  return RbListPage._RbList || {}
}

// ~ 高级查询操作

const AdvFilters = {
  /**
   * @param {Element} el 控件
   * @param {String} entity 实体
   */
  init(el, entity) {
    this.__el = $(el)
    this.__entity = entity

    this.__el.find('.J_filterbtn').on('click', () => {
      this.current = null
      this.showAdvFilter()
    })

    this.__$customAdvWrap = $('#dropdown-menu-advfilter')
    $(document.body).on('click', (e) => {
      if (!e.target) return
      const $target = $(e.target)
      if (
        $target.hasClass('J_filterbtn') ||
        $target.parent().hasClass('J_filterbtn') ||
        $target.hasClass('dropdown-menu-advfilter') ||
        $target.parents('.dropdown-menu-advfilter')[0] ||
        $target.hasClass('modal') ||
        $target.parents('.modal')[0] ||
        $target.parents('.select2-container')[0] ||
        $target.hasClass('select2-selection__choice__remove')
      ) {
        return
      }
      if (this.__customAdv && !this.__$customAdvWrap.hasClass('hide')) {
        this.__$customAdvWrap.addClass('hide')
      }
    })

    const $alld = $('.adv-search .dropdown-item:eq(0)')
    $alld.on('click', () => this._effectFilter($alld, 'aside'))

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
        const $item = $(`<div class="dropdown-item J_custom" data-id="${item.id}"><a class="text-truncate"></a></div>`).appendTo($menu)
        $item
          .on('click', () => that._effectFilter($item, 'aside'))
          .find('>a')
          .text(item.name)

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
            RbAlert.create(<b>{$L('确认删除此高级查询？')}</b>, {
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
                    this.disabled()
                  }
                })
              },
            })
            return false
          })
        }
      })

      // ASIDE
      if ($('#asideFilters, .quick-filter-tabs').length > 0) {
        const $ghost = $('.adv-search .dropdown-menu').clone()
        $ghost.removeAttr('class')
        $ghost.removeAttr('style')
        $ghost.removeAttr('data-ps-id')
        $ghost.find('.ps-scrollbar-x-rail, .ps-scrollbar-y-rail, .action').remove()
        $ghost.find('.dropdown-item').on('click', function () {
          $ghost.find('.dropdown-item').removeClass('active')
          $(this).addClass('active')
          that._effectFilter($(this), 'aside')
        })

        $ghost.clone(true).appendTo($('#asideFilters').empty())
        $ghost.clone(true).appendTo($('.quick-filter-tabs').empty())
      }

      if (!$defaultFilter) $defaultFilter = $('.adv-search .dropdown-item:eq(0)')
      $defaultFilter.trigger('click')
    })
  },

  _effectFilter(item, rel) {
    this.current = item.data('id')
    $('.adv-search .J_name').text(item.find('>a').text())
    if (rel === 'aside') {
      const current = this.current
      $('#asideFilters .dropdown-item')
        .removeClass('active')
        .each(function () {
          if ($(this).data('id') === current) {
            $(this).addClass('active')
            return false
          }
        })
      $('.quick-filter-tabs .dropdown-item')
        .removeClass('active')
        .each(function () {
          if ($(this).data('id') === current) {
            $(this).addClass('active')
            return false
          }
        })
    }

    if (this.current === '$ALL$') this.current = null
    _RbList().setAdvFilter(this.current)
  },

  showAdvFilter(id, useCopyId) {
    const that = this

    const props = {
      entity: this.__entity,
      id: id || null,
      onConfirm: function (id) {
        $storage.set(_RbList().__defaultFilterKey, id)
        that.loadFilters()
        that.__savedCached[id] = null
      },
    }

    if (!id) {
      if (this.__customAdv) {
        this.__$customAdvWrap.toggleClass('hide')
      } else {
        // `useCopyId` 2.9.4 取消 useCopyId，可能引起误解
        if (useCopyId) {
          this._getFilter(useCopyId, (res) => {
            renderRbcomp(<ListAdvFilter {...props} filter={res.filter} />, this.__$customAdvWrap, function () {
              that.__customAdv = this
            })
          })
        } else {
          const storageKey = `CustomAdv-${props.entity}`
          let storageFilter = localStorage.getItem(storageKey)
          if (storageFilter) storageFilter = JSON.parse(storageFilter)

          renderRbcomp(<ListAdvFilter {...props} filter={storageFilter} />, this.__$customAdvWrap, function () {
            that.__customAdv = this
          })
        }
      }
    } else {
      this.current = id
      if (this.__savedCached[id]) {
        const res = this.__savedCached[id]
        renderRbcomp(<ListAdvFilter {...props} title={$L('修改高级查询')} filter={res.filter} filterName={res.name} shareTo={res.shareTo} inModal />)
      } else {
        this._getFilter(id, (res) => {
          this.__savedCached[id] = res
          renderRbcomp(<ListAdvFilter {...props} title={$L('修改高级查询')} filter={res.filter} filterName={res.name} shareTo={res.shareTo} inModal />)
        })
      }
    }
  },

  _getFilter(id, call) {
    $.get(`/app/entity/advfilter/get?id=${id}`, (res) => call(res.data))
  },
}

// ~ 列表记录批量操作

class BatchOperator extends RbFormHandler {
  constructor(props) {
    super(props)
    this.state.dataRange = props.listRef.getSelectedIds(true).length > 0 ? 1 : 2
  }

  render() {
    const _listRef = this.props.listRef
    const selectedRows = _listRef.getSelectedIds(true).length
    const pageRows = _listRef.state.rowsData.length
    const queryRows = _listRef.getLastQueryTotal()

    return (
      <RbModal title={this._title} ref={(c) => (this._dlg = c)} disposeOnHide>
        <div className="form batch-form">
          <div className="form-group">
            <label className="text-bold">{$L('选择数据范围')}</label>
            <div>
              {selectedRows > 0 && (
                <label className="custom-control custom-control-sm custom-radio mb-2">
                  <input className="custom-control-input" name="dataRange" type="radio" checked={~~this.state.dataRange === 1} value="1" onChange={this.handleChange} />
                  <span className="custom-control-label">
                    {$L('选中的记录')} ({$L('共 %d 条', selectedRows)})
                  </span>
                </label>
              )}
              <label className="custom-control custom-control-sm custom-radio mb-2">
                <input className="custom-control-input" name="dataRange" type="radio" checked={~~this.state.dataRange === 2} value="2" onChange={this.handleChange} />
                <span className="custom-control-label">
                  {$L('当前页的记录')} ({$L('共 %d 条', pageRows)})
                </span>
              </label>
              <label className="custom-control custom-control-sm custom-radio mb-2">
                <input className="custom-control-input" name="dataRange" type="radio" checked={~~this.state.dataRange === 3} value="3" onChange={this.handleChange} />
                <span className="custom-control-label">
                  {$L('查询后的记录')} ({$L('共 %d 条', queryRows)})
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
          <button className="btn btn-secondary btn-spacem mr-2" type="button" onClick={() => this.handleCancel()}>
            {$L('取消')}
          </button>
          <button className="btn btn-primary btn-space mr-1" type="button" onClick={() => this.handleConfirm()}>
            {this._confirmText || $L('确定')}
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

  handleConfirm() {}

  handleCancel() {
    if (!this._taskid) {
      this.hide()
      return
    }

    const that = this
    RbAlert.create($L('是否取消/终止当前操作？'), {
      onConfirm: function () {
        if (!that._taskid) {
          this.hide()
          return
        }

        this.disabled(true)
        $.post(`/commons/task/cancel?taskid=${that._taskid}`, (res) => {
          if (res.error_code !== 0) {
            RbHighbar.error(res.error_msg)
          } else {
            $(that._btns).find('.btn-secondary').button('loading')
            this.hide()
          }
        })
      },
    })
  }
}

// ~ 数据导出

// eslint-disable-next-line no-unused-vars
class DataExport extends BatchOperator {
  constructor(props) {
    super(props)
    this._title = $L('数据导出')
    this._confirmText = $L('导出')
  }

  renderOperator() {
    const reports = this.state.reports || []
    return (
      <div className="form-group">
        <label className="text-bold">{$L('选择导出格式')}</label>
        <div style={{ width: 325 }}>
          <select className="form-control form-control-sm" ref={(c) => (this._$report = c)} defaultValue="xls">
            <option value="csv">CSV</option>
            <option value="xls">Excel</option>
            <optgroup label={$L('使用报表模板')}>
              {reports.map((item) => {
                const outputType = item.outputType || 'excel'
                return (
                  <RF key={item.id}>
                    {outputType.includes('excel') && <option value={`${item.id}`}>{item.name}</option>}
                    {outputType.includes('pdf') && <option value={`${item.id}&output=pdf`}>{item.name} (PDF)</option>}
                  </RF>
                )
              })}
              {reports.length === 0 && <option disabled>{$L('无')}</option>}
            </optgroup>
          </select>
        </div>
      </div>
    )
  }

  handleConfirm() {
    const useReport = $(this._$report).val() || 'csv'
    this.disabled(true)
    $.post(`/app/${this.props.entity}/export/submit?dr=${this.state.dataRange}&report=${useReport}`, JSON.stringify(this.getQueryData()), (res) => {
      if (res.error_code === 0) {
        this.hide()
        $openWindow(`${rb.baseUrl}/filex/download/${res.data.fileKey}?temp=yes&attname=${$encode(res.data.fileName)}`)
      } else {
        RbHighbar.error(res.error_msg)
        this.disabled(false)
      }
    })
  }

  componentDidMount() {
    $.get(`/app/${this.props.entity}/report/available?type=2`, (res) => {
      this.setState({ reports: res.data }, () => {
        this.__select2 = $(this._$report).select2({
          templateResult: function (res) {
            const text = res.text.split(' (PDF)')
            const $span = $('<span></span>').text(text[0])
            if (text.length > 1) $('<span class="badge badge-default badge-pill">PDF</span>').appendTo($span)
            return $span
          },
        })
      })
    })
  }
}

// ~ 批量修改

let BatchUpdate__taskid
window.onbeforeunload = function () {
  if (BatchUpdate__taskid) return 'SHOW-CLOSE-CONFIRM'
}

// eslint-disable-next-line no-unused-vars
class BatchUpdate extends BatchOperator {
  constructor(props) {
    super(props)
    this._title = $L('批量修改')
    this._confirmText = $L('修改')
  }

  componentDidMount() {
    $.get(`/app/${this.props.entity}/batch-update/fields`, (res) => {
      const fields = (res.data || []).filter((x) => !$isSysMask(x.label))
      this.setState({ fields })
    })
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
                      <span className="badge badge-warning">{field.label}</span>
                    </div>
                    <div className="col-2 pl-0 pr-0">
                      <span className="badge badge-warning">{BU_OPS[item.op]}</span>
                    </div>
                    <div className="col-6">
                      {item.op !== 'NULL' && <span className="badge badge-warning text-break text-left">{FieldValueSet.formatFieldText(item.value, field)}</span>}
                      <a className="del" onClick={() => this.delItem(item.field)} title={$L('移除')}>
                        <i className="zmdi zmdi-close" />
                      </a>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
          <div className="batch-editor">
            {this.state.fields && <BatchUpdateEntry ref={(c) => (this._buEntry = c)} fields={this.state.fields} entity={this.props.entity} />}
            <div className="mt-1">
              <button className="btn btn-primary btn-sm btn-outline" onClick={() => this.addItem()} type="button">
                + {$L('添加')}
              </button>
            </div>
          </div>
        </div>
      </div>
    )
  }

  addItem() {
    const item = this._buEntry.buildItem()
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

  handleConfirm() {
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
    RbAlert.create(<b>{$L('请再次确认修改数据范围和修改内容。开始修改吗？')}</b>, {
      onConfirm: function () {
        this.hide()
        that.disabled(true, true)
        $.post(`/app/${that.props.entity}/batch-update/submit?dr=${that.state.dataRange}`, JSON.stringify(_data), (res) => {
          if (res.error_code === 0) {
            const mp_parent = $(that._dlg._element).find('.modal-body').attr('id', $random('node-'))
            const mp = new Mprogress({ template: 1, start: true, parent: '#' + $(mp_parent).attr('id') })
            that._checkState(res.data, mp)

            // v36 终止
            that._taskid = res.data
            $(that._btns).find('.btn-secondary').button('reset')
          } else {
            that.disabled(false)
            RbHighbar.error(res.error_msg)
          }
        })
      },
      countdown: 5,
    })
  }

  _checkState(taskid, mp) {
    BatchUpdate__taskid = taskid
    $.get(`/commons/task/state?taskid=${taskid}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.hasError) {
          mp && mp.end()
          RbHighbar.error(res.data.hasError)
          return
        }

        const cp = res.data.progress
        if (res.data.isCompleted) {
          BatchUpdate__taskid = null
          mp && mp.end()
          $(this._btns)
            .find('.btn-primary')
            .text(res.data.isInterrupted ? $L('已终止') : $L('已完成'))
          RbHighbar.success($L('成功修改 %d 条记录', res.data.succeeded))

          RbListPage.reload()
          this._taskid = null
          setTimeout(() => {
            this.disabled(false)
            this.hide()
          }, 3000)
        } else {
          mp && mp.set(cp)
          setTimeout(() => this._checkState(taskid, mp), 1500)
        }
      }
    })
  }
}

const BU_OPS = {
  SET: $L('修改为'),
  NULL: $L('置空'),
  // TODO 支持更多修改模式
  // 250813 也可以触发器修改
  // PREFIX: $L('前添加'),
  // SUFFIX: $L('后添加'),
  // REPLACE: $L('替换'),
  // PLUS: $L('加上'),
  // MINUS: $L('减去'),
  // MULTIPLY: $L('乘以'),
  // DIVIDE: $L('除以'),
}

// 批量修改编辑器
class BatchUpdateEntry extends React.Component {
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
        this.setState({ selectOp: $op2s.val() }, () => this._renderFieldValueSet())
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
          <span className="text-muted">{$L('修改字段')}</span>
        </div>
        <div className="col-2 pl-0 pr-0">
          <select className="form-control form-control-sm" ref={(c) => (this._$op = c)}>
            <option value="SET">{BU_OPS['SET']}</option>
            <option value="NULL">{BU_OPS['NULL']}</option>
          </select>
          <span className="text-muted">{$L('修改方式')}</span>
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
    const data = {
      field: this.state.selectField,
      op: this.state.selectOp,
    }

    const field = this.props.fields.find((x) => this.state.selectField === x.name)
    if (data.op === 'NULL') {
      if (!field.nullable) {
        RbHighbar.create($L('%s不能为空', field.label))
        return null
      } else {
        return data
      }
    }

    data.value = this._FieldValue.val()
    if (data.value === false) {
      // 格式不正确
      return null
    } else if (!data.value) {
      RbHighbar.create($L('请填写新值'))
      return null
    } else {
      return data
    }
  }
}

// ~ 批量审批

// eslint-disable-next-line no-unused-vars
class BatchApprove extends BatchOperator {
  constructor(props) {
    super(props)
    this._title = (
      <RF>
        {$L('批量审批')}
        <sup className="rbv" />
      </RF>
    )
    this._confirmText = $L('审批')
  }

  renderOperator() {
    const approveState = ~~this.state.approveState
    return (
      <div>
        <div className="form-group">
          <label className="text-bold">{$L('审批方式')}</label>
          <div>
            <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-0">
              <input className="custom-control-input" type="radio" name="approveState" value="10" onClick={this.handleChange} />
              <span className="custom-control-label">{$L('通过')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-0">
              <input className="custom-control-input" type="radio" name="approveState" value="11" onClick={this.handleChange} />
              <span className="custom-control-label">{$L('驳回')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-0">
              <input className="custom-control-input" type="radio" name="approveState" value="1" onClick={this.handleChange} />
              <span className="custom-control-label">{$L('提交')}</span>
            </label>
          </div>
        </div>

        <div className={`form-group ${approveState >= 10 ? '' : 'hide'}`}>
          <label className="text-bold">{$L('批注')}</label>
          <textarea className="form-control form-control-sm row2x" name="approveRemark" placeholder={$L('输入批注')} maxLength="600" onChange={this.handleChange} />
        </div>
        <div className={`form-group ${approveState === 1 ? '' : 'hide'}`}>
          <label className="text-bold">{$L('审批流程')}</label>
          <select className="form-control form-control-sm" ref={(c) => (this._$useApproval = c)} />
        </div>

        <RbAlertBox message={$L('仅允许你审批或提交的记录，才能审批成功')} type="info" className="mb-0" />
      </div>
    )
  }

  componentDidMount() {
    // super.componentDidMount()

    $.get(`/app/entity/approval/alist?entity=${wpc.entity[0]}&valid=true`, (res) => {
      $(this._$useApproval).select2({
        placeholder: $L('无'),
        allowClear: false,
        language: {
          noResults: () => $L('无适用流程'),
        },
        data: res.data || [],
      })
    })
  }

  handleConfirm() {
    if (rb.commercial < 10) {
      RbHighbar.error(WrapHtml($L('免费版不支持批量审批功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    if (!this.state.approveState) return RbHighbar.create($L('请选择审批方式'))

    const _data = {
      queryData: this.getQueryData(),
      approveContent: {
        state: this.state.approveState,
        remark: this.state.approveRemark || null,
        approvalId: $val(this._$useApproval) || null,
      },
    }
    if (~~this.state.approveState === 1) {
      if (!_data.approveContent.approvalId) {
        RbHighbar.create($L('请选择审批流程'))
        return
      }
    } else {
      if ($empty(this.state.approveRemark)) {
        RbHighbar.create($L('请输入批注'))
        return
      }
    }
    if (rb.env === 'dev') console.log(JSON.stringify(_data))

    const that = this
    RbAlert.create(<b>{$L('请再次确认审批数据范围和审批方式。开始审批吗？')}</b>, {
      onConfirm: function () {
        this.hide()
        that.disabled(true, true)
        $.post(`/app/entity/approval/approve-batch?dr=${that.state.dataRange}&entity=${that.props.entity}`, JSON.stringify(_data), (res) => {
          if (res.error_code === 0) {
            const mp_parent = $(that._dlg._element).find('.modal-body').attr('id', $random('node-'))
            const mp = new Mprogress({ template: 1, start: true, parent: '#' + $(mp_parent).attr('id') })
            that._checkState(res.data, mp)

            // v36 终止
            that._taskid = res.data
            $(that._btns).find('.btn-secondary').button('reset')
          } else {
            that.disabled(false)
            RbHighbar.error(res.error_msg)
          }
        })
      },
      countdown: 5,
    })
  }

  _checkState(taskid, mp) {
    $.get(`/commons/task/state?taskid=${taskid}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.hasError) {
          mp && mp.end()
          RbHighbar.error(res.data.hasError)
          return
        }

        const cp = res.data.progress
        if (res.data.isCompleted) {
          mp && mp.end()
          $(this._btns)
            .find('.btn-primary')
            .text(res.data.isInterrupted ? $L('已终止') : $L('已完成'))
          if (res.data.succeeded > 0) {
            RbHighbar.success($L('批量审批完成。成功 %d 条，失败 %d 条', res.data.succeeded, res.data.total - res.data.succeeded))
          } else {
            RbHighbar.create($L('没有任何符合批量审批条件的记录'))
          }

          RbListPage.reload()
          setTimeout(() => {
            this.disabled(false)
            this.hide()
          }, 2000)
        } else {
          mp && mp.set(cp)
          setTimeout(() => this._checkState(taskid, mp), 1500)
        }
      }
    })
  }
}

// ~ 通用操作

// eslint-disable-next-line no-unused-vars
const RbListCommon = {
  inUrlViewId: null,

  init: function (wpc) {
    const gs = $urlp('gs', location.hash)
    if (gs) {
      // eslint-disable-next-line no-undef
      // _showGlobalSearch(gs)
      $('.input-search>input').val($decode(gs))
    }

    // 快速查询
    const $btn = $('.input-search .input-group-btn .btn'),
      $input = $('.input-search input')
    $btn.on('click', () => _RbList().searchQuick())
    $input
      .on('keydown', (e) => {
        e.which === 13 && $btn.trigger('click')
      })
      .on('paste', (e) => {
        const c = (e.originalEvent && e.originalEvent.clipboardData && e.originalEvent.clipboardData.getData('text/plain')) || ''
        let cArray = []
        c.split('\n').forEach((item) => {
          item = $trim(item)
          if (item && item !== '') cArray.push(item)
        })
        // 多值查询
        if (cArray.length > 1) {
          setTimeout(() => $input.val(cArray.join('|')), 200)
        }
      })
    $('.input-search .btn-input-clear').on('click', () => {
      $input.val('')
      $btn.trigger('click')
    })

    // via 默认过滤
    const via = $urlp('via') || $urlp('via', location.hash)
    if (via) {
      wpc.protocolFilter = `via:${via}`
      const $cleanVia = $(`<div class="badge badge-warning filter-badge J_via-filter">${$L('当前数据已过滤')}<a class="close" title="${$L('查看全部数据')}">&times;</a></div>`).appendTo(
        '.dataTables_filter'
      )
      $cleanVia.find('a').on('click', () => {
        wpc.protocolFilter = null
        RbListPage.reload()
        $cleanVia.remove()
      })
    } else {
      // d 强制过滤
      let def40 = $urlp('def')
      if (def40 && def40.length >= 20) {
        $('.main-content .nav-tabs a[href]').each(function () {
          const $this = $(this)
          $this.attr('href', `${$this.attr('href')}?def=${def40}`)
        })

        def40 = def40.split(':') // FILTER:LAYOUT
        if (def40[0]) {
          if (def40[0].startsWith('014-')) wpc.protocolFilterAnd = `via:${def40[0]}`
          else console.log('Use listConfig :', def40[0])
        }
        if (def40[1]) {
          if (def40[1].startsWith('014-')) wpc.protocolFilterAnd = `via:${def40[1]}`
          else console.log('Use listConfig :', def40[1])
        }
      }
    }

    const entity = wpc.entity
    if (!entity) return

    RbListPage.init(wpc.listConfig, entity, wpc.privileges)
    if (wpc.advFilter !== false) AdvFilters.init('.adv-search', entity[0])

    const newProps = { title: $L('新建%s', entity[1]), entity: entity[0], icon: entity[2], showExtraButton: true }
    const $new = $('.J_new')
      .attr('disabled', false)
      .on('click', () => RbFormModal.create(newProps))
    if (wpc.formsAttr) {
      $new.next().removeClass('hide')
      const $next = $new.next().next()
      wpc.formsAttr.map((n) => {
        $(`<a class="dropdown-item" data-id="${n.id}">${n.name || $L('默认布局')}</a>`)
          .appendTo($next)
          .on('click', () => RbFormModal.create({ ...newProps, specLayout: n.id }, true))
      })
    } else {
      const $next = $new.next()
      if ($next.hasClass('hide') && $next.hasClass('dropdown-toggle')) {
        $next.next().remove()
        $next.remove()
      }
    }

    $('.J_export').on('click', () => renderRbcomp(<DataExport listRef={_RbList()} entity={entity[0]} />))
    $('.J_batch-update').on('click', () => renderRbcomp(<BatchUpdate listRef={_RbList()} entity={entity[0]} />))
    $('.J_batch-approve').on('click', () => renderRbcomp(<BatchApprove listRef={_RbList()} entity={entity[0]} />))
    $('.J_record-merge').on('click', () => {
      const ids = _RbList().getSelectedIds(true)
      if (ids.length < 2) return RbHighbar.createl('请至少选择两条记录')
      renderRbcomp(<RecordMerger listRef={_RbList()} entity={entity[0]} hasDetails={!!$('.J_details')[0]} ids={ids} />)
    })

    // 自动打开新建
    if (location.hash === '#!/New') {
      setTimeout(() => $('.J_new').trigger('click'), 200)
    }
  },
}

// ~ 数据列表

const wpc = window.__PageConfig || {}

const _DL_COLUMN_MIN_WIDTH = 30
const _DL_COLUMN_MAX_WIDTH = 500
const _DL_COLUMN_DEF_WIDTH = 130

// IE/Edge 不支持首/列固定
const supportFixedColumns = !($.browser.msie || $.browser.msedge)

// eslint-disable-next-line no-unused-vars
class RbList extends React.Component {
  constructor(props) {
    super(props)

    this._$wrapper = $(props.$wrapper || '#react-list')
    this._entity = props.config.entity

    this.__defaultFilterKey = `AdvFilter-${this._entity}`
    this.__sortFieldKey = `SortField-${this._entity}`
    this.__columnWidthKey = `ColumnWidth-${this._entity}.`

    const fields = props.config.fields || []

    // 排序
    let sort = $storage.get(this.__sortFieldKey) || props.config.sort
    if (sort) {
      sort = sort.split(':')
    } else {
      for (let i = 0; i < fields.length; i++) {
        if (fields[i].sort) {
          sort = [fields[i].field, `sort-${fields[i].sort}`]
          break
        }
      }
    }
    // 设置的默认排序
    this.__defaultSort = props.config.sort
    for (let i = 0; i < fields.length; i++) {
      if (fields[i].sort) {
        this.__defaultSort = `${fields[i].field}:${fields[i].sort}`
        break
      }
    }

    for (let i = 0; i < fields.length; i++) {
      const cw = $storage.get(this.__columnWidthKey + fields[i].field)
      if (!!cw && ~~cw >= _DL_COLUMN_MIN_WIDTH) fields[i].width = ~~cw

      if (sort && sort[0] === fields[i].field) fields[i].sort = sort[1]
      else fields[i].sort = null
      if (window.UNSORT_FIELDTYPES.includes(fields[i].type)) fields[i].unsort = true
    }

    delete props.config.fields
    this.state = { ...props, fields: fields, rowsData: [], pageNo: 1, pageSize: 20, inLoad: true }

    this.__defaultColumnWidth = this._$wrapper.width() / 10
    if (this.__defaultColumnWidth < _DL_COLUMN_DEF_WIDTH) this.__defaultColumnWidth = _DL_COLUMN_DEF_WIDTH

    this.pageNo = 1
    this.pageSize = $storage.get('ListPageSize') || 20
    this.advFilterId = wpc.advFilter !== true ? null : $storage.get(this.__defaultFilterKey) // 无高级查询
    this.fixedColumns = supportFixedColumns && props.uncheckbox !== true
    // v4.1 可编辑
    this.enabledListEditable = ['RecordList', 'DetailList'].includes(wpc.type) && (window.__LAB_DATALIST_EDITABLE41 === true || wpc.enabledListEditable)
  }

  render() {
    const lastIndex = this.state.fields.length
    let rowActions = window.FrontJS ? window.FrontJS.DataList.__rowActions : []
    if (!['RecordList', 'DetailList'].includes(wpc.type)) rowActions = []

    return (
      <RF>
        <div className="row rb-datatable-body">
          <div className="col-sm-12">
            <div className="rb-scroller" ref={(c) => (this._$scroller = c)}>
              <table className={`table table-hover table-striped ${window.__LAB_DATALIST_BORDERED42 && 'table-bordered42'}`}>
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
                      const cWidth = item.width || this.__defaultColumnWidth
                      const style2 = { width: cWidth }
                      const clazz = `unselect sortable ${idx === 0 && this.fixedColumns ? 'column-fixed column-fixed-2nd' : ''}`
                      return (
                        <th key={`column-${item.field}`} style={style2} className={clazz} data-field={item.field} onClick={(e) => !item.unsort && this._sortField(item.field, e)}>
                          <div style={style2}>
                            <span style={{ width: cWidth - 8 }}>{item.label}</span>
                            <i className={`zmdi ${item.sort || ''}`} />
                            <i className="dividing" />
                          </div>
                        </th>
                      )
                    })}
                    <th className="column-empty" />
                    {rowActions.length > 0 && <th className="col-action column-fixed" />}
                  </tr>
                </thead>
                <tbody ref={(c) => (this._$tbody = c)}>
                  {this.state.rowsData.map((item) => {
                    const primaryKey = item[lastIndex]
                    const rowKey = `row-${primaryKey.id}`
                    return (
                      <tr
                        key={rowKey}
                        data-id={primaryKey.id}
                        onClick={(e) => this._clickRow(e)}
                        onDoubleClick={(e) => {
                          if (this.enabledListEditable) return // v4.1
                          $stopEvent(e, true)
                          this._openView(e.currentTarget)
                        }}>
                        {this.props.uncheckbox !== true && (
                          <td key={`${rowKey}-checkbox`} className={`column-checkbox ${supportFixedColumns ? 'column-fixed' : ''}`}>
                            <div>
                              <label className="custom-control custom-control-sm custom-checkbox">
                                <input className="custom-control-input" type="checkbox" />
                                <i className="custom-control-label" />
                              </label>
                            </div>
                          </td>
                        )}
                        {item.map((cell, index) => {
                          return this.renderCell(cell, index, primaryKey)
                        })}
                        <td className="column-empty" />
                        {rowActions.length > 0 && (
                          <td className="col-action column-fixed">
                            <div>
                              {rowActions.map((btn, idx) => {
                                return (
                                  <button
                                    key={idx}
                                    type="button"
                                    className={`btn btn-sm btn-link w-auto ${btn._eaid && 'disabled'} ${btn.title && 'bs-tooltip'}`}
                                    title={btn.title || null}
                                    data-eaid={btn._eaid || null}
                                    onClick={(e) => {
                                      if ($(e.target).hasClass('disabled')) return
                                      typeof btn.onClick === 'function' && btn.onClick(primaryKey.id, e)
                                    }}>
                                    <span className={`text-${btn.type || ''}`}>
                                      {btn.icon && <i className={`icon zmdi zmdi-${btn.icon}`} />}
                                      {btn.text && <span>{btn.text}</span>}
                                    </span>
                                  </button>
                                )
                              })}
                            </div>
                          </td>
                        )}
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
      </RF>
    )
  }

  componentDidMount() {
    // FIXME 拖到底部才能左右滚动

    const $scroller = $(this._$scroller)
    $scroller.perfectScrollbar({
      // suppressScrollY: true,
      // wheelSpeed: 1,
    })

    if (this.props.unpin !== true) {
      $('.main-content').addClass('pb-0')
      if (supportFixedColumns) $scroller.find('.table').addClass('table-header-fixed')

      $addResizeHandler(() => {
        let mh = $(window).height() - (61 + 20 + 61 + 60 + 3) /* Nav, MarginTop20, TableHeader, TableFooter */
        if ($('.main-content>.nav-tabs-classic')[0]) mh -= 38 // Has detail-tab
        if ($('.main-content .quick-filter-pane')[0]) mh -= 92 // Has filter-pane
        if ($('.main-content .quick-filter-tabs')[0]) mh -= 44 // Has list-view

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
      containment: $scroller.find('.rb-datatable-body'),
      axis: 'x',
      helper: 'clone',
      start: function () {
        that.__columnResizing = true
      },
      stop: function (event, ui) {
        const field = $(event.target).parents('th').data('field')
        let left = ui.position.left - 0
        if (left < _DL_COLUMN_MIN_WIDTH) left = _DL_COLUMN_MIN_WIDTH
        else if (left > _DL_COLUMN_MAX_WIDTH) left = _DL_COLUMN_MAX_WIDTH
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

    // 首次由外部查询 eg.AdvFilter
    if (wpc.advFilter !== true) this.fetchList(this._buildQuick())
    // 按键操作
    if (['RecordList', 'DetailList'].includes(wpc.type)) $(document).on('keydown', (e) => this._keyEvent(e))
    // v4.1 取消选中
    if (this.enabledListEditable) {
      $(document).on('click.unselect', (e) => {
        const $target = $(e.target)
        if ($target.closest('.data-list').length === 0 && $target.closest('.rb-wrapper').length > 0) {
          $(this._$tbody).find('td.editable').removeClass('editable')
        }
      })
    }
  }

  fetchList(filter) {
    const fields = []
    let sort = null
    this.state.fields.forEach((item) => {
      fields.push(item.field)
      if (item.sort) sort = `${item.field}:${item.sort.replace('sort-', '')}`
    })
    if (!sort && this.__defaultSort) sort = this.__defaultSort

    this.lastFilter = filter || this.lastFilter
    const reload = this._forceReload || this.pageNo === 1
    this._forceReload = false

    const query = {
      entity: this._entity,
      fields: fields,
      pageNo: this.pageNo,
      pageSize: this.pageSize,
      filter: this.lastFilter,
      advFilter: this.advFilterId,
      protocolFilter: this.props.protocolFilter || wpc.protocolFilter,
      protocolFilterAnd: this.props.protocolFilterAnd || wpc.protocolFilterAnd,
      sort: sort,
      reload: reload,
      statsField: wpc.statsField === true && rb.commercial > 0,
    }
    this.__lastQueryEntry = query

    const loadingTimer = setTimeout(() => {
      this.setState({ inLoad: true }, () => this._$wrapper.addClass('rb-loading-active'))
    }, 400)

    if (query.filter && (query.filter.items || []).length > 0) $logRBAPI(JSON.stringify(query.filter), 'FilterBody')

    $.post(`/app/${this._entity}/data-list`, JSON.stringify(RbList.queryBefore(query)), (res) => {
      if (res.error_code === 0) {
        this.setState({ rowsData: res.data.data || [], inLoad: false }, () => {
          this._clearSelected()
          $(this._$scroller).scrollTop(0)

          setTimeout(() => {
            RbList.renderAfter(this)
            RbList.renderAfter40(this)
          }, 0)
        })

        if (reload && this._Pagination) {
          this.__holdRowsStats = res.data.stats
          this._Pagination.setState({ rowsTotal: res.data.total, rowsStats: this.__holdRowsStats, pageNo: this.pageNo })
        }
      } else {
        RbHighbar.error(res.error_msg)
      }

      clearTimeout(loadingTimer)
      this._$wrapper.removeClass('rb-loading-active')
    })
  }

  // 渲染表格及相关事件处理

  renderCell(cellVal, index, primaryKey) {
    if (this.state.fields.length === index) return null
    const field = this.state.fields[index]
    if (!field) return null

    const cellKey = `row-${primaryKey.id}-${index}`
    const width = this.state.fields[index].width || this.__defaultColumnWidth
    let type = field.type
    if (cellVal === '$NOPRIVILEGES$') {
      type = cellVal
    } else if (field.field === this.props.config.nameField) {
      cellVal = primaryKey
      type = '$NAME$'
    }

    const c = CellRenders.render(cellVal, type, width, `${cellKey}.${field.field}`)
    // v4.1 快捷编辑
    const cProps = {}
    if (this.enabledListEditable) {
      cProps.onClick = (e) => {
        const $el = $(e.currentTarget)
        if ($el.hasClass('editable')) {
          LiteFormModal.create(primaryKey.id, [field.field], $L('编辑%s', field.label))
        } else {
          $(this._$tbody).find('td.editable').removeClass('editable')
          $el.addClass('editable')
        }
      }
    }
    // 首行固定
    if (index === 0 && this.fixedColumns) {
      cProps.className = `${c.props.className || ''} column-fixed column-fixed-2nd`
    }
    return React.cloneElement(c, { ...cProps })
  }

  // 全选
  _toggleRows(e, uncheck) {
    const $body = $(this._$tbody)
    if (e.target.checked) {
      $body.find('>tr').addClass('active').find('.custom-control-input').prop('checked', true)
    } else {
      $body.find('>tr').removeClass('active').find('.custom-control-input').prop('checked', false)
    }
    if (!uncheck) this._checkSelected()
  }

  // 单选
  _clickRow(e) {
    const $target = $(e.target)
    if ($target.hasClass('custom-control-label')) return // ignored

    const $tr = $target.parents('tr')
    let holdSelected
    if ($target.hasClass('column-checkbox')) {
      const $chk = $tr.find('.custom-control-input')[0]
      $chk.checked = !$chk.checked
      holdSelected = true
    } else {
      holdSelected = $target.hasClass('custom-checkbox') || $target.parents('.custom-checkbox').hasClass('custom-checkbox')
    }

    if (holdSelected) {
      if ($tr.find('.custom-control-input')[0].checked) $tr.addClass('active')
      else $tr.removeClass('active')
    } else {
      this._toggleRows({ target: { checked: false } }, true)
      $tr.addClass('active').find('.custom-control-input')[0].checked = true
    }

    this._checkSelected()
  }

  _checkSelected() {
    const chkSelected = $(this._$tbody).find('>tr .custom-control-input:checked').length
    const chkTotal = this.state.rowsData.length

    // 全选/半选/全清
    if (chkSelected === 0) {
      $(this._checkAll).prop('checked', false).parent().removeClass('indeterminate')
    } else if (chkSelected !== chkTotal) {
      $(this._checkAll).prop('checked', false).parent().addClass('indeterminate')
    }

    if (chkSelected > 0 && chkSelected === chkTotal) {
      $(this._checkAll).prop('checked', true).parent().removeClass('indeterminate')
    }

    // 操作按钮状态
    const $oper = $('.dataTables_oper')
    $oper.find('.J_delete,.J_view,.J_edit,.J_assign,.J_share,.J_unshare').attr('disabled', true)
    if (chkSelected > 0) $oper.find('.J_delete,.J_view,.J_edit,.J_assign,.J_share,.J_unshare').attr('disabled', false)

    // 分页组件
    if (this._Pagination) {
      this._Pagination.setState({ selectedTotal: chkSelected }, () => {
        if (wpc.statsField !== true || rb.commercial < 10) return

        if (chkSelected > 1) {
          const ids = this.getSelectedIds(true)
          const qurey = {
            protocolFilter: `ids:${ids.join('|')}`,
            entity: this._entity,
            fields: [],
            statsField: true,
          }

          $.post(`/app/${this._entity}/data-list-stats`, JSON.stringify(qurey), (res) => {
            this._Pagination.setState({ rowsStats: res.data || [] })
            if (res.error_code !== 0) RbHighbar.error(res.error_msg)
          })
        } else {
          this._Pagination.setState({ rowsStats: this.__holdRowsStats })
        }
      })
    }
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
        if (fields[i].sort === 'sort-desc') fields[i].sort = 'sort-asc'
        else if (fields[i].sort === 'sort-asc') fields[i].sort = null
        else fields[i].sort = 'sort-desc'

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

  _openView($tr) {
    if (!wpc.type) return
    const id = $($tr).data('id')
    if (!wpc.forceSubView) {
      location.hash = `!/View/${this._entity}/${id}`
    }
    CellRenders.clickView({ id: id, entity: this._entity })
  }

  _keyEvent(e) {
    if (!$(e.target).is('body')) return
    if (!(e.keyCode === 40 || e.keyCode === 38 || e.keyCode === 13)) return

    let $chk = $(this._$tbody).find('>tr .custom-control-input:checked').first()
    if (e.keyCode === 13) {
      let $go = $chk.eq(0).parents('tr')
      if ($go[0]) {
        this._clickRow({ target: $go.find('td:eq(1)') })
        this._openView($go)
      }
      return
    }

    let $go = $chk.eq(0).parents('tr')
    if ($go[0]) {
      $go = e.keyCode === 40 ? $go.next() : $go.prev()
    } else {
      // No selected
    }
    if (!$go[0]) {
      $chk = $(this._$tbody).find('>tr .custom-control-input')
      $chk = e.keyCode === 40 ? $chk.first() : $chk.last()
      $go = $chk.eq(0).parents('tr')
    }
    $go[0] && this._clickRow({ target: $go.find('td:eq(1)') })
  }
  // Next or Prev
  jumpView(go) {
    let $chk = $(this._$tbody).find('>tr .custom-control-input:checked').first()
    let $go = $chk.eq(0).parents('tr')
    if ($go[0]) {
      $go = go === 1 ? $go.next() : $go.prev()
    } else {
      // No selected
    }
    if (!$go[0]) {
      $chk = $(this._$tbody).find('>tr .custom-control-input')
      $chk = go === 1 ? $chk.first() : $chk.last()
      $go = $chk.eq(0).parents('tr')
    }
    if ($go[0]) {
      this._clickRow({ target: $go.find('td:eq(1)') })
      this._openView($go)
    }
  }

  // -- 外部接口

  // 分页设置
  setPage(pageNo, pageSize) {
    this.pageNo = pageNo || this.pageNo
    if (pageSize) {
      this.pageSize = pageSize
      $storage.set('ListPageSize', pageSize)
    }
    this.fetchList()
  }

  // 设置高级过滤器 ID
  setAdvFilter(id) {
    this.advFilterId = id
    this.pageNo = 1
    this.fetchList(this._buildQuick())
    if (id) $storage.set(this.__defaultFilterKey, id)
    else $storage.remove(this.__defaultFilterKey)
  }

  // 重新加载
  reload() {
    this._forceReload = true
    this.fetchList()
  }

  // 搜索
  search(filter, fromAdv) {
    this.pageNo = 1
    this.fetchList(filter)

    // 高级查询
    if (fromAdv === true) {
      $('.J_filterbtn .indicator-primary').remove()
      if (filter.items.length > 0) $('<i class="indicator-primary bg-warning"></i>').appendTo('.J_filterbtn')
    }
  }

  // @el - search element
  searchQuick = (el) => this.search(this._buildQuick(el))

  _buildQuick(el) {
    el = $(el || '.input-search>input')
    const q = $trim(el.val())
    if (!q && !this.lastFilter) return null

    return {
      entity: this._entity,
      type: 'QUICK',
      values: { 1: q },
      quickFields: el.data('quickfields') || null,
    }
  }

  // 取选中 ID[]
  getSelectedIds(hideWarning) {
    const selected = []
    $(this._$tbody)
      .find('>tr .custom-control-input:checked')
      .each(function () {
        selected.push($(this).parents('tr').data('id'))
      })

    if (selected.length === 0 && hideWarning !== true) RbHighbar.create($L('未选中任何记录'))
    return selected
  }

  // 获取最后查询记录总数
  getLastQueryTotal() {
    return this._Pagination ? this._Pagination.state.rowsTotal : 0
  }

  // 获取最后查询条件
  getLastQueryEntry() {
    return $clone(this.__lastQueryEntry)
  }

  // -- HOOK

  // 查询前回调，可以对查询机进行二次封装
  static queryBefore(query) {
    return query
  }

  // 组件渲染后调用
  // eslint-disable-next-line no-unused-vars
  static renderAfter(listObj) {}
  // eslint-disable-next-line no-unused-vars
  static renderAfter40(listObj) {}
}

// 分页组件
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
        <div className="col-12 col-lg-6">
          <div className="dataTables_info">{this.renderStats()}</div>
        </div>
        <div className="col-12 col-lg-6">
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
          <div className="float-right paging_sizes paging_sizes-no">
            <input className="form-control form-control-sm text-center" title={$L('页码')} placeholder={$L('页码')} onKeyDown={this.setPageNo} />
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

  renderStats() {
    return (
      <div>
        {this.state.rowsTotal > 0 && <span>{$L('共 %d 条记录', this.state.rowsTotal)}</span>}
        {this.state.selectedTotal > 1 && <span className="stat-item">{$L('已选中 %d 条', this.state.selectedTotal)}</span>}
        {(this.state.rowsStats || []).map((item, idx) => {
          return (
            <span key={idx} className="stat-item">
              {item.label}
              <strong style={{ color: item.color || '#fbbc05' }}>{item.value}</strong>
            </span>
          )
        })}
        {rb.isAdminUser && wpc.statsField && ['RecordList', 'DetailList'].includes(wpc.type) && (
          <a
            className="list-stats-settings"
            onClick={() => {
              RbModal.create(
                `/p/admin/metadata/list-stats?entity=${this._entity}`,
                <RF>
                  {$L('配置统计列')}
                  <sup className="rbv" />
                </RF>
              )
            }}>
            <i className="icon zmdi zmdi-settings" title={$L('配置统计列')} />
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

  setPageNo = (e) => {
    const pn = ~~e.target.value
    if (e.keyCode === 13 && pn && pn > 0) {
      this.goto(Math.min(pn, this.__pageTotal))
    }
  }

  setPageSize = (e) => {
    const ps = ~~e.target.value
    this.setState({ pageSize: ps, pageNo: 1 }, () => {
      this.props.$$$parent.setPage(1, ps)
    })
  }
}

// 数据列表（单元格）渲染
const CellRenders = {
  // 打开记录
  clickView(v, e) {
    const _RbViewModal = window.RbViewModal ? window.RbViewModal : parent && parent.RbViewModal ? parent.RbViewModal : null
    if (_RbViewModal && wpc.forceOpenNewtab !== true) {
      _RbViewModal.create({ id: v.id, entity: v.entity }, wpc.forceSubView)
    } else {
      window.open(`${rb.baseUrl}/app/redirect?id=${v.id}&type=newtab`)
    }
    e && $stopEvent(e, true)
    return false
  },

  // 打开预览
  clickPreview(v, idx, e) {
    e && $stopEvent(e)
    RbPreview.create(v, idx)
    return false
  },

  __RENDERS: {},
  addRender(type, func) {
    this.__RENDERS[type] = func
  },

  // 单元格渲染
  render(value, type, width, key) {
    const style2 = { width: width || _DL_COLUMN_MIN_WIDTH }
    let fieldKey = null
    if (window.FrontJS && wpc.entity) {
      fieldKey = key.split('.').slice(1)
      fieldKey = `${wpc.entity[0]}.${fieldKey.join('.')}`

      const _FN = window.FrontJS.DataList.__cellRenders[fieldKey]
      if (typeof _FN === 'function') {
        const fnRet = _FN(value, style2, key)
        if (fnRet !== false) return fnRet
      }
    }

    if (!value) return this.renderSimple(value, style2, key)

    const cellComp = (this.__RENDERS[type] || this.renderSimple)(value, style2, key)
    if (fieldKey && window.FrontJS && wpc.entity) {
      const enable42 = window.FrontJS.DataList.__cellCopys.includes(fieldKey)
      if (enable42) {
        return React.cloneElement(cellComp, {
          'data-copy': true,
          title: $L('点击复制'),
          onClick: (e) => {
            if (e.target.tagName === 'A' || $(e.target).closest('a').length) return // fix:4.2.3 链接不复制
            $stopEvent(e, true)
            $clipboard2($(e.currentTarget).text(), true)
          },
        })
      }
    }
    return cellComp
  },

  /**
   * @see #render
   * @param {*} v 值
   * @param {*} s 样式
   * @param {*} k key of React (contains fieldName)
   */
  renderSimple(v, s, k) {
    if (typeof v === 'string' && v.length > 300) v = v.substr(0, 300)
    // v3.8 引用字段无值的情况下审批相关字段也应无值
    // else if (k.endsWith('.approvalId') && !v) v = $L('未提交')
    // else if (k.endsWith('.approvalState') && !v) v = $L('草稿')

    return (
      <td key={k}>
        <div style={s} title={typeof v === 'string' ? v : null}>
          {v || ''}
        </div>
      </td>
    )
  },

  /**
   * @param {*} v 值
   */
  formatSimple(v) {
    if (Array.isArray(v)) {
      const array = []
      v.forEach((item) => {
        if (typeof item === 'object') array.push(item.text || item.name)
        else array.push(item)
      })
      v = array
    }

    if (Array.isArray(v)) return v.join(', ')

    if (typeof v === 'object') {
      if (Array.isArray(v.text)) v = v.text.join(', ')
      else v = v.text
    }
    return v ? v : $empty(v) ? null : v
  },
}

// 名称字段
CellRenders.addRender('$NAME$', (v, s, k) => {
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
CellRenders.addRender('$NOPRIVILEGES$', (v, s, k) => {
  return (
    <td key={k}>
      <div style={s} className="column-nopriv">
        [{$L('无权限')}]
      </div>
    </td>
  )
})
// 不同类型
CellRenders.addRender('IMAGE', (v, s, k) => {
  v = v || []
  const vLen = v.length
  return (
    <td key={k} className="td-sm" title={$L('共 %d 项', vLen)}>
      <div className="column-imgs" style={s}>
        {v.map((item, idx) => {
          const imgName = $fileCutName(item)
          const imgUrl = $isFullUrl(item) ? item : `${rb.baseUrl}/filex/img/${item}`
          return (
            <a key={item} title={imgName} onClick={(e) => CellRenders.clickPreview(v, idx, e)}>
              <img src={`${imgUrl}?imageView2/2/w/100/interlace/1/q/100`} alt="IMG" />
            </a>
          )
        })}
      </div>
    </td>
  )
})
CellRenders.addRender('FILE', (v, s, k) => {
  v = v || []
  const vLen = v.length
  return (
    <td key={k} title={$L('共 %d 项', vLen)}>
      <div className="column-files" style={s}>
        {v.map((item) => {
          const fileName = $fileCutName(item)
          return (
            <a key={item} title={fileName} onClick={(e) => CellRenders.clickPreview(item, null, e)}>
              {fileName}
            </a>
          )
        })}
      </div>
    </td>
  )
})
const _renderReference = (v, s, k) => {
  return (
    <td key={k}>
      <div style={s} title={v.text}>
        <a href={`#!/View/${v.entity}/${v.id}`} onClick={(e) => CellRenders.clickView(v, e)}>
          {v.text}
        </a>
      </div>
    </td>
  )
}
CellRenders.addRender('REFERENCE', _renderReference)
CellRenders.addRender('ANYREFERENCE', _renderReference)
CellRenders.addRender('N2NREFERENCE', (v, s, k) => {
  v = v || []
  const vLen = v.length
  return (
    <td key={k} className="td-sm" title={$L('共 %d 项', vLen)}>
      <div className="column-multi" style={s}>
        {v.map((item) => {
          return (
            <a key={item.id} title={item.text} className="badge" href={`#!/View/${item.entity}/${item.id}`} onClick={(e) => CellRenders.clickView(item, e)}>
              {item.text}
            </a>
          )
        })}
      </div>
    </td>
  )
})
CellRenders.addRender('URL', (v, s, k) => {
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
CellRenders.addRender('EMAIL', (v, s, k) => {
  return (
    <td key={k}>
      <div style={s} title={v}>
        {$env.isDingTalk() ? (
          <a>{v}</a>
        ) : (
          <a href={`mailto:${v}`} className="column-url" onClick={(e) => $stopEvent(e)}>
            {v}
          </a>
        )}
      </div>
    </td>
  )
})
CellRenders.addRender('PHONE', (v, s, k) => {
  return (
    <td key={k}>
      <div style={s} title={v}>
        {$env.isDingTalk() || $env.isWxWork() ? (
          <a>{v}</a>
        ) : (
          <a href={`tel:${v}`} className="column-url" onClick={(e) => $stopEvent(e)}>
            {v}
          </a>
        )}
      </div>
    </td>
  )
})
const APPROVAL_STATE_CLAZZs = {
  [$L('审批中')]: 'warning',
  [$L('驳回')]: 'danger',
  [$L('通过')]: 'success',
}
CellRenders.addRender('STATE', (v, s, k) => {
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
const _renderNumber = (v, s, k) => {
  // 负数
  if ((v + '').includes('-')) {
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
}
CellRenders.addRender('DECIMAL', _renderNumber)
CellRenders.addRender('NUMBER', _renderNumber)
CellRenders.addRender('MULTISELECT', (v, s, k) => {
  const vLen = (v.text || []).length
  return (
    <td key={k} className="td-sm" title={$L('共 %d 项', vLen)}>
      <div className="column-multi" style={s}>
        {(v.text || []).map((item) => {
          if (typeof item === 'object') {
            return (
              <span key={item.text} className="badge" title={item.text} style={$tagStyle2(item.color)}>
                {item.text}
              </span>
            )
          } else {
            return (
              <span key={item} className="badge" title={item}>
                {item}
              </span>
            )
          }
        })}
      </div>
    </td>
  )
})
CellRenders.addRender('AVATAR', (v, s, k) => {
  const imgUrl = $isFullUrl(v) ? v : `${rb.baseUrl}/filex/img/${v}?imageView2/2/w/100/interlace/1/q/100`
  return (
    <td key={k} className="user-avatar">
      <img src={imgUrl} alt="Avatar" />
    </td>
  )
})
CellRenders.addRender('LOCATION', (v, s, k) => {
  return (
    <td key={k}>
      <div style={s} title={v.text}>
        <a
          href={`#!/Map:${v.lng || ''},${v.lat || ''}`}
          onClick={(e) => {
            $stopEvent(e, true)
            if (window.BaiduMapModal) BaiduMapModal.view(v)
          }}>
          {v.text}
        </a>
      </div>
    </td>
  )
})
CellRenders.addRender('SIGN', (v, s, k) => {
  return (
    <td key={k} className="user-avatar sign">
      <div style={s}>
        <img alt="SIGN" src={v} />
      </div>
    </td>
  )
})
const _renderOptionField = (v, s, k) => {
  // Use badge
  if (typeof v === 'object') {
    return (
      <td key={k} className="td-sm column-state">
        <div style={s} title={v.text}>
          <span className="badge" style={$tagStyle2(v.color)}>
            {v.text}
          </span>
        </div>
      </td>
    )
  } else {
    return CellRenders.renderSimple(v, s, k)
  }
}
CellRenders.addRender('PICKLIST', _renderOptionField)
CellRenders.addRender('CLASSIFICATION', _renderOptionField)
CellRenders.addRender('TAG', (v, s, k) => {
  const vLen = (v || []).length
  return (
    <td key={k} className="td-sm" title={$L('共 %d 项', vLen)}>
      <div className="column-multi" style={s}>
        {(v || []).map((item) => {
          return (
            <span key={item.name} className="badge" title={item.name} style={$tagStyle2(item.color)}>
              {item.name}
            </span>
          )
        })}
      </div>
    </td>
  )
})
CellRenders.addRender('BARCODE', (v, s, k) => {
  const codeUrl = `${rb.baseUrl}/commons/barcode/render-auto?t=${$encode(v)}`
  const isbar = v.startsWith('BC:')
  v = v.substr(3)
  return (
    <td key={k} className="td-sm">
      <div className="column-imgs" style={s}>
        <a
          onClick={() => {
            RbAlert.create(
              <div className="mb-3 text-center">
                <img src={`${codeUrl}&w=${isbar ? 64 * 2 : 80 * 3}`} alt={v} style={{ maxWidth: '100%' }} />
                {!isbar && <div className="text-muted mt-2 mb-1 text-break text-bold">{v}</div>}
              </div>,
              {
                type: 'clear',
              }
            )
          }}>
          <img src={`${codeUrl}&w=${isbar ? 64 : 120}`} alt={v} />
        </a>
      </div>
    </td>
  )
})

// ~~ 记录合并

class RecordMerger extends RbModalHandler {
  constructor(props) {
    super(props)
    this.state.keepMain = props.ids[0]
  }

  render() {
    const datas = this.state.datas || []
    const idData = datas[0]

    return (
      <RbModal title={$L('记录合并')} ref={(c) => (this._dlg = c)} disposeOnHide width="1000" maximize>
        <div style={{ padding: 10 }}>
          <div className="record-merge-table">
            <h5 className="text-bold fs-14 m-0 mb-2">{$L('请选择需要保留的值')}</h5>
            <table className="table table-bordered table-hover table-sm m-0">
              <thead ref={(c) => (this._$thead = c)}>
                <tr>
                  <th width="200">{$L('字段/记录')}</th>
                  {idData &&
                    idData.map((item, idx) => {
                      if (idx === 0) return null
                      return (
                        <th key={idx} data-id={item[0]} onClick={() => this.setState({ keepMain: item[0] })}>
                          <a href={`${rb.baseUrl}/app/redirect?id=${item[0]}&type=newtab`} target="_blank" title={$L('打开')} onClick={(e) => $stopEvent(e)}>
                            <b className="fs-12">{item[1]}</b>
                            <i className="icon zmdi zmdi zmdi-open-in-new ml-1" />
                          </a>
                          {this.state.keepMain === item[0] && <span className="badge badge-success badge-pill ml-1">{$L('主')}</span>}
                        </th>
                      )
                    })}
                </tr>
              </thead>
              <tbody ref={(c) => (this._$tbody = c)}>
                {datas.map((item, idx) => {
                  if (idx === 0) return null
                  if ($isSysMask(item[0][1])) return null

                  let chk
                  const data4field = []
                  for (let i = 1; i < item.length; i++) {
                    let v = item[i]
                    let activeClazz
                    if ($empty(v)) {
                      v = <span className="text-muted">{$L('空')}</span>
                    } else {
                      if ($.isArray(v)) {
                        v = v.map(function (item) {
                          return (
                            <a key={item} onClick={() => RbPreview.create(item)}>
                              {$fileCutName(item)}
                            </a>
                          )
                        })
                      }

                      activeClazz = 'active'
                      if (chk) activeClazz = null
                      if (activeClazz) chk = true
                    }

                    const IS_COMMONS = item[0][2]
                    if (IS_COMMONS) activeClazz = 'sysfield'

                    data4field.push(
                      <td key={`${idx}-${i}`} data-index={i} className={activeClazz} onClick={(e) => !IS_COMMONS && this._selectValue(e)}>
                        <div>{v}</div>
                      </td>
                    )
                  }

                  const fieldMeta = item[0]
                  return (
                    <tr key={fieldMeta[0]} data-field={fieldMeta[0]}>
                      <th>{fieldMeta[1]}</th>
                      {data4field}
                    </tr>
                  )
                })}

                {this.props.hasDetails && idData && (
                  <tr className="bt2" ref={(c) => (this._$mergeDetails = c)}>
                    <th>{$L('合并明细记录')}</th>
                    {idData &&
                      idData.map((item, idx) => {
                        if (idx === 0) return null
                        return (
                          <td key={idx}>
                            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline pl">
                              <input className="custom-control-input" type="checkbox" defaultChecked value={item[0]} />
                              <span className="custom-control-label"> {$L('是')}</span>
                            </label>
                          </td>
                        )
                      })}
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div className="mt-2 mb-1" ref={(c) => (this._$btn = c)}>
          <div className="float-left ml-2"></div>
          <div className="float-right mr-1">
            <button className="btn btn-secondary btn-space mr-2" type="button" onClick={() => this.hide()}>
              {$L('取消')}
            </button>
            <button className="btn btn-primary btn-space mr-1" type="button" onClick={() => this._post()}>
              {$L('合并')}
            </button>
          </div>
          <div className="clearfix" />
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    $.get(`/app/${this.props.entity}/record-merge/fetch-datas?ids=${this.props.ids.join(',')}`, (res) => {
      this.setState({ datas: res.data || [] })
    })
  }

  _selectValue(e) {
    const $td = $(e.currentTarget)
    $td.parent().find('td').removeClass('active')
    $td.addClass('active')
  }

  _post() {
    const that = this
    RbAlert.create(
      <RF>
        <b>{$L('确认合并吗？')}</b>
        <div className="mt-1">
          <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
            <input className="custom-control-input" type="checkbox" defaultChecked />
            <span className="custom-control-label"> {$L('合并后自动删除被合并记录')}</span>
          </label>
        </div>
      </RF>,
      {
        onConfirm: function () {
          const del = $(this._element).find('input')[0].checked
          this.hide()
          that._post2(del)
        },
      }
    )
  }

  _post2(del) {
    const merged = {}
    $(this._$tbody)
      .find('tr')
      .each((idx, item) => {
        const field = $(item).data('field')
        const index = ~~$(item).find('td.active').data('index')
        if (index > 0) {
          const id = $(this._$thead).find(`th:eq(${index})`).data('id')
          merged[field] = id || null
        }
      })

    const details = []
    $(this._$mergeDetails)
      .find('input[checked]')
      .each(function () {
        details.push($(this).val())
      })

    // 主第一、排重
    let ids = [this.state.keepMain]
    this.props.ids.forEach(function (id) {
      if (!ids.includes(id)) ids.push(id)
    })

    const url = `/app/${this.props.entity}/record-merge/merge?ids=${ids.join(',')}&deleteAfter=${del || false}&mergeDetails=${details.join(',')}`
    const $btn = $(this._$btn).find('.btn').button('loading')
    $.post(url, JSON.stringify(merged), (res) => {
      if (res.error_code === 0) {
        this.hide()
        RbHighbar.success($L('合并成功'))
        this.props.listRef.reload()

        setTimeout(() => {
          window.RbViewModal.create({ id: res.data, entity: this.props.entity })
          if (window.RbListPage) {
            location.hash = `!/View/${this.props.entity}/${res.data}`
          }
        }, 500)
      } else {
        RbHighbar.error(res.error_msg)
        $btn.button('reset')
      }
    })
  }
}

// 列表图表部件
const ChartsWidget = {
  init: function () {
    // 复写
    window.chart_remove = function (box) {
      box.parent().animate({ opacity: 0 }, function () {
        box.parent().remove()
        ChartsWidget.saveWidget()
      })
    }

    // eslint-disable-next-line no-undef
    ECHART_BASE.grid = { left: 40, right: 20, top: 30, bottom: 20 }

    $('#asideShows a[href="#asideCharts"]').on('click', () => {
      this._chartLoaded !== true && this.loadWidget()
    })
    $('#asideCharts .charts--add').on('click', () => this.showChartSelect())
    $('#asideCharts .charts-wrap')
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
    renderRbcomp(<ChartSelect select={(c) => this.renderChart(c, true)} entity={wpc.entity[0]} />, function () {
      ChartsWidget.__chartSelect = this
      this.setState({ appended: ChartsWidget.__currentCharts() })
    })
  },

  renderChart: function (chart, append) {
    const $w = $(`<div id="chart-${chart.chart}"></div>`).appendTo('#asideCharts .charts-wrap')
    // eslint-disable-next-line no-undef
    renderRbcomp(detectChart({ ...chart, editable: true }, chart.chart), $w, function () {
      if (append) ChartsWidget.saveWidget()
    })
  },

  loadWidget: function () {
    $.get(`/app/${wpc.entity[0]}/widget-charts`, (res) => {
      this._chartLoaded = true
      this.__config = res.data || {}
      res.data && $(res.data.config).each((idx, chart) => this.renderChart(chart))
    })
  },

  saveWidget: function () {
    const charts = this.__currentCharts(true)
    $.post(`/app/${wpc.entity[0]}/widget-charts?id=${this.__config.id || ''}`, JSON.stringify(charts), (res) => {
      ChartsWidget.__config.id = res.data
      $('.page-aside.widgets .tab-content').perfectScrollbar('update')
    })
  },

  __currentCharts: function (o) {
    const charts = []
    $('#asideCharts .charts-wrap>div').each(function () {
      const id = $(this).attr('id').substr(6)
      if (o) charts.push({ chart: id })
      else charts.push(id)
    })
    return charts
  },
}

// 分组
const CategoryWidget = {
  __ALL: '$ALL$',

  init() {
    let _init = false
    $('#asideShows a[href="#asideCategory"]').on('click', () => {
      if (_init) return
      renderRbcomp(
        <AsideTree4Category
          entity={wpc.entity[0]}
          onItemClick={(query) => {
            if (!query || query[0] === CategoryWidget.__ALL) wpc.protocolFilter = null
            else wpc.protocolFilter = `category:${wpc.entity[0]}:${query.join('$$$$')}`
            _RbList().pageNo = 1
            RbListPage.reload()
          }}
        />,
        'asideCategory'
      )
      _init = true
    })
  },
}

const _FrontJS = window.FrontJS
const _EasyAction = window.EasyAction
// eslint-disable-next-line no-unused-vars
const EasyAction4List = {
  init(items) {
    if (!(_FrontJS && _EasyAction && items)) return
    const _List = _FrontJS.DataList

    // 工具栏
    const _eaDatalist = items['datalist']
    if (_eaDatalist) {
      _eaDatalist.forEach((item) => {
        item = _EasyAction.fixItem(item)
        if (!item) return

        item.onClick = () => _EasyAction.handleOp(item)
        item.items &&
          item.items.forEach((itemL2) => {
            itemL2.onClick = () => _EasyAction.handleOp(itemL2)
          })
        _List.addButton(item)
      })
    }

    // 记录行
    const _eaDatarow = items['datarow']
    if (_eaDatarow) {
      _eaDatarow.forEach((item) => {
        item = _EasyAction.fixItem(item)
        if (!item) return

        item.onClick = (id) => _EasyAction.handleOp(item, id)
        item._eaid = item.id
        _List.regRowButton(item)
      })

      const RbList_renderAfter40 = RbList.renderAfter40
      RbList.renderAfter40 = function (listObj) {
        typeof RbList_renderAfter40 === 'function' && RbList_renderAfter40(listObj)

        // ROW
        $(listObj._$tbody)
          .find('tr')
          .each(function () {
            const $row = $(this)
            const id = $row.data('id')
            // ACTION
            _EasyAction.checkShowFilter(_eaDatarow, id, (res) => {
              $row.find('.col-action button[data-eaid]').each((i, b) => {
                const $this = $(b)
                if (res[$this.data('eaid')]) {
                  $this.removeClass('disabled hide')
                } else {
                  $this.addClass('hide')
                }

                if ($this.hasClass('bs-tooltip')) {
                  setTimeout(() => {
                    $this.tooltip({})
                    // $this.on('show.bs.tooltip', function () {
                    //   $(this).tooltip('update') // 强制更新位置
                    // })
                  }, 300)
                }
              })
            })
          })
      }
    }
  },
}

class AsideTree4Category extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    return (
      <div className={`aside-2tree ${!this.state._allowChild && 'hide-collapse'}`} ref={(c) => (this._$element = c)}>
        <ul className="list-unstyled m-0 ">
          {this.state.datas &&
            this.state.datas.map((item) => {
              let hasChild = item.hasChild
              if (typeof hasChild === 'undefined') {
                hasChild = this.state._allowChild
              }
              return <TreeNode key={item.id} {...item} hasChild={hasChild} entity={this.props.entity} $$$parent={this} />
            })}
        </ul>
      </div>
    )
  }

  componentDidMount() {
    $.get(`/app/${this.props.entity}/widget-category-data`, (res) => {
      const _data = res.data || {}
      const datas = [{ id: CategoryWidget.__ALL, text: $L('全部数据'), hasChild: false }, ...(_data.data || [])]
      this.setState({ datas: datas, _allowChild: _data.hasChild }, () => {
        $(this._$element).find('li:eq(0)').addClass('active')
      })
    })
  }

  queryList(query) {
    typeof this.props.onItemClick === 'function' && this.props.onItemClick(query)
  }
}

class TreeNode extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, _expand: false }
  }

  render() {
    const props = this.props
    let hasChild = this.state.hasChild === true
    if (hasChild && this.state.children && this.state.children.length === 0) hasChild = false
    if (this.state.children && this.state.children.length > 0) hasChild = true

    return (
      <RF>
        <li data-id={props.id} ref={(c) => (this._$node = c)}>
          <span className={`collapse-icon ${!hasChild && 'no-child'}`} onClick={() => hasChild && this.handleExpand()}>
            <i className={`zmdi zmdi-chevron-right ${this.state._expand && 'open'} `} />
          </span>
          <a className="text-ellipsis" onClick={() => this.handleClick()} title={props.text}>
            {props.text}
          </a>
        </li>
        {this.state.children && (
          <ul className={`list-unstyled m-0 ${!this.state._expand && 'hide'}`} _title2={$L('无')}>
            {this.state.children.map((item) => {
              let hasChild = this.state._allowChild
              return <TreeNode key={item.id} {...item} hasChild={hasChild} entity={this.props.entity} $$$parent={this} />
            })}
          </ul>
        )}
      </RF>
    )
  }

  handleExpand() {
    this.setState({ _expand: !this.state._expand })
    if (this.state.children) return

    const url = `/app/${this.props.entity}/widget-category-data?filterVal=${$encode(this.filterVal().join('$$$$'))}`
    $.get(url, (res) => {
      const _data = res.data || {}
      const datas = [...(_data.data || [])]
      this.setState({ children: datas, _allowChild: _data.hasChild })
    })
  }

  handleClick() {
    $('#asideCategory ul>li').removeClass('active')
    $(this._$node).addClass('active')
    this.queryList(this.filterVal())
  }

  filterVal() {
    if (typeof this.props.$$$parent.filterVal === 'function') {
      let vv = this.props.$$$parent.filterVal() || []
      vv.push(this.props.id)
      return vv
    }
    return [this.props.id]
  }

  queryList(q) {
    typeof this.props.$$$parent.queryList === 'function' && this.props.$$$parent.queryList(q)
  }
}
