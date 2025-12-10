/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~~ 数据列表配置
// eslint-disable-next-line no-unused-vars
class DataListSettings extends RbModalHandler {
  render() {
    const state = this.state || {}
    const filterLen = state.filterData ? (state.filterData.items || []).length : 0

    return (
      <RbModal title={$L('编辑图表')} disposeOnHide ref={(c) => (this._dlg = c)}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('图表数据来源')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref={(c) => (this._$entity = c)}>
                {(state.entities || []).map((item) => {
                  return (
                    <option key={item.name} value={item.name}>
                      {item.entityLabel}
                    </option>
                  )
                })}
              </select>
            </div>
          </div>
          <div className="form-group row pb-0 DataList-showfields">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('显示字段')}</label>
            <div className="col-sm-7">
              <div className="sortable-box rb-scroller h200" ref={(c) => (this._$showfields = c)}>
                <ol className="dd-list" _title={$L('无')}></ol>
              </div>
              <div>
                <select className="form-control form-control-sm" ref={(c) => (this._$afields = c)}>
                  <option value=""></option>
                  {(state.afields || []).map((item) => {
                    return (
                      <option key={item.field} value={item.field}>
                        {item.label}
                      </option>
                    )
                  })}
                </select>
              </div>
            </div>
          </div>

          <div className="form-group row pb-1">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('附加过滤条件')}</label>
            <div className="col-sm-7">
              <a className="btn btn-sm btn-link pl-0 text-left down-2" onClick={() => this._showFilter()}>
                {filterLen > 0 ? $L('已设置条件') + ` (${filterLen})` : $L('点击设置')}
              </a>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('最大显示条数')}</label>
            <div className="col-sm-7">
              <input type="number" className="form-control form-control-sm" placeholder="40" ref={(c) => (this._$pageSize = c)} />
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('图表名称')}</label>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" placeholder={$L('数据列表')} ref={(c) => (this._$chartTitle = c)} />
            </div>
          </div>
          {rb.isAdminUser && (
            <div className="form-group row pb-2 pt-1">
              <label className="col-sm-3 col-form-label text-sm-right"></label>
              <div className="col-sm-7">
                <label className="custom-control custom-control-sm custom-checkbox mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._$shareChart = c)} />
                  <span className="custom-control-label">
                    {$L('共享此图表')}
                    <i className="zmdi zmdi-help zicon" title={$L('共享后其他用户也可以使用 (不能修改)')} />
                  </span>
                </label>
              </div>
            </div>
          )}

          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._$btn = c)}>
              <button className="btn btn-primary" type="button" onClick={() => this.handleConfirm()}>
                {$L('保存')}
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
    let $showfields = $(this._$showfields).perfectScrollbar()
    $showfields = $showfields
      .find('ol')
      .sortable({
        placeholder: 'dd-placeholder',
        handle: '.dd3-handle',
        axis: 'y',
      })
      .disableSelection()

    const that = this
    const props = this.props

    let $afields2
    function _loadFields() {
      if (!that._entity) {
        $(that._$afields).select2({
          placeholder: $L('无可用字段'),
        })
        return
      }

      $.get(`/app/${that._entity}/list-fields`, (res) => {
        // clear last
        if ($afields2) {
          $(that._$afields).select2('destroy')
          $showfields.empty()
          that.setState({ filterData: null })
        }

        that._afields = (res.data || {}).fieldList || []
        that.setState({ afields: that._afields }, () => {
          $afields2 = $(that._$afields)
            .select2({
              placeholder: $L('添加显示字段'),
              allowClear: false,
            })
            .val('')
            .on('change', (e) => {
              let name = e.target.value
              $showfields.find('li').each(function () {
                if ($(this).data('key') === name) {
                  name = null
                }
              })

              const x = name ? that._afields.find((x) => x.field === name) : null
              if (!x) return

              const $item = $(
                `<li class="dd-item dd3-item" data-key="${x.field}"><div class="dd-handle dd3-handle"></div><div class="dd3-content">${x.label}</div><div class="dd3-action"></div></li>`
              ).appendTo($showfields)

              if (!window.UNSORT_FIELDTYPES.includes(x.type)) {
                $(`<a title="${$L('默认排序')}"><i class="zmdi mdi mdi-sort-alphabetical-ascending sort"></i></a>`)
                  .appendTo($item.find('.dd3-action'))
                  .on('click', () => {
                    const hasActive = $item.hasClass('active')
                    $showfields.find('.dd-item').removeClass('active')
                    $item.addClass('active')
                    if (hasActive) $item.find('.sort').toggleClass('desc')
                  })

                // init
                if (props.entity === that._entity && props.sort) {
                  const s = props.sort.split(':')
                  if (s[0] === name) {
                    $item.addClass('active')
                    if (s[1] === 'desc') $item.find('.sort').toggleClass('desc')
                  }
                }
              }

              $(`<a title="${$L('移除')}"><i class="zmdi zmdi-close"></i></a>`)
                .appendTo($item.find('.dd3-action'))
                .on('click', () => $item.remove())
            })

          // init
          if (props.entity === that._entity && props.fields) {
            props.fields.forEach((name) => {
              $afields2.val(name).trigger('change')
            })
            $afields2.val('').trigger('change')
          }
        })
      })
    }

    $.get('/commons/metadata/entities?detail=yes', (res) => {
      this.setState({ entities: res.data || [] }, () => {
        const $s = $(this._$entity).select2({
          allowClear: false,
        })

        if (props.entity && props.entity !== 'User') $s.val(props.entity || null)
        $s.on('change', (e) => {
          this._entity = e.target.value
          _loadFields()
        }).trigger('change')
      })
    })

    // init
    $(this._$pageSize).val(props.pageSize || null)
    $(this._$chartTitle).val(props.title || null)
    if (props.filter) this.setState({ filterData: props.filter })
    if ((props.option || {}).shareChart) $(this._$shareChart).attr('checked', true)
  }

  _showFilter() {
    renderRbcomp(
      <AdvFilter
        entity={this._entity}
        filter={this.state.filterData || null}
        title={$L('附加过滤条件')}
        inModal
        canNoFilters
        onConfirm={(s) => {
          this.setState({ filterData: s })
        }}
      />
    )
  }

  handleConfirm() {
    const fields = []
    let sort = null
    $(this._$showfields)
      .find('li')
      .each(function () {
        const $this = $(this)
        fields.push($this.data('key'))

        if ($this.hasClass('active')) {
          sort = $this.data('key') + `:${$this.find('.desc')[0] ? 'desc' : 'asc'}`
        }
      })

    const post = {
      type: 'DataList',
      entity: $(this._$entity).val(),
      title: $(this._$chartTitle).val() || $L('数据列表'),
      option: {
        shareChart: $val(this._$shareChart) && rb.isAdminUser,
      },
      fields: fields,
      pageSize: $(this._$pageSize).val(),
      filter: this.state.filterData || null,
      sort: sort,
    }

    if (!post.entity) return RbHighbar.create($L('请选择图表数据来源'))
    if (post.fields.length === 0) return RbHighbar.create($L('请添加显示字段'))
    if (post.pageSize && post.pageSize > 500) post.pageSize = 500

    const $btn = $(this._$btn).find('.btn').button('loading')
    $.post(`/dashboard/builtin-chart-save?id=${this.props.chart}`, JSON.stringify(post), (res) => {
      $btn.button('reset')
      if (res.error_code === 0) {
        typeof this.props.onConfirm === 'function' && this.props.onConfirm(post)
        this.hide()
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

// ~~ 标题配置
// eslint-disable-next-line no-unused-vars
class HeadingTextSettings extends RbModalHandler {
  render() {
    const style = this.props.style || {}
    return (
      <RbModal title={$L('编辑标题文字')} disposeOnHide ref={(c) => (this._dlg = c)}>
        <div className="form HeadingText-settings">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('标题')}</label>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" placeholder={$L('标题文字')} defaultValue={this.props.title} ref={(c) => (this._$title = c)} />
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('样式')}</label>
            <div className="col-sm-7 HeadingText-style">
              <div className="float-left">
                <input
                  className="form-control form-control-sm text-center"
                  type="text"
                  title={$L('文字大小')}
                  defaultValue={style.fontSize || ''}
                  ref={(c) => (this._$fontSize = c)}
                  placeholder={$L('默认')}
                  maxLength="3"
                />
              </div>
              <div className="float-left ml-2">
                <input type="color" title={$L('文字颜色')} defaultValue={style.color || ''} ref={(c) => (this._$color = c)} />
              </div>
              <div className="float-left ml-2">
                <input type="color" title={$L('背景颜色')} defaultValue={style.bgcolor || ''} ref={(c) => (this._$bgcolor = c)} />
              </div>
              <div className="float-left ml-2">
                <input
                  className="form-control form-control-sm text-center"
                  type="text"
                  title={$L('圆角')}
                  defaultValue={style.bgradius || ''}
                  ref={(c) => (this._$bgradius = c)}
                  placeholder={$L('默认')}
                  maxLength="3"
                />
              </div>
              <div className="clearfix" />
            </div>
          </div>
          {rb.isAdminUser && (
            <div className="form-group row pb-2 pt-1">
              <label className="col-sm-3 col-form-label text-sm-right" />
              <div className="col-sm-7">
                <label className="custom-control custom-control-sm custom-checkbox mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._$shareChart = c)} />
                  <span className="custom-control-label">
                    {$L('共享此图表')}
                    <i className="zmdi zmdi-help zicon" title={$L('共享后其他用户也可以使用 (不能修改)')} />
                  </span>
                </label>
              </div>
            </div>
          )}

          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._$btn = c)}>
              <button className="btn btn-primary" type="button" onClick={() => this.handleConfirm()}>
                {$L('保存')}
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
    $([this._$fontSize, this._$color, this._$bgcolor, this._$bgradius]).tooltip({})
    // init
    if ((this.props.option || {}).shareChart) $(this._$shareChart).attr('checked', true)
  }

  handleConfirm() {
    const post = {
      type: 'HeadingText',
      entity: 'User',
      title: $val(this._$title),
      style: {
        fontSize: $val(this._$fontSize) || null,
        color: $val(this._$color) || null,
        bgcolor: $val(this._$bgcolor) || null,
        bgradius: $val(this._$bgradius) || null,
      },
      option: {
        shareChart: $val(this._$shareChart) && rb.isAdminUser,
      },
    }
    // No color
    if (post.style.color === '#000000') post.style.color = null
    if (post.style.bgcolor === '#000000') post.style.bgcolor = null

    const $btn = $(this._$btn).find('.btn').button('loading')
    $.post(`/dashboard/builtin-chart-save?id=${this.props.chart}`, JSON.stringify(post), (res) => {
      $btn.button('reset')
      if (res.error_code === 0) {
        typeof this.props.onConfirm === 'function' && this.props.onConfirm(post)
        this.hide()
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

// ~~ 标题配置
// eslint-disable-next-line no-unused-vars
class EmbedFrameSettings extends RbModalHandler {
  render() {
    return (
      <RbModal title={$L('编辑嵌入页面')} disposeOnHide ref={(c) => (this._dlg = c)}>
        <div className="form EmbedFrame-settings">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('页面地址')}</label>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" placeholder={$L('页面地址')} defaultValue={this.props.url} ref={(c) => (this._$url = c)} />
              <p className="form-text">{$L('支持绝对地址或相对地址')}</p>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('图表名称')}</label>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" placeholder={$L('嵌入页面')} defaultValue={this.props.title} ref={(c) => (this._$title = c)} />
            </div>
          </div>
          {rb.isAdminUser && (
            <div className="form-group row pb-2 pt-1">
              <label className="col-sm-3 col-form-label text-sm-right"></label>
              <div className="col-sm-7">
                <label className="custom-control custom-control-sm custom-checkbox mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._$shareChart = c)} />
                  <span className="custom-control-label">
                    {$L('共享此图表')}
                    <i className="zmdi zmdi-help zicon" title={$L('共享后其他用户也可以使用 (不能修改)')} />
                  </span>
                </label>
              </div>
            </div>
          )}

          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._$btn = c)}>
              <button className="btn btn-primary" type="button" onClick={() => this.handleConfirm()}>
                {$L('保存')}
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
    // init
    if ((this.props.option || {}).shareChart) $(this._$shareChart).attr('checked', true)
  }

  handleConfirm() {
    const post = {
      type: 'EmbedFrame',
      entity: 'User',
      url: $val(this._$url),
      title: $val(this._$title),
      option: {
        shareChart: $val(this._$shareChart) && rb.isAdminUser,
      },
    }
    if (!post.url) {
      return RbHighbar.createl('请输入页面地址')
    } else if (!($regex.isUrl(post.url) || $regex.isUrl(`https://getrebuild.com${post.url}`))) {
      return RbHighbar.createl('请输入有效的页面地址')
    }

    const $btn = $(this._$btn).find('.btn').button('loading')
    $.post(`/dashboard/builtin-chart-save?id=${this.props.chart}`, JSON.stringify(post), (res) => {
      $btn.button('reset')
      if (res.error_code === 0) {
        typeof this.props.onConfirm === 'function' && this.props.onConfirm(post)
        this.hide()
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

// ~~ 我的常用配置
// eslint-disable-next-line no-unused-vars
class MyBookmarkSettings extends RbModalHandler {
  constructor(props) {
    super(props)
    this._actionType = 1
  }

  render() {
    return (
      <RbModal title={$L('添加常用')} disposeOnHide ref={(c) => (this._dlg = c)}>
        <div className="form MyBookmark-settings">
          <div className="form-group row pt-1">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('选择操作')}</label>
            <div className="col-sm-7">
              <div className="tab-container" ref={(c) => (this._$content = c)}>
                <ul className="nav nav-tabs">
                  <li className="nav-item">
                    <a className="nav-link active" style={{ paddingTop: 7 }} href="#MBA1" data-toggle="tab" onClick={() => (this._actionType = 1)}>
                      {$L('新建')}
                    </a>
                  </li>
                  <li className="nav-item">
                    <a className="nav-link" style={{ paddingTop: 7 }} href="#MBA2" data-toggle="tab" onClick={() => (this._actionType = 2)}>
                      {$L('列表')}
                    </a>
                  </li>
                  <li className="nav-item">
                    <a className="nav-link" style={{ paddingTop: 7 }} href="#MBA3" data-toggle="tab" onClick={() => (this._actionType = 3)}>
                      {$L('外部地址')}
                    </a>
                  </li>
                </ul>
                <div className="tab-content m-0 p-0 pt-3">
                  <div className="tab-pane active" id="MBA1">
                    <div>
                      <label>{$L('新建记录')}</label>
                      <select className="form-control form-control-sm">
                        <option value="" />
                        <optgroup label={$L('业务实体')}>
                          {this.state._entities &&
                            this.state._entities.map((item) => {
                              if (item.mainEntity) return null
                              return (
                                <option key={item.name} value={item.name}>
                                  {item.entityLabel}
                                </option>
                              )
                            })}
                        </optgroup>
                      </select>
                    </div>
                  </div>
                  <div className="tab-pane" id="MBA2">
                    <div>
                      <label>{$L('进入列表')}</label>
                      <select className="form-control form-control-sm">
                        <option value="" />
                        <optgroup label={$L('业务实体')}>
                          {this.state._entities &&
                            this.state._entities.map((item) => {
                              return (
                                <option key={item.name} value={item.name}>
                                  {item.entityLabel}
                                </option>
                              )
                            })}
                        </optgroup>
                      </select>
                    </div>
                  </div>
                  <div className="tab-pane" id="MBA3">
                    <div>
                      <label>{$L('支持绝对地址或相对地址')}</label>
                      <input type="text" className="form-control form-control-sm" placeholder={$L('输入外部地址')} />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('名称')}</label>
            <div className="col-sm-7">
              <div className="row" ref={(c) => (this._$name = c)}>
                <div className="col-10 pr-0">
                  <div className="input-group">
                    <span className="input-group-prepend">
                      <span className="input-group-text" title={$L('选择图标')}>
                        <i className="zmdi zmdi-texture" />
                      </span>
                    </span>
                    <input type="text" className="form-control form-control-sm" maxLength="100" placeholder={$L('名称')} />
                  </div>
                </div>
                <div className="col-2">
                  <input type="color" title={$L('配色')} />
                </div>
              </div>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._$btn = c)}>
              <button className="btn btn-primary" type="button" onClick={() => this.handleConfirm()}>
                {$L('保存')}
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
    // super.componentDidMount()
    $(this._$name).find('input[type=color]').tooltip({})
    let rndColor = RBCOLORS[Math.floor(Math.random() * (RBCOLORS.length + 1))]
    if (rndColor) $(this._$name).find('input[type=color]').val(rndColor)

    $.get('/commons/metadata/entities?detail=true&filterSys=true', (res) => {
      this.setState({ _entities: res.data || [] }, () => {
        $(this._$content)
          .find('select')
          .select2({
            allowClear: false,
            placeholder: $L('选择'),
          })
          .on('change', (e) => {
            let entity = e.currentTarget.value
            entity = this.state._entities.find((item) => item.name === entity)
            $(this._$name).find('input[type=text]').val(entity.entityLabel)
            $(this._$name)
              .find('i')
              .attr({ 'class': `zmdi zmdi-${entity.icon}`, 'data-icon': entity.icon })
          })
      })
    })

    const $icon = $(this._$name)
      .find('.input-group-text')
      .on('click', () => {
        window.clickIcon = function (s) {
          $icon.find('i').attr({
            'class': `zmdi zmdi-${s}`,
            'data-icon': s,
          })
          RbModal.hide()
        }
        RbModal.create('/p/common/search-icon', $L('选择图标'))
      })
  }

  handleConfirm() {
    const append = {
      icon: $(this._$name).find('i').attr('data-icon'),
      name: $(this._$name).find('input').val(),
      color: $(this._$name).find('input[type=color]').val(),
      actionType: this._actionType,
      actionParams: [],
      _id: $random(),
    }
    if (this._actionType === 1) {
      append.actionParams.push($('#MBA1 select').val())
      if (!append.actionParams[0]) {
        return RbHighbar.create($L('请选择新建记录'))
      }
    } else if (this._actionType === 2) {
      append.actionParams.push($('#MBA2 select').val())
      if (!append.actionParams[0]) {
        return RbHighbar.create($L('请选择进入列表'))
      }
    } else if (this._actionType === 3) {
      append.actionParams.push($('#MBA3 input').val())
      if (!append.actionParams[0]) {
        return RbHighbar.create($L('请输入外部地址'))
      }
    }
    // # color
    if (append.color === '#000000') append.color = null

    const $btn = $(this._$btn).find('.btn').button('loading')
    $.post(`/dashboard/builtin-chart-save?id=${this.props.chart}&append=true`, JSON.stringify(append), (res) => {
      $btn.button('reset')
      if (res.error_code === 0) {
        typeof this.props.onConfirm === 'function' && this.props.onConfirm(append)
        this.hide()
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}
