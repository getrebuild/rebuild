/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 列显示

const _configLabels = {}
const _configWidths = {}
let _configSorts = {}

$(document).ready(() => {
  const entity = $urlp('entity')
  const settingsUrl = `/app/${entity}/list-fields`

  // Hide if bizz
  if (['User', 'Department', 'Role', 'Team'].includes(entity)) $('#shareTo').addClass('hide')

  let overwriteMode = false
  let shareToComp
  let cfgid = $urlp('id')

  $.get(`${settingsUrl}?id=${cfgid || ''}`, (res) => {
    const _data = res.data || {}
    cfgid = _data.configId || ''

    $(_data.fieldList).each(function () {
      // eslint-disable-next-line no-undef
      if (!$isSysMask(this.label)) render_unset([this.field, this.label])
    })
    $(_data.configList).each(function () {
      const fkey = this.field
      $(`.unset-list li[data-key="${fkey}"]`).trigger('click')

      _configLabels[fkey] = this.label2
      _configWidths[fkey] = this.width
      _configSorts[fkey] = this.sort
    })

    refreshConfigStar()

    // 覆盖自有配置
    if (res.data) {
      overwriteMode = !rb.isAdminUser && res.data.shareTo !== 'SELF'
    }

    $.get(`${settingsUrl}/alist`, (res) => {
      const ccfg = res.data.find((x) => x[0] === cfgid)
      if (rb.isAdminUser) {
        renderRbcomp(<Share2 title={$L('列显示')} list={res.data} configName={ccfg ? ccfg[1] : ''} shareTo={_data.shareTo} entity={entity} id={_data.configId} />, 'shareTo', function () {
          shareToComp = this
        })
      } else {
        const data = res.data.map((x) => {
          x[4] = entity
          return x
        })
        // eslint-disable-next-line no-undef
        renderSwitchButton(data, $L('列显示'), cfgid)
      }

      // 有自有才提示覆盖
      if (overwriteMode) {
        const haveSelf = res.data.find((x) => x[2] === 'SELF')
        overwriteMode = !!haveSelf
      }
    })
  })

  const $btn = $('.J_save').on('click', () => {
    const config = []
    $('.J_config>li').each(function () {
      const field = $(this).data('key')
      config.push({
        field: field,
        label2: _configLabels[field] || null,
        width: _configWidths[field] || null,
        sort: _configSorts[field] || null,
      })
    })
    if (config.length === 0) return RbHighbar.create($L('请至少选择 1 个显示列'))

    const saveFn = function () {
      $btn.button('loading')
      const shareToData = shareToComp ? shareToComp.getData() : { shareTo: 'SELF' }
      const url = `${settingsUrl}?id=${cfgid}&configName=${shareToData.configName || ''}&shareTo=${shareToData.shareTo || ''}`

      $.post(url, JSON.stringify(config), (res) => {
        if (res.error_code === 0) parent.location.reload()
        $btn.button('reset')
      })
    }

    if (overwriteMode) {
      RbAlert.create($L('保存将覆盖你现有的列显示。继续吗？'), {
        confirm: function () {
          this.hide()
          saveFn()
        },
      })
    } else {
      saveFn()
    }
  })
})

const refreshConfigStar = function () {
  $('.dd-list.J_config .dd-item').each(function () {
    const fkey = $(this).data('key')
    if (_configLabels[fkey] || _configWidths[fkey] || _configSorts[fkey]) {
      $(this).addClass('star')
    } else {
      $(this).removeClass('star')
    }
  })
}

const ShowStyles_Comps = {}

// eslint-disable-next-line no-undef
render_item_after = function ($item) {
  const fkey = $item.data('key')
  const $a = $(`<a class="mr-1" title="${$L('显示样式')}"><i class="zmdi zmdi-edit"></i></a>`)
  $item.find('.dd3-action>a').before($a)

  $a.on('click', () => {
    if (ShowStyles_Comps[fkey]) {
      ShowStyles_Comps[fkey].show()
    } else {
      renderRbcomp(
        <ShowStyles2
          label={_configLabels[fkey]}
          width={_configWidths[fkey]}
          sort={_configSorts[fkey]}
          onConfirm={(s) => {
            _configLabels[fkey] = s.label
            _configWidths[fkey] = s.width
            if (s.sort) {
              _configSorts = {}
              _configSorts[fkey] = s.sort
            }
            refreshConfigStar()
          }}
        />,
        null,
        function () {
          ShowStyles_Comps[fkey] = this
        }
      )
    }
  })
}

// eslint-disable-next-line no-undef
class ShowStyles2 extends ShowStyles {
  constructor(props) {
    super(props)
    this.state = { sortActive: props.sort }
  }

  renderExtras() {
    return (
      <RF>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('默认宽度')}</label>
          <div className="col-sm-7" ref={(c) => (this._$width = c)}>
            <input className="bslider form-control form-control-sm" type="text" data-slider-min="0" data-slider-max="500" data-slider-step="10" />
            <div className="form-text mt-0">{$L('默认')}</div>
          </div>
        </div>
        <div className="form-group row pt-1 pb-1">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('默认排序')}</label>
          <div className="col-sm-7" ref={(c) => (this._$sort = c)}>
            <a
              className="btn btn-link btn-sm pl-0 text-left mt-1 up-1"
              onClick={() => {
                if (this.state.sortActive === 'asc') this.setState({ sortActive: 'desc' })
                else if (this.state.sortActive === 'desc') this.setState({ sortActive: null })
                else this.setState({ sortActive: 'asc' })
              }}>
              <i className={`mdi mdi-sort-alphabetical-ascending ${this.state.sortActive || ''}`} />
            </a>
          </div>
        </div>
      </RF>
    )
  }

  componentDidMount() {
    super.componentDidMount()

    const $w = $(this._$width)
    const $tip = $w.find('.form-text')
    const that = this
    $w.find('input')
      .slider({ value: this.props.width || 0 })
      .on('change', function (e) {
        let v = e.value.newValue
        if (v > 0 && v < 30) v = 30
        $setTimeout(
          () => {
            if (v === 0) $tip.html($L('默认'))
            else $tip.html(`${$L('宽度')} <b>${v}</b>`)
            that._width = v
          },
          200,
          'bslider-change'
        )
      })

    if (this.props.width) {
      $tip.html(`${$L('宽度')} <b>${this.props.width}</b>`)
    }
  }

  saveProps() {
    const data = {
      label: $(this._$label).val() || '',
      width: this._width || 0,
      sort: this.state.sortActive || null,
    }
    typeof this.props.onConfirm === 'function' && this.props.onConfirm(data)
    this.hide()
  }
}
