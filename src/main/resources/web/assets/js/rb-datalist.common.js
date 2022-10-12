/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global FieldValueSet, Share2Settings */
// 列表公共操作

const _RbList = function () {
  return RbListPage._RbList || {}
}

// ~~ 高级查询

class ListAdvFilter extends AdvFilter {
  render() {
    const filterComp = super.render()
    return this.props.inModal ? filterComp : <div className="dropdown-menu-advfilter">{filterComp}</div>
  }

  renderAction() {
    return (
      <div className="item dialog-footer">
        {this.props.inModal ? (
          <RF>
            <div className="float-left">
              <div className="float-left input">
                <input className="form-control form-control-sm text" maxLength="20" value={this.state.filterName || ''} data-id="filterName" onChange={this.handleChange} />
              </div>
              {rb.isAdminUser && <Share2 ref={(c) => (this._shareTo = c)} shareTo={this.props.shareTo} noSwitch />}
            </div>
            <div className="float-right">
              <button
                className="btn btn-primary"
                type="button"
                onClick={() => {
                  const name = this.state.filterName
                  const shareTo = this._shareTo ? this._shareTo.getData().shareTo : null
                  this.post(name, shareTo)
                }}>
                {$L('保存')}
              </button>
              <button className="btn btn-primary btn-outline" type="button" onClick={() => this.searchNow()}>
                <i className="icon zmdi zmdi-search" /> {$L('立即查询')}
              </button>
            </div>
          </RF>
        ) : (
          <div className="float-right">
            <div className="btn-group">
              <button className="btn btn-primary btn-outline" type="button" onClick={() => this.searchNow()}>
                <i className="icon zmdi zmdi-search" /> {$L('立即查询')}
              </button>
              <button className="btn btn-primary btn-outline dropdown-toggle w-auto" type="button" data-toggle="dropdown" style={{ marginLeft: -1 }}>
                <i className="icon zmdi zmdi-chevron-down" />
              </button>
              <div className="dropdown-menu dropdown-menu-right">
                <a className="dropdown-item" onClick={() => this.handleSave()}>
                  {$L('保存')}
                </a>
              </div>
            </div>
            <button className="btn btn-secondary" type="button" onClick={() => this.searchNow(true)}>
              <i className="icon mdi mdi-restore" /> {$L('清除')}
            </button>
          </div>
        )}

        <div className="clearfix" />
      </div>
    )
  }

  searchNow(clear) {
    if (clear) {
      RbListPage._RbList.search({ items: [] }, true)
    } else {
      const adv = this.toFilterJson(true)
      if (adv) RbListPage._RbList.search(adv, true)
    }
  }

  post(name, shareTo) {
    const filter = this.toFilterJson(this.props.canNoFilters)
    if (!filter) return

    let url = `/app/${this.props.entity}/advfilter/post?id=${this.props.id || ''}`
    if (name) url += `&name=${$encode(name)}`
    if (shareTo) url += `&shareTo=${$encode(shareTo)}`

    $.post(url, JSON.stringify(filter), (res) => {
      if (res.error_code === 0) {
        this.props.inModal && this._dlg.hide()
        typeof this.props.onConfirm === 'function' && this.props.onConfirm(res.data.id)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }

  handleSave() {
    const filter = this.toFilterJson(this.props.canNoFilters)
    if (!filter) return

    renderRbcomp(
      <ListAdvFilterSave
        configName={this.props.filterName}
        shareTo={this.props.shareTo}
        onConfirm={(d) => {
          this.post(d.configName, d.shareTo)
        }}
      />
    )
  }
}

class ListAdvFilterSave extends Share2Settings {
  renderContent() {
    return (
      <div className="form">
        {rb.isAdminUser && (
          <div className="form-group">
            <label className="text-bold">{$L('共享给')}</label>
            <UserSelector ref={(c) => (this._UserSelector = c)} selected={this.state.selected} />
            <p className="form-text">{$L('可以共享给不同的角色或职能部门，便于统一管理')}</p>
          </div>
        )}
        <div className="form-group">
          <input type="text" className="form-control form-control-sm" placeholder={$L('我的查询')} value={this.state.configName || ''} name="configName" onChange={this.handleChange} />
        </div>

        <div className="form-group mb-1">
          <button className="btn btn-primary btn-space" type="button" onClick={() => this.handleConfirm()}>
            {$L('确定')}
          </button>
        </div>
      </div>
    )
  }

  getData() {
    const s = super.getData()
    // eslint-disable-next-line no-undef
    if (s.shareTo === SHARE_ALL && !rb.isAdminUser) s.shareTo = null
    return s
  }
}

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
      console.log(e.target, e.currentTarget)
      if (!e.target) return
      var $target = $(e.target)
      if (
        $target.hasClass('J_filterbtn') ||
        $target.parent().hasClass('J_filterbtn') ||
        $target.hasClass('dropdown-menu-advfilter') ||
        $target.parents('.dropdown-menu-advfilter')[0] ||
        $target.hasClass('modal') ||
        $target.parents('.modal')[0] ||
        $target.parents('.select2-container')[0]
      ) {
        return
      }

      if (this.__customAdv) {
        this.__$customAdvWrap.addClass('hide')
      }
    })

    const $alldata = $('.adv-search .dropdown-item:eq(0)')
    $alldata.on('click', () => this._effectFilter($alldata, 'aside'))

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

        // v3.1 取消
        // if ($ghost.find('.dropdown-item').length < 2 && typeof window.startTour === 'function') {
        //   const $hub = $('<div class="mt-3"></div>').appendTo($('#asideFilters'))
        //   renderRbcomp(
        //     <RbAlertBox
        //       type="info"
        //       message={
        //         <RF>
        //           <a className="mr-1" href="#" onClick={(e) => AdvFilters.showAddCommonQuery(e)}>
        //             {$L('添加')}
        //           </a>
        //           {$L('常用查询方便以后使用')}
        //         </RF>
        //       }
        //     />,
        //     $hub
        //   )
        // }
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
          renderRbcomp(<ListAdvFilter {...props} />, this.__$customAdvWrap, function () {
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

  showAddCommonQuery(e) {
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
}

// ~~ 添加常用查询

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

            that._checkState(res.data, mp)
          } else {
            that.disabled(false)
            RbHighbar.error(res.error_msg)
          }
        })
      },
    })
  }

  _checkState(taskid, mp) {
    $.get(`/commons/task/state?taskid=${taskid}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.hasError) {
          setTimeout(() => {
            if (mp) mp.end()
          }, 510)

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
            RbListPage.reload()
          }, 500)
        } else {
          mp && mp.set(cp)
          setTimeout(() => this._checkState(taskid, mp), 1000)
        }
      }
    })
  }
}

// ~ 批量修改编辑器

const BUE_OPTYPES = {
  SET: $L('修改为'),
  NULL: $L('置空'),
  // TODO 支持更多修改模式
  // PREFIX: $L('前添加'),
  // SUFFIX: $L('后添加'),
  // PLUS: $L('加上'),
  // MINUS: $L('减去'),
}

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
    const data = {
      field: this.state.selectField,
      op: this.state.selectOp,
    }

    const field = this.props.fields.find((x) => this.state.selectField === x.name)
    if (data.op === 'NULL') {
      if (!field.nullable) {
        RbHighbar.create($L('%s 不能为空', field.label))
        return null
      } else {
        return data
      }
    }

    data.value = this._FieldValue.val()
    if (!data.value) return null
    else return data
  }
}

// ~~ 通用操作

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
    if (!entity) return

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

// default: 数据列表

const wpc = window.__PageConfig || {}

const COLUMN_MIN_WIDTH = 30
const COLUMN_MAX_WIDTH = 800
const COLUMN_DEF_WIDTH = 130

// IE/Edge 不支持首/列固定
const supportFixedColumns = !($.browser.msie || $.browser.msedge) && $(window).width() > 767

// eslint-disable-next-line no-unused-vars
class RbList extends React.Component {
  constructor(props) {
    super(props)

    this._$wrapper = $(props.$wrapper || '#react-list')
    this._entity = props.config.entity

    this.__defaultFilterKey = `AdvFilter-${this._entity}`
    this.__sortFieldKey = `SortField-${this._entity}`
    this.__columnWidthKey = `ColumnWidth-${this._entity}.`

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

    this.__defaultColumnWidth = this._$wrapper.width() / 10
    if (this.__defaultColumnWidth < COLUMN_DEF_WIDTH) this.__defaultColumnWidth = COLUMN_DEF_WIDTH

    this.pageNo = 1
    this.pageSize = $storage.get('ListPageSize') || 20
    this.advFilterId = wpc.advFilter !== true ? null : $storage.get(this.__defaultFilterKey) // 无高级查询
    this.fixedColumns = supportFixedColumns && props.uncheckbox !== true
  }

  render() {
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
                      const cWidth = item.width || this.__defaultColumnWidth
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
                    const primaryKey = item[lastIndex]
                    const rowKey = `row-${primaryKey.id}`
                    return (
                      <tr key={rowKey} data-id={primaryKey.id} onClick={(e) => this._clickRow(e, true)} onDoubleClick={(e) => this._openView(e.currentTarget)}>
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
                          return this.renderCell(cell, index, primaryKey)
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
    // FIXME 拖到底部才能左右滚动

    const $scroller = $(this._rblistScroller)
    $scroller.perfectScrollbar({
      // suppressScrollY: true,
      // wheelSpeed: 1,
    })

    // Use `pin`
    if (this.props.unpin !== true && $(window).height() > 666 && $(window).width() >= 1280) {
      $('.main-content').addClass('pb-0')
      if (supportFixedColumns) $scroller.find('.table').addClass('table-header-fixed')

      $addResizeHandler(() => {
        let mh = $(window).height() - 210
        if ($('.main-content>.nav-tabs-classic').length > 0) mh -= 38 // Has tab
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
      containment: $scroller.find('.rb-datatable-body'),
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
    if (wpc.advFilter !== true) this.fetchList(this._buildQuick())

    // 按键操作
    if (wpc.type === 'RecordList' || wpc.type === 'DetailList') $(document).on('keydown', (e) => this._keyEvent(e))
  }

  fetchList(filter) {
    const fields = []
    let fieldSort = null
    this.state.fields.forEach(function (item) {
      fields.push(item.field)
      if (item.sort) fieldSort = `${item.field}:${item.sort.replace('sort-', '')}`
    })
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
      sort: fieldSort,
      reload: reload,
      statsField: wpc.statsField === true && rb.commercial > 0,
    }
    this.__lastQueryEntry = query

    const loadingTimer = setTimeout(() => {
      this.setState({ inLoad: true }, () => this._$wrapper.addClass('rb-loading-active'))
    }, 400)

    $.post(`/app/${this._entity}/data-list`, JSON.stringify(query), (res) => {
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
    if (field.field === this.props.config.nameField) {
      cellVal = primaryKey
      type = '$NAME$'
    } else if (cellVal === '$NOPRIVILEGES$') {
      type = cellVal
    }

    // @see rb-datalist.common.js
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
      this._openView($tr)
    }
  }

  _openView($tr) {
    if (!wpc.type) return
    const id = $($tr).data('id')
    if (!wpc.forceSubView) {
      location.hash = `!/View/${this._entity}/${id}`
    }
    CellRenders.clickView({ id: id, entity: this._entity })
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
   * 设置高级过滤器 ID
   */
  setAdvFilter(id) {
    this.advFilterId = id
    this.pageNo = 1
    this.fetchList(this._buildQuick())
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
    const q = el.val()
    if (!q && !this.lastFilter) return null

    return {
      entity: this._entity,
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
    return $clone(this.__lastQueryEntry)
  }

  /**
   * 渲染完成后回调
   */
  static renderAfter() {}
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
                  <i className="support-plat mdi mdi-monitor" title={$L('支持 PC')} style={{ marginTop: 5, marginLeft: 5 }} />
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

// 列表（单元格）渲染
const CellRenders = {
  // 打开记录
  clickView(v, e) {
    if (window.RbViewModal) {
      window.RbViewModal.create({ id: v.id, entity: v.entity }, wpc.forceSubView)
    } else if (parent && parent.RbViewModal) {
      parent.RbViewModal.create({ id: v.id, entity: v.entity }, wpc.forceSubView)
    }
    e && $stopEvent(e)
    return false
  },

  // 打开预览
  clickPreview(v, idx, e) {
    RbPreview.create(v, idx)
    e && $stopEvent(e)
    return false
  },

  __renders: {},

  addRender(type, func) {
    this.__renders[type] = func
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
    <td key={k} className="td-sm" title={$L('共 %d 项', vLen)}>
      <div className="column-imgs" style={s}>
        {v.map((item, idx) => {
          const imgName = $fileCutName(item)
          const imgUrl = $isFullUrl(item) ? item : `${rb.baseUrl}/filex/img/${item}`
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
    <td key={k} className="td-sm" title={$L('共 %d 项', vLen)}>
      <div className="column-multi" style={s}>
        {v.map((item) => {
          return (
            <a key={item.id} title={item.text} className="badge hover-color" href={`#!/View/${item.entity}/${item.id}`} onClick={(e) => CellRenders.clickView(item, e)}>
              {item.text}
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

const renderNumber = function (v, s, k) {
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
}
CellRenders.addRender('DECIMAL', renderNumber)
CellRenders.addRender('NUMBER', renderNumber)

CellRenders.addRender('MULTISELECT', function (v, s, k) {
  const vLen = (v.text || []).length
  return (
    <td key={k} className="td-sm" title={$L('共 %d 项', vLen)}>
      <div className="column-multi" style={s}>
        {(v.text || []).map((item) => {
          if (typeof item === 'object') {
            const style2 = { borderColor: item.color, backgroundColor: item.color, color: '#fff' }
            // const style2 = { borderColor: item.color, color: item.color }
            return (
              <span key={item.text} className="badge" title={item.text} style={style2}>
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

CellRenders.addRender('AVATAR', function (v, s, k) {
  const imgUrl = $isFullUrl(v) ? v : `${rb.baseUrl}/filex/img/${v}?imageView2/2/w/100/interlace/1/q/100`
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

CellRenders.addRender('PICKLIST', function (v, s, k) {
  // Use badge
  if (typeof v === 'object') {
    const style2 = { borderColor: v.color, backgroundColor: v.color, color: '#fff' }
    // const style2 = { borderColor: v.color, color: v.color, backgroundColor: '#fff' }
    return (
      <td key={k} className="td-sm column-state">
        <div style={s} title={v.text}>
          <span className="badge" style={style2}>
            {v.text}
          </span>
        </div>
      </td>
    )
  } else {
    return CellRenders.renderSimple(v, s, k)
  }
})
