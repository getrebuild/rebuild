/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const _configLabels = {}

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
          _configLabels[key] = this[1]
        }
        $(`.unset-list li[data-key="${key}"]`).trigger('click')
      })
      $('#relatedAutoExpand').attr('checked', res.data.config.autoExpand === true)
      $('#relatedAutoHide').attr('checked', res.data.config.autoHide === true)
    }

    if (!res.data.refs || res.data.refs.length === 0) {
      $(`<li class="dd-item nodata">${$L('暂无数据')}</li>`).appendTo('.unset-list')
    }
  })

  const $btn = $('.J_save').click(function () {
    let config = []
    $('.J_config>li').each(function () {
      const $this = $(this)
      config.push([$this.data('key'), $this.attr('data-label') || ''])
    })
    config = {
      items: config,
      autoExpand: $val('#relatedAutoExpand'),
      autoHide: $val('#relatedAutoHide'),
    }

    $btn.button('loading')
    $.post(url, JSON.stringify(config), function (res) {
      $btn.button('reset')
      if (res.error_code === 0) parent.location.reload()
      else RbHighbar.error(res.error_msg)
    })
  })
})

const ShowStyles_Comps = {}
// eslint-disable-next-line no-undef
render_item_after = function ($item) {
  const key = $item.data('key')
  const $a = $(`<a class="mr-1" title="${$L('显示样式')}"><i class="zmdi zmdi-edit"></i></a>`)
  $item.find('.dd3-action>a').before($a)

  $a.on('click', function () {
    if (ShowStyles_Comps[key]) {
      ShowStyles_Comps[key].show()
    } else {
      renderRbcomp(
        // eslint-disable-next-line react/jsx-no-undef
        <ShowStyles
          label={_configLabels[key]}
          onConfirm={(s) => {
            $item.attr({
              'data-label': s.label || '',
            })
            _configLabels[key] = s.label
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
