/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const _configLabels = {}
const _configFilters = {}
const _configLayouts = {}

$(document).ready(() => {
  const entity = $urlp('entity'),
    type = $urlp('type')
  const url = `/admin/entity/${entity}/view-addons?type=${type}`

  $.get(url, (res) => {
    $(res.data.refs).each(function () {
      // eslint-disable-next-line no-undef
      render_unset(this)
    })

    if (res.data.config) {
      $(res.data.config.items).each(function () {
        let key = this
        // compatible: v2.8
        if (typeof this !== 'string') {
          key = this[0]
          _configLabels[key] = this[1] // label
          _configFilters[key] = this[2] // filter
          _configLayouts[key] = this[3] // layout
        }
        $(`.unset-list li[data-key="${key}"]`).trigger('click')
      })

      _refreshConfigStar()

      $('#relatedAutoExpand').attr('checked', res.data.config.autoExpand === true)
      $('#relatedAutoHide').attr('checked', res.data.config.autoHide === true)
      $('#relatedDefaultList').attr('checked', res.data.config.defaultList === true)
    }

    if (!res.data.refs || res.data.refs.length === 0) {
      $(`<li class="dd-item nodata">${$L('暂无数据')}</li>`).appendTo('.unset-list')
    }
  })

  const $btn = $('.J_save').on('click', () => {
    let config = []
    $('.J_config>li').each(function () {
      const key = $(this).data('key')
      config.push([key, _configLabels[key] || null, _configFilters[key] || null, _configLayouts[key] || null])
    })

    config = {
      items: config,
      autoExpand: $val('#relatedAutoExpand'),
      autoHide: $val('#relatedAutoHide'),
      defaultList: $val('#relatedDefaultList'),
    }

    $btn.button('loading')
    $.post(url, JSON.stringify(config), (res) => {
      $btn.button('reset')
      if (res.error_code === 0) parent.location.reload()
      else RbHighbar.error(res.error_msg)
    })
  })

  $unhideDropdown($('.J_extoption'))
})

const _refreshConfigStar = function () {
  $('.dd-list.J_config .dd-item').each(function () {
    const key = $(this).data('key')
    if (_configLabels[key] || _configFilters[key] || _configLayouts[key]) {
      $(this).addClass('star')
    } else {
      $(this).removeClass('star')
    }
  })
}

const ShowStyles_Comps = {}

// eslint-disable-next-line no-undef
render_item_after = function ($item) {
  const key = $item.data('key')
  const $a = $(`<a class="mr-1" title="${$L('显示样式')}"><i class="zmdi zmdi-edit"></i></a>`)
  $item.find('.dd3-action>a').before($a)

  $a.on('click', () => {
    if (ShowStyles_Comps[key]) {
      ShowStyles_Comps[key].show()
    } else {
      const entity = key.split('.')[0]
      const isFilter = ['ProjectTask', '', 'Feeds', 'Attachment'].includes(entity) // 内置的不支持

      renderRbcomp(
        <ShowStyles2
          label={_configLabels[key]}
          filter={_configFilters[key]}
          layout={_configLayouts[key]}
          onConfirm={(s) => {
            _configLabels[key] = s.label
            _configFilters[key] = s.filter
            _configLayouts[key] = s.layout
            _refreshConfigStar()
          }}
          entity={entity}
          filterShow={$urlp('type') === 'TAB' && !isFilter}
          listLayoutShow={$urlp('type') === 'TAB' && !isFilter}
          formLayoutShow={$urlp('type') === 'ADD' && !isFilter}
        />,
        null,
        function () {
          ShowStyles_Comps[key] = this
        },
      )
    }
  })
}

// eslint-disable-next-line no-undef
class ShowStyles2 extends ShowStyles {
  constructor(props) {
    super(props)
    this.state = { filter: props.filter }
  }

  renderExtras() {
    return (
      <RF>
        {this.props.formLayoutShow && this.renderExtrasFormLayout()}
        {this.props.listLayoutShow && this.renderExtrasListLayout()}
        {this.props.filterShow && this.renderExtrasFilter()}
      </RF>
    )
  }

  renderExtrasFilter() {
    let filterText = parent && parent.AdvFilter ? parent.AdvFilter.renderFilterText(this.state.filter) : null
    return (
      <div className="form-group row pt-1">
        <label className="col-sm-3 col-form-label text-sm-right">{$L('附加过滤条件')}</label>
        <div className="col-sm-7">
          <a
            className="btn btn-sm btn-link pl-0 text-left down-2"
            onClick={() => {
              parent._showFilterForAddons &&
                parent._showFilterForAddons({
                  entity: this.props.entity,
                  filter: this.state.filter,
                  onConfirm: (s) => {
                    if (s.items.length === 0) s = null // No items
                    this.setState({ filter: s })
                  },
                })
            }}>
            {filterText || $L('点击设置')}
          </a>
          <p className="form-text mb-0 mt-0">{$L('符合过滤条件的数据才会在相关项列表中显示')}</p>
        </div>
      </div>
    )
  }

  renderExtrasFormLayout() {
    return (
      <div className="form-group row pt-1">
        <label className="col-sm-3 col-form-label text-sm-right">{$L('指定布局')}</label>
        <div className="col-sm-7">
          <select className="form-control form-control-sm" ref={(c) => (this._$formLayout = c)}>
            <option value="">{$L('自动')}</option>
          </select>
        </div>
      </div>
    )
  }

  renderExtrasListLayout() {
    return (
      <div className="form-group row pt-1">
        <label className="col-sm-3 col-form-label text-sm-right">{$L('指定列显示')}</label>
        <div className="col-sm-7">
          <select className="form-control form-control-sm" ref={(c) => (this._$listLayout = c)}>
            <option value="">{$L('自动')}</option>
          </select>
        </div>
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount && super.componentDidMount()

    if (this._$formLayout) {
      $.get(`/admin/entity/${this.props.entity}/get-forms-attr`, (res) => {
        res.data &&
          res.data.forEach((item) => {
            $(`<option value="${item.id}">${item.name}</option>`).appendTo(this._$formLayout)
          })
        this.props.layout && $(this._$formLayout).val(this.props.layout)
      })
    }

    if (this._$listLayout) {
      $.get(`/app/${this.props.entity}/list-fields/alist`, (res) => {
        res.data &&
          res.data.forEach((item) => {
            $(`<option value="${item[0]}">${item[1] || $L('未命名')}</option>`).appendTo(this._$listLayout)
          })
        this.props.layout && $(this._$listLayout).val(this.props.layout)
      })
    }
  }

  saveProps() {
    const data = {
      label: $(this._$label).val() || '',
      filter: this.state.filter || null,
      layout: $(this._$formLayout).val() || $(this._$listLayout).val() || null,
    }
    typeof this.props.onConfirm === 'function' && this.props.onConfirm(data)
    this.hide()
  }
}
