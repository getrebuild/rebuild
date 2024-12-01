/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable react/no-string-refs */

const wpc = window.__PageConfig

$(document).ready(() => {
  _listmodeAction()

  const metaid = wpc.id
  if (!metaid) {
    $('.J_drop-confirm').next().removeClass('hide')
    $('.J_drop-confirm').remove()
    $('.J_drop-check').parent().parent().remove()
    return
  }

  $('.J_drop-check').on('click', function () {
    $('.J_drop-confirm').attr('disabled', !$(this).prop('checked'))
  })

  const $drop = $('.J_drop-confirm').on('click', () => {
    if (!$('.J_drop-check').prop('checked')) return
    if (!window.__PageConfig.isSuperAdmin) {
      RbHighbar.error($L('仅超级管理员可删除实体'))
      return
    }

    RbAlert.create($L('实体删除后将无法恢复，请务必谨慎操作。确认删除吗？'), $L('删除实体'), {
      type: 'danger',
      confirmText: $L('删除'),
      confirm: function () {
        $drop.button('loading')
        this.disabled(true)
        $.post(`../entity-drop?id=${metaid}&force=${$('.J_drop-force').prop('checked')}`, (res) => {
          if (res.error_code === 0) {
            RbHighbar.success($L('实体已删除'))
            setTimeout(() => location.replace('../../entities'), 1500)
          } else {
            RbHighbar.error(res.error_msg)
            this.disabled()
          }
        })
      },
      countdown: 5,
    })
  })
})

// 列表模式
function _listmodeAction() {
  if (rb.commercial < 10) {
    $('.mode-select .btn').on('click', () => RbHighbar.error(WrapHtml($L('免费版不支持列表模式功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)'))))
    return
  }
  if (!wpc.id) {
    $('.mode-select .btn').on('click', () => RbHighbar.create($L('系统内置实体暂不支持')))
    return
  }

  if (wpc.extConfig && wpc.extConfig.advListMode) {
    $('.mode-select .btn[data-mode=' + wpc.extConfig.advListMode + ']')
      .addClass('active')
      .text($L('当前模式'))
  }

  $('.mode-select .J_mode-select').on('click', function () {
    const $btn = $(this)
    RbAlert.create($L('确认切换到此列表模式？'), {
      onConfirm: function () {
        this.disabled(true)
        const mode = $btn.data('mode')
        modeSave({ advListMode: mode }, () => location.reload())
      },
    })
  })

  // Mode's Option
  $('.mode-select .J_mode1-option').on('click', () => renderDlgcomp(<DlgMode1Option />, '_DlgMode1Option'))
  $('.mode-select .J_mode2-option').on('click', () => renderDlgcomp(<DlgMode2Option />, '_DlgMode2Option'))
  $('.mode-select .J_mode3-option').on('click', () => renderDlgcomp(<DlgMode3Option />, '_DlgMode3Option'))
}

function modeSave(newOption, next) {
  const extConfig = wpc.extConfig ? { ...wpc.extConfig, ...newOption } : { ...newOption }
  const data = {
    metadata: { entity: 'MetaEntity', id: wpc.id },
    extConfig: extConfig,
  }

  $.post('../entity-update', JSON.stringify(data), function (res) {
    if (res.error_code === 0) typeof next === 'function' && next()
    else RbHighbar.error(res.error_msg)
  })
}

const _CATEGORY_TYPES = ['PICKLIST', 'MULTISELECT', 'CLASSIFICATION', 'DATE', 'DATETIME', 'REFERENCE', 'N2NREFERENCE', 'TEXT']
// 模式选项
class DlgMode1Option extends RbFormHandler {
  constructor(props) {
    super(props)
    this._categoryFields = []
  }

  render() {
    return (
      <RbModal title={$L('标准模式选项')} ref={(c) => (this._dlg = c)}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('在侧栏显示')}</label>
            <div className="col-sm-9 aside-show" ref={(c) => (this._$asideShow = c)}>
              <div className="aside-item">
                <div className="switch-button switch-button-xs">
                  <input type="checkbox" id="advListHideFilters" defaultChecked={wpc.extConfig && !wpc.extConfig.advListHideFilters} />
                  <span>
                    <label htmlFor="advListHideFilters" />
                  </span>
                </div>
                <span className="ml-2 down-5 d-inline-block">{$L('常用查询')}</span>
              </div>
              {CompCategory(this)}
              <div className="aside-item">
                <div className="switch-button switch-button-xs">
                  <input type="checkbox" id="advListHideCharts" defaultChecked={wpc.extConfig && !wpc.extConfig.advListHideCharts} />
                  <span>
                    <label htmlFor="advListHideCharts" />
                  </span>
                </div>
                <span className="ml-2 down-5 d-inline-block">{$L('图表')}</span>
              </div>
            </div>
          </div>

          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('在顶栏显示')}</label>
            <div className="col-sm-9">
              <div className="bosskey-show mb-2">
                <div className="switch-button switch-button-xs">
                  <input type="checkbox" id="advListFilterTabs" defaultChecked={wpc.extConfig && wpc.extConfig.advListFilterTabs} />
                  <span>
                    <label htmlFor="advListFilterTabs" />
                  </span>
                </div>
                <span className="ml-2 down-5 d-inline-block">{$L('列表视图')}</span>
              </div>
              <div>
                <div className="switch-button switch-button-xs">
                  <input type="checkbox" id="advListFilterPane" defaultChecked={wpc.extConfig && wpc.extConfig.advListFilterPane} />
                  <span>
                    <label htmlFor="advListFilterPane" />
                  </span>
                </div>
                <span className="ml-2 down-5 d-inline-block">{$L('查询面板')}</span>
              </div>
            </div>
          </div>

          <div className="form-group row footer">
            <div className="col-sm-9 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={this.save}>
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
    CompCategory_componentDidMount(this)
  }

  save = () => {
    const o = {
      advListHideFilters: !$val('#advListHideFilters'),
      advListHideCharts: !$val('#advListHideCharts'),
      advListFilterPane: $val('#advListFilterPane'),
      advListFilterTabs: $val('#advListFilterTabs'),
    }
    if (this.state.advListShowCategory) {
      o.advListShowCategory = []
      this._categoryFields.forEach((refid) => {
        let c = this.refs[refid]
        c && o.advListShowCategory.push(c.val())
      })
    } else {
      o.advListShowCategory = null
    }

    this.disabled(true)
    modeSave(o, () => {
      this.hide()
      location.reload()
    })
  }
}

class DlgMode2Option extends RbFormHandler {
  render() {
    return (
      <RbModal title={$L('详情模式选项')} ref={(c) => (this._dlg = c)}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('显示字段')}</label>
            <div className="col-sm-9 pt-1">
              <div className="mode23-fields mode2-fields" ref={(c) => (this._$showFields = c)}>
                <a data-toggle="dropdown" className="L0">
                  {$L('无')}
                </a>
                <div className="dropdown-menu auto-scroller" />
                <a data-toggle="dropdown" data-reference="toggle" className="L1">
                  <em>{$L('名称字段')}</em>
                </a>
                <a data-toggle="dropdown" data-reference="toggle" className="L1-2 float-right mr-0">
                  <em>{$L('审批状态')}</em>
                </a>
                <div className="mt-1"></div>
                <a data-toggle="dropdown" data-reference="toggle">
                  {$L('创建时间')}
                </a>
                <a data-toggle="dropdown" data-reference="toggle">
                  {$L('创建人')}
                </a>
                <a data-toggle="dropdown" data-reference="toggle">
                  {$L('无')}
                </a>
              </div>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-9 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={this.save}>
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
    let $clickItem
    const $menu = $(this._$showFields).find('.dropdown-menu')
    $.get(`../list-field?entity=${wpc.entityName}`, (res) => {
      const _data = [{ fieldName: null, fieldLabel: $L('默认'), displayTypeName: 'N' }]
      _data.push(...res.data)

      const fieldsLables = {}
      _data.forEach((item) => {
        fieldsLables[item.fieldName] = item.fieldLabel
        if (['BARCODE', 'FILE', 'SIGN'].includes(item.displayTypeName)) return null

        const item2 = item
        $(`<a class="dropdown-item" data-name="${item.fieldName}" data-type="${item.displayTypeName}"></a>`)
          .text(item.fieldLabel)
          .appendTo($menu)
          .on('click', function () {
            $($clickItem).attr('data-name', item2.fieldName).text(item2.fieldLabel)
          })
      })
      $menu.perfectScrollbar()

      const $showFields = $(this._$showFields)
        .find('>a')
        .attr('title', $L('选择显示字段'))
        .on('click', function () {
          $clickItem = this
        })

      $(this._$showFields)
        .on('show.bs.dropdown', () => {
          this.onFieldsMenuShow($clickItem, $menu)
        })
        .on('hide.bs.dropdown', () => {
          $menu.find('.dropdown-item').removeClass('hide')
        })

      if (wpc.extConfig) {
        const showFields = this.__mode3 ? wpc.extConfig.mode3ShowFields : wpc.extConfig.mode2ShowFields
        if (showFields) {
          showFields.forEach((item, idx) => {
            if (item) {
              $showFields
                .eq(idx)
                .attr({ 'data-name': item })
                .text(fieldsLables[item] || `[${item.toUpperCase()}]`)
            }
          })
        }
      }
      // this._loadAfter && this._loadAfter(res.data)
    })
  }

  onFieldsMenuShow($item, $menu) {
    if ($($item).hasClass('L0')) {
      $menu.find('a').addClass('hide')
      $menu.find('a[data-type="N"]').removeClass('hide')
      $menu.find('a[data-type="IMAGE"]').removeClass('hide')
      $menu.find('a[data-type="AVATAR"]').removeClass('hide')
    } else {
      $menu.find('a[data-type="IMAGE"]').addClass('hide')
      $menu.find('a[data-type="AVATAR"]').addClass('hide')
    }
  }

  save = () => {
    const modeShowFields = []
    $(this._$showFields)
      .find('>a')
      .each(function () {
        modeShowFields.push($(this).attr('data-name') || null)
      })

    let o = {
      [this.__mode3 ? 'mode3ShowFields' : 'mode2ShowFields']: modeShowFields,
    }
    if (this._saveBefore) {
      o = this._saveBefore(o)
      if (o === false) return
    }

    this.disabled(true)
    modeSave(o, () => {
      this.hide()
      location.reload()
    })
  }
}

class DlgMode3Option extends DlgMode2Option {
  constructor(props) {
    super(props)
    this.__mode3 = true
    this._categoryFields = []
  }

  render() {
    return (
      <RbModal title={$L('卡片模式选项')} ref={(c) => (this._dlg = c)}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('在侧栏显示')}</label>
            <div className="col-sm-9 aside-show">
              <div className="aside-item">
                <div className="switch-button switch-button-xs">
                  <input type="checkbox" id="mode3ShowFilters" defaultChecked={wpc.extConfig && wpc.extConfig.mode3ShowFilters} />
                  <span>
                    <label htmlFor="mode3ShowFilters" />
                  </span>
                </div>
                <span className="ml-2 down-5 d-inline-block">{$L('常用查询')}</span>
              </div>
              {CompCategory(this, 'mode3ShowCategory')}
              <div className="aside-item">
                <div className="switch-button switch-button-xs">
                  <input type="checkbox" id="mode3ShowCharts" defaultChecked={wpc.extConfig && wpc.extConfig.mode3ShowCharts} />
                  <span>
                    <label htmlFor="mode3ShowCharts" />
                  </span>
                </div>
                <span className="ml-2 down-5 d-inline-block">{$L('图表')}</span>
              </div>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('显示字段')}</label>
            <div className="col-sm-9 pt-1">
              <div className="mode23-fields mode3-fields" ref={(c) => (this._$showFields = c)}>
                <a data-toggle="dropdown" className="L1">
                  <em>{$L('图片字段')}</em>
                </a>
                <div className="dropdown-menu auto-scroller" />
                <div className="mt-1"></div>
                <a data-toggle="dropdown" data-reference="toggle" className="L2">
                  <em>{$L('名称字段')}</em>
                </a>
                <div className="mt-1"></div>
                <a data-toggle="dropdown" data-reference="toggle">
                  {$L('无')}
                </a>
                <a data-toggle="dropdown" data-reference="toggle">
                  {$L('无')}
                </a>
                <a data-toggle="dropdown" data-reference="toggle">
                  {$L('无')}
                </a>
              </div>
            </div>
          </div>

          <div className="form-group row footer">
            <div className="col-sm-9 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={this.save}>
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
    super.componentDidMount()
    CompCategory_componentDidMount(this, 'mode3ShowCategory')
  }

  onFieldsMenuShow($item, $menu) {
    if ($($item).hasClass('L1')) {
      $menu.find('a').addClass('hide')
      $menu.find('a[data-type="N"]').removeClass('hide')
      $menu.find('a[data-type="IMAGE"]').removeClass('hide')
    } else {
      $menu.find('a[data-type="IMAGE"]').addClass('hide')
    }
  }

  _saveBefore(o) {
    o.mode3ShowFilters = $val('#mode3ShowFilters')
    o.mode3ShowCharts = $val('#mode3ShowCharts')
    if (this.state.advListShowCategory) {
      o.mode3ShowCategory = []
      this._categoryFields.forEach((refid) => {
        let c = this.refs[refid]
        c && o.mode3ShowCategory.push(c.val())
      })
    } else {
      o.mode3ShowCategory = null
    }
    return o
  }
}

const CompCategory = (_this, name = 'advListShowCategory') => {
  return (
    <div className="aside-item" ref={(c) => (_this._$category = c)}>
      <div className="switch-button switch-button-xs">
        <input type="checkbox" id={name} defaultChecked={wpc.extConfig && wpc.extConfig[name]} />
        <span>
          <label htmlFor={name} />
        </span>
      </div>
      <span className="ml-2 down-5 d-inline-block">{$L('分组')}</span>
      <div className="clearfix"></div>
      <div className={`advListShowCategory-set ${_this.state.advListShowCategory ? '' : 'hide'}`}>
        <div className="row">
          <div className="col-6 pr-0">
            <label className="mb-1">{$L('分组字段')}</label>
          </div>
          <div className="col-5 pr-0">
            <label className="mb-1">{$L('字段格式')}</label>
          </div>
        </div>
        {_this.state.advListShowCategoryFields &&
          _this.state.categoryFields &&
          _this.state.categoryFields.map((item) => {
            const refid = $random('item-')
            _this._categoryFields.push(refid)
            return (
              <RF key={item.key}>
                <CompCategoryItem
                  {...item}
                  fields={_this.state.advListShowCategoryFields}
                  handleRemove={(key2) => {
                    const categoryFields = []
                    _this.state.categoryFields.forEach((item) => {
                      if (key2 !== item.key) categoryFields.push(item)
                    })
                    _this.setState({ categoryFields })
                  }}
                  key2={item.key}
                  ref={refid}
                />
              </RF>
            )
          })}
        <div className="row">
          <div className="col-7">
            <a
              href="###"
              onClick={(e) => {
                $stopEvent(e, true)
                const categoryFields = _this.state.categoryFields || []
                if (categoryFields.length >= 9) {
                  RbHighbar.create($L('最多可添加 9 个'))
                  return false
                }
                categoryFields.push({ key: $random('item-') })
                _this.setState({ categoryFields })
              }}>
              <i className="zmdi zmdi-plus-circle icon" /> {$L('添加')}
            </a>
          </div>
        </div>
      </div>
    </div>
  )
}
const CompCategory_componentDidMount = (_this, name = 'advListShowCategory') => {
  const $el = $(`#${name}`).on('change', function () {
    _this.setState({ advListShowCategory: $val(this) ? true : null })
    // fields
    if (!_this.state.advListShowCategoryFields) {
      $.get(`/commons/metadata/fields?entity=${wpc.entityName}`, (res) => {
        const fs = []
        res.data &&
          res.data.forEach((item) => {
            if (_CATEGORY_TYPES.includes(item.type)) fs.push(item)
          })
        _this.setState({ advListShowCategoryFields: fs })
      })
    }
  })

  // init
  let categoryFields = []
  if (wpc.extConfig && wpc.extConfig[name]) {
    $el.trigger('change')
    // comp:v3.9
    let set = wpc.extConfig[name]
    if (set && typeof set === 'string') {
      wpc.extConfig[name].split(';').forEach((item) => {
        const ff = item.split(':')
        categoryFields.push({ key: $random('item-'), field: ff[0], format: ff[1] })
      })
    } else {
      wpc.extConfig[name].forEach((item) => {
        categoryFields.push({ key: $random('item-'), ...item })
      })
    }
  } else {
    categoryFields.push({ key: $random('item-') })
  }
  _this.setState({ categoryFields })
}

// 分組
class CompCategoryItem extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    const hasFilter = this.state.filter && this.state.filter.items ? this.state.filter.items.length : 0
    return (
      <div className="row item">
        <div className="col-6 pr-0">
          <select className="form-control form-control-sm" ref={(c) => (this._$field = c)}>
            {this.props.fields.map((item) => {
              return (
                <option key={item.name} value={item.name}>
                  {item.label}
                </option>
              )
            })}
          </select>
        </div>
        <div className="col-5 pr-0">
          <select className="form-control form-control-sm" ref={(c) => (this._$format = c)}>
            {this.state.fieldFormats &&
              this.state.fieldFormats.map((item) => {
                return (
                  <option key={item[0]} value={item[0]}>
                    {item[1]}
                  </option>
                )
              })}
          </select>
        </div>
        <div className="col-1 pl-0 pr-0 text-right">
          <button className="btn btn-light w-auto dropdown-toggle" type="button" data-toggle="dropdown" title={$L('更多选项')}>
            <i className="icon zmdi zmdi-more fs-18" />
          </button>
          <div className="dropdown-menu dropdown-menu-sm">
            <a className="dropdown-item" onClick={() => this.props.handleRemove(this.props.key2)}>
              {$L('移除')}
            </a>
            <span className="dropdown-item-text">{$L('排序')}</span>
            <a
              className="dropdown-item"
              data-sort={this.state.sort || 0}
              onClick={(e) => {
                $stopEvent(e, true)
                if (this.state.sort === 1) this.setState({ sort: 2 })
                else if (this.state.sort === 0) this.setState({ sort: 1 })
                else if (this.state.sort === 2) this.setState({ sort: 0 })
                else this.setState({ sort: 1 })
              }}>
              {
                [
                  $L('默认'),
                  <RF key="asc">
                    {$L('正序')} <i className="mdi mdi-sort-alphabetical-ascending" />
                  </RF>,
                  <RF key="desc">
                    {$L('倒序')} <i className="mdi mdi-sort-alphabetical-descending" />
                  </RF>,
                ][this.state.sort || 0]
              }
            </a>
            <span className="dropdown-item-text">{$L('过滤条件')}</span>
            <a className="dropdown-item" onClick={() => this._showAdvFilter()}>
              {hasFilter > 0 ? $L('已设置条件') + ` (${hasFilter})` : $L('无')}
            </a>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const $field = $(this._$field).select2({
      allowClear: false,
    })
    const $format = $(this._$format).select2({
      allowClear: true,
      placeholder: $L('默认'),
      language: {
        noResults: function () {
          return $L('默认')
        },
      },
    })

    $field.on('change', (e) => {
      const s = e.target.value
      const found = this.props.fields.find((x) => x.name === s)

      let formats = []
      let formatInit = this.props.format
      if (found && found.type === 'CLASSIFICATION') {
        formats = [
          [0, $L('%d 级分类', 1)],
          [1, $L('%d 级分类', 2)],
          [2, $L('%d 级分类', 3)],
          [3, $L('%d 级分类', 4)],
        ]
      } else if (found && (found.type === 'DATE' || found.type === 'DATETIME')) {
        formats = [
          ['yyyy', 'YYYY'],
          ['yyyy-MM', 'YYYY-MM'],
          ['yyyy-MM-dd', 'YYYY-MM-DD'],
        ]
      } else if (found && found.type === 'REFERENCE') {
        formats = null
        $.get(`/commons/metadata/fields?entity=${found.ref[0]}`, (res) => {
          formats = []
          res.data &&
            res.data.forEach((item) => {
              if (item.type === 'REFERENCE' && item.ref[0] === found.ref[0]) {
                if (!['createdBy', 'modifiedBy'].includes(item.name)) {
                  formats.push([item.name, item.label])
                }
              }
            })
          this.setState({ fieldFormats: formats, _mode: 2 }, () => {
            $format.val(formatInit || null).trigger('change')
            formatInit = null
          })
        })
      }

      if (formats) {
        this.setState({ fieldFormats: formats, _mode: 1 }, () => {
          $format.val(formatInit || null).trigger('change')
          formatInit = null
        })
      }
    })

    // init
    if (this.props.field) {
      $(this._$field).val(this.props.field)
    }
    $(this._$field).trigger('change')
  }

  _showAdvFilter() {
    if (this._AdvFilter) {
      this._AdvFilter.show()
    } else {
      const that = this
      renderRbcomp(<AdvFilter entity={wpc.entityName} filter={that.state.filter} onConfirm={(filter) => that.setState({ filter })} canNoFilters inModal title={$L('过滤条件')} />, function () {
        that._AdvFilter = this
      })
    }
  }

  val() {
    return {
      field: $(this._$field).val(),
      format: $(this._$format).val() || null,
      sort: this.state.sort || 0,
      filter: this.state.filter || null,
    }
  }
}
