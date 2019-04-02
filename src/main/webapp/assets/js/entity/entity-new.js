$(document).ready(function () {
  let sbtn = $('.btn-primary').click(function () {
    let entityLabel = $val('#entityLabel'),
      comments = $val('#comments')
    if (!entityLabel) {
      rb.highbar('请输入实体名称')
      return
    }
    let _data = {
      label: entityLabel,
      comments: comments
    }
    if ($val('#isSlave') === 'true') {
      _data.masterEntity = $val('#masterEntity')
      if (!_data.masterEntity) {
        rb.highbar('请选择选择主实体')
        return
      }
    }
    _data = JSON.stringify(_data)

    sbtn.button('loading')
    $.post(rb.baseUrl + '/admin/entity/entity-new?nameField=' + $val('#nameField'), _data, function (res) {
      if (res.error_code === 0) parent.location.href = rb.baseUrl + '/admin/entity/' + res.data + '/base'
      else rb.hberror(res.error_msg)
      sbtn.button('reset')
    })
  })

  let entitiesLoaded = false
  $('#isSlave').click(function () {
    $('.J_masterEntity').toggleClass('hide')
    parent.rb.modalResize()
    if (entitiesLoaded === false) {
      entitiesLoaded = true
      $.get(rb.baseUrl + '/commons/metadata/entities', function (res) {
        $(res.data).each(function () {
          $('<option value="' + this.name + '">' + this.label + '</option>').appendTo('#masterEntity')
        })
      })
    }
  })
})