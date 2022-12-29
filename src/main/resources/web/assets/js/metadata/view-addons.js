/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const _configLabels = {}
const _configFilters = {}

$(document).ready(function () {
  const entity = $urlp('entity'),
    type = $urlp('type')
  const url = `/admin/entity/${entity}/view-addons?type=${type}`

  $.get(url, function (res) {
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
        }
        $(`.unset-list li[data-key="${key}"]`).trigger('click')
      })

      refreshConfigStar()

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
      config.push([key, _configLabels[key] || null, _configFilters[key] || null])
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
})

const refreshConfigStar = function () {
  $('.dd-list.J_config .dd-item').each(function () {
    const key = $(this).data('key')
    if (_configLabels[key] || _configFilters[key]) {
      $(this).addClass('star')
    } else {
      $(this).removeClass('star')
    }
  })
}

const ShowStyles_Comps = {}

// 不支持条件
const _NO_FILTERS = ['ProjectTask', '', 'Feeds', 'Attachment']

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

      renderRbcomp(
        <ShowStyles2
          label={_configLabels[key]}
          onConfirm={(s) => {
            _configLabels[key] = s.label
            refreshConfigStar()
          }}
          filter={_configFilters[key]}
          filterShow={$urlp('type') === 'TAB' && !_NO_FILTERS.includes(entity)}
          filterEntity={entity}
          filterConfirm={(s) => {
            _configFilters[key] = s
            refreshConfigStar()
          }}
        />,
        null,
        function () {
          ShowStyles_Comps[key] = this
        }
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
    const fsl = this.state.filter && this.state.filter.items ? this.state.filter.items.length : 0
    return (
      this.props.filterShow && (
        <div className="form-group row pt-1">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('附加过滤条件')}</label>
          <div className="col-sm-7">
            <a className="btn btn-sm btn-link pl-0 text-left down-2" onClick={() => this.showFilter()}>
              {fsl > 0 ? `${$L('已设置条件')} (${fsl})` : $L('点击设置')}
            </a>
            <p className="form-text mb-0 mt-0">{$L('符合过滤条件的数据才会在相关项列表中显示')}</p>
          </div>
        </div>
      )
    )
  }

  showFilter() {
    parent._showFilterForAddons &&
      parent._showFilterForAddons({
        entity: this.props.filterEntity,
        filter: this.state.filter,
        onConfirm: (s) => {
          if (s.items.length === 0) s = null // No items
          this.props.filterConfirm(s)
          this.setState({ filter: s })
        },
      })
  }
}
