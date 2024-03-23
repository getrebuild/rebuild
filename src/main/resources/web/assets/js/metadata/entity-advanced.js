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
      call: function () {
        $countdownButton($(this._dlg).find('.btn-danger'))
      },
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

const CAT_TYPES = ['PICKLIST', 'MULTISELECT', 'CLASSIFICATION', 'DATE', 'DATETIME', 'REFERENCE', 'N2NREFERENCE']
// 模式选项
class DlgMode1Option extends RbFormHandler {
  render() {
    return (
      <RbModal title={$L('标准模式选项')} ref={(c) => (this._dlg = c)}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('在侧栏显示')}</label>
            <div className="col-sm-9">
              <div>
                <div className="switch-button switch-button-xs">
                  <input type="checkbox" id="advListHideFilters" defaultChecked={wpc.extConfig && !wpc.extConfig.advListHideFilters} />
                  <span>
                    <label htmlFor="advListHideFilters" />
                  </span>
                </div>
                <span className="ml-2 down-5 d-inline-block">{$L('常用查询')}</span>
              </div>
              <div className="mt-2">
                <div className="switch-button switch-button-xs">
                  <input type="checkbox" id="advListShowCategory" defaultChecked={wpc.extConfig && wpc.extConfig.advListShowCategory} />
                  <span>
                    <label htmlFor="advListShowCategory" />
                  </span>
                </div>
                <span className="ml-2 down-5 d-inline-block">{$L('分组')}</span>
                <div className="clearfix"></div>
                <div className={`advListShowCategory-set ${this.state.advListShowCategory ? '' : 'hide'}`}>
                  <div className="row">
                    <div className="col-8">
                      <label className="mb-1">{$L('分组字段')}</label>
                      <select className="form-control form-control-sm">
                        {this.state.advListShowCategoryFields &&
                          this.state.advListShowCategoryFields.map((item) => {
                            return (
                              <option key={item.name} value={item.name}>
                                {item.label}
                              </option>
                            )
                          })}
                      </select>
                    </div>
                    <div className={`col-4 pl-0 ${this.state.advListShowCategoryFormats ? '' : 'hide'}`}>
                      <label className="mb-1">{$L('字段格式')}</label>
                      <select className="form-control form-control-sm">
                        {this.state.advListShowCategoryFormats &&
                          this.state.advListShowCategoryFormats.map((item) => {
                            return (
                              <option key={item[0]} value={item[0]}>
                                {item[1]}
                              </option>
                            )
                          })}
                      </select>
                    </div>
                  </div>
                </div>
              </div>
              <div className="mt-2">
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
            <label className="col-sm-3 col-form-label text-sm-right">{$L('在顶部显示')}</label>
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
    let $catFields, $catFormats
    const that = this
    $('#advListShowCategory').on('change', function () {
      if ($val(this)) {
        that.setState({ advListShowCategory: true })
      } else {
        that.setState({ advListShowCategory: null })
      }

      if (!$catFields) {
        $catFields = $('.advListShowCategory-set select:eq(0)')
        $catFormats = $('.advListShowCategory-set select:eq(1)')

        $.get(`/commons/metadata/fields?entity=${wpc.entityName}`, (res) => {
          const _data = []
          res.data &&
            res.data.forEach((item) => {
              if (CAT_TYPES.includes(item.type)) {
                _data.push(item)
              }
            })

          // FIELD:[FORMAT]
          let set = wpc.extConfig && wpc.extConfig.advListShowCategory ? wpc.extConfig.advListShowCategory : null
          if (set) set = set.split(':')

          that.setState({ advListShowCategoryFields: _data }, () => {
            $catFields
              .select2({
                placeholder: $L('选择分类字段'),
                allowClear: false,
              })
              .on('change', () => {
                const s = $catFields.val()
                const found = _data.find((x) => x.name === s)

                let formats
                if (found && found.type === 'CLASSIFICATION') {
                  formats = [
                    [0, $L('%d 级分类', 1)],
                    [1, $L('%d 级分类', 2)],
                    [2, $L('%d 级分类', 3)],
                    [3, $L('%d 级分类', 4)],
                  ]
                  formats = null // FIXME 无法区分几级
                } else if (found && (found.type === 'DATE' || found.type === 'DATETIME')) {
                  formats = [
                    ['yyyy', 'YYYY'],
                    ['yyyy-MM', 'YYYY-MM'],
                    ['yyyy-MM-dd', 'YYYY-MM-DD'],
                  ]
                }

                that.setState({ advListShowCategoryFormats: formats }, () => {
                  $catFormats.val(null).trigger('change')
                })
              })

            $catFormats.select2({ placeholder: $L('默认') })

            if (set) {
              $catFields.val(set[0]).trigger('change')
              setTimeout(() => {
                if (set[1]) $catFormats.val(set[1]).trigger('change')
              }, 200)
            } else {
              $catFields.trigger('change')
            }
          })
        })
      }
    })

    if (wpc.extConfig && wpc.extConfig.advListShowCategory) {
      $('#advListShowCategory').trigger('change')
    }
  }

  save = () => {
    const o = {
      advListHideFilters: !$val('#advListHideFilters'),
      advListHideCharts: !$val('#advListHideCharts'),
      advListFilterPane: $val('#advListFilterPane'),
      advListFilterTabs: $val('#advListFilterTabs'),
    }

    if (this.state.advListShowCategory) {
      o.advListShowCategory = `${$val('.advListShowCategory-set select:eq(0)')}:${$val('.advListShowCategory-set select:eq(1)') || ''}`
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
            <div className="col-sm-9">
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
  }

  render() {
    return (
      <RbModal title={$L('卡片模式选项')} ref={(c) => (this._dlg = c)}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('在侧栏显示')}</label>
            <div className="col-sm-9">
              <div>
                <div className="switch-button switch-button-xs">
                  <input type="checkbox" id="mode3ShowFilters" defaultChecked={wpc.extConfig && wpc.extConfig.mode3ShowFilters} />
                  <span>
                    <label htmlFor="mode3ShowFilters" />
                  </span>
                </div>
                <span className="ml-2 down-5 d-inline-block">{$L('常用查询')}</span>
              </div>
            </div>
          </div>

          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('显示字段')}</label>
            <div className="col-sm-9">
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
    return o
  }
}
