/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable react/no-string-refs */

const wpc = window.__PageConfig

$(document).ready(function () {
  modeAction()

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
          }
        })
      },
      call: function () {
        $countdownButton($(this._dlg).find('.btn-danger'))
      },
    })
  })
})

function modeAction() {
  if (rb.commercial < 10) {
    $('.mode-select .btn').on('click', () => {
      RbHighbar.error(WrapHtml($L('免费版不支持列表模式功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
    })
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

  // Mode-1 Option
  $('.mode-select .J_mode1-option').on('click', () => renderRbcomp(<DlgMode1Option />))
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

// const CATE_TYPES = ['PICKLIST', 'MULTISELECT', 'CLASSIFICATION', 'DATE', 'DATETIME', 'REFERENCE', 'N2NREFERENCE']
const CATE_TYPES = ['PICKLIST', 'MULTISELECT', 'CLASSIFICATION', 'REFERENCE', 'N2NREFERENCE']

// 模式选项
class DlgMode1Option extends RbFormHandler {
  render() {
    return (
      <RbModal title={$L('标准模式选项')} ref="dlg" disposeOnHide>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('显示侧栏“常用查询”')}</label>
            <div className="col-sm-9">
              <div className="switch-button switch-button-xs">
                <input type="checkbox" id="advListHideFilters" defaultChecked={wpc.extConfig && !wpc.extConfig.advListHideFilters} />
                <span>
                  <label htmlFor="advListHideFilters" />
                </span>
              </div>
            </div>
          </div>
          <div className="form-group row bosskey-show">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('显示侧栏“分组”')}</label>
            <div className="col-sm-9">
              <div className="switch-button switch-button-xs">
                <input type="checkbox" id="advListShowCategory" defaultChecked={wpc.extConfig && wpc.extConfig.advListShowCategory} />
                <span>
                  <label htmlFor="advListShowCategory" />
                </span>
              </div>
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
                    <select className="form-control form-control-sm" disabled>
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
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('显示侧栏“图表”')}</label>
            <div className="col-sm-9">
              <div className="switch-button switch-button-xs">
                <input type="checkbox" id="advListHideCharts" defaultChecked={wpc.extConfig && !wpc.extConfig.advListHideCharts} />
                <span>
                  <label htmlFor="advListHideCharts" />
                </span>
              </div>
            </div>
          </div>
          <div className="form-group row bosskey-show">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('显示顶部查询面板')}</label>
            <div className="col-sm-9">
              <div className="switch-button switch-button-xs">
                <input type="checkbox" id="advListFilterPane" defaultChecked={wpc.extConfig && wpc.extConfig.advListFilterPane} />
                <span>
                  <label htmlFor="advListFilterPane" />
                </span>
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

        $.get(`/commons/metadata/fields?entity=${wpc.entityName}&deep=2`, (res) => {
          const _data = []
          res.data &&
            res.data.forEach((item) => {
              if (CATE_TYPES.includes(item.type) && !(item.name.includes('owningDept.') || item.name.includes('owningUser.'))) {
                _data.push(item)
              }
            })

          // FIELD:[FORMAT]
          let set = wpc.extConfig && wpc.extConfig.advListShowCategory ? wpc.extConfig.advListShowCategory : null
          if (set) set = set.split(':')

          wpc.extConfig.advListShowCategory
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
    }

    if (this.state.advListShowCategory) {
      o.advListShowCategory = `${$val('.advListShowCategory-set select:eq(0)')}:${$val('.advListShowCategory-set select:eq(1)') || ''}`
    }

    this.disabled(true)
    modeSave(o, () => {
      this.hide()
      location.reload()
    })
  }
}
